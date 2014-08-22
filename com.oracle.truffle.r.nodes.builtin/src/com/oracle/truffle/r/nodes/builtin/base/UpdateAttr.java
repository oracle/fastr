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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@RBuiltin(name = "attr<-", kind = PRIMITIVE, parameterNames = {"x", "which", ""})
// 2nd parameter is "value", but should not be matched against, so ""
@SuppressWarnings("unused")
public abstract class UpdateAttr extends RInvisibleBuiltinNode {

    @Child private UpdateNames updateNames;
    @Child private UpdateDimNames updateDimNames;
    @Child private CastIntegerNode castInteger;
    @Child private CastToVectorNode castVector;
    @Child private CastListNode castList;

    private RAbstractVector updateNames(VirtualFrame frame, RAbstractVector vector, Object o) {
        if (updateNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateNames = insert(UpdateNamesFactory.create(new RNode[2], getBuiltin(), getSuppliedArgsNames()));
        }
        return (RAbstractVector) updateNames.executeStringVector(frame, vector, o);
    }

    private RAbstractVector updateDimNames(VirtualFrame frame, RAbstractVector vector, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesFactory.create(new RNode[2], getBuiltin(), getSuppliedArgsNames()));
        }
        return updateDimNames.executeList(frame, vector, o);
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

    @Specialization(guards = "nullValue")
    protected RAbstractContainer updateAttr(VirtualFrame frame, RAbstractContainer container, String name, RNull value) {
        controlVisibility();
        RVector resultVector = container.materializeNonSharedVector();
        if (name.equals(RRuntime.DIM_ATTR_KEY)) {
            resultVector.setDimensions(null, getEncapsulatingSourceSection());
        } else if (name.equals(RRuntime.NAMES_ATTR_KEY)) {
            return updateNames(frame, resultVector, value);
        } else if (name.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
            return updateDimNames(frame, resultVector, value);
        } else if (name.equals(RRuntime.CLASS_ATTR_KEY)) {
            return RVector.setClassAttr(resultVector, null, container.getElementClass() == RVector.class ? container : null);
        } else if (name.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
            resultVector.setRowNames(null);
        } else if (resultVector.getAttributes() != null) {
            resultVector.getAttributes().remove(name);
        }
        // return frame if it's one, otherwise return the vector
        return container.getElementClass() == RVector.class ? container : resultVector;
    }

    public static RAbstractContainer setClassAttrFromObject(RVector resultVector, RAbstractContainer container, Object value, SourceSection sourceSection) {
        if (value instanceof RStringVector) {
            return RVector.setClassAttr(resultVector, (RStringVector) value, container.getElementClass() == RVector.class ? container : null);
        }
        if (value instanceof String) {
            return RVector.setClassAttr(resultVector, RDataFactory.createStringVector((String) value), container.getElementClass() == RVector.class ? container : null);
        }
        throw RError.error(sourceSection, RError.Message.SET_INVALID_CLASS_ATTR);
    }

    @Specialization(guards = "!nullValue")
    protected RAbstractContainer updateAttr(VirtualFrame frame, RAbstractContainer container, String name, Object value) {
        controlVisibility();
        RVector resultVector = container.materializeNonSharedVector();
        if (name.equals(RRuntime.DIM_ATTR_KEY)) {
            RAbstractIntVector dimsVector = castInteger(frame, castVector(frame, value));
            if (dimsVector.getLength() == 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.LENGTH_ZERO_DIM_INVALID);
            }
            resultVector.setDimensions(dimsVector.materialize().getDataCopy(), getEncapsulatingSourceSection());
        } else if (name.equals(RRuntime.NAMES_ATTR_KEY)) {
            return updateNames(frame, resultVector, value);
        } else if (name.equals(RRuntime.DIMNAMES_ATTR_KEY)) {
            return updateDimNames(frame, resultVector, value);
        } else if (name.equals(RRuntime.CLASS_ATTR_KEY)) {
            return setClassAttrFromObject(resultVector, container, value, getEncapsulatingSourceSection());
        } else if (name.equals(RRuntime.ROWNAMES_ATTR_KEY)) {
            resultVector.setRowNames(castVector(frame, value));
        } else {
            // generic attribute
            resultVector.setAttr(name, value);
        }
        // return frame if it's one, otherwise return the vector
        return container.getElementClass() == RVector.class ? container : resultVector;
    }

    @Specialization(guards = "!nullValue")
    protected RAbstractContainer updateAttr(VirtualFrame frame, RAbstractVector vector, RStringVector name, Object value) {
        controlVisibility();
        return updateAttr(frame, vector, name.getDataAt(0), value);
    }

    // the guard is necessary as RNull and Object cannot be distinguished in case of multiple
    // specializations, such as in: x<-1; attr(x, "dim")<-1; attr(x, "dim")<-NULL
    public boolean nullValue(RAbstractContainer container, Object name, Object value) {
        return value == RNull.instance;
    }

    @Specialization(guards = "!nullValueforEnv")
    protected REnvironment updateAttr(VirtualFrame frame, REnvironment env, String name, Object value) {
        controlVisibility();
        env.setAttr(name, value);
        return env;
    }

    @Specialization(guards = "nullValueforEnv")
    protected REnvironment updateAttr(VirtualFrame frame, REnvironment env, String name, RNull value) {
        controlVisibility();
        env.removeAttr(name);
        return env;
    }

    public boolean nullValueforEnv(REnvironment env, String name, Object value) {
        return value == RNull.instance;
    }
}
