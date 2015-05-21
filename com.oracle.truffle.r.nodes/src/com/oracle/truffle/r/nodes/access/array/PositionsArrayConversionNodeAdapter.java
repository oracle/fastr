/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCast.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCastNodeGen.*;
import com.oracle.truffle.r.runtime.*;

public abstract class PositionsArrayConversionNodeAdapter extends RNode {

    @Children protected final ArrayPositionCast[] positionCasts;
    @Children protected final OperatorConverterNode[] operatorConverters;

    private final boolean isSubset;
    private final int length;

    public Object executeArg(Object container, Object operand, int i) {
        return positionCasts[i].executeArg(container, operand);
    }

    public Object executeConvert(Object vector, Object operand, Object exact, int i) {
        return operatorConverters[i].executeConvert(vector, operand, exact);
    }

    public boolean isSubset() {
        return isSubset;
    }

    public int getLength() {
        return length;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // this node is meant to be used only for evaluation by "executeWith" that uses
        // executeEvaluated method above
        RInternalError.shouldNotReachHere();
        return null;
    }

    public PositionsArrayConversionNodeAdapter(boolean isSubset, boolean isAssignment, int length) {
        this.isSubset = isSubset;
        this.length = length;
        ArrayPositionCast[] casts = new ArrayPositionCast[length];
        OperatorConverterNode[] converters = new OperatorConverterNode[length];
        for (int i = 0; i < length; i++) {
            casts[i] = ArrayPositionCastNodeGen.create(i, length, isAssignment, isSubset, null, null);
            converters[i] = OperatorConverterNodeGen.create(i, length, isAssignment, isSubset, null, null, null);
        }
        this.positionCasts = insert(casts);
        this.operatorConverters = insert(converters);
    }
}
