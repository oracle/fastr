/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.function.RCallNode.VarArgsAsObjectArrayNodeFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "positions", type = RNode[].class)})
public abstract class AccessArrayNode extends RNode {

    private static final VarArgsAsObjectArrayNodeFactory varArgAsObjectArrayNodeFactory = new VarArgsAsObjectArrayNodeFactory();

    abstract RNode getVector();

    @CreateCast({"vector"})
    public RNode createCastVector(RNode child) {
        return CastToVectorNodeFactory.create(child, false, false);
    }

    @CreateCast({"positions"})
    public RNode[] createCastPositions(RNode[] children) {
        RNode[] positions = new RNode[children.length];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = ArrayPositionCastFactory.create(i, getVector(), children[i], true);
        }
        return new RNode[]{varArgAsObjectArrayNodeFactory.makeList(positions, null)};
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1, guards = "wrongDimensions")
    Object access(RAbstractVector vector, Object[] positions) {
        throw RError.getIncorrectDimensions(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 10)
    RIntVector access(RIntVector vector, Object[] positions) {
        // dummy (for now)
        return RDataFactory.createIntVector(new int[]{42}, true);
    }

    protected boolean wrongDimensions(RAbstractVector vector, Object[] positions) {
        return vector.getDimensions() == null || vector.getDimensions().length != positions.length;
    }

    public static AccessArrayNode create(RNode vector, RNode[] positions) {
        return AccessArrayNodeFactory.create(vector, positions);
    }

}
