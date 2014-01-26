/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2013, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import com.oracle.truffle.api.*;

public abstract class UnaryOperation extends Operation {

    public UnaryOperation(SourceSection src, ASTNode op) {
        super(op);
        source = src;
    }

    public static ASTNode create(SourceSection src, UnaryOperator op, ASTNode operand) {
        switch (op) {
            case PLUS:
                return new Not(src, operand);
            case MINUS:
                return new UnaryMinus(src, operand);
            case NOT:
                return new Not(src, operand);
        }
        throw new Error("No node implemented for: '" + op + "' (" + operand + ")");
    }

    public enum UnaryOperator {
        REPEAT,

        PLUS, MINUS, NOT, MODEL
    }
}
