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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "levels<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "value"}, dispatch = RDispatch.INTERNAL_GENERIC)
public abstract class UpdateLevels extends RBuiltinNode {

    @Child private CastToVectorNode castVector;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    private RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        return (RAbstractVector) castVector.execute(value);
    }

    @Specialization
    protected RAbstractVector updateLevels(RAbstractVector vector, @SuppressWarnings("unused") RNull levels) {
        controlVisibility();
        RVector v = (RVector) vector.getNonShared();
        v.removeAttr(attrProfiles, RRuntime.LEVELS_ATTR_KEY);
        return v;
    }

    @Specialization(guards = "levelsNotNull(levels)")
    protected RAbstractVector updateLevels(RAbstractVector vector, Object levels) {
        controlVisibility();
        RVector v = (RVector) vector.getNonShared();
        v.setAttr(RRuntime.LEVELS_ATTR_KEY, castVector(levels));
        return v;
    }

    protected boolean levelsNotNull(Object levels) {
        return levels != RNull.instance;
    }
}
