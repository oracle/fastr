/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFrame;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

import static com.oracle.truffle.api.CompilerDirectives.SlowPath;
import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

@RBuiltin(name = "unclass", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class UnClass extends RBuiltinNode {
    private final BranchProfile objectProfile = BranchProfile.create();

    @Specialization
    @SlowPath
    protected Object unClass(RAbstractVector arg) {
        controlVisibility();
        if (arg.isObject()) {
            objectProfile.enter();
            RVector resultVector = arg.materialize();
            if (resultVector.isShared()) {
                resultVector = resultVector.copy();
            }
            return RVector.setClassAttr(resultVector, null, null);
        }
        return arg;
    }

    @Specialization
    @SlowPath
    protected Object unClass(RDataFrame arg) {
        controlVisibility();
        RDataFrame resultFrame = arg;
        if (resultFrame.isShared()) {
            resultFrame = resultFrame.copy();
        }
        return RVector.setClassAttr(resultFrame.getVector(), null, arg);
    }

}
