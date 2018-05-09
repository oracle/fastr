/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 2000-2016, The R Core Team
 * Copyright (c) 2003-2016, The R Foundation
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
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
            while (true) {
                if (y == 0 || (z = distributionFunc.eval(y - incr, true, false)) < p) {
                    return y;
                }
                y = RMath.fmax2(0, y - incr);
            }
        } else {
            while (true) {
                y = moveRight(y, incr);
                if ((rightSearchLimit > 0 && y == rightSearchLimit) || (z = distributionFunc.eval(y, true, false)) >= p) {
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

    @FunctionalInterface
    public interface DistributionFunc {
        double eval(double quantile, boolean lowerTail, boolean logP);
    }
}
