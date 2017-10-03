/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Gets length of given container. Does not actually dispatch to the 'length' function, which may be
 * overridden for some S3/S4 classes. Check if you need to get actual length, or what the 'length'
 * function returns, like in {@code seq_along}.
 */
@ImportStatic({Message.class, ForeignArray2R.class})
public abstract class RLengthNode extends RBaseNode {

    public abstract int executeInteger(Object value);

    public static RLengthNode create() {
        return RLengthNodeGen.create();
    }

    @Specialization
    protected int doNull(@SuppressWarnings("unused") RNull operand) {
        return 0;
    }

    @Specialization
    protected int doLogical(@SuppressWarnings("unused") byte operand) {
        return 1;
    }

    @Specialization
    protected int doInteger(@SuppressWarnings("unused") int operand) {
        return 1;
    }

    @Specialization
    protected int doDouble(@SuppressWarnings("unused") double operand) {
        return 1;
    }

    @Specialization
    protected int doString(@SuppressWarnings("unused") String operand) {
        return 1;
    }

    @Specialization
    protected int doSymbol(@SuppressWarnings("unused") RSymbol operand) {
        return 1;
    }

    @Specialization(guards = {"cachedClass != null", "cachedClass == operand.getClass()"})
    protected int doCachedContainer(Object operand,
                    @Cached("getContainerClass(operand)") Class<? extends RAbstractContainer> cachedClass,
                    @Cached("create()") VectorLengthProfile lengthProfile) {
        return lengthProfile.profile(cachedClass.cast(operand).getLength());
    }

    @Specialization(replaces = "doCachedContainer")
    protected int doContainer(RAbstractContainer operand,
                    @Cached("create()") VectorLengthProfile lengthProfile) {
        return lengthProfile.profile(operand.getLength());
    }

    protected static Class<? extends RAbstractContainer> getContainerClass(Object value) {
        if (value instanceof RAbstractContainer) {
            return ((RAbstractContainer) value).getClass();
        }
        return null;
    }

    @Specialization
    protected int getLength(REnvironment env,
                    @Cached("create()") VectorLengthProfile lengthProfile) {
        /*
         * This is a bit wasteful but only in the creation of the RStringVector; all the logic to
         * decide whether to include a name is still necessary
         */
        return lengthProfile.profile(env.ls(true, null, false).getLength());
    }

    @Specialization
    protected int getLength(RArgsValuesAndNames vargs) {
        return vargs.getLength();
    }

    @Specialization
    protected int getLength(@SuppressWarnings("unused") RFunction func) {
        return 1;
    }

    @Specialization
    protected int getLength(@SuppressWarnings("unused") RS4Object obj) {
        return 1;
    }

    @Specialization
    protected int getLength(@SuppressWarnings("unused") RExternalPtr ptr) {
        return 1;
    }

    protected static Node createGetSize() {
        return Message.GET_SIZE.createNode();
    }

    protected static Node createHasSize() {
        return Message.HAS_SIZE.createNode();
    }

    protected static boolean isForeignObject(TruffleObject object) {
        return RRuntime.isForeignObject(object);
    }

    @Specialization(guards = "isJavaIterable(object)")
    protected int getJavaIterableSize(TruffleObject object,
                    @Cached("READ.createNode()") Node read,
                    @Cached("createExecute(0).createNode()") Node execute,
                    @Cached("createBinaryProfile()") ConditionProfile profile) {
        try {
            Number sizeByMethod = null;
            for (String method : new String[]{"size", "getSize", "length", "getLength"}) {
                TruffleObject sizeFunction;
                try {
                    sizeFunction = (TruffleObject) ForeignAccess.sendRead(read, object, method);
                } catch (UnknownIdentifierException ex) {
                    continue;
                }
                Object value = ForeignAccess.sendExecute(execute, sizeFunction);
                if (value instanceof Number) {
                    sizeByMethod = (Number) value;
                }
            }
            if (profile.profile(sizeByMethod != null)) {
                return sizeByMethod.intValue();
            }

            TruffleObject itMethod = (TruffleObject) ForeignAccess.sendRead(read, object, "iterator");
            TruffleObject it = (TruffleObject) ForeignAccess.sendExecute(execute, itMethod);
            TruffleObject hasNextMethod = (TruffleObject) ForeignAccess.sendRead(read, it, "hasNext");
            int size = 0;
            while ((boolean) ForeignAccess.sendExecute(execute, hasNextMethod)) {
                ++size;
            }
            return size;
        } catch (ArityException | UnsupportedTypeException | UnknownIdentifierException | UnsupportedMessageException ex) {
            throw error(RError.Message.GENERIC, "error while accessing java iterable: " + ex.getMessage());
        }
    }

    @Specialization(guards = {"isForeignObject(object)", "!isJavaIterable(object)"})
    protected int getForeignSize(TruffleObject object,
                    @Cached("createHasSize()") Node hasSizeNode,
                    @Cached("createGetSize()") Node getSizeNode) {
        try {
            if (!(boolean) ForeignAccess.send(hasSizeNode, object)) {
                return 1;
            }
            return (int) ForeignAccess.send(getSizeNode, object);
        } catch (InteropException e) {
            throw RError.interopError(this, e, object);
        }
    }
}
