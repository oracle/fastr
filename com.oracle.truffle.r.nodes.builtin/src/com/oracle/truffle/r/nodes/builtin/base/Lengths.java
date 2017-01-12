/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_VALUE;
import static com.oracle.truffle.r.runtime.RError.Message.X_LIST_ATOMIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "lengths", kind = INTERNAL, parameterNames = {"x", "use.names"}, behavior = PURE)
public abstract class Lengths extends RBuiltinNode {

    @Child private RLengthNode lengthNode;

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").defaultError(RError.SHOW_CALLER, X_LIST_ATOMIC).allowNull().mustBe(abstractVectorValue());
        casts.arg("use.names").mustBe(numericValue(), RError.SHOW_CALLER, INVALID_VALUE, "use.names").asLogicalVector().findFirst().map(toBoolean());
    }

    private void initLengthNode() {
        if (lengthNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lengthNode = insert(RLengthNode.create());
        }
    }

    @Specialization
    protected RIntVector doList(VirtualFrame frame, RList list, boolean useNames) {
        initLengthNode();
        int[] data = new int[list.getLength()];
        for (int i = 0; i < data.length; i++) {
            Object elem = list.getDataAt(i);
            data[i] = lengthNode.executeInteger(frame, elem);
        }
        return createResult(list, data, useNames);
    }

    @Specialization
    protected RIntVector doNull(@SuppressWarnings("unused") RNull x, @SuppressWarnings("unused") boolean useNames) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization
    protected RIntVector doObject(RAbstractVector xa, boolean useNames) {
        int[] data = new int[xa.getLength()];
        Arrays.fill(data, 1);
        return createResult(xa, data, useNames);
    }

    private RIntVector createResult(RAbstractVector x, int[] data, boolean useNames) {
        RIntVector result = RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        if (useNames) {
            copyNames(x, result);
        }
        return result;
    }

    @TruffleBoundary
    private void copyNames(RAbstractVector x, RIntVector result) {
        result.copyNamesFrom(x);
    }
}
