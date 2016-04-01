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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFrame;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "unclass", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class UnClass extends RBuiltinNode {
    private final BranchProfile objectProfile = BranchProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Specialization
    @TruffleBoundary
    protected Object unClass(RAbstractVector arg) {
        controlVisibility();
        if (arg.isObject(attrProfiles)) {
            objectProfile.enter();
            RVector resultVector = arg.materialize();
            if (!resultVector.isTemporary()) {
                resultVector = resultVector.copy();
                assert resultVector.isTemporary();
                resultVector.incRefCount();
            }
            return RVector.setVectorClassAttr(resultVector, null, null, null);
        }
        return arg;
    }

    @Specialization
    @TruffleBoundary
    protected Object unClass(RDataFrame arg) {
        controlVisibility();
        RDataFrame resultFrame = arg;
        if (!resultFrame.isTemporary()) {
            resultFrame = resultFrame.copy();
            assert resultFrame.isTemporary();
            resultFrame.incRefCount();
        }
        return RVector.setVectorClassAttr(resultFrame.getVector(), null, arg, null);
    }

    @Specialization
    @TruffleBoundary
    protected Object unClass(RFactor arg) {
        controlVisibility();
        RFactor resultFactor = arg;
        if (!resultFactor.isTemporary()) {
            resultFactor = resultFactor.copy();
            assert resultFactor.isTemporary();
            resultFactor.incRefCount();
        }
        return RVector.setVectorClassAttr(resultFactor.getVector(), null, null, arg);
    }

    @Specialization
    protected Object unClass(RLanguage arg) {
        controlVisibility();
        if (arg.getClassAttr(attrProfiles) != null) {
            objectProfile.enter();
            RLanguage resultLang = arg;
            if (!resultLang.isTemporary()) {
                resultLang = resultLang.copy();
                resultLang.isTemporary();
                resultLang.incRefCount();
            }
            resultLang.removeAttr(attrProfiles, RRuntime.CLASS_ATTR_KEY);
            return resultLang;
        }
        return arg;
    }

    @Specialization
    protected Object unClass(RS4Object arg) {
        controlVisibility();
        if (arg.getClassAttr(attrProfiles) != null) {
            objectProfile.enter();
            RS4Object resultS4 = arg;
            if (!resultS4.isTemporary()) {
                resultS4 = resultS4.copy();
                resultS4.isTemporary();
                resultS4.incRefCount();
            }
            resultS4.removeAttr(attrProfiles, RRuntime.CLASS_ATTR_KEY);
            return resultS4;
        }
        return arg;
    }
}
