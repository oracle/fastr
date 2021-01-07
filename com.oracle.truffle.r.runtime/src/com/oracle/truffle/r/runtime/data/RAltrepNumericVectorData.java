/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltLogicalClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltRealClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltVecClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.AltrepSortedness;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Base class for altintegers, altreals and altlogicals.
 */
@ExportLibrary(VectorDataLibrary.class)
public class RAltrepNumericVectorData extends RAltrepVectorData {
    protected final AltVecClassDescriptor descriptor;

    public RAltrepNumericVectorData(AltVecClassDescriptor descriptor, RAltRepData altrepData) {
        super(altrepData);
        assert descriptor instanceof AltIntegerClassDescriptor || descriptor instanceof AltRealClassDescriptor || descriptor instanceof AltLogicalClassDescriptor;
        this.descriptor = descriptor;
        assert hasDescriptorRegisteredNecessaryMethods(descriptor);
    }

    private static boolean hasDescriptorRegisteredNecessaryMethods(AltVecClassDescriptor descriptor) {
        return descriptor.isLengthMethodRegistered() && descriptor.isDataptrMethodRegistered();
        /* TODO: && altIntClassDescr.isUnserializeMethodRegistered(); */
    }

    @ExportMessage
    public NACheck getNACheck(@Cached NACheck na) {
        na.enable(false);
        return na;
    }

    @ExportMessage
    public static class IsComplete {
        @Specialization(guards = "hasNoNAMethodRegistered(vectorData)")
        public static boolean isCompleteNativeFunction(RAltrepNumericVectorData vectorData,
                        @Cached AltrepRFFI.NoNANode noNANode) {
            return vectorData.invokeNoNA(noNANode);
        }

        @Specialization(guards = "!hasNoNAMethodRegistered(vectorData)")
        public static boolean isCompleteDefault(@SuppressWarnings("unused") RAltrepNumericVectorData vectorData) {
            return false;
        }

        protected static boolean hasNoNAMethodRegistered(RAltrepNumericVectorData vectorData) {
            if (vectorData.descriptor instanceof AltIntegerClassDescriptor) {
                return ((AltIntegerClassDescriptor) vectorData.descriptor).isNoNAMethodRegistered();
            } else if (vectorData.descriptor instanceof AltRealClassDescriptor) {
                return ((AltRealClassDescriptor) vectorData.descriptor).isNoNAMethodRegistered();
            } else if (vectorData.descriptor instanceof AltLogicalClassDescriptor) {
                return ((AltLogicalClassDescriptor) vectorData.descriptor).isNoNAMethodRegistered();
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    private boolean invokeNoNA(AltrepRFFI.NoNANode noNANode) {
        return noNANode.execute(owner);
    }

    @ExportMessage
    public static class IsSorted {
        @Specialization(guards = "hasIsSortedMethod(vectorData)")
        public static boolean doWithNativeFunction(RAltrepNumericVectorData vectorData, boolean decreasing, boolean naLast,
                        @Cached AltrepRFFI.IsSortedNode isSortedNode) {
            AltrepSortedness sortedness = isSortedNode.execute(vectorData.owner);
            if (decreasing) {
                if (naLast && sortedness == AltrepSortedness.SORTED_DECR) {
                    return true;
                } else {
                    return !naLast && sortedness == AltrepSortedness.SORTED_DECR_NA_1ST;
                }
            } else {
                if (naLast && sortedness == AltrepSortedness.SORTED_INCR) {
                    return true;
                } else {
                    return !naLast && sortedness == AltrepSortedness.SORTED_INCR_NA_1ST;
                }
            }
        }

        @Specialization(guards = "!hasIsSortedMethod(vectorData)")
        public static boolean doWithoutNativeFunction(@SuppressWarnings("unused") RAltrepNumericVectorData vectorData,
                        @SuppressWarnings("unused") boolean decreasing,
                        @SuppressWarnings("unused") boolean naLast) {
            return false;
        }

        protected static boolean hasIsSortedMethod(RAltrepNumericVectorData vectorData) {
            if (vectorData.descriptor instanceof AltIntegerClassDescriptor) {
                return ((AltIntegerClassDescriptor) vectorData.descriptor).isIsSortedMethodRegistered();
            } else if (vectorData.descriptor instanceof AltRealClassDescriptor) {
                return ((AltRealClassDescriptor) vectorData.descriptor).isIsSortedMethodRegistered();
            } else if (vectorData.descriptor instanceof AltLogicalClassDescriptor) {
                return ((AltLogicalClassDescriptor) vectorData.descriptor).isIsSortedMethodRegistered();
            } else {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }
}
