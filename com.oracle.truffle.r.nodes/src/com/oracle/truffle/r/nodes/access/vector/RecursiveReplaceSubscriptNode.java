/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;

abstract class RecursiveReplaceSubscriptNode extends RecursiveSubscriptNode {

    @Child private ReplaceVectorNode recursiveSubscriptReplace = ReplaceVectorNode.createRecursive(ElementAccessMode.SUBSCRIPT);
    @Child private ReplaceVectorNode subscriptReplace = ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private ExtractVectorNode getPositionExtract = ExtractVectorNode.createRecursive(ElementAccessMode.SUBSCRIPT);
    @Child private ExtractVectorNode recursiveSubscriptExtract = ExtractVectorNode.createRecursive(ElementAccessMode.SUBSCRIPT);

    RecursiveReplaceSubscriptNode(RAbstractListVector vector, Object position) {
        super(vector, position);
    }

    public static RecursiveReplaceSubscriptNode create(RAbstractListVector vector, Object position) {
        return RecursiveReplaceSubscriptNodeGen.create(vector, position);
    }

    public final Object apply(VirtualFrame frame, Object vector, Object[] positions, Object value) {
        assert isSupported(vector, positions);
        Object firstPosition = positionClass.cast(positions[0]);
        int length = positionLengthNode.executeInteger(frame, firstPosition);
        return execute(frame, vectorClass.cast(vector), positions, firstPosition, length, value);
    }

    protected abstract Object execute(VirtualFrame frame, Object vector, Object[] positions, Object firstPosition, int positionLength, Object value);

    @Specialization(guards = "positionLength <= 1")
    @SuppressWarnings("unused")
    protected Object doDefault(VirtualFrame frame, Object vector, Object[] positions, Object firstPosition, int positionLength, Object value) {
        return subscriptReplace.apply(frame, vector, positions, value);
    }

    /**
     * Exemplary expansion. <code>
     * a <- list(1,list(1,list(1))); a[[c(2,2,2)]] <- 2
     * Gets desugared into:
     * tmp1 <- a[[2]]
     * tmp2 <- tmp1[[2]]
     * tmp2[[2]] <- 2
     * tmp1[[2]] <- tmp2
     * a[[2]] <- tmp1
     * </code>
     */
    @Specialization(contains = "doDefault")
    @SuppressWarnings("unused")
    protected Object doRecursive(VirtualFrame frame, Object vector, Object[] positions, Object originalFirstPosition, int positionLength, Object value, //
                    @Cached("createPositionCast()") PositionCastNode positionCast) {
        Object firstPosition = positionCast.execute(originalFirstPosition);
        Object[] positionStack = new Object[positionLength];
        Object[] valueStack = new Object[positionLength];
        valueStack[0] = vector;
        Object currentVector = vector;
        for (int i = 1; i < positionLength; i++) {
            Object parentPosition = getPositionValue(frame, firstPosition, i - 1);
            positionStack[i - 1] = parentPosition;
            try {
                if (!(currentVector instanceof RAbstractListVector)) {
                    throw indexingFailed(i);
                }
                currentVector = recursiveSubscriptExtract.apply(frame, currentVector, new Object[]{parentPosition}, RLogical.TRUE, RLogical.TRUE);
                if (currentVector == RNull.instance) {
                    throw noSuchIndex(i);
                }
                valueStack[i] = currentVector;
            } catch (RecursiveIndexNotFoundError e) {
                throw noSuchIndex(i);
            }
        }
        Object recursiveValue = value;
        positionStack[positionLength - 1] = getPositionValue(frame, firstPosition, positionLength - 1);
        for (int i = positionLength - 1; i >= 1; i--) {
            recursiveValue = recursiveSubscriptReplace.apply(frame, valueStack[i], new Object[]{positionStack[i]}, recursiveValue);
        }
        // the last recursive replace need to have recursive set to false
        recursiveValue = subscriptReplace.apply(frame, valueStack[0], new Object[]{positionStack[0]}, recursiveValue);

        return recursiveValue;
    }

    protected PositionCastNode createPositionCast() {
        return PositionCastNode.create(ElementAccessMode.SUBSCRIPT, false);
    }

    private Object getPositionValue(VirtualFrame frame, Object firstPosition, int i) {
        return getPositionExtract.apply(frame, firstPosition, new Object[]{RInteger.valueOf(i + 1)}, RLogical.TRUE, RLogical.TRUE);
    }
}
