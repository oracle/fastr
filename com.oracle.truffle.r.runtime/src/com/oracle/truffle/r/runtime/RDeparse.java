/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.text.*;
import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.gnur.*;
import com.oracle.truffle.r.runtime.nodes.*;

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
 * handled in {@code RASTDeparse} via the {@link RRuntimeASTAccess} interface.
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
    public static final char BACKTICK = '`';
    public static final char DQUOTE = '"';

    public enum PP {
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

    // TODO for consistency make an enum
    public static final int PREC_FN = 0;
    public static final int PREC_LEFT = 1;
    public static final int PREC_EQ = 2;
    public static final int PREC_RIGHT = 3;
    public static final int PREC_TILDE = 4;
    public static final int PREC_OR = 5;
    public static final int PREC_AND = 6;
    public static final int PREC_NOT = 7;
    public static final int PREC_COMPARE = 8;
    public static final int PREC_SUM = 9;
    public static final int PREC_PROD = 10;
    public static final int PREC_PERCENT = 11;
    public static final int PREC_COLON = 12;
    public static final int PREC_SIGN = 13;
    public static final int PREC_POWER = 14;
    public static final int PREC_DOLLAR = 15;
    public static final int PREC_NS = 16;
    public static final int PREC_SUBSET = 17;

    public static class PPInfo {
        public final PP kind;
        public final int prec;
        public final boolean rightassoc;

        public PPInfo(PP kind, int prec, boolean rightassoc) {
            this.kind = kind;
            this.prec = prec;
            this.rightassoc = rightassoc;
        }

        public PPInfo changePrec(int newPrec) {
            return new PPInfo(kind, newPrec, rightassoc);
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
    @CompilationFinal private static final Func[] FUNCTAB = new Func[]{
        new Func("+", new PPInfo(PP.BINARY, PREC_SUM, false)),
        new Func("-", new PPInfo(PP.BINARY, PREC_SUM, false)),
        new Func("*", new PPInfo(PP.BINARY, PREC_PROD, false)),
        new Func("/", new PPInfo(PP.BINARY, PREC_PROD, false)),
        new Func("^", new PPInfo(PP.BINARY2, PREC_POWER, false)),
        new Func("%%", new PPInfo(PP.BINARY2, PREC_PERCENT, false)),
        new Func("%/%", new PPInfo(PP.BINARY2, PREC_PERCENT, false)),
        new Func("%*%", new PPInfo(PP.BINARY2, PREC_PERCENT, false)),
        new Func("==", new PPInfo(PP.BINARY, PREC_COMPARE, false)),
        new Func("!=", new PPInfo(PP.BINARY, PREC_COMPARE, false)),
        new Func("<", new PPInfo(PP.BINARY, PREC_COMPARE, false)),
        new Func("<=", new PPInfo(PP.BINARY, PREC_COMPARE, false)),
        new Func(">=", new PPInfo(PP.BINARY, PREC_COMPARE, false)),
        new Func(">", new PPInfo(PP.BINARY, PREC_COMPARE, false)),
        new Func("&", new PPInfo(PP.BINARY, PREC_AND, false)),
        new Func("|", new PPInfo(PP.BINARY, PREC_OR, false)),
        new Func("!", new PPInfo(PP.BINARY, PREC_NOT, false)),
        new Func("&&", new PPInfo(PP.BINARY, PREC_AND, false)),
        new Func("||", new PPInfo(PP.BINARY, PREC_OR, false)),
        new Func(":", new PPInfo(PP.BINARY2, PREC_COLON, false)),
        new Func("~", new PPInfo(PP.BINARY, PREC_TILDE, false)),

        new Func("if", new PPInfo(PP.IF, PREC_FN, true)),
        new Func("while", new PPInfo(PP.WHILE, PREC_FN, false)),
        new Func("for", new PPInfo(PP.FOR, PREC_FN, false)),
        new Func("repeat", new PPInfo(PP.REPEAT, PREC_FN, false)),
        new Func("break", new PPInfo(PP.BREAK, PREC_FN, false)),
        new Func("next", new PPInfo(PP.NEXT, PREC_FN, false)),
        new Func("return", new PPInfo(PP.RETURN, PREC_FN, false)),
        new Func("function", new PPInfo(PP.FUNCTION, PREC_FN, false)),
        new Func("{", new PPInfo(PP.CURLY, PREC_FN, false)),
        new Func("(", new PPInfo(PP.PAREN, PREC_FN, false)),
        new Func("<-", new PPInfo(PP.ASSIGN, PREC_LEFT, true)),
        new Func("=", new PPInfo(PP.ASSIGN, PREC_LEFT, true)),
        new Func("<<-", new PPInfo(PP.ASSIGN, PREC_LEFT, true)),
        new Func("[", new PPInfo(PP.SUBSET, PREC_SUBSET, false)),
        new Func("[[", new PPInfo(PP.SUBSET, PREC_SUBSET, false)),
        new Func("$", new PPInfo(PP.DOLLAR, PREC_DOLLAR, false)),
        new Func("@", new PPInfo(PP.DOLLAR, PREC_DOLLAR, false)),
    };
    // @formatter:on

    public static final PPInfo BUILTIN = new PPInfo(PP.FUNCALL, PREC_FN, false);
    private static final PPInfo USERBINOP = new PPInfo(PP.BINARY2, PREC_PERCENT, false);

    public static Func getFunc(String op) {
        for (Func func : FUNCTAB) {
            if (func.op.equals(op)) {
                return func;
            }
        }
        // user binary op?
        if (isUserBinop(op)) {
            return new Func(op, USERBINOP);
        }
        return null;
    }

    private static boolean isUserBinop(String op) {
        int len = op.length();
        return op.charAt(0) == '%' && op.charAt(len - 1) == '%';
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

    public static final class State {
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
        private int opts;
        @SuppressWarnings("unused") private int sourceable;
        @SuppressWarnings("unused") private int longstring;
        private final int maxlines;
        private boolean active = true;
        @SuppressWarnings("unused") private int isS4;
        private boolean changed;

        private static class NodeSourceInfo {
            private final int startCharIndex;
            private int endCharIndex;

            NodeSourceInfo(int startCharIndex) {
                this.startCharIndex = startCharIndex;
            }
        }

        /**
         * Used when generating {@link SourceSection}s during deparse.
         */
        private HashMap<RSyntaxNode, NodeSourceInfo> nodeMap;

        private State(int widthCutOff, boolean backtick, int maxlines, int opts, boolean needVector) {
            this.cutoff = widthCutOff;
            this.backtick = backtick;
            this.maxlines = maxlines == -1 ? Integer.MAX_VALUE : maxlines;
            this.opts = opts;
            lines = needVector ? new ArrayList<>() : null;
        }

        public static State createPrintableState() {
            return new RDeparse.State(RDeparse.MAX_Cutoff, false, -1, 0, false);
        }

        public static State createPrintableStateWithSource() {
            State result = new RDeparse.State(RDeparse.MAX_Cutoff, false, -1, 0, false);
            result.nodeMap = new HashMap<>();
            return result;
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

        public void mark() {
            changed = false;
        }

        public boolean changed() {
            return changed;
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
            changed = true;
        }

        public void append(char ch) {
            preAppend();
            sb.append(ch);
            len++;
            changed = true;
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
            changed = true;
        }

        public void writeOpenCurlyNLIncIndent() {
            append('{');
            writeline();
            incIndent();
        }

        public void writeNLOpenCurlyIncIndent() {
            writeline();
            append('{');
            incIndent();
        }

        public void writeNLDecIndentCloseCurly() {
            writeline();
            decIndent();
            append('}');
        }

        public void decIndentWriteCloseCurly() {
            decIndent();
            append('}');
        }

        @Override
        public String toString() {
            // assumes needVector == false
            return sb.toString();
        }

        boolean showAttributes() {
            return (opts & SHOWATTRIBUTES) != 0;
        }

        private int dIndent = 0;

        private void dIndent() {
            for (int i = 0; i < dIndent; i++) {
                System.out.print(' ');
            }
        }

        @SuppressWarnings("unused")
        private void trace(boolean enter, RSyntaxNode node) {
            String ms;
            if (enter) {
                ms = "start";
                dIndent();
                dIndent += 2;
            } else {
                ms = "end";
                dIndent -= 2;
                dIndent();
            }
            System.out.printf("%sNodeDeparse (%d): %s%n", ms, +sb.length(), node);
        }

        public void startNodeDeparse(RSyntaxNode node) {
            if (nodeMap != null) {
                // trace(true, node);
                nodeMap.put(node, new NodeSourceInfo(sb.length()));
            }
        }

        public void endNodeDeparse(RSyntaxNode node) {
            if (nodeMap != null) {
                // trace(false, node);
                NodeSourceInfo nsi = nodeMap.get(node);
                nsi.endCharIndex = sb.length();
            }
        }

        public void assignSourceSections() {
            assert nodeMap != null;
            String sourceString = toString();
            Source source = Source.fromText(sourceString, "deparse");
            for (Map.Entry<RSyntaxNode, NodeSourceInfo> entry : nodeMap.entrySet()) {
                RSyntaxNode node = entry.getKey();
                NodeSourceInfo nsi = entry.getValue();
                // may have had one initially
                node.unsetSourceSection();
                node.setSourceSection(source.createSection("", nsi.startCharIndex, nsi.endCharIndex - nsi.startCharIndex));
            }
        }
    }

    /**
     * Version for use by {@code RSerialize} to convert a CLOSXP/LANGSXP/PROMSXP into a parseable
     * string.
     */
    @TruffleBoundary
    public static String deparse(RPairList pl) {
        State state = new State(80, true, -1, 0, false);
        return deparse2buff(state, pl).sb.toString();
    }

    /**
     * Version to generate a printable string for e.g., error messages.
     */
    @TruffleBoundary
    public static String deparseForPrint(Object expr) {
        State state = State.createPrintableState();
        return deparse2buff(state, expr).sb.toString();
    }

    private static String stateToString(boolean abbrev, State state) {
        String result;
        int len = state.lines.size();
        if (len > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                String line = state.lines.get(i);
                sb.append(line);
                if (i < len - 1) {
                    sb.append('\n');
                }
            }
            result = sb.toString();
        } else {
            result = state.lines.get(0);
        }
        if (abbrev && result.length() > 13) {
            result = result.substring(0, 14);
        }
        return result;
    }

    @TruffleBoundary
    public static String deparse1Line(Object expr, boolean abbrev) {
        State state = deparse1WithCutoff(expr, MAX_Cutoff, true, 0, -1);
        return stateToString(abbrev, state);
    }

    @TruffleBoundary
    public static String deparse1Line(Object expr, boolean abbrev, int cutoff, int opts) {
        State state = deparse1WithCutoff(expr, cutoff, true, opts, -1);
        return stateToString(abbrev, state);
    }

    /**
     * Version for {@code deparse}.
     */
    @TruffleBoundary
    public static String[] deparse(Object expr, int cutoff, boolean backtick, int opts, int nlines) {
        State state = deparse1WithCutoff(expr, cutoff, backtick, opts, nlines);
        String[] data = new String[state.lines.size()];
        state.lines.toArray(data);
        return data;
    }

    private static State deparse1WithCutoff(Object expr, int cutoff, boolean backtick, int opts, int nlines) {
        State state = new State(cutoff, backtick, nlines, opts, true);
        deparse2buff(state, expr);
        state.writeline();
        return state;
    }

    @TruffleBoundary
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

            case MISSINGARG_SXP:
            case EMPTYARG_SXP:
                break;

            case SYMSXP: {
                String name = ((RSymbol) obj).getName();
                if (state.backtick) {
                    name = quotify(name, BACKTICK);
                }
                state.append(name);
                break;
            }

            case CHARSXP:
                state.append((String) obj);
                break;

            case PROMSXP: {
                RPairList f = (RPairList) obj;
                deparse2buff(state, f.cdr());
                break;
            }

            case CLOSXP: {
                RPairList f = (RPairList) obj;
                state.append("function (");
                if (f.car() instanceof RPairList) {
                    args2buff(state, f.car(), false, true);
                }
                state.append(") ");
                state.writeline();
                deparse2buff(state, f.cdr());
                break;
            }

            case FUNSXP: {
                RFunction f = (RFunction) obj;
                if (f.isBuiltin()) {
                    state.append(".Primitive(");
                    state.append("\"");
                    state.append(f.getName());
                    state.append("\")");
                } else {
                    RContext.getRRuntimeASTAccess().deparse(state, f);
                }
                break;
            }

            case ENVSXP:
                state.append("<environment>");
                break;

            case FASTR_FACTOR:
                deparseList(state, ((RFactor) obj).getVector());
                break;

            case FASTR_DATAFRAME:
                deparseList(state, ((RDataFrame) obj).getVector());
                break;

            case VECSXP:
                deparseList(state, (RList) obj);
                break;

            case EXPRSXP:
                RExpression expr = (RExpression) obj;
                state.append("expression(");
                vec2buff(state, expr.getList());
                state.append(')');
                break;

            case LISTSXP: {
                state.append("list(");
                RPairList s = (RPairList) obj;
                RPairList t = s;
                while (t != null && t.cdr() != RNull.instance) {
                    if (t.getTag() != null && !t.isNullTag()) {
                        deparse2buff(state, t.getTag());
                        state.append(" = ");
                    }
                    deparse2buff(state, t.car());
                    state.append(", ");
                    t = next(t);
                }
                if (t.getTag() != null && !t.isNullTag()) {
                    deparse2buff(state, t.getTag());
                    state.append(" = ");
                }
                deparse2buff(state, t.car());
                state.append(')');
                break;
            }

            case LANGSXP: {
                if (obj instanceof RLanguage) {
                    RContext.getRRuntimeASTAccess().deparse(state, (RLanguage) obj);
                    break;
                }
                RPairList f = (RPairList) obj;
                Object car = f.car();
                Object cdr = f.cdr();
                SEXPTYPE carType = typeof(car);
                if (carType == SEXPTYPE.SYMSXP) {
                    RSymbol symbol = (RSymbol) car; // TODO could be a promise according to GnuR
                    String op = symbol.getName();
                    boolean userBinop = false;
                    if (RContext.isPrimitiveBuiltin(op) || (userBinop = isUserBinop(op))) {
                        RPairList pl = cdr instanceof RPairList ? (RPairList) cdr : null;
                        PPInfo fop;
                        if (userBinop) {
                            // TODO check for named args and deparse as normal function
                            fop = USERBINOP;
                        } else {
                            fop = ppInfo(op);
                        }
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
                            } else if (userBinop) {
                                fop = new PPInfo(PP.BINARY, fop.prec, fop.rightassoc);
                            }
                        }
                        switch (fop.kind) {
                            case ASSIGN: {
                                Object left = pl.car();
                                boolean parens = needsParens(fop, left, true);
                                if (parens) {
                                    state.append('(');
                                }
                                deparse2buff(state, left);
                                state.append(' ');
                                state.append(op);
                                state.append(' ');
                                Object right = pl.cadr();
                                parens = needsParens(fop, right, false);
                                deparse2buff(state, right);
                                if (parens) {
                                    state.append(')');
                                }
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
                                Object left = pl.car();
                                boolean parens = needsParens(fop, left, true);
                                if (parens) {
                                    state.append('(');
                                }
                                deparse2buff(state, pl.car());
                                if (parens) {
                                    state.append(')');
                                }
                                if (fop.kind == PP.BINARY) {
                                    state.append(' ');
                                }
                                state.append(op);
                                if (fop.kind == PP.BINARY) {
                                    state.append(' ');
                                }
                                if (fop.kind == PP.BINARY) {
                                    lbreak = state.linebreak(lbreak);
                                }
                                Object right = pl.cadr();
                                parens = needsParens(fop, right, false);
                                if (parens) {
                                    state.append('(');
                                }
                                deparse2buff(state, right);
                                if (parens) {
                                    state.append(')');
                                }
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
                                Object left = pl.car();
                                boolean parens = needsParens(fop, left, true);
                                if (parens) {
                                    state.append('(');
                                }
                                deparse2buff(state, left);
                                if (parens) {
                                    state.append(')');
                                }
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
                                Object left = pl.car();
                                boolean parens = needsParens(fop, left, true);
                                if (parens) {
                                    state.append('(');
                                }
                                deparse2buff(state, left);
                                if (parens) {
                                    state.append(')');
                                }
                                state.append(op);
                                args2buff(state, pl.cdr(), false, false);
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
                                args2buff(state, pl.car(), false, true);
                                state.append(") ");
                                deparse2buff(state, pl.cadr());
                                break;

                            case DOLLAR: {
                                Object left = pl.car();
                                boolean parens = needsParens(fop, left, true);
                                if (parens) {
                                    state.append('(');
                                }
                                deparse2buff(state, left);
                                if (parens) {
                                    state.append(')');
                                }
                                state.append(op);
                                Object right = pl.cadr();
                                if (right instanceof RSymbol) {
                                    deparse2buff(state, right);
                                } else {
                                    parens = needsParens(fop, right, true);
                                    if (parens) {
                                        state.append('(');
                                    }
                                    deparse2buff(state, right);
                                    if (parens) {
                                        state.append(')');
                                    }
                                }
                                break;
                            }

                            case FUNCALL:
                            case RETURN: {
                                if (state.backtick) {
                                    state.append(quotify(op, BACKTICK));
                                } else {
                                    state.append(quotify(op, DQUOTE)); // quote?
                                }
                                state.append('(');
                                state.inlist++;
                                args2buff(state, cdr, false, false);
                                state.inlist--;
                                state.append(')');
                                break;
                            }

                            case NEXT:
                                state.append("next");
                                break;

                            case BREAK:
                                state.append("break");
                                break;

                            default:
                                throw RInternalError.unimplemented();
                        }
                    } else {
                        // TODO promise?
                        if (op.equals("::") || op.equals(":::")) {
                            // special case
                            deparse2buff(state, f.cadr());
                            state.append(op);
                            deparse2buff(state, f.caddr());
                        } else {
                            state.append(quotify(op, BACKTICK));
                            state.append('(');
                            args2buff(state, cdr, false, false);
                            state.append(')');
                        }
                    }
                } else if (carType == SEXPTYPE.CLOSXP || carType == SEXPTYPE.SPECIALSXP || carType == SEXPTYPE.BUILTINSXP) {
                    if (parenthesizeCaller(car)) {
                        state.append('(');
                        deparse2buff(state, car);
                        state.append(')');
                    } else {
                        deparse2buff(state, car);
                    }
                    state.append('(');
                    args2buff(state, cdr, false, false);
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
                    args2buff(state, f.cdr(), false, false);
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
                vector2buff(state, checkScalarVector(obj));
                break;

            case BCODESXP: {
                /*
                 * This should only happen in a call from RSerialize when unserializing a CLOSXP.
                 * There is no value in following GnuR and appending <bytecode>, as we need the
                 * source., which is (we expect) in the RPairList cdr (which is an RList).
                 * Experimentally, only the first element of the list should be deparsed.
                 */
                // state.append("<bytecode>");
                RPairList pl = (RPairList) obj;
                RList plcdr = (RList) pl.cdr();
                deparse2buff(state, plcdr.getDataAtAsObject(0));
                break;
            }

            case FASTR_DOUBLE:
            case FASTR_INT:
            case FASTR_BYTE:
            case FASTR_COMPLEX:
                vecElement2buff(state, SEXPTYPE.convertFastRScalarType(type), obj);
                break;

            case FASTR_CONNECTION:
                // TODO GnuR deparses this as a structure
                state.append("NULL");
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

    private static RAbstractVector checkScalarVector(Object obj) {
        if (obj instanceof String) {
            return RDataFactory.createStringVectorFromScalar((String) obj);
        } else if (obj instanceof Byte) {
            return RDataFactory.createLogicalVectorFromScalar((Byte) obj);
        } else if (obj instanceof Integer) {
            return RDataFactory.createIntVectorFromScalar((Integer) obj);
        } else if (obj instanceof Double) {
            return RDataFactory.createDoubleVectorFromScalar((Double) obj);
        } else if (obj instanceof RComplex) {
            return RDataFactory.createComplexVectorFromScalar((RComplex) obj);
        } else {
            return (RAbstractVector) obj;
        }
    }

    @SuppressWarnings("unused")
    private static boolean curlyahead(Object obj) {
        return false;
    }

    /**
     * Check for whether we need to parenthesize a caller. The unevaluated ones are tricky: We want
     *
     * <pre>
     *  x$f(z)
     *  x[n](z)
     *  base::mean(x)
     * </pre>
     *
     * but <pre< (f+g)(z) (function(x) 1)(x)
     * </pre>
     * etc.
     */
    private static boolean parenthesizeCaller(Object s) {
        if (s instanceof RPairList) {
            RPairList pl = (RPairList) s;
            if (pl.getType() == SEXPTYPE.CLOSXP) {
                return true;
            } else if (pl.getType() == SEXPTYPE.LANGSXP) {
                Object car = pl.car();
                if (car instanceof RSymbol) {
                    String op = ((RSymbol) car).getName();
                    if (isUserBinop(op)) {
                        return true;
                    }
                    if (RContext.isPrimitiveBuiltin(op)) {
                        PPInfo info = ppInfo(op);
                        if (info.prec >= PREC_DOLLAR || info.kind == PP.FUNCALL || info.kind == PP.PAREN || info.kind == PP.CURLY) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    return true;
                }

            }
        }
        return false;
    }

    private static RPairList next(RPairList pairlist) {
        if (pairlist.cdr() == RNull.instance) {
            return null;
        } else {
            return (RPairList) pairlist.cdr();
        }
    }

    /**
     * Deparse list, dataframe, factor (different representation types in FastR).
     */
    private static void deparseList(State state, RVector obj) {
        if (state.showAttributes()) {
            attr1(state, obj);
        }
        state.append("list(");
        vec2buff(state, obj);
        state.append(')');
        if (state.showAttributes()) {
            attr2(state, obj);
        }

    }

    /**
     * Check for needing parentheses in expressions. needsparens looks at an arg to a unary or
     * binary operator to determine if it needs to be parenthesized when deparsed.{@code mainop} is
     * a unary or binary operator, {@code arg} is an argument to it, on the left if
     * {@code left == true}.
     */
    private static boolean needsParens(PPInfo mainop, Object arg, boolean left) {
        if (arg instanceof RPairList) {
            RPairList pl = (RPairList) arg;
            if (pl.getType() == SEXPTYPE.LANGSXP) {
                Object car = pl.car();
                if (car instanceof RSymbol) {
                    String op = ((RSymbol) car).getName();
                    if (RContext.isPrimitiveBuiltin(op)) {
                        PPInfo arginfo = ppInfo(op);
                        switch (arginfo.kind) {
                            case BINARY:
                            case BINARY2: {
                                Object cdr = pl.cdr();
                                int length = (cdr instanceof RPairList) ? ((RPairList) cdr).getLength() : 1;
                                switch (length) {
                                    case 1:
                                        if (!left) {
                                            return false;
                                        }
                                        if (arginfo.prec == PREC_SUM) {
                                            arginfo = arginfo.changePrec(PREC_SIGN);
                                        }
                                        break;
                                    case 2:
                                        break;
                                    default:
                                        return false;
                                }
                                return checkPrec(mainop, arginfo, left);
                            }

                            case ASSIGN:
                            case SUBSET:
                            case UNARY:
                            case DOLLAR:
                                return checkPrec(mainop, arginfo, left);

                            case FOR:
                            case IF:
                            case WHILE:
                            case REPEAT:
                                return left;
                            default:
                                return false;
                        }
                    } else if (isUserBinop(op)) {
                        if (mainop.prec == PREC_PERCENT || (mainop.prec == PREC_PERCENT && left == mainop.rightassoc)) {
                            return true;
                        }
                    }
                }
            }
        } else if (isComplexLengthOne(arg)) {
            if (mainop.prec > PREC_SUM || (mainop.prec == PREC_SUM && left == mainop.rightassoc)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkPrec(PPInfo mainop, PPInfo arginfo, boolean left) {
        return mainop.prec > arginfo.prec || (mainop.prec == arginfo.prec && left == mainop.rightassoc);
    }

    private static boolean isComplexLengthOne(Object arg) {
        return ((arg instanceof RComplexVector && ((RComplexVector) arg).getLength() == 1) || arg instanceof RComplex);
    }

    @TruffleBoundary
    /**
     * Handles {@link RList}, (@link RExpression}, {@link RDataFrame} and {@link RFactor}. Method
     * name same as GnuR.
     */
    private static State vec2buff(State state, RVector v) {
        int n = v.getLength();
        boolean lbreak = false;
        Object names = v.getNames();
        RStringVector snames = names == RNull.instance ? null : (RStringVector) names;
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                state.append(", ");
            }
            lbreak = state.linebreak(lbreak);
            String sname = snames == null ? null : snames.getDataAt(i);
            if (snames != null && ((sname = snames.getDataAt(i)) != null)) {
                state.append(sname);
                state.append(" = ");
            }
            deparse2buff(state, v.getDataAtAsObject(i));
        }
        if (lbreak) {
            state.decIndent();
        }
        return state;
    }

    @TruffleBoundary
    private static State args2buff(State state, Object args, @SuppressWarnings("unused") boolean lineb, boolean formals) {
        boolean lbreak = false;
        RPairList arglist;
        if (args instanceof RNull) {
            arglist = null;
        } else {
            arglist = (RPairList) args;
        }
        while (arglist != null) {
            Object argTag = arglist.getTag();
            if (argTag != null && argTag != RNull.instance) {
                String rs = ((RSymbol) arglist.getTag()).getName();
                if (rs.equals("...")) {
                    state.append(rs);
                } else {
                    state.append(quotify(rs, state.backtick ? BACKTICK : DQUOTE));
                }

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

    @TruffleBoundary
    private static State vector2buff(State state, RAbstractVector vec) {
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
            // TODO seq detection and COMPAT?
            if (len > 1) {
                state.append("c(");
                surround = true;
            }
            RIntVector intVec = (RIntVector) vec;
            for (int i = 0; i < len; i++) {
                int val = intVec.getDataAt(i);
                if (RRuntime.isNA(val)) {
                    state.append("NA_integer_");
                } else {
                    state.append(Integer.toString(val));
                    state.append('L');
                }
                if (i < len - 1) {
                    state.append(", ");
                }
            }
        } else {
            // TODO NA checks
            if (len > 1) {
                state.append("c(");
                surround = true;
            } else if (len == 1 && type == SEXPTYPE.CPLXSXP) {
                state.append('(');
                surround = true;
            }
            for (int i = 0; i < len; i++) {
                Object element = vec.getDataAtAsObject(i);
                if (type == SEXPTYPE.REALSXP && RRuntime.isNA((double) element)) {
                    state.append("NA_real_");
                } else if (type == SEXPTYPE.STRSXP && RRuntime.isNA((String) element)) {
                    state.append("NA_character_");
                } else if (type == SEXPTYPE.CPLXSXP && RRuntime.isNA((RComplex) element)) {
                    state.append("NA_complex_");
                } else {
                    vecElement2buff(state, type, element);
                }

                if (i < len - 1) {
                    state.append(", ");
                }
            }
        }
        if (surround) {
            state.append(')');
        }
        return state;
    }

    private static State vecElement2buff(State state, SEXPTYPE type, Object element) {
        switch (type) {
            case STRSXP:
                // TODO encoding
                state.append('"');
                String s = (String) element;
                for (int i = 0; i < s.length(); i++) {
                    char ch = s.charAt(i);
                    int charInt = ch;
                    switch (ch) {
                        case '\n':
                            state.append("\\n");
                            break;
                        case '\r':
                            state.append("\\r");
                            break;
                        case '\t':
                            state.append("\\t");
                            break;
                        case '\f':
                            state.append("\\f");
                            break;
                        case '\\':
                            state.append("\\\\");
                            break;
                        case '"':
                            state.append("\\\"");
                            break;
                        case 0x7:
                            state.append("\\a");
                            break;
                        case 0x8:
                            state.append("\\b");
                            break;
                        case 0xB:
                            state.append("\\v");
                            break;
                        default:
                            if (Character.isISOControl(ch)) {
                                state.append("\\x" + Integer.toHexString(charInt));
                            } else if (charInt > 0x7F) {
                                state.append("\\u" + Integer.toHexString(charInt));
                            } else {
                                state.append(ch);
                            }
                            break;
                    }
                }
                state.append('"');
                break;
            case LGLSXP:
                byte lgl = (byte) element;
                state.append(lgl == RRuntime.LOGICAL_TRUE ? "TRUE" : (lgl == RRuntime.LOGICAL_FALSE ? "FALSE" : "NA"));
                break;
            case REALSXP:
                double d = (double) element;
                state.append(encodeReal(d));
                break;
            case INTSXP:
                int i = (int) element;
                state.append(Integer.toString(i));
                state.append('L');
                break;
            case CPLXSXP:
                RComplex c = (RComplex) element;
                String reRep = encodeReal(c.getRealPart());
                String imRep = encodeReal(c.getImaginaryPart());
                state.append(reRep);
                state.append('+');
                state.append(imRep);
                state.append('i');
                break;
            default:
                assert false;
        }
        return state;
    }

    private static final DecimalFormatSymbols decimalFormatSymbols;
    private static final DecimalFormat decimalFormat;
    private static final DecimalFormat simpleDecimalFormat;

    static {
        decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setExponentSeparator("e");
        decimalFormatSymbols.setNaN("NaN");
        decimalFormatSymbols.setInfinity("Inf");
        decimalFormat = new DecimalFormat("#.##################E0", decimalFormatSymbols);
        simpleDecimalFormat = new DecimalFormat("#.##################", decimalFormatSymbols);
    }

    private static String encodeReal(double d) {
        if (d == 0 || withinSimpleRealRange(d)) {
            return simpleDecimalFormat.format(d);
        } else {
            String str = decimalFormat.format(d);
            if (!str.contains("e-") && str.contains("e")) {
                return str.replace("e", "e+");
            } else {
                return str;
            }
        }
    }

    private static boolean withinSimpleRealRange(double d) {
        return (d > 0.0001 || d < -0.0001) && d < 100000 && d > -100000;
    }

    public static String quotify(String name, State state) {
        return quotify(name, state.backtick ? BACKTICK : DQUOTE);
    }

    public static String quotify(String name, char qc) {
        if (isValidName(name) || name.length() == 0) {
            return name;
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append(qc);
            for (int i = 0; i < name.length(); i++) {
                char ch = name.charAt(i);
                if (ch == '\\') {
                    sb.append(ch);
                }
                sb.append(ch);
            }
            sb.append(qc);
            return sb.toString();
        }
    }

    private static final String[] keywords = {"NULL", "NA", "TRUE", "FALSE", "Inf", "NaN", "NA_integer_", "NA_real_", "NA_character_", "NA_complex_", "function", "while", "repeat", "for", "if", "in",
                    "else", "next", "break", "..."};

    public static boolean isKeyword(String name) {
        for (int i = 0; i < keywords.length; i++) {
            if (name.equals(keywords[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidName(String name) {
        char ch = safeCharAt(name, 0);
        if (ch != '.' && !Character.isLetter(ch)) {
            return false;
        }
        if (ch == '.' && Character.isDigit(safeCharAt(name, 1))) {
            return false;
        }
        int i = 1;
        ch = safeCharAt(name, i);
        while (Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == '.' | ch == '_') {
            i++;
            ch = safeCharAt(name, i);
        }
        if (ch != 0) {
            return false;
        }
        if (name.equals("...")) {
            return true;
        }
        if (isKeyword(name)) {
            return false;
        }
        return true;
    }

    private static char safeCharAt(String s, int i) {
        if (i < s.length()) {
            return s.charAt(i);
        } else {
            return 0;
        }
    }

    private static boolean hasAttributes(Object obj) {
        // TODO check (and ignore) function source attribute
        if (obj instanceof RAttributable) {
            RAttributes attrs = ((RAttributable) obj).getAttributes();
            return attrs != null && !attrs.isEmpty();
        } else {
            return false;
        }
    }

    private static void attr1(State state, Object obj) {
        if (hasAttributes(obj)) {
            state.append("structure(");
        }
    }

    private static void attr2(State state, Object obj) {
        if (obj instanceof RAttributable) {
            RAttributes attrs = ((RAttributable) obj).getAttributes();
            if (attrs != null) {
                Iterator<RAttribute> iter = attrs.iterator();
                while (iter.hasNext()) {
                    RAttribute attr = iter.next();
                    // TODO ignore function source attribute
                    String attrName = attr.getName();
                    state.append(", ");
                    String dotName = null;
                    switch (attrName) {
                        case "dimnames":
                            dotName = ".Dimnames";
                            break;
                        case "dim":
                            dotName = ".Dim";
                            break;
                        case "names":
                            dotName = ".Names";
                            break;
                        case "tsp":
                            dotName = ".Tsp";
                            break;
                        case "levels":
                            dotName = ".Label";
                            break;

                        default: {
                            state.opts = SIMPLEDEPARSE;
                            if (attrName.contains(" ")) {
                                state.append('"');
                                state.append(attrName);
                                state.append('"');
                            } else {
                                state.append(attrName);
                            }
                        }

                    }
                    if (dotName != null) {
                        state.append(dotName);
                    }
                    state.append(" = ");
                    deparse2buff(state, attr.getValue());
                    state.append(')');
                }
            }
        }
    }

}
