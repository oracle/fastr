/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.antlr.v4.runtime.ANTLRErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RParserFactory;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public class DefaultRParserFactory extends RParserFactory {

    public static final class ThrowImmediatelyANTSimulator extends ParserATNSimulator {

        public ThrowImmediatelyANTSimulator(org.antlr.v4.runtime.Parser parser, ATN atn, DFA[] decisionToDFA, PredictionContextCache sharedContextCache) {
            super(parser, atn, decisionToDFA, sharedContextCache);
        }

        public ThrowImmediatelyANTSimulator(ATN atn, DFA[] decisionToDFA, PredictionContextCache sharedContextCache) {
            super(atn, decisionToDFA, sharedContextCache);
        }

        @Override
        protected int getSynValidOrSemInvalidAltThatFinishedDecisionEntryRule(ATNConfigSet configs, ParserRuleContext outerContext) {
            return ATN.INVALID_ALT_NUMBER;
        }
    }

    private static final class ThrowImmediatelyErrorStrategy implements ANTLRErrorStrategy {

        private static final ThrowImmediatelyErrorStrategy INSTANCE = new ThrowImmediatelyErrorStrategy();

        @Override
        public void reset(org.antlr.v4.runtime.Parser recognizer) {
            // empty
        }

        @Override
        public Token recoverInline(org.antlr.v4.runtime.Parser recognizer) throws RecognitionException {
            throw new IllegalArgumentException(new NoViableAltException(recognizer));
        }

        @Override
        public void recover(org.antlr.v4.runtime.Parser recognizer, RecognitionException e) throws RecognitionException {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void sync(org.antlr.v4.runtime.Parser recognizer) throws RecognitionException {
            // empty
        }

        @Override
        public boolean inErrorRecoveryMode(org.antlr.v4.runtime.Parser recognizer) {
            return false;
        }

        @Override
        public void reportMatch(org.antlr.v4.runtime.Parser recognizer) {
            // empty
        }

        @Override
        public void reportError(org.antlr.v4.runtime.Parser recognizer, RecognitionException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private static final class ThrowImmediatelyErrorListener extends BaseErrorListener {

        public static final ThrowImmediatelyErrorListener INSTANCE = new ThrowImmediatelyErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            if (offendingSymbol instanceof Token) {
                throw new IllegalArgumentException(new NoViableAltException((org.antlr.v4.runtime.Parser) recognizer));
            } else {
                Lexer lexer = (Lexer) recognizer;
                throw new IllegalArgumentException(new LexerNoViableAltException(lexer, lexer.getInputStream(), lexer._tokenStartCharIndex, null));
            }
        }
    }

    private static class DefaultParser implements Parser {

        private static final boolean ASSERTIONS_ENABLED;

        static {
            boolean assertionsEnabled = false;
            assert (assertionsEnabled = true) == true;
            ASSERTIONS_ENABLED = assertionsEnabled;
        }

        @Override
        public List<RSyntaxNode> script(Source source, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language) throws ParseException {
            RContext context = RContext.getInstance();
            RLexer lexer = new RLexer(CharStreams.fromString(source.getCharacters().toString()));
            RParser parser = new RParser(source, lexer, builder, language, context.sourceCache);
            parser.removeErrorListeners();
            parser.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
            lexer.removeErrorListeners();
            lexer.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
            parser.setErrorHandler(ThrowImmediatelyErrorStrategy.INSTANCE);
            parser.setBuildParseTree(false);
            try {
                try {
                    return parser.script().v;
                } catch (IllegalArgumentException e) {
                    if (e.getCause() instanceof RecognitionException) {
                        throw (RecognitionException) e.getCause();
                    } else {
                        throw e;
                    }
                }
            } catch (RecognitionException e) {
                throw handleRecognitionException(source, e);
            } catch (StackOverflowError e) {
                handleStackOverflow(source);
                throw e;
            }
        }

        @Override
        public List<RSyntaxNode> statements(Source source, Source fullSource, int startLine, RCodeBuilder<RSyntaxNode> builder, TruffleRLanguage language) throws ParseException {
            RContext context = RContext.getInstance();
            RLexer lexer = new RLexer(CharStreams.fromString(source.getCharacters().toString()));
            RParser parser = new RParser(source, lexer, fullSource, startLine, builder, language, context.sourceCache);
            parser.removeErrorListeners();
            parser.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
            lexer.removeErrorListeners();
            lexer.addErrorListener(ThrowImmediatelyErrorListener.INSTANCE);
            parser.setErrorHandler(ThrowImmediatelyErrorStrategy.INSTANCE);
            parser.setBuildParseTree(false);
            try {
                try {
                    return parser.script().v;
                } catch (IllegalArgumentException e) {
                    if (e.getCause() instanceof RecognitionException) {
                        throw (RecognitionException) e.getCause();
                    } else {
                        throw e;
                    }
                }
            } catch (RecognitionException e) {
                throw handleRecognitionException(source, e);
            } catch (StackOverflowError e) {
                handleStackOverflow(source);
                throw e;
            }
        }

        private static void handleStackOverflow(Source source) {
            if (ASSERTIONS_ENABLED) {
                System.err.println("StackOverflowError during parsing of:\n");
                System.err.print(source.getCharacters());
                System.err.println("\n---------");
            }
        }

        private static ParseException handleRecognitionException(Source source, RecognitionException e) throws IncompleteSourceException, ParseException {
            Token token = e.getOffendingToken();
            if (token == null) {
                LexerNoViableAltException lexerException = (LexerNoViableAltException) e;
                int start = lexerException.getStartIndex();
                int lineNumber = source.getLineNumber(start);
                CharSequence line = lineNumber <= source.getLineCount() ? source.getCharacters(lineNumber) : "";
                String substring = line.subSequence(0, Math.min(line.length(), start - source.getLineStartOffset(lineNumber) + 1)).toString();
                String contents = token == null ? (substring.length() == 0 ? "" : substring.substring(substring.length() - 1)) : token.getText();
                int lineNr = lineNumber > source.getLineCount() ? source.getLineCount() : lineNumber;
                if (lexerException.getInputStream().LA(1) == IntStream.EOF) {
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
                if (token.getType() == Token.EOF) {
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
