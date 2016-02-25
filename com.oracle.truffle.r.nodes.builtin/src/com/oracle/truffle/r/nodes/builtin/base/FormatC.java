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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "formatC", kind = INTERNAL, parameterNames = {"x", "mode", "width", "digits", "format", "flat", "i.strlen"})
public abstract class FormatC extends RBuiltinNode {

    @Child private CastStringNode castStringNode;

    protected final BranchProfile errorProfile = BranchProfile.create();

    private RStringVector castStringVector(Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(true, true, true, false));
        }
        return (RStringVector) ((RStringVector) castStringNode.executeString(o)).copyDropAttributes();
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        // if (children.length != getSuppliedSignature().getLength()) {
        // errorProfile.enter();
        // throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENTS_PASSED,
        // children.length, ".Internal(formatC)", getSuppliedSignature().getLength());
        // }
        // // cast to vector as appropriate to eliminate NULL values
        // children[2] = CastIntegerNodeGen.create(CastToVectorNodeGen.create(children[2], false,
        // false,
        // false, false), false, false, false);
        // children[3] = CastIntegerNodeGen.create(CastToVectorNodeGen.create(children[3], false,
        // false,
        // false, false), false, false, false);
        // children[6] = CastIntegerNodeGen.create(CastToVectorNodeGen.create(children[6], false,
        // false,
        // false, false), false, false, false);
        casts.toInteger(2).toInteger(3).toInteger(6);
    }

    @SuppressWarnings("unused")
    @Specialization
    RAttributable formatC(RAbstractContainer x, RAbstractStringVector modeVec, RAbstractIntVector widthVec, RAbstractIntVector digitsVec, RAbstractStringVector formatVec,
                    RAbstractStringVector flagVec, RAbstractIntVector iStrlen) {
        RStringVector res = castStringVector(x);
        return res.setClassAttr(null, false);
    }
}
