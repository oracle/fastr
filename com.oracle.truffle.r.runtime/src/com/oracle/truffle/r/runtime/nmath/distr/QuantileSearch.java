/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2016, The R Core Team
 * Copyright (c) 2003-2016, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.nmath.distr;

import com.oracle.truffle.r.runtime.nmath.RMath;

/**
 * Searches for a quantile of given random variable using it's distribution function. The search
 * takes steps of given size until it reaches the quantile or until it steps over it. This class and
 * its {@link #simpleSearch(double, double, double)} method correspond to several {@code do_search}
 * functions in GnuR.
 */
public final class QuantileSearch {
    /**
     * This is the value of the distribution function where the search finished last time.
     */
    private double z;
    private final double rightSearchLimit;
    private final DistributionFunc distributionFunc;

    /**
     * @param rightSearchLimit If set to non-negative value, then the search to the right will be
     *            limited by it
     * @param distributionFunc The distribution function, all parameters except the quantile,
     *            lowerTail, and logP are fixed.
     */
    public QuantileSearch(double rightSearchLimit, DistributionFunc distributionFunc) {
        this.rightSearchLimit = rightSearchLimit;
        this.distributionFunc = distributionFunc;
    }

    /**
     * Constructs the object without {@code rightSearchLimit}.
     */
    public QuantileSearch(DistributionFunc distributionFunc) {
        this.rightSearchLimit = -1;
        this.distributionFunc = distributionFunc;
    }

    public double simpleSearch(double yIn, double p, double incr) {
        z = distributionFunc.eval(yIn, true, false);
        return search(yIn, p, incr);
    }

    /**
     * Invokes {@link #simpleSearch(double, double, double)} iteratively dividing the increment step
     * by {@code incrDenominator} until the step is greater than the result times the
     * {@code resultFactor}, then the result is deemed 'close enough' and returned.
     * 
     * @param initialY where to start the search (quantile)
     * @param p the target of the search (probability)
     * @param initialIncr initial value for the increment step
     * @param resultFactor see the method doc.
     * @param incrDenominator see the method doc.
     * @return the quantile (number close to it) for {@code p}.
     */
    public double iterativeSearch(double initialY, double p, double initialIncr, double resultFactor, double incrDenominator) {
        assert initialIncr > 0. : "initialIncr zero or negative. Maybe result of too small initialY?";
        double result = initialY;
        double oldIncr;
        double incr = initialIncr;
        z = distributionFunc.eval(initialY, true, false);
        do {
            oldIncr = incr;
            debugPrintf("QSearch step: result=%.12g, p=%.12g, incr=%.12g\n", result, p, incr);
            result = search(result, p, incr);
            incr = RMath.fmax2(1, Math.floor(incr / incrDenominator));
        } while (oldIncr > 1 && incr > result * resultFactor);
        return result;
    }

    /**
     * The same as {@link #iterativeSearch(double, double, double, double, double)}, but with
     * default values for the missing parameters.
     */
    public double iterativeSearch(double initialY, double p) {
        return iterativeSearch(initialY, p, Math.floor(initialY * 0.001), 1e-15, 100);
    }

    private double search(double yIn, double p, double incr) {
        double y = yIn;
        // are we to the left or right of the desired value -> move to the right or left to get
        // closer
        if (z >= p) {
            debugPrintf("--- QSearch left incr=%.12g\n", incr);
            while (true) {
                if (y == 0 || (z = distributionFuncEval(y - incr)) < p) {
                    return y;
                }
                y = RMath.fmax2(0, y - incr);
            }
        } else {
            debugPrintf("--- QSearch right incr=%.12g\n", incr);
            while (true) {
                y = moveRight(y, incr);
                if ((rightSearchLimit > 0 && y == rightSearchLimit) || (z = distributionFuncEval(y)) >= p) {
                    return y;
                }
            }
        }
    }

    private double moveRight(double y, double incr) {
        if (rightSearchLimit < 0) {
            return y + incr;
        } else {
            return RMath.fmin2(y + incr, rightSearchLimit);
        }
    }

    private double distributionFuncEval(double y) {
        debugPrintf("distributionFunc(%.12g) = ", y);
        double result = distributionFunc.eval(y, true, false);
        debugPrintf("%.12g\n", result);
        return result;
    }

    @FunctionalInterface
    public interface DistributionFunc {
        double eval(double quantile, boolean lowerTail, boolean logP);
    }

    @SuppressWarnings("unused")
    private static void debugPrintf(String fmt, Object... args) {
        // System.out.printf(fmt, args);
    }
}
