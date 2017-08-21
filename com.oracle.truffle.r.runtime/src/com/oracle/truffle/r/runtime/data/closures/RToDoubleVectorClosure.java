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

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

abstract class RToDoubleVectorClosure extends RToVectorClosure implements RAbstractDoubleVector {

    @Override
    public final RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createDoubleVector(new double[newLength], newIsComplete);
    }

    @Override
    public final RDoubleVector materialize() {
        int length = getLength();
        double[] result = new double[length];
        for (int i = 0; i < result.length; i++) {
            double data = getDataAt(i);
            result[i] = data;
        }
        return RDataFactory.createDoubleVector(result, getVector().isComplete(), getVector().getDimensions(), getVector().getNames());
    }

    @Override
    public final RDoubleVector copyWithNewDimensions(int[] newDimensions) {
        return materialize().copyWithNewDimensions(newDimensions);
    }
}

final class RLogicalToDoubleVectorClosure extends RToDoubleVectorClosure implements RAbstractDoubleVector {

    private final RLogicalVector vector;

    RLogicalToDoubleVectorClosure(RLogicalVector vector) {
        this.vector = vector;
    }

    @Override
    public RLogicalVector getVector() {
        return vector;
    }

    @Override
    public double getDataAt(int index) {
        byte data = vector.getDataAt(index);
        if (RRuntime.isNA(data)) {
            return RRuntime.DOUBLE_NA;
        }
        return RRuntime.logical2doubleNoCheck(data);
    }
}

final class RIntToDoubleVectorClosure extends RToDoubleVectorClosure implements RAbstractDoubleVector {

    private final RIntVector vector;

    RIntToDoubleVectorClosure(RIntVector vector) {
        this.vector = vector;
    }

    @Override
    public RIntVector getVector() {
        return vector;
    }

    @Override
    public double getDataAt(int index) {
        int data = vector.getDataAt(index);
        if (RRuntime.isNA(data)) {
            return RRuntime.DOUBLE_NA;
        }
        return RRuntime.int2doubleNoCheck(data);
    }
}

final class RIntSequenceToDoubleVectorClosure extends RToDoubleVectorClosure implements RAbstractDoubleVector {

    private final RIntSequence vector;

    RIntSequenceToDoubleVectorClosure(RIntSequence vector) {
        this.vector = vector;
    }

    @Override
    public RIntSequence getVector() {
        return vector;
    }

    @Override
    public double getDataAt(int index) {
        int data = vector.getDataAt(index);
        if (RRuntime.isNA(data)) {
            return RRuntime.DOUBLE_NA;
        }
        return RRuntime.int2doubleNoCheck(data);
    }
}

final class RRawToDoubleVectorClosure extends RToDoubleVectorClosure implements RAbstractDoubleVector {

    private final RRawVector vector;

    RRawToDoubleVectorClosure(RRawVector vector) {
        this.vector = vector;
    }

    @Override
    public RRawVector getVector() {
        return vector;
    }

    @Override
    public double getDataAt(int index) {
        return RRuntime.raw2double(vector.getDataAt(index));
    }
}
