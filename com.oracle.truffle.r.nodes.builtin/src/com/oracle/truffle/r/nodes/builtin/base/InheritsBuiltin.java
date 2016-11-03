/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.Message.NOT_CHARACTER_VECTOR;
import static com.oracle.truffle.r.runtime.RError.Message.NOT_LEN_ONE_LOGICAL_VECTOR;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.InheritsNode;
import com.oracle.truffle.r.nodes.unary.InheritsNodeGen;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "inherits", kind = INTERNAL, parameterNames = {"x", "what", "which"}, behavior = PURE)
public abstract class InheritsBuiltin extends RBuiltinNode {

    @Child InheritsNode inheritsNode = InheritsNodeGen.create();

    public abstract Object execute(Object x, Object what, Object which);

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("what").mustBe(stringValue(), NOT_CHARACTER_VECTOR, "what");
        casts.arg("which").mustBe(logicalValue(), NOT_LEN_ONE_LOGICAL_VECTOR, "which").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    protected Object doesInherit(Object x, RAbstractStringVector what, boolean which) {
        return inheritsNode.execute(x, what, which);
    }
}
