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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("attributes<-")
@SuppressWarnings("unused")
public abstract class UpdateAttributes extends RBuiltinNode {

    @Child UpdateNames updateNames;
    @Child private CastIntegerNode castInteger;
    @Child private CastToVectorNode castVector;
    @Child private CastListNode castList;

    private void updateNamesStringVector(VirtualFrame frame, RAbstractVector vector, Object o) {
        if (updateNames == null) {
            CompilerDirectives.transferToInterpreter();
            updateNames = adoptChild(UpdateNamesFactory.create(new RNode[1], getBuiltin()));
        }
        updateNames.executeStringVector(frame, vector, o);
    }

    private RAbstractIntVector castInteger(VirtualFrame frame, RAbstractVector vector) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreter();
            castInteger = adoptChild(CastIntegerNodeFactory.create(null, true, false));
        }
        return (RAbstractIntVector) castInteger.executeCast(frame, vector);
    }

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreter();
            castVector = adoptChild(CastToVectorNodeFactory.create(null, false, false, false));
        }
        return castVector.executeRAbstractVector(frame, value);
    }

    private RList castList(VirtualFrame frame, Object value) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreter();
            castList = adoptChild(CastListNodeFactory.create(null, true, false));
        }
        return castList.executeList(frame, value);
    }

    @Specialization
    public RAbstractVector updateAttributes(VirtualFrame frame, RAbstractVector abstractVector, RNull list) {
        RVector resultVector = abstractVector.materialize();
        resultVector.resetAllAttributes(true);
        return resultVector;
    }

    @Specialization
    public RAbstractVector updateAttributes(VirtualFrame frame, RAbstractVector abstractVector, RList list) {
        Object listNamesObject = list.getNames();
        if (listNamesObject == null || listNamesObject == RNull.instance) {
            throw RError.getAttributesNamed(getEncapsulatingSourceSection());
        }
        RStringVector listNames = (RStringVector) listNamesObject;
        int numAttributes = list.getLength();
        RVector resultVector;
        if (numAttributes == 0) {
            resultVector = abstractVector.materialize();
            resultVector.resetAllAttributes(true);
        } else {
            resultVector = abstractVector.materialize();
            HashMap<String, Object> attributeMap = resultVector.resetAllAttributes(false);
            if (attributeMap == null) {
                resultVector.setAttributes(new LinkedHashMap<String, Object>());
            }
            // error checking is a little weird - seems easier to separate it than weave it into the
            // update loop
            if (listNames.getLength() > 1) {
                for (int i = 1; i < numAttributes; i++) {
                    String attrName = listNames.getDataAt(i);
                    if (attrName == RRuntime.NAMES_ATTR_EMPTY_VALUE) {
                        throw RError.getAllAttributesNames(getEncapsulatingSourceSection(), i + 1);
                    }
                }
            }
            // has to be reported if no other name is undefined
            if (listNames.getDataAt(0) == RRuntime.NAMES_ATTR_EMPTY_VALUE) {
                throw RError.getZeroLengthVariable(getEncapsulatingSourceSection());
            }
            // set the dim attribute first
            for (int i = 0; i < numAttributes; i++) {
                Object value = list.getDataAt(i);
                String attrName = listNames.getDataAt(i);
                if (attrName.equals(RRuntime.DIM_ATTR_KEY)) {
                    if (value == RNull.instance) {
                        resultVector.setDimensions(null, getEncapsulatingSourceSection());
                    } else {
                        RAbstractIntVector dimsVector = castInteger(frame, castVector(frame, value));
                        if (dimsVector.getLength() == 0) {
                            throw RError.getLengthZeroDimInvalid(getEncapsulatingSourceSection());
                        }
                        resultVector.setDimensions(dimsVector.materialize().getDataCopy(), getEncapsulatingSourceSection());
                    }
                }
            }
            // set the remaining attributes in order
            for (int i = 0; i < numAttributes; i++) {
                Object value = list.getDataAt(i);
                String attrName = listNames.getDataAt(i);
                if (attrName.equals(RRuntime.DIM_ATTR_KEY)) {
                    continue;
                } else if (attrName.equals(RRuntime.NAMES_ATTR_KEY)) {
                    updateNamesStringVector(frame, resultVector, value);
                } else if (attrName.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
                    if (value == RNull.instance) {
                        resultVector.setDimNames(null, getEncapsulatingSourceSection());
                    } else {
                        resultVector.setDimNames(castList(frame, value), getEncapsulatingSourceSection());
                    }
                } else {
                    if (value == RNull.instance) {
                        resultVector.getAttributes().remove(attrName);
                    } else {
                        resultVector.getAttributes().put(attrName, value);
                    }
                }
            }
        }
        return resultVector;
    }

    @Generic
    public RList doOther(VirtualFrame frame, Object vector, Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getAttributesListOrNull(getEncapsulatingSourceSection());
    }

}
