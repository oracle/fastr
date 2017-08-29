/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

abstract class RToIntVectorClosure extends RToVectorClosure implements RAbstractIntVector {

    protected RToIntVectorClosure(boolean keepAttributes) {
        super(keepAttributes);
    }

    @Override
    public final RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createIntVector(new int[newLength], newIsComplete);
    }

    @Override
    public final RIntVector materialize() {
        int length = getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            int data = getDataAt(i);
            result[i] = data;
        }
        RIntVector materialized = RDataFactory.createIntVector(result, getVector().isComplete());
        copyAttributes(materialized);
        return materialized;
    }

    @TruffleBoundary
    private void copyAttributes(RIntVector materialized) {
        if (keepAttributes) {
            materialized.copyAttributesFrom(getVector());
        }
    }

    @Override
    public final RAbstractIntVector copyWithNewDimensions(int[] newDimensions) {
        if (keepAttributes) {
            return materialize().copyWithNewDimensions(newDimensions);
        }
        return this;
    }
}

final class RLogicalToIntVectorClosure extends RToIntVectorClosure implements RAbstractIntVector {

    private final RLogicalVector vector;

    RLogicalToIntVectorClosure(RLogicalVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RLogicalVector getVector() {
        return vector;
    }

    @Override
    public int getDataAt(int index) {
        byte data = vector.getDataAt(index);
        if (RRuntime.isNA(data)) {
            return RRuntime.INT_NA;
        }
        return data;
    }
}

final class RDoubleToIntVectorClosure extends RToIntVectorClosure implements RAbstractIntVector {

    private final RDoubleVector vector;
    private boolean naReported;

    RDoubleToIntVectorClosure(RDoubleVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RDoubleVector getVector() {
        return vector;
    }

    @Override
    public int getDataAt(int index) {
        double value = vector.getDataAt(index);
        if (Double.isNaN(value)) {
            return RRuntime.INT_NA;
        }
        int result = (int) value;
        if (result == Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            if (!naReported) {
                RError.warning(RError.SHOW_CALLER2, RError.Message.NA_INTRODUCED_COERCION);
                naReported = true;
            }
            return RRuntime.INT_NA;
        }
        return result;
    }
}

final class RDoubleSequenceToIntVectorClosure extends RToIntVectorClosure implements RAbstractIntVector {

    private final RDoubleSequence vector;
    private boolean naReported;

    RDoubleSequenceToIntVectorClosure(RDoubleSequence vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RDoubleSequence getVector() {
        return vector;
    }

    @Override
    public int getDataAt(int index) {
        double value = vector.getDataAt(index);
        if (Double.isNaN(value)) {
            return RRuntime.INT_NA;
        }
        int result = (int) value;
        if (result == Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            if (!naReported) {
                RError.warning(RError.SHOW_CALLER2, RError.Message.NA_INTRODUCED_COERCION);
                naReported = true;
            }
            return RRuntime.INT_NA;
        }
        return result;
    }
}

/**
 * In converting complex numbers to integers, this closure discards the imaginary parts.
 */
final class RComplexToIntVectorClosure extends RToIntVectorClosure implements RAbstractIntVector {

    private final RComplexVector vector;

    RComplexToIntVectorClosure(RComplexVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RComplexVector getVector() {
        return vector;
    }

    @Override
    public int getDataAt(int index) {
        RComplex right = vector.getDataAt(index);
        if (RRuntime.isNA(right)) {
            return RRuntime.INT_NA;
        }
        RError.warning(RError.SHOW_CALLER2, RError.Message.IMAGINARY_PARTS_DISCARDED_IN_COERCION);
        return RRuntime.complex2intNoCheck(right);
    }
}

final class RRawToIntVectorClosure extends RToIntVectorClosure implements RAbstractIntVector {

    private final RRawVector vector;

    RRawToIntVectorClosure(RRawVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RRawVector getVector() {
        return vector;
    }

    @Override
    public int getDataAt(int index) {
        return RRuntime.raw2int(vector.getDataAt(index));
    }
}
