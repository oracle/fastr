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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "unclass", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
public abstract class UnClass extends RBuiltinNode {
    private final BranchProfile objectProfile = BranchProfile.create();
    private final BranchProfile shareableProfile = BranchProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").asAttributable(true, true, true);
    }

    @Specialization
    protected RNull unClass(RNull rnull) {
        return rnull;
    }

    @TruffleBoundary
    private static Object unClassVector(RAbstractVector arg) {
        RVector resultVector = arg.materialize();
        if (!resultVector.isTemporary()) {
            resultVector = resultVector.copy();
            resultVector.incRefCount();
        }
        return RVector.setVectorClassAttr(resultVector, null);
    }

    // TODO: this specialization could go away if connections were simple vectors (we wouldn't need
    // special method for setting class attributes then)
    @Specialization
    protected Object unClass(RAbstractVector arg) {
        if (arg.isObject(attrProfiles)) {
            objectProfile.enter();
            return unClassVector(arg);
        }
        return arg;
    }

    @Specialization(guards = "notAbstractVector(arg)")
    protected Object unClass(RAttributable arg) {
        if (arg.getClassAttr(attrProfiles) != null) {
            objectProfile.enter();
            if (arg instanceof RShareable) {
                shareableProfile.enter();
                RShareable shareable = (RShareable) arg;
                if (!shareable.isTemporary()) {
                    shareable = shareable.copy();
                    shareable.incRefCount();
                }
            }
            arg.removeAttr(attrProfiles, RRuntime.CLASS_ATTR_KEY);
        }
        return arg;
    }

    protected boolean notAbstractVector(Object arg) {
        return !(arg instanceof RAbstractVector);
    }
}
