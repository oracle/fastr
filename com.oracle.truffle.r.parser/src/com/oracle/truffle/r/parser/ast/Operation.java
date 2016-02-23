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
package com.oracle.truffle.r.parser.ast;

import com.oracle.truffle.api.source.SourceSection;

public abstract class Operation extends ASTNode {

    public interface Operator {
        String getName();
    }

    public enum ArithmeticOperator implements Operator {
        ADD("+"),
        SUB("-"),
        MULT("*"),
        DIV("/"),
        MOD("%%"),

        POW("^"),

        COLON(":"),

        GE(">="),
        GT(">"),
        LE("<="),
        LT("<"),
        EQ("=="),
        NE("!="),

        OR("||"),
        ELEMENTWISEOR("|"),
        AND("&&"),
        ELEMENTWISEAND("&"),
        NOT("!"),

        OUTER_MULT("%o%"),
        MATMULT("%*%"),
        INTEGER_DIV("%/%"),
        IN("%in%");

        private final String name;

        ArithmeticOperator(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final ASTNode lhs;
    private final ArithmeticOperator op;
    private final SourceSection opSource;

    protected Operation(SourceSection source, SourceSection opSource, ArithmeticOperator op, ASTNode left) {
        super(source);
        this.lhs = left;
        this.op = op;
        this.opSource = opSource;
    }

    public final ArithmeticOperator getOperator() {
        return op;
    }

    public final SourceSection getOpSource() {
        return opSource;
    }

    public final ASTNode getLHS() {
        return lhs;
    }
}
