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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;

abstract class RecursiveExtractSubscriptNode extends RecursiveSubscriptNode {

    @Child private ExtractVectorNode getPositionExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private ExtractVectorNode recursiveSubscriptExtract = ExtractVectorNode.createRecursive(ElementAccessMode.SUBSCRIPT);
    @Child private ExtractVectorNode subscriptExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

    RecursiveExtractSubscriptNode(RAbstractListVector vector, Object position) {
        super(vector, position);
    }

    public static RecursiveExtractSubscriptNode create(RAbstractListVector vector, Object position) {
        return RecursiveExtractSubscriptNodeGen.create(vector, position);
    }

    public final Object apply(VirtualFrame frame, Object vector, Object[] positions, Object exact, Object dropDimensions) {
        Object firstPosition = positions[0];
        int length = positionLengthNode.executeInteger(frame, firstPosition);
        return execute(frame, vector, positions, firstPosition, length, exact, dropDimensions);
    }

    protected abstract Object execute(VirtualFrame frame, Object vector, Object[] positions, Object firstPosition, int positionLength, Object exact, Object dropDimensions);

    @Specialization(guards = "positionLength <= 1")
    protected Object doDefault(VirtualFrame frame, Object vector, Object[] positions, @SuppressWarnings("unused") Object firstPosition, @SuppressWarnings("unused") int positionLength, Object exact,
                    Object dropDimensions) {
        try {
            return subscriptExtract.apply(frame, vector, positions, exact, dropDimensions);
        } catch (RecursiveIndexNotFoundError e) {
            throw error(RError.Message.SUBSCRIPT_BOUNDS);
        }
    }

    @Specialization(replaces = "doDefault")
    protected Object doRecursive(VirtualFrame frame, Object vector, @SuppressWarnings("unused") Object[] positions, Object originalFirstPosition, int positionLength, Object exact,
                    Object dropDimensions,
                    @Cached("createPositionCast()") PositionCastNode positionCast) {
        Object firstPosition = positionCast.execute(originalFirstPosition);
        Object currentVector = vector;
        for (int i = 1; i < positionLength; i++) {
            Object selection = getPositionExtract.apply(frame, firstPosition, new Object[]{RInteger.valueOf(i)}, RLogical.TRUE, RLogical.TRUE);
            try {
                if (!(currentVector instanceof RAbstractListVector)) {
                    throw indexingFailed(i);
                }
                currentVector = recursiveSubscriptExtract.apply(frame, currentVector, new Object[]{selection}, exact, dropDimensions);

                if (currentVector == RNull.instance) {
                    throw error(RError.Message.SUBSCRIPT_BOUNDS);
                }
            } catch (RecursiveIndexNotFoundError e) {
                throw noSuchIndex(i);
            }
        }
        Object selection = getPositionExtract.apply(frame, firstPosition, new Object[]{RInteger.valueOf(positionLength)}, RLogical.TRUE, RLogical.TRUE);
        try {
            return subscriptExtract.apply(frame, currentVector, new Object[]{selection}, exact, dropDimensions);
        } catch (RecursiveIndexNotFoundError e) {
            throw error(RError.Message.SUBSCRIPT_BOUNDS);
        }
    }

    protected PositionCastNode createPositionCast() {
        return PositionCastNode.create(ElementAccessMode.SUBSCRIPT, true);
    }
}
