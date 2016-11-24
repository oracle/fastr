/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.signature;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Combines two signatures: 'left' and 'right' and corresponding argument values. Left signature may
 * contain varargs, but 'right' signature may not. Any name in 'left' that also appears in 'right'
 * is overridden by the value and name in 'right' (shrinking the resulting arguments/signature
 * size). The left signature is actual signature permuted according to the formal signature, which
 * specifically means that it may contain unmatched arguments (those that appear in the formal
 * signature, but were not matched by any actual argument). Unmatched arguments are removed from the
 * result.
 *
 * @see com.oracle.truffle.r.nodes.function.ArgumentMatcher
 */
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
public abstract class CombineSignaturesNode extends RBaseNode {

    protected static final int CACHE_LIMIT = 3;

    public abstract RArgsValuesAndNames execute(ArgumentsSignature left, Object[] leftValue, ArgumentsSignature right, Object[] rightValues);

    @Specialization(guards = "left.isEmpty()")
    @SuppressWarnings("unused")
    protected RArgsValuesAndNames combineLeftEmpty(ArgumentsSignature left, Object[] leftValues, ArgumentsSignature right, Object[] rightValues) {
        return new RArgsValuesAndNames(rightValues, right);
    }

    @Specialization(guards = "right.isEmpty()")
    @SuppressWarnings("unused")
    protected RArgsValuesAndNames combineRightEmpty(ArgumentsSignature left, Object[] leftValues, ArgumentsSignature right, Object[] rightValues) {
        return new RArgsValuesAndNames(leftValues, left);
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "CACHE_LIMIT", guards = {"left == leftCached", "right == rightCached", "leftValues == leftValuesCached", "!right.isEmpty()", "!left.isEmpty()"})
    protected RArgsValuesAndNames combineCached(ArgumentsSignature left, Object[] leftValues, ArgumentsSignature right, Object[] rightValues,
                    @Cached("left") ArgumentsSignature leftCached,
                    @Cached("leftValues") Object[] leftValuesCached,
                    @Cached("right") ArgumentsSignature rightCached,
                    @Cached("combine(left, leftValues, right)") CombineResult resultCached,
                    @Cached("createBinaryProfile()") ConditionProfile shufflingProfile,
                    @Cached("createBinaryProfile()") ConditionProfile noVarArgsProfile) {
        Object[] flatLeftValues = leftValues;
        if (noVarArgsProfile.profile(resultCached.varArgsInfo.hasVarArgs())) {
            flatLeftValues = resultCached.varArgsInfo.flattenValues(left, leftValues);
        }
        return new RArgsValuesAndNames(resultCached.getValues(flatLeftValues, rightValues, shufflingProfile), resultCached.signature);
    }

    @TruffleBoundary
    protected CombineResult combine(ArgumentsSignature leftOriginal, Object[] leftValues, ArgumentsSignature right) {
        // flatten any varargs, note there should not be any in right
        VarArgsHelper varArgsInfo = VarArgsHelper.create(leftOriginal, leftValues);
        ArgumentsSignature left = leftOriginal;
        if (varArgsInfo.hasVarArgs()) {
            left = varArgsInfo.flattenNames(leftOriginal);
        }

        // calculate the size of the resulting signature - some values in left are overridden by the
        // same named in right, the unmatched were removed by flattenNames
        int resultSize = left.getLength() + right.getLength();
        boolean hasShuffledValues = false;
        boolean[] rightUsed = new boolean[right.getLength()];
        for (int i = 0; i < left.getLength(); i++) {
            int rightIndex = indexOf(right, left.getName(i));
            if (rightIndex != -1) {
                resultSize--;
                hasShuffledValues = true;
                rightUsed[rightIndex] = true;
            }
        }

        // create the actual signature
        if (!hasShuffledValues) {
            // when there is no shuffling, just concatenate left and right
            String[] names = new String[resultSize];
            for (int i = 0; i < left.getLength(); i++) {
                names[i] = left.getName(i);
            }
            for (int i = 0; i < right.getLength(); i++) {
                names[i + left.getLength()] = right.getName(i);
            }
            return new CombineResult(ArgumentsSignature.get(names), null, varArgsInfo);
        }

        String[] names = new String[resultSize];
        int[] valueIndexes = new int[resultSize];
        int currentIdx = 0;
        for (int i = 0; i < left.getLength(); i++) {
            String name = left.getName(i);
            names[currentIdx] = name;
            int rightIndex = indexOf(right, name);
            if (rightIndex != -1) {
                valueIndexes[currentIdx++] = -(rightIndex + 1);
            } else {
                valueIndexes[currentIdx++] = i;
            }
        }

        for (int i = 0; i < right.getLength(); i++) {
            if (!rightUsed[i]) {
                names[currentIdx] = right.getName(i);
                valueIndexes[currentIdx++] = -(i + 1);
            }
        }

        assert currentIdx == valueIndexes.length;
        return new CombineResult(ArgumentsSignature.get(names), valueIndexes, varArgsInfo);
    }

    private static int indexOf(ArgumentsSignature signature, String name) {
        return name == null ? -1 : signature.indexOfName(name);
    }

    static final class CombineResult {
        public final ArgumentsSignature signature;

        /**
         * When positive it is index into the first array 'left' of arguments, when negative, it is
         * (-1 * index) - 1 into the second array 'right' of 'actual' arguments to NextMethod. When
         * the array is {@code null}, then there was no shuffling/removal of values and 'left' and
         * 'right' can be simply just concatenated.
         */
        private final int[] valuesIndexes;

        private final VarArgsHelper varArgsInfo;

        CombineResult(ArgumentsSignature signature, int[] valuesIndexes, VarArgsHelper varArgsInfo) {
            this.varArgsInfo = varArgsInfo;
            assert valuesIndexes == null || signature.getLength() == valuesIndexes.length;
            this.signature = signature;
            this.valuesIndexes = valuesIndexes;
        }

        Object[] getValues(Object[] left, Object[] right, ConditionProfile noShufflingProfile) {
            if (noShufflingProfile.profile(valuesIndexes == null)) {
                assert signature.getLength() == left.length + right.length;
                Object[] result = Arrays.copyOf(left, signature.getLength());
                System.arraycopy(right, 0, result, left.length, right.length);
                return result;
            }

            Object[] result = new Object[valuesIndexes.length];
            for (int i = 0; i < valuesIndexes.length; i++) {
                int index = valuesIndexes[i];
                if (index < 0) {
                    result[i] = right[-(index + 1)];
                } else {
                    result[i] = left[index];
                }
            }
            return result;
        }
    }
}
