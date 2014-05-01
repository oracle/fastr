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

package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class CastTypeNode extends RInvisibleBuiltinNode {
    @Child private CastStringNode castStringNode;
    @Child private CastComplexNode castComplexNode;
    @Child private CastDoubleNode castDoubleNode;
    @Child private CastIntegerNode castIntegerNode;
    @Child private CastLogicalNode castLogicalNode;
    @Child private CastRawNode castRawNode;
    @Child private CastListNode castListNode;
    @Child private CastToVectorNode castToVectorNode;
    @Child private Typeof typeof;

    public abstract Object execute(VirtualFrame frame, final Object value, final String type);

    @Specialization
    public Object doCast(VirtualFrame frame, final Object value, final String type) {
        initTypeof();
        RStringVector givenType = typeof.execute(frame, value);
        if (givenType == null || givenType.getLength() < 1 || givenType.getDataAt(0).equals(type)) {
            return value;
        }
        if (type.equals(RRuntime.TYPE_CHARACTER)) {
            initCastString();
            return castStringNode.executeString(frame, value);
        }
        if (type.equals(RRuntime.TYPE_COMPLEX)) {
            initCastComplex();
            return castComplexNode.executeComplex(frame, value);
        }
        if (type.equals(RRuntime.TYPE_DOUBLE) || type.equals(RRuntime.TYPE_NUMERIC)) {
            initCastDouble();
            return castDoubleNode.executeDouble(frame, value);
        }
        if (type.equals(RRuntime.TYPE_INTEGER)) {
            initCastInteger();
            return castIntegerNode.executeInt(frame, value);
        }
        if (type.equals(RRuntime.TYPE_LOGICAL)) {
            initCastLogical();
            return castLogicalNode.executeCast(frame, value);
        }
        if (type.equals(RRuntime.TYPE_RAW)) {
            initCastRaw();
            return castRawNode.executeRaw(frame, value);
        }
        if (type.equals(RRuntime.TYPE_LIST)) {
            initCastList();
            return castListNode.executeList(frame, value);
        }
        // TODO: Implement remaining types.
        return null;
    }

    private void initTypeof() {
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofFactory.create(new RNode[1], getBuiltin()));
        }

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
}
