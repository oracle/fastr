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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "class<-")
public abstract class UpdateClass extends RBuiltinNode {

    private RVector resultVector;
    @Child private CastStringNode castStringNode;
    @Child private CastComplexNode castComplexNode;
    @Child private CastDoubleNode castDoubleNode;
    @Child private CastIntegerNode castIntegerNode;
    @Child private CastLogicalNode castLogicalNode;
    @Child private CastRawNode castRawNode;

    private void initCastString() {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreter();
            castStringNode = adoptChild(CastStringNodeFactory.create(null, false, false, false));
        }
    }

    private void initCastComplex() {
        if (castComplexNode == null) {
            CompilerDirectives.transferToInterpreter();
            castComplexNode = adoptChild(CastComplexNodeFactory.create(null, false, false));
        }
    }

    private void initCastDouble() {
        if (castDoubleNode == null) {
            CompilerDirectives.transferToInterpreter();
            castDoubleNode = adoptChild(CastDoubleNodeFactory.create(null, false, false));
        }
    }

    private void initCastInteger() {
        if (castIntegerNode == null) {
            CompilerDirectives.transferToInterpreter();
            castIntegerNode = adoptChild(CastIntegerNodeFactory.create(null, false, false));
        }
    }

    private void initCastLogical() {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreter();
            castLogicalNode = adoptChild(CastLogicalNodeFactory.create(null, false, false));
        }
    }

    private void initCastRaw() {
        if (castRawNode == null) {
            CompilerDirectives.transferToInterpreter();
            castRawNode = adoptChild(CastRawNodeFactory.create(null, false, false));
        }
    }

    private RStringVector castStringVector(VirtualFrame frame, RAbstractVector o) {
        initCastString();
        return (RStringVector) castStringNode.executeStringVector(frame, o);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractVector arg, RAbstractVector className) {
        if (className.getLength() == 0) {
            return setClass(arg, RNull.instance);
        }
        return setClass(frame, arg, castStringVector(frame, className));
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractVector arg, RStringVector className) {
        if (className.getLength() > 1) {
            Map<String, Object> attrb = getAttributes(arg);
            attrb.put(RRuntime.CLASS_ATTR_KEY, className);
            return resultVector;
        }
        return setClassHelper(frame, arg, className.getDataAt(0));
    }

    @Specialization
    public Object setClass(RAbstractVector arg, @SuppressWarnings("unused") RNull className) {
        Map<String, Object> attrb = getAttributes(arg);
        if (attrb != null) {
            attrb.remove(RRuntime.CLASS_ATTR_KEY);
        }
        return resultVector;
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractLogicalVector arg, String className) {
        if (className.equals(RRuntime.TYPE_LOGICAL)) {
            return arg;
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractStringVector arg, String className) {
        if (className.equals(RRuntime.TYPE_CHARACTER)) {
            return arg;
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractComplexVector arg, String className) {
        if (className.equals(RRuntime.TYPE_COMPLEX)) {
            return arg;
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractDoubleVector arg, String className) {
        if (className.equals(RRuntime.TYPE_DOUBLE) || className.equals(RRuntime.TYPE_NUMERIC)) {
            return arg;
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractIntVector arg, String className) {
        if (className.equals(RRuntime.TYPE_INTEGER) || className.equals(RRuntime.TYPE_NUMERIC)) {
            return arg;
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClass(VirtualFrame frame, RAbstractRawVector arg, String className) {
        if (className.equals(RRuntime.TYPE_RAW)) {
            return arg;
        }
        return setClassHelper(frame, arg, className);
    }

    @Specialization
    public Object setClassHelper(VirtualFrame frame, RAbstractVector arg, String className) {
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
        if (className.equals(RRuntime.TYPE_NUMERIC)) {
            initCastDouble();
            return castDoubleNode.executeDouble(frame, arg);
        }
        if (className.equals(RRuntime.TYPE_MATRIX)) {
            if (RRuntime.isMatrix(arg)) {
                return setClass(arg, RNull.instance);
            }
            throw RError.getNotMatixUpdateClass(getEncapsulatingSourceSection(), arg.getDimensions().length);

        }
        if (className.equals(RRuntime.TYPE_ARRAY)) {
            if (arg.getDimensions().length > 0) {
                return setClass(arg, RNull.instance);
            }
            throw RError.getNotArrayUpdateClass(getEncapsulatingSourceSection());
        }
        Map<String, Object> attrb = getAttributes(arg);
        attrb.put(RRuntime.CLASS_ATTR_KEY, className);
        return resultVector;
    }

    private Map<String, Object> getAttributes(RAbstractVector arg) {
        resultVector = arg.materialize();
        if (resultVector.isShared()) {
            resultVector = resultVector.copy();
        }
        Map<String, Object> attrb = resultVector.getAttributes();
        if (attrb == null) {
            attrb = new LinkedHashMap<>();
            resultVector.setAttributes((LinkedHashMap<String, Object>) attrb);
        }
        return attrb;
    }
}
