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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChild("operand")
public abstract class VectorPositionCast extends RNode {

    protected VectorPositionCast(NACheck positionNACheck) {
        this.positionNACheck = positionNACheck;
    }

    protected VectorPositionCast(VectorPositionCast other) {
        this.positionNACheck = other.positionNACheck;
    }

    private final NACheck positionNACheck;

    @Specialization
    protected int doDoublePosition(double operand) {
        positionNACheck.enable(operand);
        if (positionNACheck.check(operand)) {
            return RRuntime.INT_NA;
        }
        return (int) operand;
    }

    @Specialization
    protected int doInt(int operand) {
        positionNACheck.enable(operand);
        return operand;
    }

    @Specialization
    protected byte doBoolean(byte operand) {
        positionNACheck.enable(operand);
        return operand;
    }

    @Specialization
    protected String doString(String operand) {
        positionNACheck.enable(operand);
        return operand;
    }

    public static boolean sizeOneVector(RAbstractVector operand) {
        return operand.getLength() == 1;
    }

    public static boolean canConvertIntSequence(RDoubleSequence operand) {
        double start = operand.getStart();
        double stride = operand.getStride();
        return ((int) stride == stride) && start >= 1.0;
    }

    protected static boolean greaterEqualOneSequence(RIntSequence operand) {
        return operand.getStart() >= 1 && (operand.getStride() > 0 || operand.getEnd() > 0);
    }

    protected static boolean startingZeroSequence(RIntSequence operand) {
        return operand.getStart() == 0 && operand.getStride() > 0;
    }

    @Specialization(guards = "greaterEqualOneSequence")
    protected RIntSequence doIntVectorPositiveSequence(RIntSequence operand) {
        return operand;
    }

    @Specialization(guards = "startingZeroSequence")
    protected RIntSequence doIntVectorPositiveIncludingZeroSequence(RIntSequence operand) {
        return operand.removeFirst();
    }

    @Specialization(guards = {"!greaterEqualOneSequence", "!startingZeroSequence"})
    protected RIntVector doIntVector(RIntSequence operand) {
        return (RIntVector) operand.createVector();
    }

    @Specialization(guards = "canConvertIntSequence")
    protected RIntSequence doDoubleSequenceToIntConverstion(RDoubleSequence operand) {
        return RDataFactory.createIntSequence((int) operand.getStart(), (int) operand.getStride(), operand.getLength());
    }

    @Specialization(guards = "!canConvertIntSequence")
    protected RIntVector doDoubleSequence(@SuppressWarnings("unused") RDoubleSequence operand) {
        throw Utils.nyi();
    }

    @Specialization(guards = "sizeOneVector")
    protected int doIntVectorSizeOne(RIntVector operand) {
        positionNACheck.enable(operand);
        return operand.getDataAt(0);
    }

    @Specialization(guards = "!sizeOneVector")
    protected RIntVector doIntVector(RIntVector operand) {
        positionNACheck.enable(operand);
        return operand;
    }

    @Specialization
    protected RStringVector doIntVector(RStringVector operand) {
        positionNACheck.enable(operand);
        return operand;
    }

    @Specialization
    protected RIntVector doDoubleVector(RDoubleVector operand) {
        int dataLength = operand.getLength();
        positionNACheck.enable(operand);
        int[] intData = new int[dataLength];
        for (int i = 0; i < dataLength; ++i) {
            intData[i] = doDoublePosition(operand.getDataAt(i));
        }
        return RDataFactory.createIntVector(intData, operand.isComplete());
    }

    @Specialization
    protected RLogicalVector doLogicalVector(RLogicalVector operand) {
        return operand;
    }

    @Specialization
    protected RMissing doMissing(RMissing missing) {
        return missing;
    }

    @Specialization
    protected int doNull(@SuppressWarnings("unused") RNull nul) {
        return 0;
    }
}
