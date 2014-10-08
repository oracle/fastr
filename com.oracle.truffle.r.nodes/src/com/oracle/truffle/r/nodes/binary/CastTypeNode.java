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
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class CastTypeNode extends BinaryNode {

    @Child private CastStringNode castStringNode;
    @Child private CastComplexNode castComplexNode;
    @Child private CastDoubleNode castDoubleNode;
    @Child private CastIntegerNode castIntegerNode;
    @Child private CastLogicalNode castLogicalNode;
    @Child private CastRawNode castRawNode;
    @Child private CastListNode castListNode;
    @Child private CastToVectorNode castToVectorNode;
    @Child private TypeofNode typeof;

    public abstract Object execute(VirtualFrame frame, Object value, RType type);

    @SuppressWarnings("unused")
    @Specialization(guards = "isSameType")
    protected RAbstractVector doCast(VirtualFrame frame, RAbstractVector value, RType type) {
        return value;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSameType", "isString"})
    protected Object doCastString(VirtualFrame frame, RAbstractVector value, RType type) {
        initCastString();
        return castStringNode.executeString(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSameType", "isComplex"})
    protected Object doCastComplex(VirtualFrame frame, RAbstractVector value, RType type) {
        initCastComplex();
        return castComplexNode.executeComplex(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSameType", "isDouble"})
    protected Object doCastDouble(VirtualFrame frame, RAbstractVector value, RType type) {
        initCastDouble();
        return castDoubleNode.executeDouble(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSameType", "isInteger"})
    protected Object doCastInteger(VirtualFrame frame, RAbstractVector value, RType type) {
        initCastInteger();
        return castIntegerNode.executeInt(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSameType", "isLogical"})
    protected Object doCastLogical(VirtualFrame frame, RAbstractVector value, RType type) {
        initCastLogical();
        return castLogicalNode.executeLogical(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSameType", "isRaw"})
    protected Object doCastRaw(VirtualFrame frame, RAbstractVector value, RType type) {
        initCastRaw();
        return castRawNode.executeRaw(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isSameType", "isList"})
    protected RList doCastList(VirtualFrame frame, RAbstractVector value, RType type) {
        initCastList();
        return castListNode.executeList(frame, value);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doCastUnknown(RAbstractVector value, RType type) {
        return null;
    }

    @SuppressWarnings("unused")
    protected static boolean isString(RAbstractVector value, RType type) {
        return type == RType.Character;
    }

    @SuppressWarnings("unused")
    protected static boolean isComplex(RAbstractVector value, RType type) {
        return type == RType.Complex;
    }

    @SuppressWarnings("unused")
    protected static boolean isDouble(final RAbstractVector value, RType type) {
        return type == RType.Double || type == RType.Numeric;
    }

    @SuppressWarnings("unused")
    protected static boolean isInteger(RAbstractVector value, RType type) {
        return type == RType.Integer;
    }

    @SuppressWarnings("unused")
    protected static boolean isLogical(RAbstractVector value, RType type) {
        return type == RType.Logical;
    }

    @SuppressWarnings("unused")
    protected static boolean isRaw(RAbstractVector value, RType type) {
        return type == RType.Raw;
    }

    @SuppressWarnings("unused")
    protected static boolean isList(RAbstractVector value, RType type) {
        return type == RType.List;
    }

    protected boolean isSameType(VirtualFrame frame, RAbstractVector value, RType type) {
        initTypeof();
        RType givenType = typeof.execute(frame, value);
        return givenType.getName().equals(type);
    }

    private void initTypeof() {
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofNodeFactory.create(null));
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
