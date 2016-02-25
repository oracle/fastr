/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-1997, Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998, Ross Ihaka
 * Copyright (c) 1998-2012, The R Core Team
 * Copyright (c) 2005, The R Foundation
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.ops;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class UnaryArithmetic extends Operation {

    public static final UnaryArithmeticFactory NEGATE = Negate::new;
    public static final UnaryArithmeticFactory ROUND = Round::new;
    public static final UnaryArithmeticFactory FLOOR = Floor::new;
    public static final UnaryArithmeticFactory TRUNC = Trunc::new;
    public static final UnaryArithmeticFactory CEILING = Ceiling::new;
    public static final UnaryArithmeticFactory PLUS = Plus::new;
    public static final UnaryArithmeticFactory[] ALL = new UnaryArithmeticFactory[]{NEGATE, ROUND, FLOOR, CEILING, PLUS};

    public UnaryArithmetic() {
        super(false, false);
    }

    public abstract int op(byte op);

    public abstract int op(int op);

    public abstract double op(double op);

    public abstract double opd(double op, int digits);

    public abstract RComplex op(double re, double im);

    public abstract RComplex opd(double re, double im, int digits);

    public static class Negate extends UnaryArithmetic {

        @Override
        public int op(int op) {
            return -op;
        }

        @Override
        public double op(double op) {
            return -op;
        }

        @Override
        public int op(byte op) {
            return -(int) op;
        }

        @Override
        public RComplex op(double re, double im) {
            return RDataFactory.createComplex(op(re), op(im));
        }

        @Override
        @TruffleBoundary
        public double opd(double op, int digits) {
            throw new UnsupportedOperationException();
        }

        @Override
        @TruffleBoundary
        public RComplex opd(double re, double im, int digits) {
            throw new UnsupportedOperationException();
        }

    }

    public static class Plus extends UnaryArithmetic {

        @Override
        public int op(int op) {
            return op;
        }

        @Override
        public double op(double op) {
            return op;
        }

        @Override
        public int op(byte op) {
            return op;
        }

        @Override
        public RComplex op(double re, double im) {
            return RDataFactory.createComplex(re, im);
        }

        @Override
        @TruffleBoundary
        public double opd(double op, int digits) {
            throw new UnsupportedOperationException();
        }

        @Override
        @TruffleBoundary
        public RComplex opd(double re, double im, int digits) {
            throw new UnsupportedOperationException();
        }

    }

    public static class Round extends UnaryArithmetic {

        @Child private BinaryArithmetic pow;

        @Override
        public int op(int op) {
            return op;
        }

        @Override
        public double op(double op) {
            return Math.rint(op);
        }

        @Override
        public int op(byte op) {
            return op;
        }

        @Override
        public RComplex op(double re, double im) {
            return RDataFactory.createComplex(op(re), op(im));
        }

        @Override
        public double opd(double op, int digits) {
            return fround(op, digits);
        }

        @Override
        public RComplex opd(double re, double im, int digits) {
            return zrround(re, im, digits);
        }

        // The logic for fround and zrround (z_rround) is derived from GNU R.

        private static final int F_MAX_DIGITS = 308; // IEEE constant

        private double fround(double x, double digits) {
            double pow10;
            double sgn;
            double intx;
            int dig;

            if (Double.isNaN(x) || Double.isNaN(digits)) {
                return x + digits;
            }
            if (!RRuntime.isFinite(x)) {
                return x;
            }
            if (digits == Double.POSITIVE_INFINITY) {
                return x;
            } else if (digits == Double.NEGATIVE_INFINITY) {
                return 0.0;
            }

            double dd = digits;
            double xx = x;

            if (dd > F_MAX_DIGITS) {
                dd = F_MAX_DIGITS;
            }

            dig = (int) Math.floor(dd + 0.5);
            if (xx < 0.0) {
                sgn = -1.0;
                xx = -xx;
            } else {
                sgn = 1.0;
            }

            if (dig == 0) {
                return sgn * op(xx);
            } else if (dig > 0) {
                pow10 = rpowdi(10.0, dig);
                intx = Math.floor(xx);
                // System.out.println(String.format("X %.22f RINT1 %.22f POW10 %.22f INTX %.22f",
                // new BigDecimal(x),
                // new BigDecimal(Math.rint((xx - intx) * pow10)), new BigDecimal(pow10),
                // new BigDecimal(intx)));
                return sgn * (intx + Math.rint((xx - intx) * pow10) / pow10);
            } else {
                pow10 = rpowdi(10.0, -dig);
                // System.out.println(String.format("RINT2 %.22f", new BigDecimal(Math.rint(xx /
                // pow10))));
                return sgn * Math.rint(xx / pow10) * pow10;
            }
        }

        private double rpowdi(double x, int n) {
            double result = 1.0;

            if (Double.isNaN(x)) {
                return x;
            }
            if (n != 0) {
                if (!RRuntime.isFinite(x)) {
                    return rpow(x, n);
                }
                int nn = n;
                double xx = x;
                boolean isNeg = (n < 0);
                if (isNeg) {
                    nn = -nn;
                }
                for (;;) {
                    if ((nn & 1) != 0) {
                        result *= xx;
                    }
                    if ((nn >>= 1) != 0) {
                        xx *= xx;
                    } else {
                        break;
                    }
                }
                if (isNeg) {
                    result = 1.0 / result;
                }
            }
            return result;
        }

        private static double myfmod(double x1, double x2) {
            double q = x1 / x2;
            return x1 - Math.floor(q) * x2;
        }

        private double rpow(double x, double y) {
            if (x == 1.0 || y == 0.0) {
                return 1.0;
            }
            if (x == 0.0) {
                if (y > 0.0) {
                    return 0.0;
                }
                return Double.POSITIVE_INFINITY;
            }
            if (RRuntime.isFinite(x) && RRuntime.isFinite(y)) {
                if (pow == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    pow = insert(BinaryArithmetic.POW.create());
                }
                return pow.op(x, y);
            }
            if (Double.isNaN(x) || Double.isNaN(y)) {
                return x + y; // assuming IEEE 754; otherwise return NaN
            }
            if (!RRuntime.isFinite(x)) {
                if (x > 0) { /* Inf ^ y */
                    return (y < 0.0) ? 0.0 : Double.POSITIVE_INFINITY;
                } else { /* (-Inf) ^ y */
                    if (RRuntime.isFinite(y) && y == Math.floor(y)) { /* (-Inf) ^ n */
                        return (y < 0.0) ? 0.0 : (myfmod(y, 2.0) != 0.0 ? x : -x);
                    }
                }
            }
            if (!RRuntime.isFinite(y)) {
                if (x >= 0) {
                    if (y > 0) { /* y == +Inf */
                        return (x >= 1) ? Double.POSITIVE_INFINITY : 0.0;
                    } else {
                        /* y == -Inf */
                        return (x < 1) ? Double.POSITIVE_INFINITY : 0.0;
                    }
                }
            }
            // all other cases: (-Inf)^{+-Inf, non-int}; (neg)^{+-Inf}
            return Double.NaN;
        }

        private RComplex zrround(double re, double im, int digits) {
            return RDataFactory.createComplex(fround(re, digits), fround(im, digits));
        }

    }

    public static class Floor extends Round {

        @Override
        public double op(double op) {
            return Math.floor(op);
        }
    }

    public static class Trunc extends Round {

        @Override
        public double op(double op) {
            if (op > 0) {
                return Math.floor(op);
            } else {
                return Math.ceil(op);
            }
        }
    }

    public static class Ceiling extends Round {

        @Override
        public double op(double op) {
            return Math.ceil(op);
        }
    }

}
