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

import java.util.*;

import com.oracle.truffle.api.source.*;

public abstract class Operation extends ASTNode {

    static final int MIN_PRECEDENCE = 0;
    static final int MAX_PRECEDENCE = 100;

    private static final int EQ_PRECEDENCE = 1;
    private static final int OR_PRECEDENCE = EQ_PRECEDENCE + 1;
    private static final int AND_PRECEDENCE = OR_PRECEDENCE + 1;
    private static final int NOT_PRECEDENCE = AND_PRECEDENCE + 1;
    private static final int COMPARE_PRECEDENCE = NOT_PRECEDENCE + 1;

    private static final int ADD_PRECEDENCE = COMPARE_PRECEDENCE + 1;
    private static final int SUB_PRECEDENCE = ADD_PRECEDENCE;

    private static final int MULT_PRECEDENCE = SUB_PRECEDENCE + 1;

    private static final int MAT_MULT_PRECEDENCE = MULT_PRECEDENCE + 1;
    private static final int OUTER_MULT_PRECEDENCE = MAT_MULT_PRECEDENCE;
    private static final int INTEGER_DIV_PRECEDENCE = MAT_MULT_PRECEDENCE;
    private static final int IN_PRECEDENCE = MAT_MULT_PRECEDENCE;
    private static final int MOD_PRECEDENCE = MAT_MULT_PRECEDENCE;

    private static final int COLON_PRECEDENCE = MAT_MULT_PRECEDENCE + 1;
    private static final int SIGN_PRECEDENCE = COLON_PRECEDENCE + 1;
    private static final int POW_PRECEDENCE = SIGN_PRECEDENCE + 1;

    public interface Operator {
        String getName();

        boolean isUnary();
    }

    public enum ArithmeticOperator implements Operator {
        ADD("+", ADD_PRECEDENCE, false),
        SUB("-", SUB_PRECEDENCE, false),
        MULT("*", MULT_PRECEDENCE, false),
        DIV("/", MULT_PRECEDENCE, false),
        MOD("%%", MOD_PRECEDENCE, false),

        POW("^", POW_PRECEDENCE, false),

        COLON(":", COLON_PRECEDENCE, false),

        GE(">=", COMPARE_PRECEDENCE, false),
        GT(">", COMPARE_PRECEDENCE, false),
        LE("<=", COMPARE_PRECEDENCE, false),
        LT("<", COMPARE_PRECEDENCE, false),
        EQ("==", EQ_PRECEDENCE, false),
        NE("!=", EQ_PRECEDENCE, false),

        OR("||", OR_PRECEDENCE, false),
        ELEMENTWISEOR("|", OR_PRECEDENCE, false),
        AND("&&", AND_PRECEDENCE, false),
        ELEMENTWISEAND("&", AND_PRECEDENCE, false),

        OUTER_MULT("%o%", OUTER_MULT_PRECEDENCE, false),
        MATMULT("%*%", MAT_MULT_PRECEDENCE, false),
        INTEGER_DIV("%/%", INTEGER_DIV_PRECEDENCE, false),
        IN("%in%", IN_PRECEDENCE, false),

        UNARY_PLUS("+", SIGN_PRECEDENCE, true),
        UNARY_MINUS("-", SIGN_PRECEDENCE, true),
        UNARY_NOT("!", NOT_PRECEDENCE, true);

        private final String name;
        private final int precedence;
        private final boolean isUnary;

        ArithmeticOperator(String name, int precedence, boolean isUnary) {
            this.name = name;
            this.precedence = precedence;
            this.isUnary = isUnary;
        }

        public String getName() {
            return name;
        }

        public int getPrecedence() {
            return precedence;
        }

        public boolean isUnary() {
            return isUnary;
        }
    }

    private final ASTNode lhs;
    private final ArithmeticOperator op;

    protected Operation(SourceSection source, ArithmeticOperator op, ASTNode left) {
        super(source);
        this.op = op;
        this.lhs = left;
    }

    public ArithmeticOperator getOperator() {
        return op;
    }

    @Override
    public int getPrecedence() {
        return op.getPrecedence();
    }

    public ASTNode getLHS() {
        return lhs;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(getLHS().accept(v));
    }
}
