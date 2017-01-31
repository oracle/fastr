/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2012, The R Core Team
 * Copyright (c) 2003-2008, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notIntNA;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.RError.Message.NA_IN_PROB_VECTOR;
import static com.oracle.truffle.r.runtime.RError.Message.NEGATIVE_PROBABILITY;
import static com.oracle.truffle.r.runtime.RError.Message.NO_POSITIVE_PROBABILITIES;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ReuseNonSharedNode;
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.distr.RMultinom;
import com.oracle.truffle.r.runtime.nmath.distr.Rbinom;
import com.oracle.truffle.r.runtime.rng.RRNG;

/**
 * Implements the vectorization of {@link RMultinom}.
 */
public abstract class RMultinomNode extends RExternalBuiltinNode.Arg3 {
    private final Rbinom rbinom = new Rbinom();

    public static RMultinomNode create() {
        return RMultinomNodeGen.create();
    }

    static {
        Casts casts = new Casts(RMultinomNode.class);
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
            isComplete &= RMultinom.rmultinom(size, probs, k, result, ik, rand, rbinom);
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
        for (double prob : p) {
            if (!Double.isFinite(prob)) {
                throw RError.error(SHOW_CALLER, NA_IN_PROB_VECTOR);
            }
            if (prob < 0.0) {
                throw RError.error(SHOW_CALLER, NEGATIVE_PROBABILITY);
            }
            if (prob > 0.0) {
                npos++;
                sum += prob;
            }
        }
        if (npos == 0) {
            throw RError.error(SHOW_CALLER, NO_POSITIVE_PROBABILITIES);
        }
        for (int i = 0; i < p.length; i++) {
            p[i] /= sum;
        }
    }
}
