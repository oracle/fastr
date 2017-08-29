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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

abstract class RToStringVectorClosure extends RToVectorClosure implements RAbstractStringVector {

    protected RToStringVectorClosure(boolean keepAttributes) {
        super(keepAttributes);
    }

    @Override
    public final RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createStringVector(new String[newLength], newIsComplete);
    }

    @Override
    public final RStringVector materialize() {
        int length = getLength();
        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            String data = getDataAt(i);
            result[i] = data;
        }
        RStringVector materialized = RDataFactory.createStringVector(result, getVector().isComplete());
        copyAttributes(materialized);
        return materialized;
    }

    @TruffleBoundary
    private void copyAttributes(RStringVector materialized) {
        if (keepAttributes) {
            materialized.copyAttributesFrom(getVector());
        }
    }

    @Override
    public final RAbstractStringVector copyWithNewDimensions(int[] newDimensions) {
        if (!keepAttributes) {
            return materialize().copyWithNewDimensions(newDimensions);
        }
        return this;
    }
}

final class RLogicalToStringVectorClosure extends RToStringVectorClosure {

    private final RLogicalVector vector;

    RLogicalToStringVectorClosure(RLogicalVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RLogicalVector getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        byte data = vector.getDataAt(index);
        if (!vector.isComplete() && RRuntime.isNA(data)) {
            return RRuntime.STRING_NA;
        }
        return RRuntime.logicalToStringNoCheck(data);

    }
}

final class RIntToStringVectorClosure extends RToStringVectorClosure {

    private final RIntVector vector;

    RIntToStringVectorClosure(RIntVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RIntVector getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        int data = vector.getDataAt(index);
        if (!vector.isComplete() && RRuntime.isNA(data)) {
            return RRuntime.STRING_NA;
        }
        return RRuntime.intToStringNoCheck(data);
    }
}

final class RIntSequenceToStringVectorClosure extends RToStringVectorClosure {

    private final RIntSequence vector;

    RIntSequenceToStringVectorClosure(RIntSequence vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RIntSequence getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        int data = vector.getDataAt(index);
        if (!vector.isComplete() && RRuntime.isNA(data)) {
            return RRuntime.STRING_NA;
        }
        return RRuntime.intToStringNoCheck(data);
    }
}

final class RDoubleToStringVectorClosure extends RToStringVectorClosure {

    private final RDoubleVector vector;

    RDoubleToStringVectorClosure(RDoubleVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RDoubleVector getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        double data = vector.getDataAt(index);
        if (!vector.isComplete() && RRuntime.isNA(data)) {
            return RRuntime.STRING_NA;
        } else {
            return RContext.getRRuntimeASTAccess().encodeDouble(data);
        }
    }
}

final class RDoubleSequenceToStringVectorClosure extends RToStringVectorClosure {

    private final RDoubleSequence vector;

    RDoubleSequenceToStringVectorClosure(RDoubleSequence vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RDoubleSequence getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        double data = vector.getDataAt(index);
        if (!vector.isComplete() && RRuntime.isNA(data)) {
            return RRuntime.STRING_NA;
        } else {
            return RContext.getRRuntimeASTAccess().encodeDouble(data);
        }
    }
}

final class RComplexToStringVectorClosure extends RToStringVectorClosure {

    private final RComplexVector vector;

    RComplexToStringVectorClosure(RComplexVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RComplexVector getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        RComplex data = vector.getDataAt(index);
        if (!vector.isComplete() && RRuntime.isNA(data)) {
            return RRuntime.STRING_NA;
        }
        return RContext.getRRuntimeASTAccess().encodeComplex(data);
    }
}

final class RRawToStringVectorClosure extends RToStringVectorClosure {

    private final RRawVector vector;

    RRawToStringVectorClosure(RRawVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RRawVector getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        return RRuntime.rawToString(vector.getDataAt(index));
    }
}

/*
 * This closure is meant to be used only for implementation of the binary operators.
 */
final class RFactorToStringVectorClosure extends RToStringVectorClosure {

    private final RAbstractIntVector vector;
    private final RAbstractStringVector levels;
    private final boolean withNames;

    RFactorToStringVectorClosure(RAbstractIntVector vector, RAbstractStringVector levels, boolean withNames, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
        this.levels = levels;
        this.withNames = withNames;
    }

    @Override
    public RAbstractIntVector getVector() {
        return vector;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, @SuppressWarnings("hiding") boolean keepAttributes) {
        switch (type) {
            case Character:
                return this;
            default:
                return null;
        }
    }

    @Override
    public String getDataAt(int index) {
        int val = ((RIntVector) vector).getDataAt(index);
        if (!vector.isComplete() && RRuntime.isNA(val)) {
            return RRuntime.STRING_NA;
        } else {
            String l = levels.getDataAt(val - 1);
            if (!levels.isComplete() && RRuntime.isNA(l)) {
                return "NA"; // for comparison
            } else {
                return l;
            }
        }
    }

    @Override
    public RStringVector getNames() {
        return withNames ? super.getNames() : null;
    }
}
