/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2012, The R Core Team
 * Copyright (c) 2003-2008, The R Foundation
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.rng.RRNG;

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

@RBuiltin(name = "sample", kind = RBuiltinKind.INTERNAL, parameterNames = {"x", "size", "replace", "prob"})
public abstract class Sample extends RBuiltinNode {
    private final ConditionProfile sampleSizeProfile = ConditionProfile.createBinaryProfile();

    @Override
    public RNode[] getParameterValues() {
        // x, size, replace = FALSE, prob = NULL
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RNull.instance)};
    }

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[0] = CastIntegerNodeGen.create(arguments[0], true, false, false);
        arguments[1] = CastIntegerNodeGen.create(arguments[1], true, false, false);
        arguments[2] = CastLogicalNodeGen.create(arguments[2], true, false, false);
        arguments[3] = CastDoubleNodeGen.create(arguments[3], true, false, false);
        return arguments;
    }

    @Specialization(guards = "invalidFirstArgument")
    @SuppressWarnings("unused")
    protected RIntVector doSampleInvalidFirstArg(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_FIRST_ARGUMENT);
    }

    @Specialization(guards = "invalidProb")
    @SuppressWarnings("unused")
    protected RIntVector doSampleInvalidProb(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_NUM_PROB);
    }

    @Specialization(guards = "largerPopulation")
    @SuppressWarnings("unused")
    protected RIntVector doSampleLargerPopulation(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.SAMPLE_LARGER_THAN_POPULATION);
    }

    @Specialization(guards = "invalidSizeArgument")
    @SuppressWarnings("unused")
    protected RIntVector doSampleInvalidSize(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, RRuntime.intToString(size, false));

    }

    @Specialization(guards = {"!invalidFirstArgument", "!invalidProb", "!largerPopulation", "!invalidSizeArgument", "withReplacement"})
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

    @Specialization(guards = {"!invalidFirstArgument", "!invalidProb", "!largerPopulation", "!invalidSizeArgument", "!withReplacement"})
    @TruffleBoundary
    protected RIntVector doSampleNoReplacement(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        double[] probArray = prob.getDataCopy();
        fixupProbability(probArray, x, size, isRepeatable);
        return RDataFactory.createIntVector(probSampleWithoutReplace(x, probArray, size), RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "invalidFirstArgumentNullProb")
    protected RIntVector doSampleInvalidFirstArgument(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_FIRST_ARGUMENT);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "invalidSizeArgument")
    protected RIntVector doSampleInvalidSizeArgument(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, RRuntime.intToString(size, false));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "largerPopulation")
    protected RIntVector doSampleInvalidLargerPopulation(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INCORRECT_NUM_PROB);
    }

    @Specialization(guards = {"!invalidFirstArgumentNullProb", "!invalidSizeArgument", "!largerPopulation"})
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
    @Specialization(guards = "invalidIsRepeatable")
    protected RIntVector doSampleInvalidIsRepeatable(int x, int size, byte isRepeatable, RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, RRuntime.logicalToString(isRepeatable));
    }

    @TruffleBoundary
    private void fixupProbability(double[] probArray, int x, int size, byte isRepeatable) {
        // The following code is transcribed from GNU R src/main/random.c lines 429-449
        int nonZeroProbCount = 0;
        double probSum = 0;
        for (double aProb : probArray) {
            if (!RRuntime.isFinite(aProb)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NA_IN_PROB_VECTOR);
            }
            if (aProb < 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NEGATIVE_PROBABILITY);
            }
            if (aProb > 0) {
                probSum += aProb;
                nonZeroProbCount++;
            }
        }
        if (nonZeroProbCount == 0 || (isRepeatable == RRuntime.LOGICAL_FALSE && size > nonZeroProbCount)) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.TOO_FEW_POSITIVE_PROBABILITY);
        }
        for (int i = 0; i < x; i++) {
            probArray[i] /= probSum;
        }
    }

    @SuppressWarnings("unused")
    protected static boolean invalidFirstArgumentNullProb(final int x, final int size, final byte isRepeatable, final RNull prob) {
        return !RRuntime.isFinite(x) || x < 0 || x > 4.5e15 || (size > 0 && x == 0);
    }

    @SuppressWarnings("unused")
    protected static boolean invalidSizeArgument(final int x, final int size, final byte isRepeatable, final RNull prob) {
        return RRuntime.isNA(size) || size < 0;
    }

    @SuppressWarnings("unused")
    protected static boolean largerPopulation(final int x, final int size, final byte isRepeatable, final RNull prob) {
        return isRepeatable == RRuntime.LOGICAL_FALSE && size > x;
    }

    @SuppressWarnings("unused")
    protected static boolean invalidFirstArgument(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        return RRuntime.isNA(x) || x < 0 || (size > 0 && x == 0);
    }

    @SuppressWarnings("unused")
    protected static boolean invalidSizeArgument(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        return RRuntime.isNA(size) || size < 0;
    }

    @SuppressWarnings("unused")
    protected static boolean invalidProb(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        return prob.getLength() != x;
    }

    @SuppressWarnings("unused")
    protected static boolean withReplacement(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        return isRepeatable == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected static boolean largerPopulation(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        return isRepeatable == RRuntime.LOGICAL_FALSE && size > x;
    }

    @SuppressWarnings("unused")
    protected static boolean invalidIsRepeatable(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
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
