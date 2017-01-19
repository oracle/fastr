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
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;

@NodeChild("operand")
public abstract class RLengthNode extends RNode {

    @Override
    public abstract int executeInteger(VirtualFrame frame);

    public abstract int executeInteger(VirtualFrame frame, Object value);

    public static RLengthNode create() {
        return RLengthNodeGen.create(null);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doNull(RNull operand) {
        return 0;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doLogical(byte operand) {
        return 1;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doInteger(int operand) {
        return 1;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doDouble(double operand) {
        return 1;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doString(String operand) {
        return 1;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected int doSymbol(RSymbol operand) {
        return 1;
    }

    @Specialization(guards = {"cachedClass != null", "cachedClass == operand.getClass()"})
    protected int doCachedContainer(Object operand,
                    @Cached("getContainerClass(operand)") Class<? extends RAbstractContainer> cachedClass,
                    @Cached("create()") VectorLengthProfile lengthProfile) {
        return lengthProfile.profile(cachedClass.cast(operand).getLength());
    }

    @Specialization(contains = "doCachedContainer")
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
    protected int getLength(@SuppressWarnings("unused") RFunction func) {
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

    @Specialization(guards = "isForeignObject(object)")
    protected int getForeignSize(VirtualFrame frame, TruffleObject object,
                    @Cached("createHasSize()") Node hasSizeNode,
                    @Cached("createGetSize()") Node getSizeNode) {
        try {
            if (!(boolean) ForeignAccess.send(hasSizeNode, frame, object)) {
                return 1;
            }
            return (int) ForeignAccess.send(getSizeNode, frame, object);
        } catch (InteropException e) {
            throw RError.interopError(this, e, object);
        }
    }
}
