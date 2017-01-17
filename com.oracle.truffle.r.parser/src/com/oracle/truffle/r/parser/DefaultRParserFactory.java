/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.parser;

import java.util.List;

import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.RParserFactory;
import com.oracle.truffle.r.runtime.context.Engine.IncompleteSourceException;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;

public class DefaultRParserFactory extends RParserFactory {

    private static class DefaultParser<T> implements Parser<T> {

        @Override
        public List<T> script(Source source, RCodeBuilder<T> builder) throws ParseException {
            try {
                try {
                    RParser<T> parser = new RParser<>(source, builder);
                    return parser.script();
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
        public RootCallTarget rootFunction(Source source, String name, RCodeBuilder<T> builder) throws ParseException {
            RParser<T> parser = new RParser<>(source, builder);
            try {
                return parser.root_function(name);
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
            return ((RecognitionException) t).line;
        }

        @Override
        public int charPositionInLine(Throwable t) {
            assert isRecognitionException(t);
            return ((RecognitionException) t).charPositionInLine;
        }

        private static ParseException handleRecognitionException(Source source, RecognitionException e) throws IncompleteSourceException, ParseException {
            String line = e.line <= source.getLineCount() ? source.getCode(e.line) : "";
            String substring = line.substring(0, Math.min(line.length(), e.charPositionInLine + 1));
            String token = e.token == null ? (substring.length() == 0 ? "" : substring.substring(substring.length() - 1)) : e.token.getText();
            if (e.token != null && e.token.getType() == Token.EOF && (e instanceof NoViableAltException || e instanceof MismatchedTokenException)) {
                // the parser got stuck at the eof, request another line
                throw new IncompleteSourceException(e, source, token, substring, e.line);
            } else {
                throw new ParseException(e, source, token, substring, e.line);
            }
        }
    }

    @Override
    protected <T> Parser<T> createParser() {
        return new DefaultParser<>();
    }
}
