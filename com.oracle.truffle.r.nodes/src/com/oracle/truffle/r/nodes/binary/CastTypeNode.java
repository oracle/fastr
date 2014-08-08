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
import com.oracle.truffle.r.runtime.data.model.*;

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

    @SuppressWarnings("unused")
    @Specialization(order = 0, guards = "isSameType")
    public RAbstractVector doCast(VirtualFrame frame, final RAbstractVector value, final String type) {
        return value;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1, guards = {"!isSameType", "isString"})
    public Object doCastString(VirtualFrame frame, final RAbstractVector value, final String type) {
        initCastString();
        return castStringNode.executeString(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 2, guards = {"!isSameType", "isComplex"})
    public Object doCastComplex(VirtualFrame frame, final RAbstractVector value, final String type) {
        initCastComplex();
        return castComplexNode.executeComplex(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 4, guards = {"!isSameType", "isDouble"})
    public Object doCastDouble(VirtualFrame frame, final RAbstractVector value, final String type) {
        initCastDouble();
        return castDoubleNode.executeDouble(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 6, guards = {"!isSameType", "isInteger"})
    public Object doCastInteger(VirtualFrame frame, final RAbstractVector value, final String type) {
        initCastInteger();
        return castIntegerNode.executeInt(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 8, guards = {"!isSameType", "isLogical"})
    public Object doCastLogical(VirtualFrame frame, final RAbstractVector value, final String type) {
        initCastLogical();
        return castLogicalNode.executeLogical(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 10, guards = {"!isSameType", "isRaw"})
    public Object doCastRaw(VirtualFrame frame, final RAbstractVector value, final String type) {
        initCastRaw();
        return castRawNode.executeRaw(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 12, guards = {"!isSameType", "isList"})
    public RList doCastList(VirtualFrame frame, final RAbstractVector value, final String type) {
        initCastList();
        return castListNode.executeList(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 14)
    public Object doCastUnknown(VirtualFrame frame, final RAbstractVector value, final String type) {
        return null;
    }

    @SuppressWarnings("unused")
    protected static boolean isString(VirtualFrame frame, final RAbstractVector value, final String type) {
        return type.equals(RRuntime.TYPE_CHARACTER);
    }

    @SuppressWarnings("unused")
    protected static boolean isComplex(VirtualFrame frame, final RAbstractVector value, final String type) {
        return type.equals(RRuntime.TYPE_COMPLEX);
    }

    @SuppressWarnings("unused")
    protected static boolean isDouble(VirtualFrame frame, final RAbstractVector value, final String type) {
        return type.equals(RRuntime.TYPE_DOUBLE) || type.equals(RRuntime.TYPE_NUMERIC);
    }

    @SuppressWarnings("unused")
    protected static boolean isInteger(VirtualFrame frame, final RAbstractVector value, final String type) {
        return type.equals(RRuntime.TYPE_INTEGER);
    }

    @SuppressWarnings("unused")
    protected static boolean isLogical(VirtualFrame frame, final RAbstractVector value, final String type) {
        return type.equals(RRuntime.TYPE_LOGICAL);
    }

    @SuppressWarnings("unused")
    protected static boolean isRaw(VirtualFrame frame, final RAbstractVector value, final String type) {
        return type.equals(RRuntime.TYPE_RAW);
    }

    @SuppressWarnings("unused")
    protected static boolean isList(VirtualFrame frame, final RAbstractVector value, final String type) {
        return type.equals(RRuntime.TYPE_LIST);
    }

    protected boolean isSameType(VirtualFrame frame, final RAbstractVector value, final String type) {
        initTypeof();
        String givenType = typeof.execute(frame, value);
        return givenType.equals(type);
    }

    private void initTypeof() {
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofFactory.create(new RNode[1], getBuiltin(), getSuppliedArgsNames()));
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
