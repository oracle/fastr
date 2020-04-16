/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;

abstract class RecursiveExtractSubscriptNode extends RecursiveSubscriptNode {

    @Child private ExtractVectorNode getPositionExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    @Child private ExtractVectorNode recursiveSubscriptExtract = ExtractVectorNode.createRecursive(ElementAccessMode.SUBSCRIPT);
    @Child private ExtractVectorNode subscriptExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
    private ConditionProfile emptyProfile = ConditionProfile.createBinaryProfile();

    RecursiveExtractSubscriptNode(RAbstractContainer vector, Object position) {
        super(vector, position);
    }

    public static RecursiveExtractSubscriptNode create(RAbstractContainer vector, Object position) {
        return RecursiveExtractSubscriptNodeGen.create(vector, position);
    }

    public final Object apply(Object vector, Object[] positions, Object exact, Object dropDimensions) {
        Object firstPosition = positions[0];
        int length;
        if (emptyProfile.profile(positions[0] == REmpty.instance)) {
            length = 1;
        } else {
            length = positionLengthNode.executeInteger(firstPosition);
        }
        return execute(vector, positions, firstPosition, length, exact, dropDimensions);
    }

    protected abstract Object execute(Object vector, Object[] positions, Object firstPosition, int positionLength, Object exact, Object dropDimensions);

    @Specialization(guards = "positionLength <= 1")
    protected Object doDefault(Object vector, Object[] positions, @SuppressWarnings("unused") Object firstPosition, @SuppressWarnings("unused") int positionLength, Object exact,
                    Object dropDimensions) {
        try {
            return subscriptExtract.apply(vector, positions, exact, dropDimensions);
        } catch (RecursiveIndexNotFoundError e) {
            throw error(RError.Message.SUBSCRIPT_BOUNDS);
        }
    }

    @Specialization(replaces = "doDefault")
    protected Object doRecursive(Object vector, @SuppressWarnings("unused") Object[] positions, Object originalFirstPosition, int positionLength, Object exact,
                    Object dropDimensions,
                    @Cached("createPositionCast()") PositionCastNode positionCast) {
        Object firstPosition = positionCast.execute(originalFirstPosition);
        Object currentVector = vector;
        for (int i = 1; i < positionLength; i++) {
            Object selection = getPositionExtract.apply(firstPosition, new Object[]{RDataFactory.createIntVectorFromScalar(i)}, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE);
            try {
                if (!(currentVector instanceof RAbstractListVector || currentVector instanceof RPairList)) {
                    throw indexingFailed(i);
                }
                currentVector = recursiveSubscriptExtract.apply(currentVector, new Object[]{selection}, exact, dropDimensions);

                if (currentVector == RNull.instance) {
                    throw error(RError.Message.SUBSCRIPT_BOUNDS);
                }
            } catch (RecursiveIndexNotFoundError e) {
                throw noSuchIndex(i);
            }
        }
        Object selection = getPositionExtract.apply(firstPosition, new Object[]{RDataFactory.createIntVectorFromScalar(positionLength)}, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE);
        try {
            return subscriptExtract.apply(currentVector, new Object[]{selection}, exact, dropDimensions);
        } catch (RecursiveIndexNotFoundError e) {
            throw error(RError.Message.SUBSCRIPT_BOUNDS);
        }
    }

    protected PositionCastNode createPositionCast() {
        return PositionCastNode.create(ElementAccessMode.SUBSCRIPT, true);
    }
}
