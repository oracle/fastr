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
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;

abstract class RToComplexVectorClosure extends RToVectorClosure implements RAbstractComplexVector {

    protected RToComplexVectorClosure(boolean keepAttributes) {
        super(keepAttributes);
    }

    @Override
    public final RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createComplexVector(new double[newLength << 1], newIsComplete);
    }

    @Override
    public final RComplexVector materialize() {
        int length = getLength();
        double[] result = new double[length << 1];
        for (int i = 0; i < length; i++) {
            RComplex data = getDataAt(i);
            int index = i << 1;
            result[index] = data.getRealPart();
            result[index + 1] = data.getImaginaryPart();
        }
        RComplexVector materialized = RDataFactory.createComplexVector(result, getVector().isComplete());
        copyAttributes(materialized);
        return materialized;
    }

    @TruffleBoundary
    private void copyAttributes(RComplexVector materialized) {
        if (keepAttributes) {
            materialized.initAttributes(getVector().getAttributes());
        }
    }

    @Override
    public final RAbstractComplexVector copyWithNewDimensions(int[] newDimensions) {
        if (!keepAttributes) {
            return materialize().copyWithNewDimensions(newDimensions);
        }
        return this;
    }
}

final class RLogicalToComplexVectorClosure extends RToComplexVectorClosure implements RAbstractComplexVector {

    private final RLogicalVector vector;

    RLogicalToComplexVectorClosure(RLogicalVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RLogicalVector getVector() {
        return vector;
    }

    @Override
    public RComplex getDataAt(int index) {
        byte real = vector.getDataAt(index);
        return RRuntime.isNA(real) ? RComplex.createNA() : RDataFactory.createComplex(real, 0.0);
    }
}

final class RIntToComplexVectorClosure extends RToComplexVectorClosure implements RAbstractComplexVector {

    private final RIntVector vector;

    RIntToComplexVectorClosure(RIntVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RIntVector getVector() {
        return vector;
    }

    @Override
    public RComplex getDataAt(int index) {
        int real = vector.getDataAt(index);
        return RRuntime.isNA(real) ? RComplex.createNA() : RDataFactory.createComplex(real, 0.0);
    }
}

final class RIntSequenceToComplexVectorClosure extends RToComplexVectorClosure implements RAbstractComplexVector {

    private final RIntSequence vector;

    RIntSequenceToComplexVectorClosure(RIntSequence vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RIntSequence getVector() {
        return vector;
    }

    @Override
    public RComplex getDataAt(int index) {
        int real = vector.getDataAt(index);
        return RRuntime.isNA(real) ? RComplex.createNA() : RDataFactory.createComplex(real, 0.0);
    }
}

final class RDoubleToComplexVectorClosure extends RToComplexVectorClosure implements RAbstractComplexVector {

    private final RDoubleVector vector;

    RDoubleToComplexVectorClosure(RDoubleVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RDoubleVector getVector() {
        return vector;
    }

    @Override
    public RComplex getDataAt(int index) {
        double real = vector.getDataAt(index);
        return Double.isNaN(real) ? RComplex.createNA() : RDataFactory.createComplex(real, 0.0);
    }
}

final class RDoubleSequenceToComplexVectorClosure extends RToComplexVectorClosure implements RAbstractComplexVector {

    private final RDoubleSequence vector;

    RDoubleSequenceToComplexVectorClosure(RDoubleSequence vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RDoubleSequence getVector() {
        return vector;
    }

    @Override
    public RComplex getDataAt(int index) {
        double real = vector.getDataAt(index);
        return Double.isNaN(real) ? RComplex.createNA() : RDataFactory.createComplex(real, 0.0);
    }
}

final class RRawToComplexVectorClosure extends RToComplexVectorClosure implements RAbstractComplexVector {

    private final RRawVector vector;

    RRawToComplexVectorClosure(RRawVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RRawVector getVector() {
        return vector;
    }

    @Override
    public RComplex getDataAt(int index) {
        return RRuntime.raw2complex(vector.getDataAt(index));
    }
}
