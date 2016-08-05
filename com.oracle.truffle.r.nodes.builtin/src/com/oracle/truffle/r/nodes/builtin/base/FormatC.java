/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "formatC", kind = INTERNAL, parameterNames = {"x", "mode", "width", "digits", "format", "flat", "i.strlen"}, behavior = PURE)
public abstract class FormatC extends RBuiltinNode {

    @Child private CastStringNode castStringNode;

    private RStringVector castStringVector(Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(true, true, true, false));
        }
        return (RStringVector) ((RStringVector) castStringNode.executeString(o)).copyDropAttributes();
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(2).toInteger(3).toInteger(6);
    }

    @SuppressWarnings("unused")
    @Specialization
    RAttributable formatC(RAbstractContainer x, RAbstractStringVector modeVec, RAbstractIntVector widthVec, RAbstractIntVector digitsVec, RAbstractStringVector formatVec,
                    RAbstractStringVector flagVec, RAbstractIntVector iStrlen) {
        RStringVector res = castStringVector(x);
        return res.setClassAttr(null);
    }
}
