/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2012, The R Core Team
 * Copyright (c) 2003-2008, The R Foundation
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.rng.RRNG;

@RBuiltin(name = "sample", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "size", "replace", "prob"})
public abstract class Sample extends RBuiltinNode {
    private final ConditionProfile sampleSizeProfile = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(0);
        casts.toInteger(1);
        casts.toLogical(2);
        casts.toDouble(3);
    }

    @Specialization(guards = "invalidFirstArgument(x, size)")
    @SuppressWarnings("unused")
    protected RIntVector doSampleInvalidFirstArg(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INVALID_FIRST_ARGUMENT);
    }

    @Specialization(guards = "invalidProb(x, prob)")
    @SuppressWarnings("unused")
    protected RIntVector doSampleInvalidProb(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INCORRECT_NUM_PROB);
    }

    @Specialization(guards = "largerPopulation(x, size, isRepeatable)")
    @SuppressWarnings("unused")
    protected RIntVector doSampleLargerPopulation(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.SAMPLE_LARGER_THAN_POPULATION);
    }

    @Specialization(guards = "invalidSizeArgument(size)")
    @SuppressWarnings("unused")
    protected RIntVector doSampleInvalidSize(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INVALID_ARGUMENT, RRuntime.intToString(size));

    }

    @Specialization(guards = {"!invalidFirstArgument(x, size)", "!invalidProb(x, prob)", "!largerPopulation(x, size, isRepeatable)", "!invalidSizeArgument(size)", "withReplacement(isRepeatable)"})
    @TruffleBoundary
    protected RIntVector doSampleWithReplacement(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        // The following code is transcribed from GNU R src/main/random.c lines 493-501 in
        // function do_sample.
        double[] probArray = prob.getDataCopy();
        fixupProbability(probArray, x, size, isRepeatable);
        int nc = 0;
        for (double aProb : probArray) {
            if (x * aProb > 0.1) {
                nc++;
            }
        }
        if (nc > 200) {
            // TODO implement walker
            throw new UnsupportedOperationException("walker_ProbSampleReplace is not yet implemented");
        } else {
            return RDataFactory.createIntVector(probSampleReplace(x, probArray, size), RDataFactory.COMPLETE_VECTOR);
        }
    }

    @Specialization(guards = {"!invalidFirstArgument(x, size)", "!invalidProb(x, prob)", "!largerPopulation(x, size, isRepeatable)", "!invalidSizeArgument(size)", "!withReplacement(isRepeatable)"})
    @TruffleBoundary
    protected RIntVector doSampleNoReplacement(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        double[] probArray = prob.getDataCopy();
        fixupProbability(probArray, x, size, isRepeatable);
        return RDataFactory.createIntVector(probSampleWithoutReplace(x, probArray, size), RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "invalidFirstArgumentNullProb(x, size)")
    protected RIntVector doSampleInvalidFirstArgument(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INVALID_FIRST_ARGUMENT);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "invalidSizeArgument(size)")
    protected RIntVector doSampleInvalidSizeArgument(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INVALID_ARGUMENT, RRuntime.intToString(size));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "largerPopulation(x, size, isRepeatable)")
    protected RIntVector doSampleInvalidLargerPopulation(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INCORRECT_NUM_PROB);
    }

    @Specialization(guards = {"!invalidFirstArgumentNullProb(x, size)", "!invalidSizeArgument(size)", "!largerPopulation(x, size, isRepeatable)"})
    @TruffleBoundary
    protected RIntVector doSample(final int x, final int size, final byte isRepeatable, @SuppressWarnings("unused") final RNull prob) {
        // TODO:Add support of long integers.
        // The following code is transcribed from GNU R src/main/random.c lines 533-545 in
        // function do_sample.
        int[] result = new int[size];
        /* avoid allocation for a single sample */
        if (sampleSizeProfile.profile(isRepeatable == RRuntime.LOGICAL_TRUE || size < 2)) {
            for (int i = 0; i < size; i++) {
                result[i] = (int) (x * RRNG.unifRand() + 1);
            }
        } else {
            int n = x;
            int[] ix = new int[n];
            for (int i = 0; i < n; i++) {
                ix[i] = i;
            }
            for (int i = 0; i < size; i++) {
                int j = (int) (n * RRNG.unifRand());
                result[i] = ix[j] + 1;
                ix[j] = ix[--n];
            }
        }
        return RDataFactory.createIntVector(result, true);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "invalidIsRepeatable(isRepeatable)")
    protected RIntVector doSampleInvalidIsRepeatable(int x, int size, byte isRepeatable, RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INVALID_ARGUMENT, RRuntime.logicalToString(isRepeatable));
    }

    @TruffleBoundary
    private void fixupProbability(double[] probArray, int x, int size, byte isRepeatable) {
        // The following code is transcribed from GNU R src/main/random.c lines 429-449
        int nonZeroProbCount = 0;
        double probSum = 0;
        for (double aProb : probArray) {
            if (!RRuntime.isFinite(aProb)) {
                throw RError.error(this, RError.Message.NA_IN_PROB_VECTOR);
            }
            if (aProb < 0) {
                throw RError.error(this, RError.Message.NEGATIVE_PROBABILITY);
            }
            if (aProb > 0) {
                probSum += aProb;
                nonZeroProbCount++;
            }
        }
        if (nonZeroProbCount == 0 || (isRepeatable == RRuntime.LOGICAL_FALSE && size > nonZeroProbCount)) {
            throw RError.error(this, RError.Message.TOO_FEW_POSITIVE_PROBABILITY);
        }
        for (int i = 0; i < x; i++) {
            probArray[i] /= probSum;
        }
    }

    protected static boolean invalidFirstArgumentNullProb(final int x, final int size) {
        return !RRuntime.isFinite(x) || x < 0 || x > 4.5e15 || (size > 0 && x == 0);
    }

    protected static boolean invalidSizeArgument(int size) {
        return RRuntime.isNA(size) || size < 0;
    }

    protected static boolean largerPopulation(final int x, final int size, final byte isRepeatable) {
        return isRepeatable == RRuntime.LOGICAL_FALSE && size > x;
    }

    protected static boolean invalidFirstArgument(final int x, final int size) {
        return RRuntime.isNA(x) || x < 0 || (size > 0 && x == 0);
    }

    protected static boolean invalidProb(final int x, final RAbstractVector prob) {
        return prob.getLength() != x;
    }

    protected static boolean withReplacement(final byte isRepeatable) {
        return isRepeatable == RRuntime.LOGICAL_TRUE;
    }

    protected static boolean invalidIsRepeatable(final byte isRepeatable) {
        return RRuntime.isNA(isRepeatable);
    }

    @TruffleBoundary
    private int[] probSampleReplace(int n, double[] probArray, int resultSize) {
        // The following code is transcribed from GNU R src/main/random.c lines 309-335
        int[] result = new int[resultSize];
        int[] perm = new int[n];

        for (int i = 0; i < n; i++) {
            perm[i] = i + 1;
        }
        heapSort(perm, probArray);
        for (int i = 1; i < n; i++) {
            probArray[i] += probArray[i - 1];
        }
        for (int i = 0; i < resultSize; i++) {
            int j = 0;
            double rU = RRNG.unifRand();
            for (j = 0; j < n - 1; j++) {
                if (rU <= probArray[j]) {
                    break;
                }
            }
            result[i] = perm[j];
        }
        return result;
    }

    @TruffleBoundary
    private int[] probSampleWithoutReplace(int n, double[] probArray, int resultSize) {
        // The following code is transcribed from GNU R src/main/random.c lines 396-428
        int[] ans = new int[resultSize];
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i + 1;
        }
        heapSort(perm, probArray);
        double totalMass = 1;
        for (int i = 0, n1 = n - 1; i < resultSize; i++, n1--) {
            double rT = totalMass * RRNG.unifRand();
            double mass = 0;
            int j = 0;
            for (j = 0; j < n1; j++) {
                mass += probArray[j];
                if (rT <= mass) {
                    break;
                }
            }
            ans[i] = perm[j];
            totalMass -= probArray[j];
            for (int k = j; k < n1; k++) {
                probArray[k] = probArray[k + 1];
                perm[k] = perm[k + 1];
            }
        }
        return ans;
    }

    @TruffleBoundary
    private void buildheap(double[] keys, int[] values) {
        for (int i = (keys.length >> 1); i >= 0; i--) {
            minHeapify(keys, i, keys.length, values);
        }
    }

    @TruffleBoundary
    private void minHeapify(double[] keys, int currentIndex, int heapSize, int[] values) {
        int leftChildIndex = currentIndex << 1;
        int rightChildIndex = leftChildIndex + 1;
        int lowestElementIndex = currentIndex;
        if (leftChildIndex < heapSize && keys[leftChildIndex] < keys[currentIndex]) {
            lowestElementIndex = leftChildIndex;
        }
        if (rightChildIndex < heapSize && keys[rightChildIndex] < keys[currentIndex]) {
            lowestElementIndex = rightChildIndex;
        }
        if (lowestElementIndex != currentIndex) {
            exchange(keys, currentIndex, lowestElementIndex, values);
            minHeapify(keys, lowestElementIndex, heapSize, values);
        }
    }

    @TruffleBoundary
    private static void exchange(double[] keys, int i, int j, int[] values) {
        double c = keys[i];
        keys[i] = keys[j];
        keys[j] = c;
        int temp = values[i];
        values[i] = values[j];
        values[j] = temp;
    }

    @TruffleBoundary
    private void heapSort(int[] values, double[] keys) {
        buildheap(keys, values);
        for (int i = keys.length - 1; i > 0; i--) {
            exchange(keys, 0, i, values);
            minHeapify(keys, 0, i, values);
        }
    }
}
