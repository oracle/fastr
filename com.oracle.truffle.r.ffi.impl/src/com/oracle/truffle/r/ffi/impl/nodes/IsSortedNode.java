/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;

/**
 * Represents all possible *_IS_SORTED upcalls eg. INTEGER_IS_SORTED. Note that all these upcalls just dispatches
 * to {@code VectorDataLibrary.isSorted} message, therefore, we can merge them in this class.
 */
@GenerateUncached
@ImportStatic(DSLConfig.class)
public abstract class IsSortedNode extends FFIUpCallNode.Arg1 {

    public static IsSortedNode create() {
        return IsSortedNodeGen.create();
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    public int isIntSorted(RIntVector intVec,
                    @CachedLibrary("intVec.getData()") VectorDataLibrary dataLibrary) {
        return isVectorSorted(intVec, dataLibrary);
    }

    @Specialization(replaces = "isIntSorted")
    public int isIntSortedUncached(RIntVector intVec) {
        return isVectorSorted(intVec, VectorDataLibrary.getFactory().getUncached(intVec.getData()));
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    public int isDoubleSorted(RDoubleVector doubleVector,
                    @CachedLibrary("doubleVector.getData()") VectorDataLibrary dataLibrary) {
        return isVectorSorted(doubleVector, dataLibrary);
    }

    @Specialization(replaces = "isDoubleSorted")
    public int isDoubleSortedUncached(RDoubleVector doubleVector) {
        return isVectorSorted(doubleVector, VectorDataLibrary.getFactory().getUncached(doubleVector.getData()));
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    public int isLogicalSorted(RLogicalVector logicalVector,
                    @CachedLibrary("logicalVector.getData()") VectorDataLibrary dataLibrary) {
        return isVectorSorted(logicalVector, dataLibrary);
    }

    @Specialization(replaces = "isLogicalSorted")
    public int isLogicalSortedUncached(RLogicalVector logicalVector) {
        return isVectorSorted(logicalVector, VectorDataLibrary.getFactory().getUncached(logicalVector.getData()));
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    public int isStringSorted(RStringVector stringVector,
                    @CachedLibrary("stringVector.getData()") VectorDataLibrary dataLibrary) {
        return isVectorSorted(stringVector, dataLibrary);
    }

    @Specialization(replaces = "isStringSorted")
    public int isStringSortedUncached(RStringVector stringVector) {
        return isVectorSorted(stringVector, VectorDataLibrary.getFactory().getUncached(stringVector.getData()));
    }

    @Fallback
    public int isSortedFallback(@SuppressWarnings("unused") Object x) {
        return AltrepSortedness.UNKNOWN_SORTEDNESS.getValue();
    }

    private static int isVectorSorted(RAbstractAtomicVector vector, VectorDataLibrary dataLibrary) {
        Object vecData = vector.getData();
        // The translation between (boolean descending, boolean naLast) to AltrepSortedness enum is
        // inspired by code in GNU-R (sort.c : makeSortEnum).
        AltrepSortedness sortedness = null;
        if (dataLibrary.isSorted(vecData, false, false)) {
            sortedness = AltrepSortedness.SORTED_INCR_NA_1ST;
        } else if (dataLibrary.isSorted(vecData, false, true)) {
            sortedness = AltrepSortedness.SORTED_INCR;
        } else if (dataLibrary.isSorted(vecData, true, false)) {
            sortedness = AltrepSortedness.SORTED_DECR_NA_1ST;
        } else if (dataLibrary.isSorted(vecData, true, true)) {
            sortedness = AltrepSortedness.SORTED_DECR;
        } else {
            sortedness = AltrepSortedness.UNKNOWN_SORTEDNESS;
        }
        return sortedness.getValue();
    }
}
