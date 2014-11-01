/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.text.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.gnur.*;

/**
 * Deparsing R objects.
 *
 * There are two distinct clients of this class:
 * <ul>
 * <li>{@code RSerialize} when it needs to convert an unserialized GnuR {@code pairlist} instance
 * that denotes a closure into an {@link RFunction} which is, currently, done by deparsing and
 * reparsing the value.</li>
 * <li>The {@code deparse} builtin.</li>
 * </ul>
 *
 * Much of the code here is related to case 1, which would be unnecessary if unserialize created
 * ASTs for language elements directly rather than via deparse/parse. The deparsing of ASTs is
 * handled in {@code RASTDeparse} via the {@link RASTHelper} interface.
 */
public class RDeparse {
    public static final int KEEPINTEGER = 1;
    public static final int QUOTEEXPRESSIONS = 2;
    public static final int SHOWATTRIBUTES = 4;
    public static final int USESOURCE = 8;
    public static final int WARNINCOMPLETE = 16;
    public static final int DELAYPROMISES = 32;
    public static final int KEEPNA = 64;
    public static final int S_COMPAT = 128;
    /* common combinations of the above */
    public static final int SIMPLEDEPARSE = 0;
    public static final int DEFAULTDEPARSE = 65; /* KEEPINTEGER | KEEPNA, used for calls */

    public static final int MIN_Cutoff = 20;
    public static final int MAX_Cutoff = 500;
    public static final int DEFAULT_Cutoff = 60;

    public static enum PP {
        FUNCALL,
        RETURN,
        BINARY,
        BINARY2,
        UNARY,
        IF,
        WHILE,
        FOR,
        BREAK,
        NEXT,
        REPEAT,
        FUNCTION,
        ASSIGN,
        CURLY,
        PAREN,
        SUBSET,
        DOLLAR;
    }

    public static final int PREC_SUM = 9;
    public static final int PREC_SIGN = 13;

    public static class PPInfo {
        public final PP kind;
        public final int prec;
        public final boolean rightassoc;

        public PPInfo(PP kind, int prec, boolean rightassoc) {
            this.kind = kind;
            this.prec = prec;
            this.rightassoc = rightassoc;
        }

    }

    public static class Func {
        public final String op;
        public final PPInfo info;

        Func(String op, PPInfo info) {
            this.op = op;
            this.info = info;
        }

    }

    // TODO COMPLETE THIS!
    // @formatter:off
    private static final Func[] FUNCTAB = new Func[]{
        new Func("+", new PPInfo(PP.BINARY, PREC_SUM, false)),
        new Func("-", new PPInfo(PP.BINARY, PREC_SUM, false)),
        new Func("*", new PPInfo(PP.BINARY, 10, false)),
        new Func("/", new PPInfo(PP.BINARY, 10, false)),
        new Func("^", new PPInfo(PP.BINARY2, 14, false)),
        new Func("%%", new PPInfo(PP.BINARY2, 11, false)),
        new Func("%/%", new PPInfo(PP.BINARY2, 11, false)),
        new Func("%*%", new PPInfo(PP.BINARY2, 11, false)),
        new Func("==", new PPInfo(PP.BINARY, 8, false)),
        new Func("!=", new PPInfo(PP.BINARY, 8, false)),
        new Func("<", new PPInfo(PP.BINARY, 8, false)),
        new Func("<=", new PPInfo(PP.BINARY, 8, false)),
        new Func(">=", new PPInfo(PP.BINARY, 8, false)),
        new Func(">", new PPInfo(PP.BINARY, 8, false)),
        new Func("&", new PPInfo(PP.BINARY, 6, false)),
        new Func("|", new PPInfo(PP.BINARY, 5, false)),
        new Func("!", new PPInfo(PP.BINARY, 7, false)),
        new Func("&&", new PPInfo(PP.BINARY, 6, false)),
        new Func("||", new PPInfo(PP.BINARY, 5, false)),
        new Func(":", new PPInfo(PP.BINARY2, 12, false)),

        new Func("if", new PPInfo(PP.IF, 0, true)),
        new Func("while", new PPInfo(PP.WHILE, 0, false)),
        new Func("for", new PPInfo(PP.FOR, 0, false)),
        new Func("repeat", new PPInfo(PP.REPEAT, 0, false)),
        new Func("break", new PPInfo(PP.BREAK, 0, false)),
        new Func("next", new PPInfo(PP.NEXT, 0, false)),
        new Func("return", new PPInfo(PP.RETURN, 0, false)),
        new Func("function", new PPInfo(PP.FUNCTION, 0, false)),
        new Func("{", new PPInfo(PP.CURLY, 0, false)),
        new Func("(", new PPInfo(PP.PAREN, 0, false)),
        new Func("<-", new PPInfo(PP.ASSIGN, 1, true)),
        new Func("[", new PPInfo(PP.SUBSET, 17, false)),
        new Func("[[", new PPInfo(PP.SUBSET, 17, false)),
        new Func("$", new PPInfo(PP.DOLLAR, 15, false)),


    };
    // @formatter:on

    public static final PPInfo BUILTIN = new PPInfo(PP.FUNCALL, 0, false);

    public static Func getFunc(String op) {
        for (Func func : FUNCTAB) {
            if (func.op.equals(op)) {
                return func;
            }
        }
        return null;
    }

    public static PPInfo ppInfo(String op) {
        Func func = getFunc(op);
        if (func == null) {
            // must be a builtin that we don't have in FUNCTAB
            return BUILTIN;
        } else {
            return func.info;
        }
    }

    public static class State {
        private final StringBuilder sb = new StringBuilder();
        private final ArrayList<String> lines;
        private int linenumber;
        private int len;
        private int incurly;
        private int inlist;
        private boolean startline;
        private int indent;
        private final int cutoff;
        private final boolean backtick;
        @SuppressWarnings("unused") private int opts;
        @SuppressWarnings("unused") private int sourceable;
        @SuppressWarnings("unused") private int longstring;
        private final int maxlines;
        private boolean active = true;
        @SuppressWarnings("unused") private int isS4;

        private State(int widthCutOff, boolean backtick, int maxlines, boolean needVector) {
            this.cutoff = widthCutOff;
            this.backtick = backtick;
            this.maxlines = maxlines == -1 ? Integer.MAX_VALUE : maxlines;
            lines = needVector ? new ArrayList<>() : null;
        }

        private void preAppend() {
            if (startline) {
                startline = false;
                indent();
            }
        }

        public void indent() {
            for (int i = 1; i <= indent; i++) {
                if (i <= 4) {
                    append("    ");
                } else {
                    append("  ");
                }
            }
        }

        public void incIndent() {
            indent++;
        }

        public void decIndent() {
            indent--;
        }

        public void append(String s) {
            preAppend();
            sb.append(s);
            len += s.length();
        }

        public void append(char ch) {
            preAppend();
            sb.append(ch);
            len++;
        }

        private boolean linebreak(boolean lbreak) {
            boolean result = lbreak;
            if (len > cutoff) {
                if (!lbreak) {
                    result = true;
                    indent++;
                }
                writeline();
            }
            return result;
        }

        public void writeline() {
            if (lines == null) {
                // nl for debugging really, we don't care about format,
                // although line length could be an issues also.
                sb.append('\n');
            } else {
                lines.add(sb.toString());
                sb.delete(0, Integer.MAX_VALUE);
            }
            linenumber++;
            if (linenumber >= maxlines) {
                active = false;
            }
            /* reset */
            len = 0;
            startline = true;
        }
    }

    /**
     * Version for use by {@code RSerialize}.
     */
    @SlowPath
    public static String deparse(RPairList pl) {
        State state = new State(80, false, -1, false);
        return deparse2buff(state, pl).sb.toString();
    }

    /**
     * Version for {@code deparse}.
     */
    @SlowPath
    public static String[] deparse(Object expr, int widthCutoff, boolean backtick, int nlines) {
        State state = new State(widthCutoff, backtick, nlines, true);
        deparse2buff(state, expr);
        state.writeline();
        String[] data = new String[state.lines.size()];
        state.lines.toArray(data);
        return data;
    }

    @SuppressWarnings("unused")
    @SlowPath
    public static State deparse2buff(State state, Object obj) {
        boolean lbreak = false;
        if (!state.active) {
            return state;
        }

        SEXPTYPE type = typeof(obj);
        switch (type) {
            case NILSXP:
                state.append("NULL");
                break;

            case SYMSXP:
                // TODO backtick
                state.append(((RSymbol) obj).getName());
                break;

            case CHARSXP:
                state.append((String) obj);
                break;

            case PROMSXP:
                RPromise promise = (RPromise) obj;
                if (promise.isEvaluated()) {
                    deparse2buff(state, promise.getValue());
                } else {
                    Object v = RContext.getEngine().evalPromise((RPromise) obj, (SourceSection) null);
                    deparse2buff(state, v);
                }
                break;

            case CLOSXP: {
                RPairList f = (RPairList) obj;
                state.append("function (");
                if (f.car() instanceof RPairList) {
                    args2buff(state, (RPairList) f.car(), false, true);
                }
                state.append(") ");
                state.writeline();
                deparse2buff(state, f.cdr());
                break;
            }

            case FUNSXP: {
                RFunction f = (RFunction) obj;
                if (f.isBuiltin()) {
                    state.append(".Primitive(\\\"");
                    state.append(f.getName());
                    state.append("\\\")");
                } else {
                    RContext.getRASTHelper().deparse(state, f);
                }
                break;
            }

            case ENVSXP:
            case VECSXP:
                assert false;
                break;
            case EXPRSXP:
                RExpression expr = (RExpression) obj;
                state.append("expression(");
                for (int i = 0; i < expr.getLength(); i++) {
                    deparse2buff(state, expr.getDataAt(i));
                    if (i == expr.getLength() - 1) {
                        break;
                    }
                    state.append(", ");
                }
                state.append(")");
                break;

            case LISTSXP: {
                state.append("list(");
                RPairList s = (RPairList) obj;
                RPairList t = s;
                while (t != null && t.cdr() != RNull.instance) {
                    if (t.getTag() != null) {
                        deparse2buff(state, t.getTag());
                        state.append(" = ");
                    }
                    deparse2buff(state, t.car());
                    state.append(", ");
                    t = next(t);
                }
                if (t.getTag() != null) {
                    deparse2buff(state, t.getTag());
                    state.append(" = ");
                }
                deparse2buff(state, t.car());
                state.append(')');
                break;
            }

            case LANGSXP: {
                if (obj instanceof RLanguage) {
                    RContext.getRASTHelper().deparse(state, (RLanguage) obj);
                    break;
                }
                RPairList f = (RPairList) obj;
                Object car = f.car();
                Object cdr = f.cdr();
                SEXPTYPE carType = typeof(car);
                if (carType == SEXPTYPE.SYMSXP) {
                    RSymbol symbol = (RSymbol) car;
                    String op = symbol.getName();
                    RPairList pl = (RPairList) cdr;
                    // TODO BUILTINSXP, SPECIALSXP, userBinOp
                    PPInfo fop = ppInfo(op);
                    if (fop.kind == PP.BINARY) {
                        switch (pl.getLength()) {
                            case 1:
                                fop = new PPInfo(PP.UNARY, fop.prec == PREC_SUM ? PREC_SIGN : fop.prec, fop.rightassoc);
                                break;
                            case 2:
                                break;
                            default:
                                assert false;
                        }
                    } else if (fop.kind == PP.BINARY2) {
                        if (pl.getLength() != 2) {
                            fop = new PPInfo(PP.FUNCALL, 0, false);
                        } else if (/* userbinop */false) {
                            // TODO
                            fop = new PPInfo(PP.BINARY, fop.prec, fop.rightassoc);
                        }
                    }
                    switch (fop.kind) {
                        case ASSIGN: {
                            // TODO needsparens
                            deparse2buff(state, pl.car());
                            state.append(' ');
                            state.append(op);
                            state.append(' ');
                            deparse2buff(state, ((RPairList) pl.cdr()).car());
                            break;
                        }

                        case IF: {
                            state.append("if (");
                            deparse2buff(state, pl.car());
                            state.append(')');
                            boolean lookahead = false;
                            if (state.incurly > 0 && state.inlist == 0) {
                                lookahead = curlyahead(pl.cadr());
                                if (!lookahead) {
                                    state.writeline();
                                    state.indent++;
                                }
                            }
                            int lenpl = pl.getLength();
                            if (lenpl > 2) {
                                deparse2buff(state, pl.cadr());
                                if (state.incurly > 0 && state.inlist == 0) {
                                    state.writeline();
                                    if (!lookahead) {
                                        state.indent--;
                                    }
                                } else {
                                    state.append(' ');
                                }
                                state.append("else ");
                                deparse2buff(state, pl.caddr());
                            } else {
                                deparse2buff(state, pl.cadr());
                                if (state.incurly > 0 && !lookahead && state.inlist == 0) {
                                    state.indent--;
                                }
                            }
                            break;
                        }

                        case WHILE: {
                            state.append("while (");
                            deparse2buff(state, pl.car());
                            state.append(") ");
                            deparse2buff(state, pl.cadr());
                            break;
                        }

                        case FOR: {
                            state.append("for (");
                            deparse2buff(state, pl.car());
                            state.append(" in ");
                            deparse2buff(state, pl.cadr());
                            state.append(") ");
                            deparse2buff(state, ((RPairList) pl.cdr()).cadr());
                            break;
                        }

                        case REPEAT:
                            state.append("repeat ");
                            deparse2buff(state, pl.car());
                            break;

                        case BINARY:
                        case BINARY2: {
                            // TODO parens
                            deparse2buff(state, pl.car());
                            state.append(' ');
                            state.append(op);
                            state.append(' ');
                            if (fop.kind == PP.BINARY) {
                                lbreak = state.linebreak(lbreak);
                            }
                            deparse2buff(state, pl.cadr());
                            if (fop.kind == PP.BINARY) {
                                if (lbreak) {
                                    state.indent--;
                                    lbreak = false;
                                }
                            }
                            break;
                        }

                        case UNARY: {
                            state.append(op);
                            deparse2buff(state, pl.car());
                            break;
                        }

                        case CURLY: {
                            state.append(op);
                            state.incurly++;
                            state.indent++;
                            state.writeline();
                            while (pl != null) {
                                deparse2buff(state, pl.car());
                                state.writeline();
                                pl = next(pl);
                            }
                            state.indent--;
                            state.append('}');
                            state.incurly--;
                            break;
                        }

                        case PAREN:
                            state.append('(');
                            deparse2buff(state, pl.car());
                            state.append(')');
                            break;

                        case SUBSET: {
                            deparse2buff(state, pl.car());
                            state.append(op);
                            args2buff(state, (RPairList) pl.cdr(), false, false);
                            if (op.equals("[")) {
                                state.append(']');
                            } else {
                                state.append("]]");
                            }
                            break;
                        }

                        case FUNCTION:
                            state.append(op);
                            state.append('(');
                            args2buff(state, (RPairList) pl.car(), false, true);
                            state.append(')');
                            deparse2buff(state, pl.cadr());
                            break;

                        case DOLLAR: {
                            // TODO needparens, etc
                            deparse2buff(state, pl.car());
                            state.append(op);
                            deparse2buff(state, pl.cadr());
                            break;
                        }

                        case FUNCALL:
                        case RETURN: {
                            if (state.backtick) {
                                state.append('`');
                                state.append(op);
                                state.append('`');
                            } else {
                                state.append(op);
                            }
                            state.append('(');
                            state.inlist++;
                            args2buff(state, (RPairList) cdr, false, false);
                            state.inlist--;
                            state.append(')');
                            break;
                        }

                        default:
                            assert false;
                    }
                } else if (carType == SEXPTYPE.CLOSXP || carType == SEXPTYPE.SPECIALSXP || carType == SEXPTYPE.BUILTINSXP) {
                    // TODO needparens, etc
                    deparse2buff(state, car);
                    state.append('(');
                    args2buff(state, (RPairList) cdr, false, false);
                    state.append(')');
                } else {
                    // lambda
                    if (parenthesizeCaller(car)) {
                        state.append('(');
                        deparse2buff(state, car);
                        state.append(')');
                    } else {
                        deparse2buff(state, car);
                    }
                    state.append('(');
                    args2buff(state, (RPairList) f.cdr(), false, false);
                    state.append(')');
                }
                break;
            }

            case STRSXP:
            case LGLSXP:
            case INTSXP:
            case REALSXP:
            case CPLXSXP:
            case RAWSXP:
                vector2buff(state, (RVector) obj);
                break;

            case FASTR_DOUBLE:
            case FASTR_INT:
            case FASTR_BYTE:
            case FASTR_STRING:
                vecElement2buff(state, SEXPTYPE.convertFastRScalarType(type), obj);
                break;

            default:
                assert false;
        }
        return state;
    }

    private static SEXPTYPE typeof(Object obj) {
        Class<?> klass = obj.getClass();
        if (klass == RPairList.class) {
            return ((RPairList) obj).getType();
        } else {
            return SEXPTYPE.typeForClass(klass);
        }
    }

    @SuppressWarnings("unused")
    private static boolean curlyahead(Object obj) {
        return false;
    }

    @SuppressWarnings("unused")
    private static boolean parenthesizeCaller(Object s) {
        // TODO implement
        return false;
    }

    private static RPairList next(RPairList pairlist) {
        if (pairlist.cdr() == RNull.instance) {
            return null;
        } else {
            return (RPairList) pairlist.cdr();
        }
    }

    @SuppressWarnings("unused")
    private static boolean needsParens(PPInfo mainop, Object arg, boolean isLeft) {
        // TODO
        assert false;
        return false;
    }

    @SlowPath
    private static State args2buff(State state, RPairList args, @SuppressWarnings("unused") boolean lineb, boolean formals) {
        boolean lbreak = false;
        RPairList arglist = args;
        while (arglist != null) {
            if (arglist.getTag() != null) {
                state.append(((RSymbol) arglist.getTag()).getName());
                if (formals) {
                    if (arglist.car() != RMissing.instance) {
                        state.append(" = ");
                        deparse2buff(state, arglist.car());
                    }
                } else {
                    state.append(" = ");
                    if (arglist.car() != RMissing.instance) {
                        deparse2buff(state, arglist.car());
                    }
                }
            } else {
                deparse2buff(state, arglist.car());
            }
            arglist = next(arglist);
            if (arglist != null) {
                state.append(", ");
                lbreak = state.linebreak(lbreak);
            }
        }
        if (lbreak) {
            state.indent--;
        }
        return state;
    }

    @SlowPath
    private static State vector2buff(State state, RVector vec) {
        SEXPTYPE type = SEXPTYPE.typeForClass(vec.getClass());
        boolean surround = false;
        int len = vec.getLength();
        if (len == 0) {
            switch (type) {
                case LGLSXP:
                    state.append("logical(0)");
                    break;
                case INTSXP:
                    state.append("integer(0)");
                    break;
                case REALSXP:
                    state.append("numeric(0)");
                    break;
                case CPLXSXP:
                    state.append("complex(0)");
                    break;
                case STRSXP:
                    state.append("character(0)");
                    break;
                case RAWSXP:
                    state.append("raw(0)");
                    break;
                default:
                    assert false;
            }
        } else if (type == SEXPTYPE.INTSXP) {
            // TODO
        } else {
            // TODO NA checks
            if (len > 1) {
                state.append("c(");
            }
            for (int i = 0; i < len; i++) {
                Object element = vec.getDataAtAsObject(i);
                vecElement2buff(state, type, element);
                if (i < len - 1) {
                    state.append(", ");
                }
            }
            if (len > 1) {
                state.append(')');
            }
            if (surround) {
                state.append(')');
            }
        }
        return state;
    }

    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    private static State vecElement2buff(State state, SEXPTYPE type, Object element) {
        switch (type) {
            case STRSXP:
                // TODO encoding
                state.append('"');
                state.append((String) element);
                state.append('"');
                break;
            case LGLSXP:
                byte lgl = (byte) element;
                state.append(lgl == RRuntime.LOGICAL_TRUE ? "TRUE" : (lgl == RRuntime.LOGICAL_FALSE ? "FALSE" : "NA"));
                break;
            case REALSXP:
                double d = (double) element;
                String dRep = Double.isInfinite(d) ? "Inf" : decimalFormat.format(d);
                state.append(dRep);
                break;
            case INTSXP:
                int i = (int) element;
                String iRep = Integer.toString(i);
                state.append(iRep);
                break;
            default:
                assert false;
        }
        return state;
    }

}
