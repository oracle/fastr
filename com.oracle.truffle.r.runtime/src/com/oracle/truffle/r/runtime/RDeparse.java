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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

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

    private static class PPInfo {
        public final PP kind;
        public final int prec;
        public final boolean rightassoc;

        PPInfo(PP kind, int prec, boolean rightassoc) {
            this.kind = kind;
            this.prec = prec;
            this.rightassoc = rightassoc;
        }

        public PPInfo changePrec(int newPrec) {
            return new PPInfo(kind, newPrec, rightassoc);
        }
    }

    private static class Func {
        public final String op;
        public final String closeOp;
        public final PPInfo info;

        Func(String op, String closeOp, PPInfo info) {
            this.op = op;
            this.closeOp = closeOp;
            this.info = info;
        }
    }

    @CompilationFinal private static final Func[] FUNCTAB = new Func[]{
                    new Func("+", null, new PPInfo(PP.BINARY, PREC_SUM, false)),
                    new Func("-", null, new PPInfo(PP.BINARY, PREC_SUM, false)),
                    new Func("*", null, new PPInfo(PP.BINARY, PREC_PROD, false)),
                    new Func("/", null, new PPInfo(PP.BINARY, PREC_PROD, false)),
                    new Func("^", null, new PPInfo(PP.BINARY2, PREC_POWER, false)),
                    new Func("%%", null, new PPInfo(PP.BINARY, PREC_PERCENT, false)),
                    new Func("%/%", null, new PPInfo(PP.BINARY, PREC_PERCENT, false)),
                    new Func("%*%", null, new PPInfo(PP.BINARY, PREC_PERCENT, false)),
                    new Func("==", null, new PPInfo(PP.BINARY, PREC_COMPARE, false)),
                    new Func("!=", null, new PPInfo(PP.BINARY, PREC_COMPARE, false)),
                    new Func("<", null, new PPInfo(PP.BINARY, PREC_COMPARE, false)),
                    new Func("<=", null, new PPInfo(PP.BINARY, PREC_COMPARE, false)),
                    new Func(">=", null, new PPInfo(PP.BINARY, PREC_COMPARE, false)),
                    new Func(">", null, new PPInfo(PP.BINARY, PREC_COMPARE, false)),
                    new Func("&", null, new PPInfo(PP.BINARY, PREC_AND, false)),
                    new Func("|", null, new PPInfo(PP.BINARY, PREC_OR, false)),
                    new Func("!", null, new PPInfo(PP.BINARY, PREC_NOT, false)),
                    new Func("&&", null, new PPInfo(PP.BINARY, PREC_AND, false)),
                    new Func("||", null, new PPInfo(PP.BINARY, PREC_OR, false)),
                    new Func(":", null, new PPInfo(PP.BINARY2, PREC_COLON, false)),
                    new Func("~", null, new PPInfo(PP.BINARY, PREC_TILDE, false)),

                    new Func("if", null, new PPInfo(PP.IF, PREC_FN, true)),
                    new Func("while", null, new PPInfo(PP.WHILE, PREC_FN, false)),
                    new Func("for", null, new PPInfo(PP.FOR, PREC_FN, false)),
                    new Func("repeat", null, new PPInfo(PP.REPEAT, PREC_FN, false)),
                    new Func("break", null, new PPInfo(PP.BREAK, PREC_FN, false)),
                    new Func("next", null, new PPInfo(PP.NEXT, PREC_FN, false)),
                    new Func("return", null, new PPInfo(PP.RETURN, PREC_FN, false)),
                    new Func("function", null, new PPInfo(PP.FUNCTION, PREC_FN, false)),
                    new Func("{", "}", new PPInfo(PP.CURLY, PREC_FN, false)),
                    new Func("(", ")", new PPInfo(PP.PAREN, PREC_FN, false)),
                    new Func("<-", null, new PPInfo(PP.ASSIGN, PREC_LEFT, true)),
                    new Func("=", null, new PPInfo(PP.ASSIGN, PREC_LEFT, true)),
                    new Func("<<-", null, new PPInfo(PP.ASSIGN, PREC_LEFT, true)),
                    new Func("[", "]", new PPInfo(PP.SUBSET, PREC_SUBSET, false)),
                    new Func("[[", "]]", new PPInfo(PP.SUBSET, PREC_SUBSET, false)),
                    new Func("$", null, new PPInfo(PP.DOLLAR, PREC_DOLLAR, false)),
                    new Func("@", null, new PPInfo(PP.DOLLAR, PREC_DOLLAR, false)),
    };

    private static final PPInfo USERBINOP = new PPInfo(PP.BINARY, PREC_PERCENT, false);

    private static Func getFunc(String op) {
        for (Func func : FUNCTAB) {
            if (func.op.equals(op)) {
                return func;
            }
        }
        // user binary op?
        if (isUserBinop(op)) {
            return new Func(op, null, USERBINOP);
        }
        return null;
    }

    private static boolean isUserBinop(String op) {
        int len = op.length();
        return len > 0 && op.charAt(0) == '%' && op.charAt(len - 1) == '%';
    }

    /**
     * Ensure that {@code node} has a {@link SourceSection} by deparsing if necessary.
     */
    public static void ensureSourceSection(RSyntaxElement node) {
        SourceSection ss = node.getSourceSection();
        if (ss == RSyntaxNode.EAGER_DEPARSE) {
            new DeparseVisitor(true, RDeparse.MAX_Cutoff, false, -1, 0).append(node).fixupSources();
        }
    }

    private static Func isInfixOperatorNode(RSyntaxElement element) {
        if (element instanceof RSyntaxCall) {
            RSyntaxElement lhs = ((RSyntaxCall) element).getSyntaxLHS();
            if (lhs instanceof RSyntaxLookup) {
                String name = ((RSyntaxLookup) lhs).getIdentifier();
                Func func = RDeparse.getFunc(name);
                if (func == null) {
                    return null;
                } else {
                    return func.info.kind == PP.RETURN ? null : func;
                }
            }
        }
        return null;
    }

    private interface C extends AutoCloseable {
        // this interface is used to get a shorter name and remove the checked exception
        @Override
        void close();
    }

    private static final class SourceSectionElement {
        public final RSyntaxElement element;
        public final int start;
        public final int length;

        SourceSectionElement(RSyntaxElement element, int start, int length) {
            this.element = element;
            this.start = start;
            this.length = length;
        }
    }

    private static final class DeparseVisitor {

        private final Visitor visitor = new Visitor();

        private final StringBuilder sb = new StringBuilder();

        private final ArrayList<SourceSectionElement> sources;

        private final int cutoff;
        private final boolean backtick;
        private int opts;
        private final int nlines;

        private int inCurly = 0;
        private int inList = 0;
        private int indent = 0;
        private int lastLineStart = 0;

        DeparseVisitor(boolean storeSource, int cutoff, boolean backtick, int opts, int nlines) {
            this.cutoff = cutoff;
            this.backtick = backtick;
            this.opts = opts;
            this.nlines = nlines;
            this.sources = storeSource ? new ArrayList<>() : null;
        }

        public String getContents() {
            // strip surplus newlines
            int length = sb.length();
            while (length > 0) {
                char c = sb.charAt(length - 1);
                if (c != '\n' && c != ' ') {
                    break;
                }
                length--;
            }
            sb.setLength(length);
            return sb.toString();
        }

        private boolean showAttributes() {
            return (opts & SHOWATTRIBUTES) != 0;
        }

        boolean quoteExpressions() {
            return (opts & QUOTEEXPRESSIONS) != 0;
        }

        private DeparseVisitor append(char ch) {
            assert ch != '\n';
            sb.append(ch);
            return this;
        }

        private DeparseVisitor append(String str) {
            assert !str.contains("\n");
            sb.append(str);
            return this;
        }

        private C withContext(RSyntaxElement... context) {
            if (sources == null) {
                return () -> {
                };
            } else {
                int startIndex = sb.length();
                return () -> {
                    for (RSyntaxElement element : context) {
                        sources.add(new SourceSectionElement(element, startIndex, sb.length() - startIndex));
                    }
                };
            }
        }

        public void fixupSources() {
            Source source = Source.fromText(sb, "deparse");
            for (SourceSectionElement s : sources) {
                s.element.setSourceSection(source.createSection(null, s.start, s.length));
            }
        }

        @SuppressWarnings("try")
        private DeparseVisitor append(String str, RSyntaxElement... context) {
            try (C c = withContext(context)) {
                append(str);
            }
            return this;
        }

        @SuppressWarnings("try")
        public DeparseVisitor append(RSyntaxElement element) {
            try (C c = withContext(element)) {
                visitor.accept(element);
            }
            return this;
        }

        private void printline() {
            sb.append("\n");
            lastLineStart = sb.length();
            for (int i = 0; i < indent; i++) {
                sb.append(i < 4 ? "    " : "  ");
            }
        }

        private static boolean isSequence(RSyntaxElement element) {
            if (element instanceof RSyntaxCall) {
                RSyntaxElement lhs = ((RSyntaxCall) element).getSyntaxLHS();
                if (lhs instanceof RSyntaxLookup) {
                    RSyntaxLookup lookup = (RSyntaxLookup) lhs;
                    return "{".equals(lookup.getIdentifier());
                }
            }
            return false;
        }

        private static String isConstantString(RSyntaxElement element) {
            if (element instanceof RSyntaxConstant) {
                return RRuntime.asStringLengthOne(((RSyntaxConstant) element).getValue());
            }
            return null;
        }

        private boolean linebreak(boolean lbreak) {
            boolean result = lbreak;
            if ((sb.length() - lastLineStart) > cutoff) {
                if (!lbreak) {
                    result = true;
                    indent++;
                }
                printline();
            }
            return result;
        }

        private C indent() {
            indent++;
            return new C() {
                @Override
                public void close() {
                    indent--;
                }
            };
        }

        private C inCurly() {
            inCurly++;
            return new C() {
                @Override
                public void close() {
                    inCurly--;
                }
            };
        }

        private final class Visitor extends RSyntaxVisitor<Void> {

            @Override
            @SuppressWarnings("try")
            protected Void visit(RSyntaxCall call) {
                RSyntaxElement lhs = call.getSyntaxLHS();
                RSyntaxElement[] args = call.getSyntaxArguments();
                if (lhs instanceof RSyntaxLookup) {
                    String symbol = ((RSyntaxLookup) lhs).getIdentifier();
                    RDeparse.Func func = RDeparse.getFunc(symbol);
                    if (func != null) {
                        PPInfo info = func.info;
                        if (args.length == 0) {
                            switch (info.kind) {
                                case BREAK:
                                case NEXT:
                                    append(func.op, call, lhs);
                                    return null;
                            }
                        } else if (args.length == 1) {
                            switch (info.kind) {
                                case BINARY:
                                case BINARY2:
                                    append(func.op, lhs).append(args[0]);
                                    return null;
                                case REPEAT:
                                    append("repeat", lhs).append(' ').append(args[0]);
                                    return null;
                                case PAREN:
                                    append(func.op, lhs).append(args[0]).append(func.closeOp);
                                    return null;
                            }
                        } else if (args.length == 2) {
                            switch (info.kind) {
                                case ASSIGN:
                                case BINARY:
                                case BINARY2:
                                    appendWithParens(args[0], info, true);
                                    if (info.kind != PP.BINARY2) {
                                        append(' ').append(func.op, lhs).append(' ');
                                    } else {
                                        append(func.op);
                                    }
                                    appendWithParens(args[1], info, false);
                                    return null;
                                case DOLLAR:
                                    appendWithParens(args[0], info, true);
                                    append(func.op, lhs);
                                    String name = isConstantString(args[1]);
                                    if (name != null && isValidName(name)) {
                                        append(name, args[1]);
                                    } else {
                                        appendWithParens(args[1], info, false);
                                    }
                                    return null;
                                case IF:
                                    append("if", lhs).append(" (").append(args[0]).append(") ");
                                    if (inCurly > 0 && inList == 0 && !isSequence(args[1])) {
                                        printline();
                                        try (C c = indent()) {
                                            append(args[1]);
                                        }
                                    } else {
                                        append(args[1]);
                                    }
                                    return null;
                                case WHILE:
                                    append("while", lhs).append(" (").append(args[0]).append(") ").append(args[1]);
                                    return null;
                            }
                        } else if (args.length == 3) {
                            switch (symbol) {
                                case "for":
                                    append("for", lhs).append(" (").append(args[0]).append(" in ").append(args[1]).append(") ").append(args[2]);
                                    return null;
                                case "if":
                                    append("if", lhs).append(" (").append(args[0]).append(") ");
                                    if (inCurly > 0 && inList == 0 && !isSequence(args[1])) {
                                        printline();
                                        try (C c = indent()) {
                                            append(args[1]).printline();
                                        }
                                    } else {
                                        append(args[1]);
                                        if (inCurly > 0 && inList == 0) {
                                            printline();
                                        } else {
                                            append(' ');
                                        }
                                    }
                                    append("else ").append(args[2]);
                                    return null;
                            }
                        }
                        switch (info.kind) {
                            case CURLY:
                                boolean braces = args.length != 1 || hasBraces(call);
                                if (braces) {
                                    append("{", lhs);
                                    try (C i = indent(); C c = inCurly()) {
                                        for (RSyntaxElement statement : args) {
                                            printline();
                                            append(statement);
                                        }
                                    }
                                    printline();
                                    append('}');
                                } else {
                                    append(args[0]);
                                }
                                return null;
                            case SUBSET:
                                appendWithParens(args[0], info, true);
                                append(func.op, lhs).appendArgs(call.getSyntaxSignature(), args, 1, false).append(func.closeOp);
                                return null;
                        }
                    }
                    if ("::".equals(symbol) || ":::".equals(symbol)) {
                        if (args.length == 0) {
                            append("NULL").append(symbol).append("NULL");
                        } else if (args.length == 1) {
                            append(args[0]).append(symbol).append("NULL");
                        } else {
                            append(args[0]).append(symbol).append(args[1]);
                        }
                        return null;
                    }
                }

                PPInfo info = new PPInfo(PP.FUNCALL, PREC_FN, false);
                appendWithParens(lhs, info, true);
                append('(').appendArgs(call.getSyntaxSignature(), args, 0, false).append(')');
                return null;
            }

            public boolean hasBraces(RSyntaxElement node) {
                SourceSection ss = node.getSourceSection();
                if (ss == null || ss == RSyntaxNode.SOURCE_UNAVAILABLE) {
                    // this is statistical guess
                    return true;
                } else {
                    return ss.getCode().startsWith("{");
                }
            }

            @Override
            @SuppressWarnings("try")
            protected Void visit(RSyntaxConstant constant) {
                // coerce scalar values to vectors and unwrap data frames and factors:
                Object value = RRuntime.asAbstractVector(constant.getValue());
                if (value instanceof RFactor) {
                    value = ((RFactor) value).getVector();
                }

                if (value instanceof RExpression) {
                    append("expression(").appendListContents(((RExpression) value).getList()).append(')');
                } else if (value instanceof RAbstractListVector) {
                    RAbstractListVector obj = (RAbstractListVector) value;
                    try (C c = withAttributes(obj)) {
                        append("list(").appendListContents(obj).append(')');
                    }
                } else if (value instanceof RAbstractVector) {
                    appendVector((RAbstractVector) value);
                } else if (value instanceof RNull) {
                    append("NULL");
                } else if (value instanceof RFunction) {
                    RFunction f = (RFunction) value;
                    if (f.isBuiltin()) {
                        append(".Primitive(\"").append(f.getName()).append("\")");
                    } else {
                        append(RContext.getRRuntimeASTAccess().getSyntaxFunction(f));
                    }
                } else if (value instanceof RPairList) {
                    RPairList pl = (RPairList) value;
                    assert pl.getType() == SEXPTYPE.LISTSXP;
                    append("pairlist(");
                    Arguments<RSyntaxElement> arguments = wrapArguments(pl);
                    appendArgs(arguments.getSignature(), arguments.getArguments(), 0, false);
                    append(')');
                } else if (value instanceof RS4Object) {
                    RS4Object s4Obj = (RS4Object) value;
                    Object clazz = s4Obj.getAttr("class");
                    String className = clazz == null ? "S4" : RRuntime.toString(RRuntime.asStringLengthOne(clazz));
                    append("new(\"").append(className).append('\"');
                    try (C c = indent()) {
                        printline();
                        if (s4Obj.getAttributes() != null) {
                            for (RAttribute att : s4Obj.getAttributes()) {
                                if (!"class".equals(att.getName())) {
                                    append(", ").append(att.getName()).append(" = ").process(att.getValue()).printline();
                                }
                            }
                        }
                    }
                    append(')');
                } else if (value instanceof RExternalPtr) {
                    append("<pointer: 0x").append(Long.toHexString(((RExternalPtr) value).getAddr())).append('>');
                } else if (value instanceof REnvironment) {
                    append("<environment>");
                } else {
                    throw RInternalError.shouldNotReachHere("unexpected: " + value);
                }
                return null;
            }

            @Override
            protected Void visit(RSyntaxLookup lookup) {
                if (!backtick || isValidName(lookup.getIdentifier())) {
                    append(lookup.getIdentifier());
                } else {
                    append(quotify(lookup.getIdentifier(), BACKTICK));
                }
                return null;
            }

            @Override
            protected Void visit(RSyntaxFunction function) {
                append("function (");
                appendArgs(function.getSyntaxSignature(), function.getSyntaxArgumentDefaults(), 0, true);
                append(") ");
                RSyntaxElement body = function.getSyntaxBody();
                boolean newline = true;
                if (body instanceof RSyntaxCall) {
                    RSyntaxCall c = (RSyntaxCall) body;
                    if (c.getSyntaxLHS() instanceof RSyntaxLookup) {
                        RSyntaxLookup l = (RSyntaxLookup) c.getSyntaxLHS();
                        if ("{".equals(l.getIdentifier())) {
                            newline = c.getSyntaxArguments().length == 1 && !hasBraces(c);
                        }

                    }
                }
                if (newline) {
                    printline();
                }
                append(body);
                return null;
            }
        }

        private void appendWithParens(RSyntaxElement arg, PPInfo mainOp, boolean isLeft) {
            Func func = isInfixOperatorNode(arg);
            boolean needsParens = false;
            if (func != null) {
                PPInfo arginfo = func.info;
                switch (arginfo.kind) {
                    case BINARY:
                    case BINARY2:
                        RSyntaxElement[] subArgs = ((RSyntaxCall) arg).getSyntaxArguments();
                        if (subArgs.length == 1) {
                            if (isLeft && arginfo.prec == RDeparse.PREC_SUM) {
                                arginfo = arginfo.changePrec(RDeparse.PREC_SIGN);
                            }
                        } else if (subArgs.length == 2) {
                            needsParens = checkPrec(mainOp, arginfo, isLeft);
                        }
                        break;
                    case UNARY:
                        needsParens = mainOp.prec > arginfo.prec || (mainOp.prec == arginfo.prec && isLeft == mainOp.rightassoc);
                        break;
                    default:
                        break;
                }
            }
            if (needsParens) {
                append('(');
                append(arg);
                append(')');
            } else {
                append(arg);
            }
        }

        private static boolean checkPrec(PPInfo mainop, PPInfo arginfo, boolean left) {
            return mainop.prec > arginfo.prec || (mainop.prec == arginfo.prec && left == mainop.rightassoc);
        }

        private DeparseVisitor appendArgs(ArgumentsSignature signature, RSyntaxElement[] args, int start, boolean formals) {
            boolean lbreak = false;
            for (int i = start; i < args.length; i++) {
                lbreak = linebreak(lbreak);
                RSyntaxElement argument = args[i];
                if (argument instanceof RSyntaxLookup && ((RSyntaxLookup) argument).getIdentifier().isEmpty()) {
                    argument = null;
                }
                if (argument instanceof RSyntaxConstant && ((RSyntaxConstant) argument).getValue() instanceof REmpty) {
                    argument = null;
                }
                String name = signature.getName(i);
                if (name != null && name.isEmpty()) {
                    name = null;
                }
                if (name != null) {
                    if (isValidName(name)) {
                        append(name);
                    } else {
                        append(quotify(name, BACKTICK));
                    }
                    if (!formals || argument != null) {
                        append(" = ");
                    }
                }
                if (argument != null) {
                    append(argument);
                }
                if (i != args.length - 1) {
                    append(", ");
                }
            }
            if (lbreak) {
                indent--;
            }
            return this;
        }

        private DeparseVisitor process(Object v) {
            assert v != null;
            assert RRuntime.asAbstractVector(v) instanceof RTypedValue : v.getClass();
            assert !(v instanceof RSyntaxElement) : v.getClass();

            RSyntaxElement element = wrap(v, false);
            if (!quoteExpressions() || element instanceof RSyntaxConstant) {
                append(element);
            } else {
                append("quote(");
                append(element);
                append(')');
            }
            return this;
        }

        private static RSyntaxElement wrap(Object v, boolean isCallLHS) {
            Object value = RRuntime.asAbstractVector(v);
            if (value instanceof RSymbol) {
                return RSyntaxLookup.createDummyLookup(null, ((RSymbol) value).getName(), isCallLHS);
            } else if (value instanceof RLanguage) {
                return ((RLanguage) value).getRep().asRSyntaxNode();
            } else if (value instanceof RPairList) {
                RPairList pl = (RPairList) value;
                switch (pl.getType()) {
                    case LANGSXP:
                        return wrapCall(pl);
                    case CLOSXP:
                        return wrapFunctionExpression(pl);
                    default:
                        throw RInternalError.shouldNotReachHere("sexptype: " + pl.getType());
                }
            } else if (value instanceof RMissing) {
                return RSyntaxLookup.createDummyLookup(null, "", false);
            } else {
                return RSyntaxConstant.createDummyConstant(null, value);
            }
        }

        private static RSyntaxElement wrapCall(RPairList pl) {
            Object car = pl.car();
            if (car instanceof RSymbol && ((RSymbol) car).getName().equals("function")) {
                RPairList fun = (RPairList) pl.cdr();
                return wrapFunctionExpression(fun);
            }
            RSyntaxElement lhs = wrap(car, true);

            Arguments<RSyntaxElement> args = wrapArguments(pl.cdr());
            return RSyntaxCall.createDummyCall(null, lhs, args.getSignature(), args.getArguments());
        }

        private static RSyntaxElement wrapFunctionExpression(RPairList fun) {
            Arguments<RSyntaxElement> args = wrapArguments(fun.car());
            RSyntaxElement body;
            Object cdr = fun.cdr();
            if (cdr instanceof RPairList) {
                RPairList pl = (RPairList) cdr;
                if (pl.getType() == SEXPTYPE.BCODESXP) {
                    RAbstractListVector list = (RAbstractListVector) fun.cddr();
                    body = wrap(list.getDataAtAsObject(0), false);
                } else if (pl.getType() == SEXPTYPE.LISTSXP) {
                    assert pl.cdr() == RNull.instance || (pl.cadr() == RNull.instance && pl.cddr() == RNull.instance);
                    body = wrap(pl.car(), false);
                } else {
                    assert pl.getType() == SEXPTYPE.LANGSXP;
                    body = wrap(pl, false);
                }
            } else {
                body = wrap(cdr, false);
            }

            return RSyntaxFunction.createDummyFunction(null, args.getSignature(), args.getArguments(), body);
        }

        private static Arguments<RSyntaxElement> wrapArguments(Object args) {
            RPairList arglist = args instanceof RNull ? null : (RPairList) args;
            ArrayList<RSyntaxElement> argElements = new ArrayList<>();
            ArrayList<String> argNames = new ArrayList<>();
            while (arglist != null) {
                Object argTag = arglist.getTag();
                if (argTag != null && argTag != RNull.instance) {
                    String rs = ((RSymbol) arglist.getTag()).getName();
                    argNames.add(rs);
                } else {
                    argNames.add(null);
                }
                argElements.add(wrap(arglist.car(), false));
                arglist = next(arglist);
            }
            RSyntaxElement[] arguments = argElements.toArray(new RSyntaxElement[argElements.size()]);
            ArgumentsSignature signature = ArgumentsSignature.get(argNames.toArray(new String[argNames.size()]));
            Arguments<RSyntaxElement> arg = new Arguments<>(arguments, signature);
            return arg;
        }

        private static RPairList next(RPairList pairlist) {
            if (pairlist.cdr() == RNull.instance) {
                return null;
            } else {
                return (RPairList) pairlist.cdr();
            }
        }

        private void appendVector(RAbstractVector vec) {
            SEXPTYPE type = SEXPTYPE.typeForClass(vec.getClass());
            int len = vec.getLength();
            if (len == 0) {
                append(vec.getRType().getClazz() + "(0)");
            } else if (len == 1) {
                vecElement2buff(type, vec.getDataAtAsObject(0), true);
            } else {
                RIntSequence sequence = asIntSequence(vec);
                if (sequence != null) {
                    append(RRuntime.intToStringNoCheck(sequence.getStart())).append(':').append(RRuntime.intToStringNoCheck(sequence.getEnd()));
                    return;
                } else {
                    // TODO COMPAT?
                    append("c(");
                    for (int i = 0; i < len; i++) {
                        Object element = vec.getDataAtAsObject(i);
                        vecElement2buff(type, element, false);
                        if (i < len - 1) {
                            append(", ");
                        }
                    }
                    append(')');
                }
            }
        }

        private static RIntSequence asIntSequence(RAbstractVector vec) {
            if (!(vec instanceof RAbstractIntVector)) {
                return null;
            }
            RAbstractIntVector intVec = (RAbstractIntVector) vec;
            if (vec instanceof RIntSequence) {
                return (RIntSequence) vec;
            }
            assert vec.getLength() >= 2;
            int start = intVec.getDataAt(0);
            if (RRuntime.isNA(start)) {
                return null;
            }
            for (int i = 1; i < vec.getLength(); i++) {
                int next = intVec.getDataAt(i);
                if (RRuntime.isNA(next) || next != start + i) {
                    return null;
                }
            }
            return RDataFactory.createIntSequence(start, 1, intVec.getLength());
        }

        private DeparseVisitor vecElement2buff(SEXPTYPE type, Object element, boolean singleElement) {
            switch (type) {
                case STRSXP:
                    String s = (String) element;
                    append(RRuntime.isNA(s) ? (singleElement ? "NA_character_" : "NA") : RRuntime.quoteString((String) element, true));
                    break;
                case LGLSXP:
                    append(RRuntime.logicalToString((byte) element));
                    break;
                case REALSXP:
                    double d = (double) element;
                    append(RRuntime.isNA(d) ? (singleElement ? "NA_real_" : "NA") : encodeReal(d));
                    break;
                case INTSXP:
                    int i = (int) element;
                    if (RRuntime.isNA(i)) {
                        append((singleElement ? "NA_integer_" : "NA"));
                    } else {
                        append(RRuntime.intToStringNoCheck(i)).append('L');
                    }
                    break;
                case CPLXSXP:
                    RComplex c = (RComplex) element;
                    if (RRuntime.isNA(c)) {
                        append((singleElement ? "NA_complex_" : "NA"));
                    } else {
                        append(encodeReal(c.getRealPart()));
                        if (c.getImaginaryPart() >= 0) {
                            append('+');
                        }
                        append(encodeReal(c.getImaginaryPart())).append('i');
                    }
                    break;
                default:
                    throw RInternalError.shouldNotReachHere("unexpected SEXPTYPE: " + type);
            }
            return this;
        }

        /**
         * Handles {@link RList}, (@link RExpression}, and {@link RFactor}. Method name same as
         * GnuR.
         */
        private DeparseVisitor appendListContents(RAbstractListVector v) {
            int n = v.getLength();
            boolean lbreak = false;
            Object names = v.getNames();
            RStringVector snames = names == RNull.instance ? null : (RStringVector) names;
            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    append(", ");
                }
                lbreak = linebreak(lbreak);
                String sname = snames == null ? null : snames.getDataAt(i);
                if (snames != null && ((sname = snames.getDataAt(i)) != null)) {
                    append(sname);
                    append(" = ");
                }
                append(wrap(v.getDataAtAsObject(i), false));
            }
            if (lbreak) {
                indent--;
            }
            return this;
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

        private C withAttributes(Object obj) {
            if (showAttributes() && hasAttributes(obj)) {
                append("structure(");
                return () -> {
                    RAttributes attrs = ((RAttributable) obj).getAttributes();
                    if (attrs != null) {
                        Iterator<RAttribute> iter = attrs.iterator();
                        while (iter.hasNext()) {
                            RAttribute attr = iter.next();
                            // TODO ignore function source attribute
                            String attrName = attr.getName();
                            append(", ");
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
                                    opts = SIMPLEDEPARSE;
                                    if (attrName.contains(" ")) {
                                        append('"');
                                        append(attrName);
                                        append('"');
                                    } else {
                                        append(attrName);
                                    }
                                }

                            }
                            if (dotName != null) {
                                append(dotName);
                            }
                            append(" = ");
                            process(attr.getValue());
                            append(')');
                        }
                    }
                };
            } else {
                return () -> {
                };
            }
        }
    }

    /**
     * Version for use by {@code RSerialize} to convert a CLOSXP/LANGSXP/PROMSXP into a parseable
     * string.
     */
    @TruffleBoundary
    public static String deparseDeserialize(Object obj) {
        Object root = obj;
        if (root instanceof RPairList) {
            RPairList pl = (RPairList) root;
            if (pl.getType() == SEXPTYPE.BCODESXP) {
                RAbstractListVector list = (RAbstractListVector) pl.cdr();
                root = list.getDataAtAsObject(0);
            }
        }
        return new DeparseVisitor(false, 80, true, 0, -1).process(root).getContents();
    }

    @TruffleBoundary
    public static String deparseSyntaxElement(RSyntaxElement element) {
        return new DeparseVisitor(false, RDeparse.MAX_Cutoff, true, 0, -1).append(element).getContents();
    }

    @TruffleBoundary
    public static String deparse(Object expr) {
        return new DeparseVisitor(false, RDeparse.MAX_Cutoff, true, 0, -1).process(expr).getContents();
    }

    @TruffleBoundary
    public static String deparse(Object expr, int cutoff, boolean backtick, int opts, int nlines) {
        return new DeparseVisitor(false, cutoff, backtick, opts, nlines).process(expr).getContents();
    }

    // TODO: this should use the DoubleVectorPrinter

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

    private static String encodeReal(double x) {
        double d = RRuntime.normalizeZero(x);
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

    private static String quotify(String name, char qc) {
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

    private static final HashSet<String> keywords = new HashSet<>(Arrays.asList("NULL", "NA", "TRUE", "FALSE", "Inf", "NaN", "NA_integer_", "NA_real_", "NA_character_", "NA_complex_", "function",
                    "while", "repeat", "for", "if", "in", "else", "next", "break", "..."));

    private static boolean isKeyword(String name) {
        return keywords.contains(name);
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
}
