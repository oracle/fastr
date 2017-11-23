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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notIntNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
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
import com.oracle.truffle.r.nodes.function.opt.UpdateShareableChildValueNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.nmath.distr.RMultinom;
import com.oracle.truffle.r.runtime.nmath.distr.Rbinom;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.rng.RRNG;

/**
 * Implements the vectorization of {@link RMultinom}.
 */
public abstract class RMultinomNode extends RExternalBuiltinNode.Arg3 {

    private final Rbinom rbinom = new Rbinom();

    private final ValueProfile randGeneratorClassProfile = ValueProfile.createClassProfile();
    private final ConditionProfile hasAttributesProfile = ConditionProfile.createBinaryProfile();
    @Child private UpdateShareableChildValueNode updateSharedAttributeNode = UpdateShareableChildValueNode.create();
    @Child private GetFixedAttributeNode getNamesNode = GetFixedAttributeNode.createNames();
    @Child private SetFixedAttributeNode setDimNamesNode = SetFixedAttributeNode.createDimNames();

    public static RMultinomNode create() {
        return RMultinomNodeGen.create();
    }

    static {
        Casts casts = new Casts(RMultinomNode.class);
        casts.arg(0).asIntegerVector().findFirst().mustBe(notIntNA(), Message.INVALID_FIRST_ARGUMENT_NAME, "n");
        casts.arg(1).asIntegerVector().findFirst().mustBe(notIntNA(), Message.INVALID_SECOND_ARGUMENT_NAME, "size");
        casts.arg(2).mustBe(missingValue().not(), Message.ARGUMENT_MISSING, "prob").mapIf(nullValue(), emptyDoubleVector()).asDoubleVector();
    }

    @Override
    protected RBaseNode getErrorContext() {
        return RError.SHOW_CALLER;
    }

    @Specialization
    protected RIntVector doMultinom(int n, int size, RAbstractDoubleVector probs,
                    @Cached("probs.access()") VectorAccess probsAccess) {
        try (SequentialIterator probsIter = probsAccess.access(probs)) {
            double sum = 0.0;
            while (probsAccess.next(probsIter)) {
                double prob = probsAccess.getDouble(probsIter);
                if (!Double.isFinite(prob)) {
                    throw error(NA_IN_PROB_VECTOR);
                }
                if (prob < 0.0) {
                    throw error(NEGATIVE_PROBABILITY);
                }
                sum += prob;
            }
            if (sum == 0) {
                throw error(NO_POSITIVE_PROBABILITIES);
            }

            RRNG.getRNGState();
            RandomNumberProvider rand = new RandomNumberProvider(randGeneratorClassProfile.profile(RRNG.currentGenerator()), RRNG.currentNormKind());
            int[] result = new int[probsAccess.getLength(probsIter) * n];
            if (size > 0) {
                for (int i = 0, ik = 0; i < n; i++, ik += probsAccess.getLength(probsIter)) {
                    double currentSum = sum;
                    int currentSize = size;
                    /* Generate the first K-1 obs. via binomials */
                    probsAccess.reset(probsIter);
                    for (int k = 0; probsAccess.next(probsIter) && k < probsAccess.getLength(probsIter) - 1; k++) {
                        /* (p_tot, n) are for "remaining binomial" */
                        /* LDOUBLE */double probK = probsAccess.getDouble(probsIter);
                        if (probK != 0.) {
                            double pp = probK / currentSum;
                            int value = (pp < 1.) ? (int) rbinom.execute(currentSize, pp, rand) : currentSize;
                            /*
                             * >= 1; > 1 happens because of rounding
                             */
                            result[ik + k] = value;
                            currentSize -= value;
                        } else {
                            result[ik + k] = 0;
                        }
                        if (n <= 0) {
                            /* we have all */
                            break;
                        }
                        /* i.e. = sum(prob[(k+1):K]) */
                        currentSum -= probK;
                    }

                    result[ik + probsAccess.getLength(probsIter) - 1] = currentSize;
                }
            }
            RRNG.putRNGState();

            // take names from probVec (if any) as row names in the result
            RIntVector resultVec = RDataFactory.createIntVector(result, true, new int[]{probsAccess.getLength(probsIter), n});
            if (hasAttributesProfile.profile(probs.getAttributes() != null)) {
                Object probsNames = getNamesNode.execute(probs.getAttributes());
                updateSharedAttributeNode.execute(probs, probsNames);
                Object[] dimnamesData = new Object[]{probsNames, RNull.instance};
                setDimNamesNode.execute(resultVec.getAttributes(), RDataFactory.createList(dimnamesData));
            }
            return resultVec;
        }
    }
}
