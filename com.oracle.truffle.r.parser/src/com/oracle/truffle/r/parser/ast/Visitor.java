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

public interface Visitor<R> {

    R visit(If iff);

    R visit(Repeat repeat);

    R visit(While wh1le);

    R visit(For n);

    R visit(Sequence sequence);

    R visit(Break n);

    R visit(Next n);

    R visit(EQ eq);

    R visit(NE ne);

    R visit(LE le);

    R visit(GE ge);

    R visit(LT lt);

    R visit(GT gt);

    R visit(Mult mult);

    R visit(MatMult mult);

    R visit(OuterMult mult);

    R visit(IntegerDiv div);

    R visit(In in);

    R visit(Mod mod);

    R visit(Pow pow);

    R visit(Div div);

    R visit(Add add);

    R visit(Sub sub);

    R visit(Colon col);

    R visit(And and);

    R visit(ElementwiseAnd and);

    R visit(Or or);

    R visit(ElementwiseOr or);

    R visit(Not n);

    R visit(UnaryMinus m);

    R visit(Constant constant);

    R visit(SimpleAccessVariable readVariable);

    R visit(SimpleAccessTempVariable readVariable);

    R visit(FieldAccess fieldAccess);

    R visit(SimpleAssignVariable assign);

    R visit(Replacement assign);

    R visit(UpdateVector update);

    R visit(UpdateField update);

    R visit(Function function);

    R visit(FunctionCall functionCall);

    R visit(AccessVector accessVector);

    R visit(ArgumentList.Default.DefaultEntry entry);
}
