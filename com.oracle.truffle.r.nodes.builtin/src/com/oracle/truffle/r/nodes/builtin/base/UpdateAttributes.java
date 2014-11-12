/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "attributes<-", kind = PRIMITIVE, parameterNames = {"obj", ""})
// 2nd parameter is "value", but should not be matched against, so ""
@SuppressWarnings("unused")
public abstract class UpdateAttributes extends RInvisibleBuiltinNode {
    private final ConditionProfile numAttributesProfile = ConditionProfile.createBinaryProfile();

    @Child private UpdateNames updateNames;
    @Child private CastIntegerNode castInteger;
    @Child private CastToVectorNode castVector;
    @Child private CastListNode castList;

    private void updateNamesStringVector(VirtualFrame frame, RAbstractVector vector, Object o) {
        if (updateNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateNames = insert(UpdateNamesFactory.create(new RNode[2], getBuiltin(), getSuppliedArgsNames()));
        }
        updateNames.executeStringVector(frame, vector, o);
    }

    private RAbstractIntVector castInteger(VirtualFrame frame, RAbstractVector vector) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeFactory.create(null, true, false, false));
        }
        return (RAbstractIntVector) castInteger.executeCast(frame, vector);
    }

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeFactory.create(null, false, false, false, false));
        }
        return (RAbstractVector) castVector.executeObject(frame, value);
    }

    private RList castList(VirtualFrame frame, Object value) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeFactory.create(null, true, false, false));
        }
        return castList.executeList(frame, value);
    }

    @Specialization
    protected RAbstractVector updateAttributes(VirtualFrame frame, RAbstractVector abstractVector, RNull list) {
        controlVisibility();
        RVector resultVector = abstractVector.materialize();
        resultVector.resetAllAttributes(true);
        return resultVector;
    }

    @Specialization
    protected RAbstractContainer updateAttributes(VirtualFrame frame, RAbstractContainer container, RList list) {
        controlVisibility();
        Object listNamesObject = list.getNames();
        if (listNamesObject == null || listNamesObject == RNull.instance) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ATTRIBUTES_NAMED);
        }
        RStringVector listNames = (RStringVector) listNamesObject;
        RVector resultVector = container.materializeNonSharedVector();
        RAbstractContainer res = resultVector;
        if (numAttributesProfile.profile(list.getLength() == 0)) {
            resultVector.resetAllAttributes(true);
        } else {
            resultVector.resetAllAttributes(false);
            // error checking is a little weird - seems easier to separate it than weave it into the
            // update loop
            if (listNames.getLength() > 1) {
                checkAttributeForEmptyValue(list);
            }
            // has to be reported if no other name is undefined
            if (listNames.getDataAt(0).equals(RRuntime.NAMES_ATTR_EMPTY_VALUE)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ZERO_LENGTH_VARIABLE);
            }
            // set the dim attribute first
            setDimAttribute(frame, resultVector, list);
            // set the remaining attributes in order
            res = setRemainingAttributes(frame, container, resultVector, list);
        }
        return res;
    }

    @TruffleBoundary
    @ExplodeLoop
    private void checkAttributeForEmptyValue(RList rlist) {
        RStringVector listNames = (RStringVector) rlist.getNames();
        int length = rlist.getLength();
        assert length > 0 : "Length should be > 0 for ExplodeLoop";
        for (int i = 1; i < length; i++) {
            String attrName = listNames.getDataAt(i);
            if (attrName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ALL_ATTRIBUTES_NAMES, i + 1);
            }
        }
    }

    @ExplodeLoop
    private void setDimAttribute(VirtualFrame virtualFrame, RVector resultVector, RList sourceList) {
        RStringVector listNames = (RStringVector) sourceList.getNames();
        int length = sourceList.getLength();
        assert length > 0 : "Length should be > 0 for ExplodeLoop";
        for (int i = 0; i < sourceList.getLength(); i++) {
            Object value = sourceList.getDataAt(i);
            String attrName = listNames.getDataAt(i);
            if (attrName.equals(RRuntime.DIM_ATTR_KEY)) {
                if (value == RNull.instance) {
                    resultVector.setDimensions(null, getEncapsulatingSourceSection());
                } else {
                    RAbstractIntVector dimsVector = castInteger(virtualFrame, castVector(virtualFrame, value));
                    if (dimsVector.getLength() == 0) {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.LENGTH_ZERO_DIM_INVALID);
                    }
                    resultVector.setDimensions(dimsVector.materialize().getDataCopy(), getEncapsulatingSourceSection());
                }
            }
        }
    }

    @ExplodeLoop
    private RAbstractContainer setRemainingAttributes(VirtualFrame virtualFrame, RAbstractContainer container, RVector resultVector, RList sourceList) {
        RStringVector listNames = (RStringVector) sourceList.getNames();
        int length = sourceList.getLength();
        assert length > 0 : "Length should be > 0 for ExplodeLoop";
        RAbstractContainer res = resultVector;
        for (int i = 0; i < sourceList.getLength(); i++) {
            Object value = sourceList.getDataAt(i);
            String attrName = listNames.getDataAt(i);
            if (attrName.equals(RRuntime.DIM_ATTR_KEY)) {
                continue;
            } else if (attrName.equals(RRuntime.NAMES_ATTR_KEY)) {
                updateNamesStringVector(virtualFrame, resultVector, value);
            } else if (attrName.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
                if (value == RNull.instance) {
                    resultVector.setDimNames(null, getEncapsulatingSourceSection());
                } else {
                    resultVector.setDimNames(castList(virtualFrame, value), getEncapsulatingSourceSection());
                }
            } else if (attrName.equals(RRuntime.CLASS_ATTR_KEY)) {
                if (value == RNull.instance) {
                    res = RVector.setVectorClassAttr(resultVector, null, container.getElementClass() == RDataFrame.class ? container : null, container.getElementClass() == RFactor.class ? container : null);
                } else {
                    res = UpdateAttr.setClassAttrFromObject(resultVector, container, value, getEncapsulatingSourceSection());
                }
            } else if (attrName.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
                resultVector.setRowNames(castVector(virtualFrame, value));
            } else if (attrName.equals(RRuntime.LEVELS_ATTR_KEY)) {
                resultVector.setLevels(castVector(virtualFrame, value));
            } else {
                if (value == RNull.instance) {
                    resultVector.removeAttr(attrName);
                } else {
                    resultVector.setAttr(attrName, value);
                }
            }
        }
        return res;
    }

    @Fallback
    public RList doOther(Object vector, Object operand) {
        controlVisibility();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.ATTRIBUTES_LIST_OR_NULL);
    }

}
