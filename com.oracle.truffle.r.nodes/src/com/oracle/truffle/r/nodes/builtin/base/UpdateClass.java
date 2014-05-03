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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "class<-")
public abstract class UpdateClass extends RInvisibleBuiltinNode {

    @Child private CastStringNode castStringNode;
    @Child private CastComplexNode castComplexNode;
    @Child private CastDoubleNode castDoubleNode;
    @Child private CastIntegerNode castIntegerNode;
    @Child private CastLogicalNode castLogicalNode;
    @Child private CastRawNode castRawNode;
    @Child private CastListNode castListNode;

    public abstract Object execute(VirtualFrame frame, RAbstractContainer vector, Object o);

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractContainer arg, RAbstractVector className) {
        controlVisibility();
        if (className.getLength() == 0) {
            return setClass(arg, RNull.instance);
        }
        return setClass(arg, castStringVector(frame, className));
    }

    @Specialization
    public Object setClass(RAbstractContainer arg, RStringVector className) {
        controlVisibility();
        RVector resultVector = arg.materializeNonSharedVector();
        return RVector.setClassAttr(resultVector, className, arg.getElementClass() == RVector.class ? arg : null);
    }

    @Specialization
    public Object setClass(RAbstractContainer arg, @SuppressWarnings("unused") RNull className) {
        controlVisibility();
        RVector resultVector = arg.materializeNonSharedVector();
        return RVector.setClassAttr(resultVector, null, arg.getElementClass() == RVector.class ? arg : null);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractLogicalVector arg, String className) {
        controlVisibility();
        if (className.equals(RRuntime.TYPE_LOGICAL)) {
            return setClass(arg, RNull.instance);
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractStringVector arg, String className) {
        controlVisibility();
        if (className.equals(RRuntime.TYPE_CHARACTER)) {
            return setClass(arg, RNull.instance);
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractComplexVector arg, String className) {
        controlVisibility();
        if (className.equals(RRuntime.TYPE_COMPLEX)) {
            return setClass(arg, RNull.instance);
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractDoubleVector arg, String className) {
        controlVisibility();
        if (className.equals(RRuntime.TYPE_DOUBLE) || className.equals(RRuntime.TYPE_NUMERIC)) {
            return setClass(arg, RNull.instance);
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractIntVector arg, String className) {
        controlVisibility();
        if (className.equals(RRuntime.TYPE_INTEGER) || className.equals(RRuntime.TYPE_NUMERIC)) {
            return setClass(arg, RNull.instance);
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractRawVector arg, String className) {
        controlVisibility();
        if (className.equals(RRuntime.TYPE_RAW)) {
            return setClass(arg, RNull.instance);
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RList arg, String className) {
        controlVisibility();
        if (className.equals(RRuntime.TYPE_LIST)) {
            return setClass(arg, RNull.instance);
        }
        return setClassHelper(frame, arg, className);
    }

    public Object setClassHelper(VirtualFrame frame, RAbstractVector arg, String className) {
        controlVisibility();
        if (className.equals(RRuntime.TYPE_CHARACTER)) {
            initCastString();
            return castStringNode.executeString(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_COMPLEX)) {
            initCastComplex();
            return castComplexNode.executeComplex(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_DOUBLE)) {
            initCastDouble();
            return castDoubleNode.executeDouble(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_INTEGER)) {
            initCastInteger();
            return castIntegerNode.executeInt(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_LOGICAL)) {
            initCastLogical();
            return castLogicalNode.executeCast(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_RAW)) {
            initCastRaw();
            return castRawNode.executeRaw(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_LIST)) {
            initCastList();
            return castListNode.executeList(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_NUMERIC)) {
            initCastDouble();
            return castDoubleNode.executeDouble(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_MATRIX)) {
            if (arg.isMatrix()) {
                return setClass(arg, RNull.instance);
            }
            final int[] dimensions = arg.getDimensions();
            int dimLength = 0;
            if (dimensions != null) {
                dimLength = dimensions.length;
            }
            throw RError.getNotMatixUpdateClass(getEncapsulatingSourceSection(), dimLength);
        }
        if (className.equals(RRuntime.TYPE_ARRAY)) {
            if (arg.isArray()) {
                return setClass(arg, RNull.instance);
            }
            throw RError.getNotArrayUpdateClass(getEncapsulatingSourceSection());
        }

        RVector resultVector = arg.materializeNonSharedVector();
        return RVector.setClassAttr(resultVector, RDataFactory.createStringVector(className), arg.getElementClass() == RVector.class ? arg : null);
    }

    private void initCastString() {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeFactory.create(null, false, false, false, false));
        }
    }

    private void initCastComplex() {
        if (castComplexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplexNode = insert(CastComplexNodeFactory.create(null, false, false, false));
        }
    }

    private void initCastDouble() {
        if (castDoubleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDoubleNode = insert(CastDoubleNodeFactory.create(null, false, false, false));
        }
    }

    private void initCastInteger() {
        if (castIntegerNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castIntegerNode = insert(CastIntegerNodeFactory.create(null, false, false, false));
        }
    }

    private void initCastLogical() {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogicalNode = insert(CastLogicalNodeFactory.create(null, false, false, false));
        }
    }

    private void initCastRaw() {
        if (castRawNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castRawNode = insert(CastRawNodeFactory.create(null, false, false, false));
        }
    }

    private void initCastList() {
        if (castListNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castListNode = insert(CastListNodeFactory.create(null, false, false, false));
        }
    }

    private RStringVector castStringVector(VirtualFrame frame, RAbstractVector o) {
        initCastString();
        return (RStringVector) castStringNode.executeString(frame, o);
    }

}
