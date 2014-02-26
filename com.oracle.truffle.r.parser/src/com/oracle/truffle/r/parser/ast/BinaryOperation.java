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

import java.util.*;

import com.oracle.truffle.api.*;

public abstract class BinaryOperation extends Operation {

    final ASTNode rhs;

    public BinaryOperation(SourceSection src, ASTNode left, ASTNode right) {
        super(left);
        this.source = src;
        this.rhs = updateParent(right);
    }

    public ASTNode getRHS() {
        return rhs;
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        return Arrays.asList(getLHS().accept(v), getRHS().accept(v));
    }

    public static ASTNode create(SourceSection src, BinaryOperator op, ASTNode left, ASTNode right) {
        switch (op) {
            case ADD:
                return new Add(src, left, right);
            case SUB:
                return new Sub(src, left, right);
            case MULT:
                return new Mult(src, left, right);
            case DIV:
                return new Div(src, left, right);
            case MOD:
                return new Mod(src, left, right);
            case POW:
                return new Pow(src, left, right);

            case OR:
                return new Or(src, left, right);
            case AND:
                return new And(src, left, right);
            case ELEMENTWISEOR:
                return new ElementwiseOr(src, left, right);
            case ELEMENTWISEAND:
                return new ElementwiseAnd(src, left, right);

            case EQ:
                return new EQ(src, left, right);
            case GE:
                return new GE(src, left, right);
            case GT:
                return new GT(src, left, right);
            case NE:
                return new NE(src, left, right);
            case LE:
                return new LE(src, left, right);
            case LT:
                return new LT(src, left, right);
            case COLON:
                return new Colon(src, left, right);
        }
        throw new Error("No node implemented for: '" + op + "' (" + left + ", " + right + ")");
    }

    public static ASTNode create(SourceSection src, String op, ASTNode left, ASTNode right) {
        if ("%o%".equals(op)) {
            return new OuterMult(src, left, right);
        }
        if ("%*%".equals(op)) {
            return new MatMult(src, left, right);
        }
        if ("%/%".equals(op)) {
            return new IntegerDiv(src, left, right);
        }
        if ("%in%".equals(op)) {
            return new In(src, left, right);
        }
        // user-defined operator
        ArgumentList args = new ArgumentList.Default();
        args.add(left);
        args.add(right);
        return new FunctionCall(src, Symbol.getSymbol(op), args);
    }

    public enum BinaryOperator {
        ASSIGN, SUPER_ASSIGN,

        ADD, SUB, MULT, DIV, MOD,

        POW,

        MODEL, COLON,

        GE, GT, LE, LT, EQ, NE,

        OR, ELEMENTWISEOR, AND, ELEMENTWISEAND,
    }
}
