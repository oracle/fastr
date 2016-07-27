/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctions.IsAtomic;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctionsFactory.IsAtomicNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "lengths", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "use.names"})
public abstract class Lengths extends RBuiltinNode {

    @Child private IsAtomic isAtomicNode;
    @Child private Length lengthNode;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toLogical(1);
    }

    private void initLengthNode() {
        if (lengthNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lengthNode = insert(LengthNodeGen.create(null));
        }
    }

    @Specialization
    protected RIntVector doList(VirtualFrame frame, RList list, byte useNames) {
        initLengthNode();
        int[] data = new int[list.getLength()];
        for (int i = 0; i < data.length; i++) {
            Object elem = list.getDataAt(i);
            data[i] = lengthNode.executeInt(frame, elem);
        }
        return createResult(list, data, useNames);
    }

    @Specialization
    protected RIntVector doNull(@SuppressWarnings("unused") RNull x, @SuppressWarnings("unused") byte useNames) {
        return RDataFactory.createIntVectorFromScalar(1);
    }

    @Fallback
    protected RIntVector doObject(VirtualFrame frame, Object x, Object useNames) {
        if (isAtomicNode == null) {
            isAtomicNode = insert(IsAtomicNodeGen.create(null));
        }
        byte isAtomic = (byte) isAtomicNode.execute(frame, x);
        if (!RRuntime.fromLogical(isAtomic)) {
            throw RError.error(this, RError.Message.X_LIST_ATOMIC);
        }
        if (x instanceof RAbstractVector) {
            RAbstractVector xa = (RAbstractVector) x;
            int[] data = new int[xa.getLength()];
            Arrays.fill(data, 1);
            return createResult(xa, data, (byte) useNames);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    private RIntVector createResult(RAbstractVector x, int[] data, byte useNames) {
        RIntVector result = RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        if (RRuntime.fromLogical(useNames)) {
            result.copyNamesFrom(attrProfiles, x);
        }
        return result;
    }
}
