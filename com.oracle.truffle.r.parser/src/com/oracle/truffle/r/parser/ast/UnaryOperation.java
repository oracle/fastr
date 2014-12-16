/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.parser.ast;

import com.oracle.truffle.api.source.*;

public abstract class UnaryOperation extends Operation {

    public UnaryOperation(SourceSection source, ASTNode op) {
        super(source, op);
    }

    public static ASTNode create(SourceSection source, UnaryOperator op, ASTNode operand) {
        switch (op) {
            case PLUS:
                return new UnaryPlus(source, operand);
            case MINUS:
                return new UnaryMinus(source, operand);
            case NOT:
                return new Not(source, operand);
        }
        throw new Error("No node implemented for: '" + op + "' (" + operand + ")");
    }

    public enum UnaryOperator {
        REPEAT,
        PLUS,
        MINUS,
        NOT
    }
}
