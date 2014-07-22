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

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.gnur.*;

/**
 * Deparsing R objects.
 *
 * There are two distinct clients of this class:
 * <ul>
 * <li>{@code RSerialize} when it needs to convert an unserialzied GnuR {@code pairlist} instance
 * that denotes a closure into an {@link RFunction} which is, currently done, by deparsing and
 * reparsing the value.</li>
 * <li>The {@code deparse} builtin.</li>
 * </ul>
 *
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

    private static enum PP {
        FUNCALL,
        RETURN,
        BINARY,
        BINARY2,
        UNARY,
        IF,
        WHILE,
        FOR,
        ASSIGN,
        CURLY,
        SUBSET,
        DOLLAR;
    }

    private static final int PREC_SUM = 9;
    private static final int PREC_SIGN = 13;

    private static class PPInfo {
        final PP kind;
        final int prec;
        final boolean rightassoc;

        PPInfo(PP kind, int prec, boolean rightassoc) {
            this.kind = kind;
            this.prec = prec;
            this.rightassoc = rightassoc;
        }

    }

    private static class Func {
        final String op;
        final PPInfo info;

        Func(String op, PPInfo info) {
            this.op = op;
            this.info = info;
        }

    }

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

        new Func("if", new PPInfo(PP.IF, 0, false)),
        new Func("{", new PPInfo(PP.CURLY, 0, false)),
        new Func("return", new PPInfo(PP.RETURN, 0, false)),
        new Func("<-", new PPInfo(PP.ASSIGN, 1, true)),
        new Func("[", new PPInfo(PP.SUBSET, 17, false)),
        new Func("$", new PPInfo(PP.DOLLAR, 15, false)),


    };
    // @formatter:on

    private static final PPInfo BUILTIN = new PPInfo(PP.FUNCALL, 0, false);

    static PPInfo ppInfo(String op) {
        for (Func func : FUNCTAB) {
            if (func.op.equals(op)) {
                return func.info;
            }
        }
        // must be a builtin that we don't have in FUNCTAB
        return BUILTIN;
    }

    private static class State {
        private final StringBuilder sb = new StringBuilder();
        private final ArrayList<String> lines;
        private int linenumber;
        private int len;
        private int incurly;
        private int inlist;
        private boolean startline;
        private int indent;
        private int cutoff;
        private boolean backtick;
        @SuppressWarnings("unused") private int opts;
        @SuppressWarnings("unused") private int sourceable;
        @SuppressWarnings("unused") private int longstring;
        private int maxlines;
        private boolean active = true;
        @SuppressWarnings("unused") private int isS4;

        State(int widthCutOff, boolean backtick, int maxlines, boolean needVector) {
            this.cutoff = widthCutOff;
            this.backtick = backtick;
            this.maxlines = maxlines;
            lines = needVector ? new ArrayList<>() : null;
        }

        void preAppend() {
            if (startline) {
                startline = false;
                indent();
            }
        }

        void indent() {
            for (int i = 1; i <= indent; i++) {
                if (i <= 4) {
                    append("    ");
                } else {
                    append("  ");
                }
            }
        }

        void append(String s) {
            preAppend();
            sb.append(s);
            len += s.length();
        }

        void append(char ch) {
            preAppend();
            sb.append(ch);
            len++;
        }

        boolean linebreak(boolean lbreak) {
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

        void writeline() {
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
        State state = new State(80, false, Integer.MAX_VALUE, false);
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
    private static State deparse2buff(State state, Object obj) {
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

            case CLOSXP: {
                RPairList f = (RPairList) obj;
                state.append("function (");
                args2buff(state, (RPairList) f.car(), false, true);
                state.append(") ");
                state.writeline();
                deparse2buff(state, f.cdr());
                break;
            }

            case LANGSXP: {
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
                    }
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

    @SlowPath
    private static State args2buff(State state, RPairList args, @SuppressWarnings("unused") boolean lineb, boolean formals) {
        boolean lbreak = false;
        RPairList arglist = args;
        while (arglist != null) {
            if (arglist.getTag() != null) {
                state.append(arglist.getTag());
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

    private static State vecElement2buff(State state, SEXPTYPE type, Object element) {
        switch (type) {
            case STRSXP:
                // TODO encoding
                state.append('"');
                state.append((String) element);
                state.append('"');
                break;
            case LGLSXP:
                state.append(RRuntime.fromLogical((byte) element) ? "TRUE" : "FALSE");
                break;
            case REALSXP:
                String rep = Double.toString((double) element);
                if (rep.equals("Infinity")) {
                    rep = "Inf";
                }
                state.append(rep);
                break;
            default:
                assert false;
        }
        return state;
    }

}
