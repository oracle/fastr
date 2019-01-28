/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder.RCodeToken;

public class TokensMap {
    private static final int MAX_TOKEN_ID = 100;
    public static final RCodeToken[] MAP = new RCodeToken[MAX_TOKEN_ID];

    static {
        try {
            Arrays.fill(MAP, RCodeToken.UNKNOWN);
            MAP[RParser.ARROW] = RCodeToken.LEFT_ASSIGN;
            MAP[RParser.SUPER_ARROW] = RCodeToken.LEFT_ASSIGN;
            MAP[RParser.RIGHT_ARROW] = RCodeToken.RIGHT_ASSIGN;
            MAP[RParser.SUPER_RIGHT_ARROW] = RCodeToken.RIGHT_ASSIGN;
            MAP[RParser.ASSIGN] = RCodeToken.EQ_ASSIGN;
            MAP[RParser.ELSE] = RCodeToken.ELSE;
            MAP[RParser.EQ] = RCodeToken.EQ;
            MAP[RParser.FOR] = RCodeToken.FOR;
            MAP[RParser.BREAK] = RCodeToken.BREAK;
            MAP[RParser.NEXT] = RCodeToken.NEXT;
            MAP[RParser.WHILE] = RCodeToken.WHILE;
            MAP[RParser.REPEAT] = RCodeToken.REPEAT;
            MAP[RParser.FUNCTION] = RCodeToken.FUNCTION;
            MAP[RParser.IF] = RCodeToken.IF;
            MAP[RParser.IN] = RCodeToken.IN;
            MAP[RParser.LBB] = RCodeToken.LBB;   // '[['
            MAP[RParser.NS_GET] = RCodeToken.NS_GET;
            MAP[RParser.NS_GET_INT] = RCodeToken.NS_GET_INT;
            MAP[RParser.OP] = RCodeToken.SPECIAL;    // %op%

            MAP[RParser.AND] = RCodeToken.AND2;
            MAP[RParser.ELEMENTWISEAND] = RCodeToken.AND;
            MAP[RParser.ELEMENTWISEOR] = RCodeToken.OR;
            MAP[RParser.OR] = RCodeToken.OR2;

            MAP[RParser.GE] = RCodeToken.GE;
            MAP[RParser.GT] = RCodeToken.GT;
            MAP[RParser.LT] = RCodeToken.LT;
            MAP[RParser.LE] = RCodeToken.LE;
            MAP[RParser.NE] = RCodeToken.NE;

            MAP[RParser.AT] = RCodeToken.AT;
            MAP[RParser.FIELD] = RCodeToken.FIELD;
            MAP[RParser.CARET] = RCodeToken.CARET;
            MAP[RParser.EXPONENT] = RCodeToken.EXPONENT;
            MAP[RParser.COLON] = RCodeToken.COLON;
            MAP[RParser.COMMA] = RCodeToken.COMMA;
            MAP[RParser.LPAR] = RCodeToken.LPAR;
            MAP[RParser.RPAR] = RCodeToken.RPAR;
            MAP[RParser.LBRACE] = RCodeToken.LBRACE;
            MAP[RParser.RBRACE] = RCodeToken.RBRACE;
            MAP[RParser.PLUS] = RCodeToken.PLUS;
            MAP[RParser.SEMICOLON] = RCodeToken.SEMICOLON;
            MAP[RParser.TILDE] = RCodeToken.TILDE;
            MAP[RParser.MINUS] = RCodeToken.MINUS;
            MAP[RParser.MULT] = RCodeToken.MULT;
            MAP[RParser.LBRAKET] = RCodeToken.LBRAKET;
            MAP[RParser.RBRAKET] = RCodeToken.RBRAKET;
            MAP[RParser.NOT] = RCodeToken.NOT;
            MAP[RParser.QM] = RCodeToken.QM;
            MAP[RParser.DIV] = RCodeToken.DIV;

            MAP[RParser.COMMENT] = RCodeToken.COMMENT;
            // Probably should not appear?
            // MAP[RParser.ESCAPE] = RCodeToken.ESCAPE;
            // MAP[RParser.ESC_SEQ] = RCodeToken.ESC_SEQ;

            MAP[RParser.COMPLEX] = RCodeToken.NUM_CONST;
            MAP[RParser.DOUBLE] = RCodeToken.NUM_CONST;
            MAP[RParser.INTEGER] = RCodeToken.NUM_CONST;
            MAP[RParser.FALSE] = RCodeToken.NUM_CONST;
            MAP[RParser.TRUE] = RCodeToken.NUM_CONST;
            MAP[RParser.HEX_DIGIT] = RCodeToken.NUM_CONST;
            MAP[RParser.INF] = RCodeToken.NUM_CONST;
            MAP[RParser.STRING] = RCodeToken.STR_CONST;
            MAP[RParser.NULL] = RCodeToken.NULL_CONST;

            MAP[RParser.NA] = RCodeToken.NUM_CONST;
            MAP[RParser.NACHAR] = RCodeToken.NUM_CONST; // really: NA_character_ is 'NUM_CONST'
            MAP[RParser.NACOMPL] = RCodeToken.NUM_CONST;
            MAP[RParser.NAINT] = RCodeToken.NUM_CONST;
            MAP[RParser.NAN] = RCodeToken.NUM_CONST;
            MAP[RParser.NAREAL] = RCodeToken.NUM_CONST;

            MAP[RParser.ID] = RCodeToken.SYMBOL;
            MAP[RParser.ID_NAME] = RCodeToken.SYMBOL;
            MAP[RParser.VARIADIC] = RCodeToken.SYMBOL;
            MAP[RParser.DD] = RCodeToken.SYMBOL;     // '..1'
            MAP[RParser.BACKTICK_NAME] = RCodeToken.SYMBOL;

            // Fragments: should not appear
            // MAP[RParser.HEX_EXPONENT] = RCodeToken.UNKNOWN;
            // MAP[RParser.HEX_ESC] = RCodeToken.UNKNOWN;
            // MAP[RParser.OCTAL_ESC] = RCodeToken.UNKNOWN;
            // MAP[RParser.OCT_DIGIT] = RCodeToken.UNKNOWN;
            // MAP[RParser.OP_NAME] = RCodeToken.UNKNOWN;
            // MAP[RParser.UNICODE_ESC] = RCodeToken.UNKNOWN;

            // Whitespace: should not appear
            // MAP[RParser.WS] = RCodeToken.UNKNOWN;
            // MAP[RParser.NEWLINE] = RCodeToken.UNKNOWN;
            // MAP[RParser.LINE_BREAK] = RCodeToken.UNKNOWN;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw RInternalError.shouldNotReachHere(ex, "TokensMap.MAX_TOKEN_ID is probably not large enough.");
        }
    }
}
