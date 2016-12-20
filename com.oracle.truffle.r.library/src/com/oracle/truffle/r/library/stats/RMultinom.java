/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2012, The R Core Team
 * Copyright (c) 2003-2008, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notIntNA;
import static com.oracle.truffle.r.runtime.RError.Message.NA_IN_PROB_VECTOR;
import static com.oracle.truffle.r.runtime.RError.Message.NEGATIVE_PROBABILITY;
import static com.oracle.truffle.r.runtime.RError.Message.NO_POSITIVE_PROBABILITIES;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.library.stats.RandGenerationFunctions.RandomNumberProvider;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ReuseNonSharedNode;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.rng.RRNG;

public abstract class RMultinom extends RExternalBuiltinNode.Arg3 {

    private final Rbinom rbinom = new Rbinom();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(0).asIntegerVector().findFirst().mustBe(notIntNA(), SHOW_CALLER, Message.INVALID_FIRST_ARGUMENT_NAME, "n");
        casts.arg(1).asIntegerVector().findFirst().mustBe(notIntNA(), SHOW_CALLER, Message.INVALID_SECOND_ARGUMENT_NAME, "size");
        casts.arg(2).asDoubleVector();
    }

    @Specialization
    protected RIntVector doMultinom(int n, int size, RAbstractDoubleVector probsVec,
                    @Cached("create()") ReuseNonSharedNode reuseNonSharedNode,
                    @Cached("createClassProfile()") ValueProfile randGeneratorClassProfile,
                    @Cached("createBinaryProfile()") ConditionProfile hasAttributesProfile,
                    @Cached("create()") UpdateShareableChildValueNode updateSharedAttributeNode,
                    @Cached("createNames()") GetFixedAttributeNode getNamesNode,
                    @Cached("createDimNames()") SetFixedAttributeNode setDimNamesNode) {
        RAbstractDoubleVector nonSharedProbs = (RAbstractDoubleVector) reuseNonSharedNode.execute(probsVec);
        double[] probs = nonSharedProbs.materialize().getDataWithoutCopying();
        fixupProb(probs);

        RRNG.getRNGState();
        RandomNumberProvider rand = new RandomNumberProvider(randGeneratorClassProfile.profile(RRNG.currentGenerator()), RRNG.currentNormKind());
        int k = probs.length;
        int[] result = new int[k * n];
        boolean isComplete = true;
        for (int i = 0, ik = 0; i < n; i++, ik += k) {
            isComplete &= rmultinom(size, probs, k, result, ik, rand);
        }
        RRNG.putRNGState();

        // take names from probVec (if any) as row names in the result
        RIntVector resultVec = RDataFactory.createIntVector(result, isComplete, new int[]{k, n});
        if (hasAttributesProfile.profile(probsVec.getAttributes() != null)) {
            Object probsNames = getNamesNode.execute(probsVec.getAttributes());
            updateSharedAttributeNode.execute(probsVec, probsNames);
            Object[] dimnamesData = new Object[]{probsNames, RNull.instance};
            setDimNamesNode.execute(resultVec.getAttributes(), RDataFactory.createList(dimnamesData));
        }
        return resultVec;
    }

    private static void fixupProb(double[] p) {
        double sum = 0.0;
        int npos = 0;
        for (int i = 0; i < p.length; i++) {
            if (!Double.isFinite(p[i])) {
                throw RError.error(SHOW_CALLER, NA_IN_PROB_VECTOR);
            }
            if (p[i] < 0.0) {
                throw RError.error(SHOW_CALLER, NEGATIVE_PROBABILITY);
            }
            if (p[i] > 0.0) {
                npos++;
                sum += p[i];
            }
        }
        if (npos == 0) {
            throw RError.error(SHOW_CALLER, NO_POSITIVE_PROBABILITIES);
        }
        for (int i = 0; i < p.length; i++) {
            p[i] /= sum;
        }
    }

    /**
     * Returns true if no element of the vector rN got assigned value NA, i.e. is stayed complete if
     * it was before. GnuR doc: `Return' vector rN[1:K] {K := length(prob)} where rN[j] ~ Bin(n,
     * prob[j]) , sum_j rN[j] == n, sum_j prob[j] == 1.
     */
    @TruffleBoundary
    private boolean rmultinom(int n, double[] prob, int maxK, int[] rN, int rnStartIdx, RandomNumberProvider rand) {
        int k;
        double pp;
        BigDecimal pTot = BigDecimal.ZERO;
        /*
         * This calculation is sensitive to exact values, so we try to ensure that the calculations
         * are as accurate as possible so different platforms are more likely to give the same
         * result.
         */

        if (RRuntime.isNA(maxK) || maxK < 1 || RRuntime.isNA(n) || n < 0) {
            if (rN.length > rnStartIdx) {
                rN[rnStartIdx] = RRuntime.INT_NA;
            }
            return false;
        }

        /*
         * Note: prob[K] is only used here for checking sum_k prob[k] = 1 ; Could make loop one
         * shorter and drop that check !
         */
        for (k = 0; k < maxK; k++) {
            pp = prob[k];
            if (!Double.isFinite(pp) || pp < 0. || pp > 1.) {
                rN[rnStartIdx + k] = RRuntime.INT_NA;
                return false;
            }
            pTot = pTot.add(new BigDecimal(pp));
            rN[rnStartIdx + k] = 0;
        }

        BigDecimal probSum = pTot.subtract(BigDecimal.ONE).abs();
        if (probSum.compareTo(new BigDecimal(1e-7)) == 1) {
            throw RError.error(SHOW_CALLER, Message.GENERIC, String.format("rbinom: probability sum should be 1, but is %s", pTot.toPlainString()));
        }
        if (n == 0) {
            return true;
        }
        if (maxK == 1 && pTot.compareTo(BigDecimal.ZERO) == 0) {
            return true; /* trivial border case: do as rbinom */
        }

        /* Generate the first K-1 obs. via binomials */
        for (k = 0; k < maxK - 1; k++) { /* (p_tot, n) are for "remaining binomial" */
            BigDecimal probK = new BigDecimal(prob[k]);
            if (probK.compareTo(BigDecimal.ZERO) != 0) {
                pp = probK.divide(pTot, RoundingMode.HALF_UP).doubleValue();
                // System.out.printf("[%d] %.17f\n", k + 1, pp);
                rN[rnStartIdx + k] = ((pp < 1.) ? (int) rbinom.execute((double) n, pp, rand) :
                /* >= 1; > 1 happens because of rounding */
                                n);
                n -= rN[rnStartIdx + k];
            } else {
                rN[rnStartIdx + k] = 0;
            }
            if (n <= 0) {
                /* we have all */
                return true;
            }
            /* i.e. = sum(prob[(k+1):K]) */
            pTot = pTot.subtract(probK);
        }

        rN[rnStartIdx + maxK - 1] = n;
        return true;
    }

}
