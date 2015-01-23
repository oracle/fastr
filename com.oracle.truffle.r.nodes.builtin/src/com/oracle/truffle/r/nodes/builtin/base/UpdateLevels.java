/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "levels<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so ""
public abstract class UpdateLevels extends RInvisibleBuiltinNode {

    @Child private CastToVectorNode castVector;
    @Child private CastStringNode castString;

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(null, false, false, false, false));
        }
        return (RAbstractVector) castVector.executeObject(frame, value);
    }

    private Object castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(null, false, true, false, false));
        }
        return castString.executeCast(frame, operand);
    }

    @Specialization
    protected RAbstractVector updateLevels(RAbstractVector vector, @SuppressWarnings("unused") RNull levels) {
        controlVisibility();
        RVector v = vector.materialize();
        v.setLevels(null);
        return v;
    }

    @Specialization
    protected RAbstractVector updateLevels(VirtualFrame frame, RAbstractVector vector, Object levels) {
        controlVisibility();
        RVector v = vector.materialize();
        v.setLevels(castVector(frame, levels));
        return v;
    }

    @Specialization
    protected RFactor updateLevels(RFactor factor, @SuppressWarnings("unused") RNull levels) {
        controlVisibility();
        factor.getVector().setLevels(null);
        return factor;
    }

    @Specialization
    protected RFactor updateLevels(VirtualFrame frame, RFactor factor, Object levels) {
        controlVisibility();
        factor.getVector().setLevels(castString(frame, castVector(frame, levels)));
        return factor;
    }
}
