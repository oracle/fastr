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

public interface Visitor<R> {

    R visit(If iff);

    R visit(Repeat repeat);

    R visit(While wh1le);

    R visit(For n);

    R visit(Sequence sequence);

    R visit(Break n);

    R visit(Next n);

    R visit(BinaryOperation op);

    R visit(UnaryOperation op);

    R visit(Constant constant);

    R visit(AccessVariable readVariable);

    R visit(AccessVariadicComponent readVariable);

    R visit(AssignVariable assign);

    R visit(Replacement replacement);

    R visit(Function function);

    R visit(Call functionCall);

    R visit(ArgNode arg);

    R visit(Missing arg);
}
