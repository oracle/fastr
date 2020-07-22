/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.nodes.altrep;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleArrayVectorData;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntArrayVectorData;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.FFIUnwrapNode;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@GenerateUncached
@ImportStatic(AltrepUtilities.class)
public abstract class AltrepDuplicateNode extends RBaseNode {
    public abstract Object execute(Object altVec, boolean deep);

    @Specialization(guards = "hasDuplicateMethodRegistered(altrepVec)")
    protected Object duplicateAltrepWithDuplicateMethod(RAbstractAtomicVector altrepVec, boolean deep,
                    @Cached ConditionProfile duplicateReturnsNullProfile,
                    @Cached FFIUnwrapNode unwrapNode,
                    @Cached AltrepRFFI.DataptrNode dataptrNode,
                    @Cached AltrepRFFI.LengthNode lengthNode,
                    @Cached AltrepRFFI.DuplicateNode duplicateNode) {
        assert altrepVec.isAltRep();
        Object duplicatedObject = duplicateNode.execute(altrepVec, deep);
        if (duplicateReturnsNullProfile.profile(duplicatedObject == null)) {
            return doStandardDuplicate(altrepVec, deep, dataptrNode, lengthNode);
        } else {
            RAbstractAtomicVector duplicatedVector = (RAbstractAtomicVector) unwrapNode.execute(duplicatedObject);
            // We have to return data, not the whole vector.
            return duplicatedVector.getData();
        }
    }

    @Specialization(replaces = {"duplicateAltrepWithDuplicateMethod"})
    protected Object doStandardDuplicate(RAbstractAtomicVector altrepVec, @SuppressWarnings("unused") boolean deep,
                    @Cached AltrepRFFI.DataptrNode dataptrNode,
                    @Cached AltrepRFFI.LengthNode lengthNode) {
        assert AltrepUtilities.isAltrep(altrepVec);
        int length = lengthNode.execute(altrepVec);
        long dataptrAddr = dataptrNode.execute(altrepVec, false);
        if (altrepVec instanceof RIntVector) {
            int[] newData = new int[length];
            NativeMemory.copyMemory(dataptrAddr, newData, ElementType.INT, length);
            return new RIntArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
        } else if (altrepVec instanceof RDoubleVector) {
            double[] newData = new double[length];
            NativeMemory.copyMemory(dataptrAddr, newData, ElementType.DOUBLE, length);
            return new RDoubleArrayVectorData(newData, RDataFactory.INCOMPLETE_VECTOR);
        } else {
            throw RInternalError.unimplemented();
        }
    }
}
