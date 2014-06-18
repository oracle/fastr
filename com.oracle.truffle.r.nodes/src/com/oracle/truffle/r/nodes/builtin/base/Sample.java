/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2012, The R Core Team
 * Copyright (c) 2003-2008, The R Foundation
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.rng.*;

@RBuiltin(name = "sample", kind = RBuiltinKind.INTERNAL)
public abstract class Sample extends RBuiltinNode {

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[0] = CastIntegerNodeFactory.create(arguments[0], true, false, false);
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        arguments[2] = CastLogicalNodeFactory.create(arguments[2], true, false, false);
        arguments[3] = CastDoubleNodeFactory.create(arguments[3], true, false, false);
        return arguments;
    }

    @Specialization(order = 10, guards = "invalidFirstArgument")
    @SuppressWarnings("unused")
    public RIntVector doSampleInvalidFirstArg(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getInvalidFirstArgument(getEncapsulatingSourceSection());
    }

    @Specialization(order = 20, guards = "invalidProb")
    @SuppressWarnings("unused")
    public RIntVector doSampleInvalidProb(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getIncorrectNumProb(getEncapsulatingSourceSection());
    }

    @Specialization(order = 30, guards = "largerPopulation")
    @SuppressWarnings("unused")
    public RIntVector doSampleLargerPopulation(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getLargerPopu(getEncapsulatingSourceSection());
    }

    @Specialization(order = 40, guards = "invalidSizeArgument")
    @SuppressWarnings("unused")
    public RIntVector doSampleInvalidSize(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getInvalidArgument(getEncapsulatingSourceSection(), RRuntime.toString(size));

    }

    @Specialization(order = 1, guards = {"!invalidFirstArgument", "!invalidProb", "!largerPopulation", "!invalidSizeArgument", "withReplacement"})
    public RIntVector doSampleWithReplacement(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
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

    @Specialization(order = 2, guards = {"!invalidFirstArgument", "!invalidProb", "!largerPopulation", "!invalidSizeArgument", "!withReplacement"})
    public RIntVector doSampleNoReplacement(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        double[] probArray = prob.getDataCopy();
        fixupProbability(probArray, x, size, isRepeatable);
        return RDataFactory.createIntVector(probSampleWithoutReplace(x, probArray, size), RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 50, guards = "invalidFirstArgumentNullProb")
    public RIntVector doSampleInvalidFirstArgument(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getInvalidFirstArgument(getEncapsulatingSourceSection());
    }

    @SuppressWarnings("unused")
    @Specialization(order = 60, guards = "invalidSizeArgument")
    public RIntVector doSampleInvalidSizeArgument(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getInvalidArgument(getEncapsulatingSourceSection(), RRuntime.toString(size));
    }

    @SuppressWarnings("unused")
    @Specialization(order = 70, guards = "largerPopulation")
    public RIntVector doSampleInvalidLargerPopulation(final int x, final int size, final byte isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getIncorrectNumProb(getEncapsulatingSourceSection());
    }

    @Specialization(order = 80, guards = {"!invalidFirstArgumentNullProb", "!invalidSizeArgument", "!largerPopulation"})
    public RIntVector doSample(final int x, final int size, final byte isRepeatable, @SuppressWarnings("unused") final RNull prob) {
        // TODO:Add support of long integers.
        // The following code is transcribed from GNU R src/main/random.c lines 533-545 in
        // function do_sample.
        int[] result = new int[size];
        /* avoid allocation for a single sample */
        if (isRepeatable == RRuntime.LOGICAL_TRUE || size < 2) {
            for (int i = 0; i < size; i++) {
                result[i] = (int) (x * RRNG.unifRand() + 1);
            }
        } else {
            int n = x;
            int[] ix = new int[x];
            for (int i = 0; i < x; i++) {
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
    @Specialization(order = 100, guards = "invalidIsRepeatable")
    public RIntVector doSampleInvalidIsRepeatable(final int x, final int size, final byte isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getInvalidArgument(getEncapsulatingSourceSection(), RRuntime.toString(isRepeatable));
    }

    private void fixupProbability(double[] probArray, int x, int size, byte isRepeatable) {
        // The following code is transcribed from GNU R src/main/random.c lines 429-449
        int nonZeroProbCount = 0;
        double probSum = 0;
        for (double aProb : probArray) {
            if (!RRuntime.isFinite(aProb)) {
                throw RError.getNAInProbVector(getEncapsulatingSourceSection());
            }
            if (aProb < 0) {
                throw RError.getNegativeProbability(getEncapsulatingSourceSection());
            }
            if (aProb > 0) {
                probSum += aProb;
                nonZeroProbCount++;
            }
        }
        if (nonZeroProbCount == 0 || (isRepeatable == RRuntime.LOGICAL_FALSE && size > nonZeroProbCount)) {
            throw RError.getTooFewPositiveProbability(getEncapsulatingSourceSection());
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

    private void buildheap(double[] keys, int[] values) {
        for (int i = (keys.length >> 1); i >= 0; i--) {
            minHeapify(keys, i, keys.length, values);
        }
    }

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

    private static void exchange(double[] keys, int i, int j, int[] values) {
        double c = keys[i];
        keys[i] = keys[j];
        keys[j] = c;
        int temp = values[i];
        values[i] = values[j];
        values[j] = temp;
    }

    private void heapSort(int[] values, double[] keys) {
        buildheap(keys, values);
        for (int i = keys.length - 1; i > 0; i--) {
            exchange(keys, 0, i, values);
            minHeapify(keys, 0, i, values);
        }
    }
}
