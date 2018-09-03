/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RError.Message.MUST_BE_STRING_OR_CONNECTION;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RSrcref;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.RConnection.ReadLineWarning;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

/**
 * Internal component of the {@code parse} base package function.
 *
 * <pre>
 * parse(file, n, text, prompt, srcfile, encoding)
 * </pre>
 *
 * There are two main modalities in the arguments:
 * <ul>
 * <li>Input is taken from "conn" or "text" (in which case conn==stdin(), but ignored).</li>
 * <li>Parse the entire input or just "n" "expressions". The FastR parser cannot handle the latter
 * case properly. It will parse the entire stream whereas GnuR stops after "n" expressions. So,
 * e.g., if there is a syntax error after the "n'th" expression, GnuR does not see it, whereas FastR
 * does and throws an error. However, if there is no error FastR can truncate the expressions vector
 * to length "n"</li>
 * </ul>
 * Despite the modality there is no value in multiple specializations for what is an inherently
 * slow-path builtin.
 * <p>
 * The inputs do not lend themselves to the correct creation of {@link Source} attributes for the
 * FastR AST. In particular the {@code source} builtin reads the input internally and calls us the
 * "text" variant. However useful information regarding the origin of the input can be found either
 * in the connection info or in the "srcfile" argument which, if not {@code RNull#instance} is an
 * {@link REnvironment} with relevant data. So we can fix up the {@link Source} attributes on the
 * AST after the parse. It's relevant to do this for the Truffle instrumentation framework.
 * <p>
 * On the R side, GnuR adds similar R attributes to the result, which is important for R tooling.
 */
@RBuiltin(name = "parse", kind = INTERNAL, parameterNames = {"conn", "n", "text", "prompt", "srcfile", "encoding"}, behavior = IO)
public abstract class Parse extends RBuiltinNode.Arg6 {

    @Child private SetFixedAttributeNode setSrcRefAttrNode = SetFixedAttributeNode.create(RRuntime.R_SRCREF);
    @Child private SetFixedAttributeNode setWholeSrcRefAttrNode = SetFixedAttributeNode.create(RRuntime.R_WHOLE_SRCREF);
    @Child private SetFixedAttributeNode setSrcFileAttrNode = SetFixedAttributeNode.create(RRuntime.R_SRCFILE);

    static {
        Casts casts = new Casts(Parse.class);
        // Note: string is captured by the R wrapper and transformed to a file, other types not
        casts.arg("conn").defaultError(MUST_BE_STRING_OR_CONNECTION, "file").mustNotBeNull().asIntegerVector().findFirst();
        casts.arg("n").asIntegerVector().findFirst(RRuntime.INT_NA).replaceNA(-1);
        casts.arg("text").mustNotBeMissing().asStringVector();
        casts.arg("prompt").asStringVector().findFirst("?");
        casts.arg("encoding").mustBe(stringValue()).asStringVector().findFirst();
    }

    @TruffleBoundary
    @Specialization
    protected RExpression parse(int conn, int n, @SuppressWarnings("unused") RNull text, String prompt, Object srcFile, String encoding) {
        String[] lines;
        RConnection connection = RConnection.fromIndex(conn);
        if (connection == StdConnections.getStdin()) {
            throw RError.nyi(this, "parse from stdin not implemented");
        }
        try (RConnection openConn = connection.forceOpen("r")) {
            lines = openConn.readLines(0, EnumSet.noneOf(ReadLineWarning.class), false);
        } catch (IOException ex) {
            throw error(RError.Message.PARSE_ERROR);
        }
        return doParse(connection, n, coalesce(lines), prompt, srcFile, encoding);
    }

    @TruffleBoundary
    @Specialization
    protected RExpression parse(int conn, int n, RAbstractStringVector text, String prompt, Object srcFile, String encoding) {
        RConnection connection = RConnection.fromIndex(conn);
        return doParse(connection, n, coalesce(text), prompt, srcFile, encoding);
    }

    private RExpression doParse(RConnection conn, int n, String coalescedLines, @SuppressWarnings("unused") String prompt, Object srcFile, @SuppressWarnings("unused") String encoding) {
        if (coalescedLines.length() == 0 || n == 0) {
            return RDataFactory.createExpression(new Object[0]);
        }
        try {
            Source source = srcFile != RNull.instance ? createSource(srcFile, coalescedLines) : createSource(conn, coalescedLines);
            RExpression exprs = RContext.getEngine().parse(source);
            if (n > 0 && n < exprs.getLength()) {
                Object[] subListData = new Object[n];
                for (int i = 0; i < n; i++) {
                    subListData[i] = exprs.getDataAt(i);
                }
                exprs = RDataFactory.createExpression(subListData);
            }
            // Handle the required R attributes
            if (srcFile instanceof REnvironment) {
                addAttributes(exprs, source, (REnvironment) srcFile);
            }
            return exprs;
        } catch (ParseException ex) {
            throw error(RError.Message.PARSE_ERROR);
        }
    }

    private static String coalesce(RAbstractStringVector lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.getLength(); i++) {
            sb.append(lines.getDataAt(i));
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String coalesce(String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Creates a {@link Source} object by gleaning information from {@code srcFile}.
     */
    private static Source createSource(Object srcFile, String coalescedLines) {
        if (srcFile instanceof REnvironment) {
            REnvironment srcFileEnv = (REnvironment) srcFile;
            Object b = srcFileEnv.get("isFile");
            boolean isFile = RRuntime.fromLogical(b != null ? (byte) b : 0);
            if (isFile) {
                // Might be a URL
                String urlFileName = RRuntime.asString(srcFileEnv.get("filename"));
                assert urlFileName != null;
                String fileName = ConnectionSupport.removeFileURLPrefix(urlFileName);
                /*
                 * N.B. GnuR compatibility problem: Truffle Source does not handle ~ in pathnames
                 * but GnuR does not appear to do tilde expansion
                 */
                fileName = Utils.tildeExpand(fileName);
                File fnf = new File(fileName);
                String path = null;
                if (!fnf.isAbsolute()) {
                    String wd = RRuntime.asString(srcFileEnv.get("wd"));
                    path = String.join(File.separator, wd, fileName);
                } else {
                    path = fileName;
                }
                Source result = createFileSource(path, coalescedLines, false);
                assert result != null : "Source created from environment should not be null";
                return result;
            } else {
                return Source.newBuilder(RRuntime.R_LANGUAGE_ID, coalescedLines, "<parse>").build();
            }
        } else {
            String srcFileText = RRuntime.asString(srcFile);
            if (srcFileText.equals("<text>")) {
                return Source.newBuilder(RRuntime.R_LANGUAGE_ID, coalescedLines, "<parse>").build();
            } else {
                return createFileSource(ConnectionSupport.removeFileURLPrefix(srcFileText), coalescedLines, false);
            }
        }
    }

    private static Source createSource(RConnection conn, String coalescedLines) {
        // TODO check if file
        ConnectionSupport.BaseRConnection bconn = ConnectionSupport.getBaseConnection(conn);
        String path = bconn.getSummaryDescription();
        return createFileSource(path, coalescedLines, bconn.isInternal());
    }

    private static Source createFileSource(String path, String chars, boolean internal) {
        try {
            return RSource.fromFileName(chars, path, internal);
        } catch (URISyntaxException e) {
            // Note: to be compatible with GnuR we construct Source even with a malformed path
            return Source.newBuilder(RRuntime.R_LANGUAGE_ID, chars, path).internal(internal).build();
        }
    }

    private void addAttributes(RExpression exprs, Source source, REnvironment srcFile) {
        Object[] srcrefData = new Object[exprs.getLength()];
        for (int i = 0; i < srcrefData.length; i++) {
            Object data = exprs.getDataAt(i);
            if ((data instanceof RPairList && ((RPairList) data).isLanguage())) {
                SourceSection ss = ((RPairList) data).getSourceSection();
                srcrefData[i] = RSrcref.createLloc(ss, srcFile);
            } else if (data instanceof RSymbol) {
                srcrefData[i] = RNull.instance;
            } else if (data == RNull.instance) {
                srcrefData[i] = data;
            } else if (data instanceof Number || data instanceof RComplex || data instanceof String) {
                // in simple cases, result of parsing can be a scalar constant
                srcrefData[i] = data;
            } else {
                throw RInternalError.unimplemented("attribute of type " + data.getClass().getSimpleName());
            }
        }
        setSrcRefAttrNode.execute(exprs, RDataFactory.createList(srcrefData));
        int[] wholeSrcrefData = new int[8];
        int endOffset = source.getCharacters().length() - 1;
        wholeSrcrefData[0] = source.getLineNumber(0);
        wholeSrcrefData[3] = source.getLineNumber(endOffset);
        source.getColumnNumber(0);
        wholeSrcrefData[6] = wholeSrcrefData[0];
        wholeSrcrefData[6] = wholeSrcrefData[3];

        setWholeSrcRefAttrNode.execute(exprs, RDataFactory.createIntVector(wholeSrcrefData, RDataFactory.COMPLETE_VECTOR));
        setSrcFileAttrNode.execute(exprs, srcFile);

        RIntVector parseData = new ParseDataVisitor(exprs).getParseData();
        srcFile.safePut("parseData", parseData);
    }

    /**
     * This class aspires to reconstruct the original parse tree through visiting a parsed
     * expression(s). The reconstruction cannot be done completely as some information is lost, but
     * the result should suffice for templating packages, such as knitr.
     */
    static class ParseDataVisitor extends RSyntaxVisitor<ParseDataVisitor.OctetNode> {

        static class OctetNode {
            static final List<OctetNode> NO_CHILDREN = java.util.Collections.emptyList();

            final int id;
            final int startLine;
            final int startColumn;
            final int endLine;
            final int endColumn;
            final TokenType tokenType;
            final String txt;
            final List<OctetNode> children;

            OctetNode(int id, int startLine, int startColumn, int endLine, int endColumn, TokenType tokenType, String txt, List<OctetNode> children) {
                this.id = id;
                this.startLine = startLine;
                this.startColumn = startColumn;
                this.endLine = endLine;
                this.endColumn = endColumn;
                this.tokenType = tokenType;
                this.txt = txt;
                this.children = children;
            }

            void store(ParseDataVisitor vis, int parentId) {
                for (OctetNode child : children) {
                    child.store(vis, id);
                }

                vis.data.add(startLine);
                vis.data.add(startColumn);
                vis.data.add(endLine);
                vis.data.add(endColumn);
                vis.data.add(tokenType.terminal ? 1 : 0);
                vis.data.add(tokenType.code);
                vis.data.add(id);
                vis.data.add(parentId);

                vis.tokens.add(tokenType.tokenName);
                vis.text.add(txt);
            }

        }

        private final RExpression exprs;

        /**
         * A list of parse data octets. Every octet corresponds to one term/non-term. The octet is
         * composed as follows:
         *
         * <pre>
         * line1 col1 line2 col2 terminal token id parent
         * </pre>
         *
         */
        private final List<Integer> data = new ArrayList<>();
        private boolean containsNA = false;

        private final List<String> tokens = new ArrayList<>();

        private final List<String> text = new ArrayList<>();

        private int idCounter = 0;

        ParseDataVisitor(RExpression exprs) {
            this.exprs = exprs;
        }

        /**
         * This enum mimics the <code>yytokentype</code> enum from <code>src/main/gram.c</code>.
         */
        enum TokenType {
            expr(77, false),
            IF(272, true, "if"),
            WHILE(274, true, "while"),
            FOR(270, true, "for"),
            IN(271, true, "in"),
            BREAK(276, true, "break"),
            REPEAT(277, true, "repeat"),
            NEXT(275, true, "next"),
            LEFT_ASSIGN(266, true, "<-"),
            EQ_ASSIGN(267, true, "="),
            RIGHT_ASSIGN(268, true, "->"),
            PLUS(43, true, "+", true, true),
            MINUS(45, true, "-", true, true),
            MULT(42, true, "*", true, true),
            DIV(47, true, "/", true, true),
            POW(94, true, "^", true, true),
            DOLLAR(36, true, "$", true, true),
            AT(64, true, "@", true, true),
            COLON(58, true, ":", true, true),
            QUESTION(63, true, "?", true, false),
            TILDE(126, true, "~", true, false),
            EXCLAMATION(33, true, "!", true, false),
            GT(278, true, ">"),
            GE(279, true, ">="),
            LT(280, true, "<"),
            LE(281, true, "<="),
            EQ(282, true, "=="),
            NE(283, true, "!="),
            AND(284, true, "&"),
            AND2(286, true, "&&"),
            OR(285, true, "|"),
            OR2(287, true, "||"),
            SYMBOL(263, true),
            SYMBOL_FUNCTION_CALL(296, true),
            SPECIAL(304, true),
            NULL_CONST(262, true),
            NUM_CONST(261, true),
            STR_CONST(260, true),
            FUNCTION(264, true),
            LPAREN(40, true, "(", true, false),
            RPAREN(41, true, ")", true, false);

            final int code;
            final boolean terminal;
            final String symbol;
            final String tokenName;
            final boolean infix;

            TokenType(int c, boolean term, String symbol, boolean useSymbolAsTokenName, boolean infix) {
                this.code = c;
                this.terminal = term;
                this.symbol = symbol;
                this.tokenName = useSymbolAsTokenName ? "'" + symbol + "'" : name();
                this.infix = infix;
            }

            TokenType(int c, boolean term, String symbol) {
                this(c, term, symbol, false, false);
            }

            TokenType(int c, boolean term) {
                this(c, term, null, false, false);
            }

            String getSymbol() {
                return symbol;
            }
        }

        private static final Map<String, TokenType> FUNCTION_TOKENS = Arrays.stream(TokenType.values()).filter(tt -> tt.symbol != null).collect(
                        Collectors.toMap(TokenType::getSymbol, Function.identity()));

        private OctetNode newOctet(RSyntaxElement element, TokenType tokenType, String txt, List<OctetNode> children) {
            return newOctet(element.getSourceSection().getStartLine(), element.getSourceSection().getStartColumn(), element.getSourceSection().getEndLine(), element.getSourceSection().getEndColumn(),
                            tokenType, txt, children);
        }

        private OctetNode newOctet(RSymbol symbol, List<OctetNode> children) {
            OctetNode octet = newOctet(RRuntime.INT_NA, RRuntime.INT_NA, RRuntime.INT_NA, RRuntime.INT_NA, TokenType.SYMBOL, symbol.getName(), children);
            containsNA = true;
            return octet;
        }

        private OctetNode newOctet(int startLine, int startColumn, int endLine, int endColumn, TokenType tokenType, String txt, List<OctetNode> children) {
            return new OctetNode(++idCounter, startLine, startColumn, endLine, endColumn, tokenType, txt, children);
        }

        @TruffleBoundary
        RIntVector getParseData() {
            int exprLen = exprs.getLength();
            List<OctetNode> rootOctets = new ArrayList<>();
            for (int i = 0; i < exprLen; i++) {
                Object x = exprs.getDataAt(i);
                if ((x instanceof RPairList && ((RPairList) x).isLanguage())) {
                    RSyntaxElement rep = ((RPairList) x).getSyntaxElement();
                    rootOctets.add(accept(rep));
                } else if (x instanceof RSymbol) {
                    rootOctets.add(newOctet((RSymbol) x, OctetNode.NO_CHILDREN));
                } else {
                    // TODO: primitives
                }
            }

            // Store the octet tree to the corresponding vectors
            for (OctetNode rootOctet : rootOctets) {
                rootOctet.store(this, 0);
            }

            int[] dataArray = new int[data.size()];
            for (int i = 0; i < dataArray.length; i++) {
                dataArray[i] = data.get(i);
            }
            RIntVector parseData = RDataFactory.createIntVector(dataArray, !containsNA);

            String[] textArray = new String[text.size()];
            for (int i = 0; i < textArray.length; i++) {
                textArray[i] = text.get(i);
            }

            String[] tokensArray = new String[tokens.size()];
            for (int i = 0; i < tokensArray.length; i++) {
                tokensArray[i] = tokens.get(i);
            }

            parseData.setAttr("tokens", RDataFactory.createStringVector(tokensArray, true));
            parseData.setAttr("text", RDataFactory.createStringVector(textArray, true));
            parseData.setClassAttr(RDataFactory.createStringVector("parseData"));
            parseData.setDimensions(new int[]{8, idCounter});

            return parseData;
        }

        @Override
        protected OctetNode visit(RSyntaxCall element) {
            LinkedList<OctetNode> children = new LinkedList<>();

            RSyntaxElement lhs = element.getSyntaxLHS();
            if (lhs instanceof RSyntaxCall) {
                children.add(accept(lhs));
                children.addAll(visitArguments(element, 0, Integer.MAX_VALUE));
            } else if (lhs instanceof RSyntaxLookup) {
                String symbol = ((RSyntaxLookup) lhs).getIdentifier();
                RDeparse.Func func = RDeparse.getFunc(symbol);
                TokenType tt = null;
                if (func != null) {
                    tt = FUNCTION_TOKENS.get(symbol);
                }
                if (tt == null) {
                    tt = TokenType.SYMBOL_FUNCTION_CALL;
                }
                if (tt.infix) {
                    children.addAll(visitArguments(element, 0, 1));
                    children.add(newOctet(lhs, tt, symbol, OctetNode.NO_CHILDREN));
                    children.addAll(visitArguments(element, 1, 2));
                } else {
                    children.add(newOctet(lhs, tt, symbol, OctetNode.NO_CHILDREN));
                    children.addAll(visitArguments(element, 0, Integer.MAX_VALUE));
                }
                if (tt == TokenType.LPAREN) {
                    OctetNode lastChild = children.getLast();
                    children.add(new OctetNode(++idCounter, lastChild.endLine, lastChild.endColumn + 1, lastChild.endLine, lastChild.endColumn + 1, TokenType.RPAREN, ")", OctetNode.NO_CHILDREN));
                }
            }

            return newOctet(element, TokenType.expr, "", children);
        }

        private List<OctetNode> visitArguments(RSyntaxCall element, int from, int to) {
            List<OctetNode> children = new ArrayList<>();
            RSyntaxElement[] args = element.getSyntaxArguments();
            for (int i = from; i < Math.min(args.length, to); i++) {
                if (args[i] != null) {
                    OctetNode argOctet = accept(args[i]);
                    if (argOctet != null) {
                        children.add(argOctet);
                    }
                }
            }
            return children;
        }

        @Override
        protected OctetNode visit(RSyntaxConstant element) {
            TokenType tt;
            Object value = element.getValue();
            if (value == RNull.instance) {
                tt = TokenType.NULL_CONST;
            } else if (value instanceof Number) {
                tt = TokenType.NUM_CONST;
            } else if (value instanceof String) {
                tt = TokenType.STR_CONST;
            } else if (value instanceof RComplex) {
                tt = TokenType.NUM_CONST;
            } else if (value == REmpty.instance || value == RMissing.instance) {
                return null;    // ignored
            } else {
                throw RInternalError.shouldNotReachHere("Unknown RSyntaxConstant in ParseDataVisitor " + (value == null ? "null" : value.getClass().getSimpleName()));
            }

            OctetNode constChild = newOctet(element, tt, element.getSourceSection().getCharacters().toString(), OctetNode.NO_CHILDREN);
            return newOctet(element, TokenType.expr, "", java.util.Collections.singletonList(constChild));
        }

        @Override
        protected OctetNode visit(RSyntaxLookup element) {
            String symbol = element.getIdentifier();
            return newOctet(element, TokenType.SYMBOL, symbol, OctetNode.NO_CHILDREN);
        }

        @Override
        protected OctetNode visit(RSyntaxFunction element) {
            List<OctetNode> children = new ArrayList<>();
            for (RSyntaxElement arg : element.getSyntaxArgumentDefaults()) {
                if (arg != null) {
                    children.add(accept(arg));
                }
            }

            children.add(accept(element.getSyntaxBody()));
            return newOctet(element, TokenType.FUNCTION, "function", children);
        }

    }
}
