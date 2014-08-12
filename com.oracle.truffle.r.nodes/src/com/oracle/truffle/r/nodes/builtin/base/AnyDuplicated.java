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
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "anyDuplicated", kind = RBuiltinKind.INTERNAL)
public abstract class AnyDuplicated extends RBuiltinNode {

    @Child private CastTypeNode castTypeNode;
    @Child private Typeof typeof;

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[2] = CastLogicalNodeFactory.create(arguments[2], true, false, false);
        return arguments;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isIncomparable", "!isFromLast", "!empty"}, order = 0)
    public int anyDuplicatedFalseIncomparablesFromStart(RAbstractVector x, byte incomparables, byte fromLast) {
        return getIndexFromStart(x);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isIncomparable", "isFromLast", "!empty"}, order = 1)
    public int anyDuplicatedFalseIncomparablesFromLast(RAbstractVector x, byte incomparables, byte fromLast) {
        return getIndexFromLast(x);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isIncomparable", "!isFromLast", "!empty"}, order = 2)
    public int anyDuplicatedTrueIncomparablesFromStart(VirtualFrame frame, RAbstractVector x, byte incomparables, byte fromLast) {
        initTypeof();
        initCastTypeNode();
        final String xType = typeof.execute(frame, x);
        return getIndexFromStart(x, (RAbstractVector) (castTypeNode.execute(frame, incomparables, xType)));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isIncomparable", "isFromLast", "!empty"}, order = 3)
    public int anyDuplicatedTrueIncomparablesFromLast(VirtualFrame frame, RAbstractVector x, byte incomparables, byte fromLast) {
        initTypeof();
        initCastTypeNode();
        String xType = typeof.execute(frame, x);
        return getIndexFromLast(x, (RAbstractVector) (castTypeNode.execute(frame, incomparables, xType)));
    }

    @Specialization(guards = {"!isFromLast", "!empty"}, order = 4)
    public int anyDuplicatedFromStart(VirtualFrame frame, RAbstractVector x, RAbstractVector incomparables, @SuppressWarnings("unused") byte fromLast) {
        initTypeof();
        initCastTypeNode();
        String xType = typeof.execute(frame, x);
        return getIndexFromStart(x, (RAbstractVector) (castTypeNode.execute(frame, incomparables, xType)));
    }

    @Specialization(guards = {"isFromLast", "!empty"}, order = 5)
    public int anyDuplicatedFromLast(VirtualFrame frame, RAbstractVector x, RAbstractVector incomparables, @SuppressWarnings("unused") byte fromLast) {
        initTypeof();
        initCastTypeNode();
        String xType = typeof.execute(frame, x);
        return getIndexFromLast(x, (RAbstractVector) (castTypeNode.execute(frame, incomparables, xType)));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "empty", order = 10)
    public int anyDuplicatedEmpty(VirtualFrame frame, RAbstractVector x, RAbstractVector incomparables, byte fromLast) {
        return 0;
    }

    @SlowPath
    private static int getIndexFromStart(RAbstractVector x, RAbstractVector incomparables) {
        HashSet<Object> incompContents = new HashSet<>();
        HashSet<Object> vectorContents = new HashSet<>();
        for (int i = 0; i < incomparables.getLength(); i++) {
            incompContents.add(incomparables.getDataAtAsObject(i));
        }
        vectorContents.add(x.getDataAtAsObject(0));
        for (int i = 1; i < x.getLength(); i++) {
            if (!incompContents.contains(x.getDataAtAsObject(i))) {
                if (vectorContents.contains(x.getDataAtAsObject(i))) {
                    return i + 1;
                } else {
                    vectorContents.add(x.getDataAtAsObject(i));
                }
            }
        }
        return 0;
    }

    @SlowPath
    private static int getIndexFromStart(RAbstractVector x) {
        HashSet<Object> vectorContents = new HashSet<>();
        vectorContents.add(x.getDataAtAsObject(0));
        for (int i = 1; i < x.getLength(); i++) {
            if (vectorContents.contains(x.getDataAtAsObject(i))) {
                return i + 1;
            } else {
                vectorContents.add(x.getDataAtAsObject(i));
            }
        }
        return 0;
    }

    @SlowPath
    public static int getIndexFromLast(RAbstractVector x, RAbstractVector incomparables) {
        HashSet<Object> incompContents = new HashSet<>();
        HashSet<Object> vectorContents = new HashSet<>();
        for (int i = 0; i < incomparables.getLength(); i++) {
            incompContents.add(incomparables.getDataAtAsObject(i));
        }
        vectorContents.add(x.getDataAtAsObject(x.getLength() - 1));
        for (int i = x.getLength() - 2; i >= 0; i--) {
            if (!incompContents.contains(x.getDataAtAsObject(i))) {
                if (vectorContents.contains(x.getDataAtAsObject(i))) {
                    return i + 1;
                } else {
                    vectorContents.add(x.getDataAtAsObject(i));
                }
            }
        }
        return 0;
    }

    @SlowPath
    private static int getIndexFromLast(RAbstractVector x) {
        HashSet<Object> vectorContents = new HashSet<>();
        vectorContents.add(x.getDataAtAsObject(x.getLength() - 1));
        for (int i = x.getLength() - 2; i >= 0; i--) {
            if (vectorContents.contains(x.getDataAtAsObject(i))) {
                return i + 1;
            } else {
                vectorContents.add(x.getDataAtAsObject(i));

            }
        }
        return 0;
    }

    @SuppressWarnings("unused")
    protected boolean isIncomparable(VirtualFrame frame, RAbstractVector x, byte incomparables, byte fromLast) {
        return incomparables == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected boolean isFromLast(VirtualFrame frame, RAbstractVector x, byte incomparables, byte fromLast) {
        return fromLast == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected boolean isFromLast(VirtualFrame frame, RAbstractVector x, RAbstractVector incomparables, byte fromLast) {
        return fromLast == RRuntime.LOGICAL_TRUE;
    }

    protected boolean empty(RAbstractVector x) {
        return x.getLength() == 0;
    }

    private void initCastTypeNode() {
        if (castTypeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castTypeNode = insert(CastTypeNodeFactory.create(new RNode[2], this.getBuiltin(), this.getSuppliedArgsNames()));
        }
    }

    private void initTypeof() {
        if (typeof == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            typeof = insert(TypeofFactory.create(new RNode[1], this.getBuiltin(), this.getSuppliedArgsNames()));
        }
    }
}
