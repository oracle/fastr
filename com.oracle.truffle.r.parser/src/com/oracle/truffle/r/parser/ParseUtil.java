/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser;

import java.util.List;

import org.antlr.runtime.*;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.parser.ast.*;

public class ParseUtil {

    static String hexChar(String... chars) {
        int value = 0;
        for (int i = 0; i < chars.length; i++) {
            value = value * 16 + Integer.parseInt(chars[i], 16);
        }
        return new String(new int[]{value}, 0, 1);
    }

    static String octChar(String... chars) {
        int value = 0;
        for (int i = 0; i < chars.length; i++) {
            value = value * 8 + Integer.parseInt(chars[i], 8);
        }
        value &= 0xff; // octal escape sequences are clamped the 0-255 range
        return new String(new int[]{value}, 0, 1);
    }

    public static List<ASTNode> parseAST(ANTLRStringStream stream, Source source) throws RecognitionException {
        CommonTokenStream tokens = new CommonTokenStream();
        tokens.setTokenSource(new RLexer(stream));
        RParser parser = new RParser(tokens);
        parser.initialize(source, new RASTBuilder());
        return parser.script();
    }

    public static Sequence parseAsSequence(ANTLRStringStream antlrStringStream, Source source) throws RecognitionException {
        List<ASTNode> list = parseAST(antlrStringStream, source);
        return new Sequence(source.createSection(null, 0, source.getLength()), list.toArray(new ASTNode[list.size()]));
    }

}
