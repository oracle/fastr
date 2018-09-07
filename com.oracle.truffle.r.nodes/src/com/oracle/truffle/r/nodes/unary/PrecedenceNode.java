/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.unary;

import static com.oracle.truffle.r.runtime.interop.ForeignArray2R.isForeignArray;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RForeignListWrapper;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteropScalar;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({Message.class, RRuntime.class, ForeignArray2R.class, Foreign2R.class})
public abstract class PrecedenceNode extends RBaseNode {

    public static final int NO_PRECEDENCE = -1;
    public static final int RAW_PRECEDENCE = 0;
    public static final int LOGICAL_PRECEDENCE = 1;
    public static final int INT_PRECEDENCE = 2;
    public static final int DOUBLE_PRECEDENCE = 3;
    public static final int COMPLEX_PRECEDENCE = 4;
    public static final int STRING_PRECEDENCE = 5;
    public static final int LIST_PRECEDENCE = 6;
    public static final int EXPRESSION_PRECEDENCE = 7;

    public static final int NUMBER_OF_PRECEDENCES = 9;

    public abstract int executeInteger(Object object, boolean recursive);

    @Specialization
    @SuppressWarnings("unused")
    protected int doNull(RNull val, boolean recursive) {
        return NO_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doFunction(RFunction func, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doEnvironment(REnvironment env, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doExternalPtr(RExternalPtr ptr, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doDoubleScalar(double d, boolean recursive) {
        return DOUBLE_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doIntScalar(int i, boolean recursive) {
        return INT_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doStringScalar(String s, boolean recursive) {
        return STRING_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doLogicalScalar(byte s, boolean recursive) {
        return LOGICAL_PRECEDENCE;
    }

    @Specialization(guards = "vector.getClass() == clazz", limit = "getCacheSize(16)")
    protected int doVector(@SuppressWarnings("unused") RAbstractAtomicVector vector, @SuppressWarnings("unused") boolean recursive,
                    @SuppressWarnings("unused") @Cached("vector.getClass()") Class<?> clazz,
                    @Cached("getVectorPrecedence(vector)") int precedence) {
        return precedence;
    }

    @Specialization(replaces = "doVector")
    protected int doVectorGeneric(RAbstractAtomicVector vector, @SuppressWarnings("unused") boolean recursive) {
        return getVectorPrecedence(vector);
    }

    protected static int getVectorPrecedence(RAbstractVector vector) {
        if (vector instanceof RAbstractDoubleVector) {
            return DOUBLE_PRECEDENCE;
        } else if (vector instanceof RAbstractIntVector) {
            return INT_PRECEDENCE;
        } else if (vector instanceof RAbstractRawVector) {
            return RAW_PRECEDENCE;
        } else if (vector instanceof RAbstractStringVector) {
            return STRING_PRECEDENCE;
        } else if (vector instanceof RAbstractLogicalVector) {
            return LOGICAL_PRECEDENCE;
        } else if (vector instanceof RAbstractComplexVector) {
            return COMPLEX_PRECEDENCE;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("unexpected vector type in PrecedenceNode " + vector.getClass().getSimpleName());
        }
    }

    @Specialization(guards = "recursive")
    protected int doListRecursive(RList val, boolean recursive,
                    @Cached("createRecursive()") PrecedenceNode precedenceNode) {
        return doListRecursiveInternal(val, precedenceNode, recursive);
    }

    @Specialization(guards = "recursive")
    protected int doListRecursive(RForeignListWrapper val, boolean recursive,
                    @Cached("createRecursive()") PrecedenceNode precedenceNode) {
        return doListRecursiveInternal(val, precedenceNode, recursive);
    }

    private static int doListRecursiveInternal(RAbstractListVector val, PrecedenceNode precedenceNode, boolean recursive) {
        int precedence = -1;
        for (int i = 0; i < val.getLength(); i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(val.getDataAt(i), recursive));
        }
        return precedence;
    }

    @Specialization(guards = {"recursive", "!list.isLanguage()"})
    protected int doPairListRecursive(RPairList list, boolean recursive,
                    @Cached("createRecursive()") PrecedenceNode precedenceNode) {
        int precedence = -1;
        for (RPairList item : list) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(item.car(), recursive));
        }
        return precedence;
    }

    protected static PrecedenceNode createRecursive() {
        return PrecedenceNodeGen.create();
    }

    @Specialization(guards = "!recursive")
    @SuppressWarnings("unused")
    protected int doList(RList val, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization(guards = "!recursive")
    @SuppressWarnings("unused")
    protected int doList(RForeignListWrapper val, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization(guards = {"!recursive", "!val.isLanguage()"})
    @SuppressWarnings("unused")
    protected int doPairList(RPairList val, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doExpression(RExpression val, boolean recursive) {
        return EXPRESSION_PRECEDENCE;
    }

    @Specialization(guards = "val.isLanguage()")
    @SuppressWarnings("unused")
    protected int doExpression(RPairList val, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doS4Object(RS4Object o, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doS4Object(RSymbol o, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doRInterop(RInteropScalar ri, boolean recursive) {
        return LIST_PRECEDENCE;
    }

    @Specialization(guards = {"!recursive", "args.getLength() == 1"})
    protected int doArgsValuesAndNames(RArgsValuesAndNames args, boolean recursive,
                    @Cached("createRecursive()") PrecedenceNode precedenceNode) {
        return precedenceNode.executeInteger(args.getArgument(0), recursive);
    }

    @Specialization(guards = {"isForeignObject(to)", "!isForeignArray(to, hasSize)"})
    @SuppressWarnings("unused")
    protected int doForeignObject(TruffleObject to, boolean recursive,
                    @Cached("HAS_SIZE.createNode()") Node hasSize) {
        return LIST_PRECEDENCE;
    }

    @Specialization(guards = {"isForeignArray(obj, hasSize)"})
    protected int doForeignArray(TruffleObject obj, boolean recursive,
                    @Cached("HAS_SIZE.createNode()") Node hasSize,
                    @Cached("GET_SIZE.createNode()") Node getSize,
                    @Cached("READ.createNode()") Node read,
                    @Cached("createRecursive()") PrecedenceNode precedenceNode,
                    @Cached("create()") Foreign2R foreign2R) {
        int precedence = -1;
        try {
            RContext context = RContext.getInstance();
            if (context.getEnv().isHostObject(obj)) {
                Object o = context.getEnv().asHostObject(obj);
                Class<?> ct = o.getClass().getComponentType();
                int prc = getPrecedence(ct, recursive);
                if (prc != -1) {
                    return prc;
                }
            }
            int size = (int) ForeignAccess.sendGetSize(getSize, obj);
            for (int i = 0; i < size; i++) {
                Object element = ForeignAccess.sendRead(read, obj, i);
                element = foreign2R.execute(element);
                if (!recursive && (isForeignArray(element, hasSize))) {
                    return LIST_PRECEDENCE;
                } else {
                    precedence = Math.max(precedence, precedenceNode.executeInteger(element, recursive));
                }
            }
        } catch (UnknownIdentifierException | UnsupportedMessageException ex) {
            throw error(RError.Message.GENERIC, "error while accessing array: " + ex.getMessage());
        }
        return precedence;
    }

    @TruffleBoundary
    private int getPrecedence(Class<?> ct, boolean recursive) {
        if (recursive && ct != null && ct.isArray()) {
            return getPrecedence(ct.getComponentType(), true);
        }
        return getPrecedence(ct);
    }

    private static int getPrecedence(Class<?> ct) {
        if (ct == Integer.class || ct == Byte.class || ct == Short.class || ct == int.class || ct == byte.class || ct == short.class) {
            return INT_PRECEDENCE;
        } else if (ct == Double.class || ct == Float.class || ct == Long.class || ct == double.class || ct == float.class || ct == long.class) {
            return DOUBLE_PRECEDENCE;
        } else if (ct == String.class || ct == Character.class || ct == char.class) {
            return STRING_PRECEDENCE;
        } else if (ct == Boolean.class || ct == boolean.class) {
            return LOGICAL_PRECEDENCE;
        }
        return NO_PRECEDENCE;
    }
}
