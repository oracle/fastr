/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

abstract class RecursiveExtractSubscriptNode extends RecursiveSubscriptNode {

    @Child private ExtractVectorNode recursiveSubscriptExtract = ExtractVectorNode.createRecursive(ElementAccessMode.SUBSCRIPT);
    @Child private ExtractVectorNode getPositionExtract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT);

    public RecursiveExtractSubscriptNode(RAbstractListVector vector, Object position) {
        super(vector, position);
    }

    public static RecursiveExtractSubscriptNode create(RAbstractListVector vector, Object position) {
        return RecursiveExtractSubscriptNodeGen.create(vector, position);
    }

    public final Object apply(Object vector, Object[] positions, Object exact, Object dropDimensions) {
        Object firstPosition = positions[0];
        int length = positionLengthNode.executeInteger(firstPosition);
        return execute(vector, positions, firstPosition, length, exact, dropDimensions);
    }

    protected abstract Object execute(Object vector, Object[] positions, Object firstPosition, int positionLength, Object exact, Object dropDimensions);

    @Specialization(guards = "positionLength <= 1")
    @SuppressWarnings("unused")
    protected Object doDefault(Object vector, Object[] positions, Object firstPosition, int positionLength, Object exact, Object dropDimensions) {
        try {
            return recursiveSubscriptExtract.apply(vector, positions, exact, dropDimensions);
        } catch (RecursiveIndexNotFoundError e) {
            errorBranch.enter();
            throw RError.error(this, RError.Message.SUBSCRIPT_BOUNDS);
        }
    }

    @Specialization(contains = "doDefault")
    @SuppressWarnings("unused")
    protected Object doRecursive(Object vector, Object[] positions, Object originalFirstPosition, int positionLength, Object exact, Object dropDimensions, //
                    @Cached("createPositionCast()") PositionCastNode positionCast) {
        Object firstPosition = positionCast.execute(originalFirstPosition);
        Object currentVector = vector;
        for (int i = 1; i <= positionLength; i++) {
            Object selection = getPositionExtract.apply(firstPosition, new Object[]{RInteger.valueOf(i)}, RLogical.TRUE, RLogical.TRUE);
            try {
                if (i < positionLength && !(currentVector instanceof RAbstractListVector)) {
                    if (currentVector == RNull.instance) {
                        throw noSuchIndex(i - 1);
                    } else {
                        throw indexingFailed(i - 1);
                    }
                } else if (i <= positionLength && currentVector == RNull.instance) {
                    throw noSuchIndex(i - 1);
                }
                currentVector = recursiveSubscriptExtract.apply(currentVector, new Object[]{selection}, exact, dropDimensions);

            } catch (RecursiveIndexNotFoundError e) {
                errorBranch.enter();
                if (i > 1) {
                    throw noSuchIndex(i - 1);
                } else {
                    throw RError.error(this, RError.Message.SUBSCRIPT_BOUNDS);
                }
            }
        }
        return currentVector;
    }

    protected PositionCastNode createPositionCast() {
        return PositionCastNode.create(ElementAccessMode.SUBSCRIPT, true);
    }
}
