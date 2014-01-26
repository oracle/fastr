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

import java.util.*;

public abstract class Operation extends ASTNode {

    public static final int EQ_PRECEDENCE = 1;
    public static final int OR_PRECEDENCE = EQ_PRECEDENCE + 1;
    public static final int AND_PRECEDENCE = OR_PRECEDENCE + 1;
    public static final int NOT_PRECEDENCE = AND_PRECEDENCE + 1;
    public static final int COMPARE_PRECEDENCE = NOT_PRECEDENCE + 1;

    public static final int ADD_PRECEDENCE = COMPARE_PRECEDENCE + 1;
    public static final int SUB_PRECEDENCE = ADD_PRECEDENCE;

    public static final int MULT_PRECEDENCE = SUB_PRECEDENCE + 1;

    public static final int MAT_MULT_PRECEDENCE = MULT_PRECEDENCE + 1;
    public static final int OUTER_MULT_PRECEDENCE = MAT_MULT_PRECEDENCE;
    public static final int INTEGER_DIV_PRECEDENCE = MAT_MULT_PRECEDENCE;
    public static final int IN_PRECEDENCE = MAT_MULT_PRECEDENCE;
    public static final int MOD_PRECEDENCE = MAT_MULT_PRECEDENCE;

    public static final int COLON_PRECEDENCE = MAT_MULT_PRECEDENCE + 1;
    public static final int SIGN_PRECEDENCE = COLON_PRECEDENCE + 1;
    public static final int POW_PRECEDENCE = SIGN_PRECEDENCE + 1;

    final ASTNode lhs;

    public Operation(ASTNode left) {
        this.lhs = updateParent(left);
    }

    public ASTNode getLHS() {
        return lhs;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(getLHS().accept(v));
    }

    public String getPrettyOperator() {
        Class<?> clazz = getClass();
        PrettyName op = clazz.getAnnotation(PrettyName.class);
        return op == null ? clazz.getSimpleName() : op.value();
    }
}
