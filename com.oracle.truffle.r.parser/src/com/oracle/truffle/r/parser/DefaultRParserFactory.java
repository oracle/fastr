/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.parser;

import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.parser.RParser.ScriptContext;
import com.oracle.truffle.r.runtime.RParserFactory;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public class DefaultRParserFactory extends RParserFactory {

    public static final class ThrowImmediatelyErrorListener extends BaseErrorListener {

        public static final ThrowImmediatelyErrorListener INSTANCE = new ThrowImmediatelyErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static class DefaultParser implements Parser {

        @Override
        public List<RSyntaxNode> script(Source source, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language) throws ParseException {
            try {
                ScriptContext script;
                try {
                    RContext context = language.getContextReference().get();
                    RLexer lexer = new RLexer(CharStreams.fromString(source.getCharacters().toString()));
                    RParser parser = new RParser(source, lexer, builder, language, context.sourceCache);
                    parser.removeErrorListeners();
                    parser.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
                    lexer.removeErrorListeners();
                    lexer.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
                    parser.setBuildParseTree(false);
                    script = parser.script();
                } catch (IllegalArgumentException e) {
                    // the lexer will wrap exceptions in IllegalArgumentExceptions
                    if (e.getCause() instanceof RecognitionException) {
                        throw (RecognitionException) e.getCause();
                    } else {
                        throw e;
                    }
                } catch (Throwable t) {
                    throw t;
                }
                return script.v;
            } catch (RecognitionException e) {
                throw handleRecognitionException(source, e);
            }
        }

        @Override
        public List<RSyntaxNode> statements(Source source, Source fullSource, int startLine, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language) throws ParseException {
            try {
                try {
                    RContext context = language.getContextReference().get();
                    RLexer lexer = new RLexer(CharStreams.fromString(source.getCharacters().toString()));
                    RParser parser = new RParser(source, lexer, fullSource, startLine, builder, language, context.sourceCache);
                    parser.removeErrorListeners();
                    parser.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
                    lexer.removeErrorListeners();
                    lexer.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
                    parser.setBuildParseTree(false);
                    return parser.script().v;
                } catch (IllegalArgumentException e) {
                    // the lexer will wrap exceptions in IllegalArgumentExceptions
                    if (e.getCause() instanceof RecognitionException) {
                        throw (RecognitionException) e.getCause();
                    } else {
                        throw e;
                    }
                }
            } catch (RecognitionException e) {
                throw handleRecognitionException(source, e);
            }
        }

        @Override
        public RootCallTarget rootFunction(Source source, String name, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language) throws ParseException {
            RContext context = language.getContextReference().get();
            RLexer lexer = new RLexer(CharStreams.fromString(source.getCharacters().toString()));
            RParser parser = new RParser(source, lexer, builder, language, context.sourceCache);
            parser.removeErrorListeners();
            parser.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
            lexer.removeErrorListeners();
            lexer.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
            parser.setBuildParseTree(false);
            try {
                return parser.root_function(name).v;
            } catch (RecognitionException e) {
                throw handleRecognitionException(source, e);
            }
        }

        @Override
        public boolean isRecognitionException(Throwable t) {
            return t instanceof RecognitionException;
        }

        @Override
        public int line(Throwable t) {
            assert isRecognitionException(t);
            return ((RecognitionException) t).getOffendingToken().getLine();
        }

        @Override
        public int charPositionInLine(Throwable t) {
            assert isRecognitionException(t);
            return ((RecognitionException) t).getOffendingToken().getCharPositionInLine();
        }

        private static ParseException handleRecognitionException(Source source, RecognitionException e) throws IncompleteSourceException, ParseException {
            Token token = e.getOffendingToken();
            if (token == null) {
                int start = e.getInputStream().index();
                int lineNumber = source.getLineNumber(start);
                CharSequence line = lineNumber <= source.getLineCount() ? source.getCharacters(lineNumber) : "";
                String substring = line.subSequence(0, Math.min(line.length(), start - source.getLineStartOffset(lineNumber) + 1)).toString();
                String contents = token == null ? (substring.length() == 0 ? "" : substring.substring(substring.length() - 1)) : token.getText();
                int lineNr = lineNumber > source.getLineCount() ? source.getLineCount() : lineNumber;
                if (e.getInputStream().LA(1) == Token.EOF && (e instanceof LexerNoViableAltException || e instanceof NoViableAltException || e instanceof InputMismatchException)) {
                    // the parser got stuck at the eof, request another line
                    throw new IncompleteSourceException(e, source, contents, substring, lineNr);
                } else {
                    throw new ParseException(e, source, contents, substring, lineNr);
                }
            } else {
                int lineNumber = token.getLine();
                CharSequence line = lineNumber <= source.getLineCount() ? source.getCharacters(lineNumber) : "";
                String substring = line.subSequence(0, Math.min(line.length(), token.getCharPositionInLine() + 1)).toString();
                String contents = token == null ? (substring.length() == 0 ? "" : substring.substring(substring.length() - 1)) : token.getText();
                int lineNr = lineNumber > source.getLineCount() ? source.getLineCount() : lineNumber;
                if (token.getType() == Token.EOF && (e instanceof NoViableAltException || e instanceof InputMismatchException)) {
                    // the parser got stuck at the eof, request another line
                    throw new IncompleteSourceException(e, source, contents, substring, lineNr);
                } else {
                    throw new ParseException(e, source, contents, substring, lineNr);
                }
            }
        }
    }

    @Override
    protected Parser createParser() {
        return new DefaultParser();
    }
}
