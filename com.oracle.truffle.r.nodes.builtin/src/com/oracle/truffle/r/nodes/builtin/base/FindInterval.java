/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2002--2016, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_ARITHMETIC;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

/**
 * Note: R wrapper function {@code findInterval(x,vec,...)} has first two arguments swapped.
 */
@RBuiltin(name = "findInterval", kind = INTERNAL, parameterNames = {"xt", "x", "rightmost.closed", "all.inside", "left.open"}, behavior = PURE_ARITHMETIC)
public abstract class FindInterval extends RBuiltinNode.Arg5 {

    static {
        Casts casts = new Casts(FindInterval.class);
        casts.arg("xt").mustBe(doubleValue(), Message.INVALID_INPUT).asDoubleVector();
        casts.arg("x").mustBe(doubleValue(), Message.INVALID_INPUT).asDoubleVector();
        casts.arg("rightmost.closed").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        casts.arg("all.inside").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("left.open").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization(guards = {"xtAccess.supports(xt)", "xAccess.supports(x)"})
    RAbstractIntVector doFindInterval(RAbstractDoubleVector xt, RAbstractDoubleVector x, boolean right, boolean inside, boolean leftOpen,
                    @Cached("createEqualityProfile()") ValueProfile leftOpenProfile,
                    @Cached("create(xt)") VectorAccess xtAccess,
                    @Cached("create(xt)") VectorAccess xAccess,
                    @Cached("create()") VectorFactory vectorFactory) {
        boolean leftOpenProfiled = leftOpenProfile.profile(leftOpen);
        try (SequentialIterator xIter = xAccess.access(x)) {
            int[] result = new int[xAccess.getLength(xIter)];
            int i = 0;
            boolean complete = true;
            int previous = 1;
            while (xAccess.next(xIter)) {
                if (xAccess.isNA(xIter)) {
                    previous = RRuntime.INT_NA;
                    complete = false;
                } else {
                    try (RandomIterator xtIter = xtAccess.randomAccess(xt)) {
                        previous = findInterval2(xtAccess, xtIter, xAccess.getDouble(xIter), right, inside, leftOpenProfiled, previous);
                    }
                }
                result[i++] = previous;
            }
            return vectorFactory.createIntVector(result, complete);
        }
    }

    @Specialization(replaces = "doFindInterval")
    RAbstractIntVector doFindIntervalGeneric(RAbstractDoubleVector xt, RAbstractDoubleVector x, boolean right, boolean inside, boolean leftOpen,
                    @Cached("createEqualityProfile()") ValueProfile leftOpenProfile,
                    @Cached("create()") VectorFactory factory) {
        return doFindInterval(xt, x, right, inside, leftOpen, leftOpenProfile, xt.slowPathAccess(), x.slowPathAccess(), factory);
    }

    // transcribed from appl/interv.c

    private int findInterval2(VectorAccess xtAccess, RandomIterator xtIter, double x, boolean right, boolean inside, boolean leftOpen, int iloIn) {
        int n = xtAccess.getLength(xtIter);
        if (n == 0) {
            return 0;
        }

        // Note: GNUR code is written with 1-based indexing (by shifting the pointer), we subtract
        // one in each vector access
        int ilo = iloIn;
        if (ilo <= 0) {
            double xt0 = xtAccess.getDouble(xtIter, 0);
            if (xsmlr(leftOpen, x, xt0)) {
                return leftBoundary(right, inside, x, xt0);
            }
            ilo = 1;
        }
        int ihi = ilo + 1;
        if (ihi >= n) {
            double xtLast = xtAccess.getDouble(xtIter, n - 1);
            if (xgrtr(leftOpen, x, xtLast)) {
                return rightBoundary(right, inside, x, xtLast, n);
            }
            if (n <= 1) {
                /* x < xt[1] */
                return leftBoundary(right, inside, x, xtAccess.getDouble(xtIter, 0));
            }
            ilo = n - 1;
            ihi = n;
        }

        if (xsmlr(leftOpen, x, xtAccess.getDouble(xtIter, ihi - 1))) {
            if (xgrtr(leftOpen, x, xtAccess.getDouble(xtIter, ilo - 1))) {
                /* `lucky': same interval as last time */
                return ilo;
            }
            /* **** now x < xt[ilo] . decrease ilo to capture x */
            int istep = 1;
            boolean done = false;
            while (true) {
                ihi = ilo;
                ilo = ihi - istep;
                if (ilo <= 1) {
                    break;
                }
                double xtilo = xtAccess.getDouble(xtIter, ilo - 1);
                if ((leftOpen && x > xtilo) || (!leftOpen && x >= xtilo)) {
                    done = true;
                    break;
                }
                istep *= 2;
            }
            if (!done) {
                ilo = 1;
                double xt0 = xtAccess.getDouble(xtIter, 0);
                if (xsmlr(leftOpen, x, xt0)) {
                    return leftBoundary(right, inside, x, xt0);
                }
            }
        } else {
            /* **** now x >= xt[ihi] . increase ihi to capture x */
            int istep = 1;
            boolean done = false;
            while (true) {
                ilo = ihi;
                ihi = ilo + istep;
                if (ihi >= n) {
                    break;
                }
                double xtihi = xtAccess.getDouble(xtIter, ihi - 1);
                if ((leftOpen && x <= xtihi) || (!leftOpen && x < xtihi)) {
                    done = true;
                    break;
                }
                istep *= 2;
            }
            if (!done) {
                double xtLast = xtAccess.getDouble(xtIter, n - 1);
                if (xgrtr(leftOpen, x, xtLast)) {
                    return rightBoundary(right, inside, x, xtLast, n);
                }
                ihi = n;
            }
        }

        // L50 and L51 in the original GNUR source differ only by ">" vs ">=" depending on leftOpen
        assert ilo <= ihi;
        while (true) {
            int middle = (ilo + ihi) / 2;
            if (middle == ilo) {
                return ilo;
            }
            /* note. it is assumed that middle = ilo in case ihi = ilo+1 . */
            double xtMiddle = xtAccess.getDouble(xtIter, middle - 1);
            if ((!leftOpen && x >= xtMiddle) || (leftOpen && x > xtMiddle)) {
                ilo = middle;
            } else {
                ihi = middle;
            }
        }
    }

    private static int leftBoundary(boolean right, boolean inside, double x, double xt0) {
        return ((inside || (right && x == xt0)) ? 1 : 0);
    }

    private static int rightBoundary(boolean right, boolean inside, double x, double xtLast, int n) {
        return ((inside || (right && x == xtLast)) ? (n - 1) : n);
    }

    private static boolean xsmlr(boolean leftOpen, double x, double val) {
        return x < val || (leftOpen && x <= val);
    }

    private static boolean xgrtr(boolean leftOpen, double x, double val) {
        return x > val || (!leftOpen && x >= val);
    }
}
