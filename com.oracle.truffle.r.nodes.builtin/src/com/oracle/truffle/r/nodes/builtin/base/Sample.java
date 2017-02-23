/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2012, The R Core Team
 * Copyright (c) 2003-2008, The R Foundation
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFinite;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.mustBe;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_FIRST_ARGUMENT;
import static com.oracle.truffle.r.runtime.RError.Message.VECTOR_SIZE_NA_NAN;
import static com.oracle.truffle.r.runtime.RError.Message.VECTOR_SIZE_TOO_LARGE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.MODIFIES_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.rng.RRNG;

@RBuiltin(name = "sample", kind = INTERNAL, parameterNames = {"x", "size", "replace", "prob"}, behavior = MODIFIES_STATE)
public abstract class Sample extends RBuiltinNode {
    private final ConditionProfile sampleSizeProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(Sample.class);
        casts.arg("x").defaultError(SHOW_CALLER, INVALID_FIRST_ARGUMENT).allowNull().mustBe(integerValue().or(doubleValue())).notNA(SHOW_CALLER, VECTOR_SIZE_NA_NAN).mapIf(doubleValue(),
                        chain(asDoubleVector()).with(findFirst().doubleElement()).with(mustBe(isFinite(), SHOW_CALLER, VECTOR_SIZE_NA_NAN)).with(
                                        mustBe(lt(4.5e15), SHOW_CALLER, VECTOR_SIZE_TOO_LARGE)).end()).asIntegerVector().findFirst().mustBe(gte0());
        casts.arg("size").defaultError(SHOW_CALLER, INVALID_ARGUMENT, "size").mustBe(integerValue().or(doubleValue()).or(stringValue())).asIntegerVector().findFirst().notNA(SHOW_CALLER,
                        INVALID_ARGUMENT, "size").mustBe(gte0(), SHOW_CALLER, INVALID_ARGUMENT, "size");
        casts.arg("replace").mustBe(integerValue().or(doubleValue()).or(logicalValue())).asLogicalVector().mustBe(singleElement()).findFirst().notNA().map(toBoolean());
        casts.arg("prob").asDoubleVector();
    }

    // Validation that correlates two or more argument values (note: positiveness of prob is checked
    // in fixupProbability)

    protected static boolean largerPopulation(final int x, final int size, final boolean isRepeatable) {
        return !isRepeatable && size > x;
    }

    protected static boolean invalidProb(final int x, final RDoubleVector prob) {
        return prob.getLength() != x;
    }

    @Specialization(guards = "invalidProb(x, prob)")
    @SuppressWarnings("unused")
    protected RIntVector doSampleInvalidProb(final int x, final int size, final boolean isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INCORRECT_NUM_PROB);
    }

    @Specialization(guards = "largerPopulation(x, size, isRepeatable)")
    @SuppressWarnings("unused")
    protected RIntVector doSampleLargerPopulation(final int x, final int size, final boolean isRepeatable, final RDoubleVector prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.SAMPLE_LARGER_THAN_POPULATION);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "largerPopulation(x, size, isRepeatable)")
    protected RIntVector doSampleInvalidLargerPopulation(final int x, final int size, final boolean isRepeatable, final RNull prob) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this, RError.Message.INCORRECT_NUM_PROB);
    }

    // Actual specializations:

    @Specialization(guards = {"!invalidProb(x, prob)", "!largerPopulation(x, size, isRepeatable)", "isRepeatable"})
    @TruffleBoundary
    protected RIntVector doSampleWithReplacement(final int x, final int size, final boolean isRepeatable, final RDoubleVector prob) {
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

    @Specialization(guards = {"!invalidProb(x, prob)", "!largerPopulation(x, size, isRepeatable)", "!isRepeatable"})
    @TruffleBoundary
    protected RIntVector doSampleNoReplacement(final int x, final int size, final boolean isRepeatable, final RDoubleVector prob) {
        double[] probArray = prob.getDataCopy();
        fixupProbability(probArray, x, size, isRepeatable);
        return RDataFactory.createIntVector(probSampleWithoutReplace(x, probArray, size), RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(guards = {"!largerPopulation(x, size, isRepeatable)"})
    @TruffleBoundary
    protected RIntVector doSample(final int x, final int size, final boolean isRepeatable, @SuppressWarnings("unused") final RNull prob) {
        // TODO:Add support of long integers.
        // The following code is transcribed from GNU R src/main/random.c lines 533-545 in
        // function do_sample.
        int[] result = new int[size];
        /* avoid allocation for a single sample */
        if (sampleSizeProfile.profile(isRepeatable || size < 2)) {
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

    @TruffleBoundary
    private void fixupProbability(double[] probArray, int x, int size, boolean isRepeatable) {
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
        if (nonZeroProbCount == 0 || (!isRepeatable && size > nonZeroProbCount)) {
            throw RError.error(this, RError.Message.TOO_FEW_POSITIVE_PROBABILITY);
        }
        for (int i = 0; i < x; i++) {
            probArray[i] /= probSum;
        }
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
