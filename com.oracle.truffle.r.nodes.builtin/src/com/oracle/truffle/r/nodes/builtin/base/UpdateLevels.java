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
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "levels<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "value"})
public abstract class UpdateLevels extends RInvisibleBuiltinNode {

    @Child private CastToVectorNode castVector;
    @Child private CastStringNode castString;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    private RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        return (RAbstractVector) castVector.execute(value);
    }

    private Object castString(Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(false, true, false, false));
        }
        return castString.execute(operand);
    }

    @Specialization
    protected RAbstractVector updateLevels(RAbstractVector vector, @SuppressWarnings("unused") RNull levels) {
        controlVisibility();
        RVector v = (RVector) vector.materializeNonShared();
        v.removeAttr(attrProfiles, RRuntime.LEVELS_ATTR_KEY);
        return v;
    }

    @Specialization(guards = "levelsNotNull(levels)")
    protected RAbstractVector updateLevels(RAbstractVector vector, Object levels) {
        controlVisibility();
        RVector v = (RVector) vector.materializeNonShared();
        v.setAttr(RRuntime.LEVELS_ATTR_KEY, castVector(levels));
        return v;
    }

    @Specialization
    protected RFactor updateLevels(RFactor factor, @SuppressWarnings("unused") RNull levels) {
        controlVisibility();
        factor.getVector().removeAttr(attrProfiles, RRuntime.LEVELS_ATTR_KEY);
        return factor;
    }

    @Specialization(guards = "levelsNotNull(levels)")
    protected RFactor updateLevels(RFactor factor, Object levels) {
        controlVisibility();
        factor.getVector().setAttr(RRuntime.LEVELS_ATTR_KEY, castString(castVector(levels)));
        return factor;
    }

    protected boolean levelsNotNull(Object levels) {
        return levels != RNull.instance;
    }
}
