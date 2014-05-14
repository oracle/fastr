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
package com.oracle.truffle.r.parser.tools;

import com.oracle.truffle.r.parser.ast.*;

public class BasicVisitor<R> implements Visitor<R> {

    public R visit(ASTNode n) {
        n.visitAll(this);
        return null;
    }

    @Override
    public R visit(Sequence n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(If n) {
        return visit((ASTNode) n);
    }

    public R visit(Loop n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Repeat n) {
        return visit((Loop) n);
    }

    @Override
    public R visit(While n) {
        return visit((Loop) n);
    }

    @Override
    public R visit(For n) {
        return visit((Loop) n);
    }

    public R visit(ControlStatement n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Next n) {
        return visit((ControlStatement) n);
    }

    @Override
    public R visit(Break n) {
        return visit((ControlStatement) n);
    }

    public R visit(Operation op) {
        return visit((ASTNode) op);
    }

    public R visit(BinaryOperation op) {
        return visit((Operation) op);
    }

    @Override
    public R visit(EQ op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(NE op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(LE op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(GE op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(LT op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(GT op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(Add op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(Sub op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(Mult op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(MatMult op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(OuterMult op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(IntegerDiv op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(In op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(Mod op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(Pow op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(Div op) {
        return visit((BinaryOperation) op);
    }

    @Override
    public R visit(Colon col) {
        return visit((BinaryOperation) col);
    }

    @Override
    public R visit(And and) {
        return visit((BinaryOperation) and);
    }

    @Override
    public R visit(ElementwiseAnd and) {
        return visit((BinaryOperation) and);
    }

    @Override
    public R visit(Or or) {
        return visit((BinaryOperation) or);
    }

    @Override
    public R visit(ElementwiseOr or) {
        return visit((BinaryOperation) or);
    }

    public R visit(UnaryOperation op) {
        return visit((ASTNode) op);
    }

    @Override
    public R visit(Not op) {
        return visit((UnaryOperation) op);
    }

    @Override
    public R visit(UnaryMinus op) {
        return visit((UnaryOperation) op);
    }

    @Override
    public R visit(Constant c) {
        return visit((ASTNode) c);
    }

    @Override
    public R visit(SimpleAccessVariable n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(SimpleAccessTempVariable n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(FieldAccess n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(SimpleAssignVariable n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(Replacement n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(UpdateVector u) {
        return visit((ASTNode) u);
    }

    @Override
    public R visit(UpdateField u) {
        return visit((ASTNode) u);
    }

    public R visit(Call n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(FunctionCall n) {
        return visit((Call) n);
    }

    @Override
    public R visit(AccessVector a) {
        return visit((Call) a);
    }

    @Override
    public R visit(Function n) {
        return visit((ASTNode) n);
    }

    @Override
    public R visit(ArgNode n) {
        return visit((ASTNode) n);
    }

}
