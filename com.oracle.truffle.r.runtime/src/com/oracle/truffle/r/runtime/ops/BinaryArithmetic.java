/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2012-2013, Purdue University
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.ops;

import static com.oracle.truffle.r.runtime.RRuntime.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/*
 * Logic derived from GNU-R, Purdue FastR and gcc.
 */
public abstract class BinaryArithmetic extends Operation {

    public static final BinaryArithmeticFactory ADD = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new Add();
        }
    };
    public static final BinaryArithmeticFactory SUBTRACT = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new Subtract();
        }
    };
    public static final BinaryArithmeticFactory MULTIPLY = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new Multiply();
        }
    };
    public static final BinaryArithmeticFactory INTEGER_DIV = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new IntegerDiv();
        }
    };

    public static final BinaryArithmeticFactory DIV = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new Div();
        }
    };

    public static final BinaryArithmeticFactory MOD = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new Mod();
        }
    };

    public static final BinaryArithmeticFactory POW = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new Pow();
        }
    };

    public static final BinaryArithmeticFactory MAX = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new Max();
        }
    };

    public static final BinaryArithmeticFactory MIN = new BinaryArithmeticFactory() {

        @Override
        public BinaryArithmetic create() {
            return new Min();
        }
    };

    private final boolean supportsIntResult;

    public BinaryArithmetic(boolean commutative, boolean associative, boolean supportsInt) {
        super(commutative, associative);
        this.supportsIntResult = supportsInt;
    }

    public final boolean isSupportsIntResult() {
        return supportsIntResult;
    }

    public abstract int op(int left, int right);

    public abstract double op(double left, double right);

    public abstract RComplex op(double leftReal, double leftImag, double rightReal, double rightImag);

    public static double fmod(double a, double b) {
        // LICENSE: transcribed code from GNU R, which is licensed under GPL
        double q = a / b;
        if (b != 0) {
            double tmp = a - Math.floor(q) * b;
            if (RRuntime.isFinite(q) && Math.abs(q) > 1 / RRuntime.EPSILON) {
                // TODO support warning here
                throw new UnsupportedOperationException();
            }
            return tmp - Math.floor(tmp / b) * b;
        } else {
            return Double.NaN;
        }
    }

    private static double convertInf(double d) {
        // This code is transcribed from FastR.
        return Math.copySign(Double.isInfinite(d) ? 1 : 0, d);
    }

    private static double convertNaN(double d) {
        if (Double.isNaN(d)) {
            return Math.copySign(0, d);
        } else {
            return d;
        }
    }

    public static class Add extends BinaryArithmetic {

        public Add() {
            super(true, true, true);
        }

        @Override
        public int op(int left, int right) {
            try {
                return ExactMath.addExact(left, right);
            } catch (ArithmeticException e) {
                CompilerDirectives.transferToInterpreter();
                replace(new AddOverflow());
                return INT_NA;
            }
        }

        @Override
        public double op(double left, double right) {
            return left + right;
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            return RDataFactory.createComplex(op(leftReal, rightReal), op(leftImag, rightImag));
        }

    }

    private static final class AddOverflow extends Add {

        @Override
        public int op(int left, int right) {
            // Borrowed from ExactMath.addExact
            int r = left + right;
            if (((left ^ r) & (right ^ r)) < 0) {
                // TODO introduced NA call
                return INT_NA;
            }
            return r;
        }

    }

    public static class Subtract extends BinaryArithmetic {

        public Subtract() {
            super(false, false, true);
        }

        @Override
        public int op(int left, int right) {
            try {
                return ExactMath.subtractExact(left, right);
            } catch (ArithmeticException e) {
                CompilerDirectives.transferToInterpreter();
                replace(new SubtractOverflow());
                return INT_NA;
            }
        }

        @Override
        public double op(double left, double right) {
            return left - right;
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            return RDataFactory.createComplex(op(leftReal, rightReal), op(leftImag, rightImag));
        }
    }

    private static final class SubtractOverflow extends Subtract {

        @Override
        public int op(int left, int right) {
            // Borrowed from ExactMath.removeExact
            int r = left - right;
            if (((left ^ right) & (left ^ r)) < 0) {
                return INT_NA;
            }
            return r;
        }

    }

    public static class Multiply extends BinaryArithmetic {

        public Multiply() {
            super(true, true, true);
        }

        @Override
        public int op(int left, int right) {
            try {
                return ExactMath.multiplyExact(left, right);
            } catch (ArithmeticException e) {
                CompilerDirectives.transferToInterpreter();
                replace(new MultiplyOverflow());
                return INT_NA;
            }
        }

        @Override
        public final double op(double left, double right) {
            return left * right;
        }

        // The code for complex multiplication is transcribed from FastR:
        // LICENSE: this code is derived from the multiplication code, which is transcribed code
        // from GCC, which is licensed under GPL

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            double[] res = new double[2];
            double[] interm = new double[4];
            complexMult(leftReal, leftImag, rightReal, rightImag, res, interm);
            if (Double.isNaN(res[0]) && Double.isNaN(res[1])) {
                CompilerDirectives.transferToInterpreter();
                MultiplyNaN multNaN = new MultiplyNaN();
                replace(multNaN);
                return MultiplyNaN.handleNaN(leftReal, leftImag, rightReal, rightImag, res, interm);
            }
            return RDataFactory.createComplex(res[0], res[1]);
        }

        protected final void complexMult(double leftReal, double leftImag, double rightReal, double rightImag, double[] res, double[] interm) {
            interm[0] = op(leftReal, rightReal);
            interm[1] = op(leftImag, rightImag);
            interm[2] = op(leftImag, rightReal);
            interm[3] = op(leftReal, rightImag);
            res[0] = interm[0] - interm[1];
            res[1] = interm[2] + interm[3];
        }
    }

    private static final class MultiplyOverflow extends Multiply {

        @Override
        public int op(int left, int right) {
            // Borrowed from ExactMath.removeExact
            long r = (long) left * (long) right;
            if ((int) r != r) {
                return INT_NA;
            }
            return (int) r;
        }

    }

    private static final class MultiplyNaN extends Multiply {

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            double[] res = new double[2];
            double[] interm = new double[4];
            complexMult(leftReal, leftImag, rightReal, rightImag, res, interm);
            if (Double.isNaN(res[0]) && Double.isNaN(res[1])) {
                return handleNaN(leftReal, leftImag, rightReal, rightImag, res, interm);
            }
            return RDataFactory.createComplex(res[0], res[1]);
        }

        protected static RComplex handleNaN(double leftReal, double leftImag, double rightReal, double rightImag, double[] res, double[] interm) {
            boolean recalc = false;
            double ra = leftReal;
            double rb = leftImag;
            double rc = rightReal;
            double rd = rightImag;
            if (Double.isInfinite(ra) || Double.isInfinite(rb)) {
                ra = convertInf(ra);
                rb = convertInf(rb);
                rc = convertNaN(rc);
                rd = convertNaN(rd);
                recalc = true;
            }
            if (Double.isInfinite(rc) || Double.isInfinite(rd)) {
                rc = convertInf(rc);
                rd = convertInf(rd);
                ra = convertNaN(ra);
                rb = convertNaN(rb);
                recalc = true;
            }
            if (!recalc && (Double.isInfinite(interm[0]) || Double.isInfinite(interm[1]) || Double.isInfinite(interm[2]) || Double.isInfinite(interm[3]))) {
                ra = convertNaN(ra);
                rb = convertNaN(rb);
                rc = convertNaN(rc);
                rd = convertNaN(rd);
                recalc = true;
            }
            if (recalc) {
                res[0] = Double.POSITIVE_INFINITY * (ra * rc - rb * rd);
                res[1] = Double.POSITIVE_INFINITY * (ra * rd + rb * rc);
            }
            return RDataFactory.createComplex(res[0], res[1]);
        }

    }

    private static class Div extends BinaryArithmetic {

        public Div() {
            super(false, false, false);
        }

        @Override
        public final int op(int left, int right) {
            throw new AssertionError();
        }

        @Override
        public final double op(double left, double right) {
            return left / right;
        }

        // The code for complex division is transcribed from FastR:
        // LICENSE: transcribed code from GCC, which is licensed under GPL
        // libgcc2

        @CompilerDirectives.CompilationFinal private boolean everSeenNaN = false;

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            double real;
            double imag;

            if (Math.abs(rightReal) < Math.abs(rightImag)) {
                double ratio = rightReal / rightImag;
                double denom = (rightReal * ratio) + rightImag;
                real = ((leftReal * ratio) + leftImag) / denom;
                imag = ((leftImag * ratio) - leftReal) / denom;
            } else {
                double ratio = rightImag / rightReal;
                double denom = (rightImag * ratio) + rightReal;
                real = ((leftImag * ratio) + leftReal) / denom;
                imag = (leftImag - (leftReal * ratio)) / denom;
            }

            if (Double.isNaN(real) && Double.isNaN(imag)) {
                if (!everSeenNaN) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    everSeenNaN = true;
                }
                if (rightReal == 0.0 && rightImag == 0.0 && (!Double.isNaN(leftReal) || !Double.isNaN(leftImag)) && leftReal != 0.0 && rightReal != 0.0) {
                    real = Math.copySign(Double.POSITIVE_INFINITY, rightReal) * leftReal;
                    imag = Math.copySign(Double.POSITIVE_INFINITY, rightReal) * leftImag;
                } else if ((Double.isInfinite(leftReal) || Double.isInfinite(leftImag)) && isFinite(rightReal) && isFinite(rightImag)) {
                    double ra = convertInf(leftReal);
                    double rb = convertInf(leftImag);
                    real = Double.POSITIVE_INFINITY * (ra * rightReal + rb * rightImag);
                    imag = Double.POSITIVE_INFINITY * (rb * rightReal - ra * rightImag);
                } else if ((Double.isInfinite(rightReal) || Double.isInfinite(rightImag)) && isFinite(leftReal) && isFinite(leftImag)) {
                    double rc = convertInf(rightReal);
                    double rd = convertInf(rightImag);
                    real = 0.0 * (leftReal * rc + leftImag * rd);
                    imag = 0.0 * (leftImag * rc - leftReal * rd);
                } else {
                    real = Double.NaN;
                    imag = Double.NaN;
                }
            }

            return RDataFactory.createComplex(real, imag);
        }
    }

    private static final class IntegerDiv extends BinaryArithmetic {

        public IntegerDiv() {
            super(false, false, true);
        }

        @Override
        public int op(int left, int right) {
            if (right != 0) {
                return (int) Math.floor((double) left / (double) right);
            } else {
                return RRuntime.INT_NA;
            }
        }

        @Override
        public double op(double a, double b) {
            double q = a / b;
            if (b != 0) {
                double qfloor = Math.floor(q);
                double tmp = a - qfloor * b;
                return qfloor + Math.floor(tmp / b);

            } else {
                return q;
            }
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            throw new UnsupportedOperationException("unsupported complex operation");
        }
    }

    public static final class Mod extends BinaryArithmetic {

        public Mod() {
            super(false, false, true);
        }

        @Override
        public int op(int left, int right) {
            // LICENSE: transcribed code from GNU R, which is licensed under GPL
            if (right != 0) {
                if (left >= 0 && right > 0) {
                    return left % right;
                } else {
                    return (int) fmod(left, right);
                }
            } else {
                return RRuntime.INT_NA;
            }
        }

        @Override
        public double op(double left, double right) {
            return fmod(left, right);
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            throw RError.getUnimplementedComplex(this.getEncapsulatingSourceSection());
        }
    }

    public static class Pow extends BinaryArithmetic {

        @CompilerDirectives.CompilationFinal private boolean exponentAlwaysTwo = true;
        @CompilerDirectives.CompilationFinal private boolean exponentAlwaysInteger = true;
        @CompilerDirectives.CompilationFinal private boolean exponentAlwaysPositiveInteger = true;

        public Pow() {
            super(false, false, false);
        }

        @Override
        public int op(int left, int right) {
            throw new AssertionError();
        }

        @Override
        public double op(double a, double b) {
            int castExponent = (int) b;

            // Special case with exponent always two.
            if (exponentAlwaysTwo) {
                if (castExponent == 2) {
                    return a * a;
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    exponentAlwaysTwo = false;
                }
            }

            // Special case with exponent always integer.
            if (exponentAlwaysInteger) {
                if (castExponent == b) {
                    if (exponentAlwaysPositiveInteger) {
                        if (castExponent >= 0) {
                            return positivePow(a, castExponent);
                        } else {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            exponentAlwaysPositiveInteger = false;
                        }
                    }
                    if (castExponent < 0) {
                        if (a == 0.0) {
                            return Double.POSITIVE_INFINITY;
                        }
                        return 1 / positivePow(a, -castExponent);
                    }
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    exponentAlwaysInteger = false;
                }
            }

            // Generic case with double exponent.
            if (isFinite(a) && isFinite(b)) {
                return Math.pow(a, b);
            }
            CompilerDirectives.transferToInterpreter();
            return replace(new PowFull()).op(a, b);
        }

        private static double positivePow(double operand, int castExponent) {
            int exponent = castExponent;
            double result = 1;
            double base = operand;
            while (exponent > 0) {
                if ((exponent & 1) == 1) {
                    result *= base;
                }
                exponent >>= 1;
                base *= base;
            }
            return result;
        }

        // The code for complex pow is transcribed from FastR:
        // LICENSE: transcribed code from GNU R, which is licensed under GPL
        // LICENSE: transcribed code from GCC, which is licensed under GPL
        // LICENSE: transcribed code from GCC, which is licensed under GPL
        // libgcc2
        // LICENSE: this code is derived from the multiplication code, which is transcribed code
        // from GCC, which is licensed under GPL
        // LICENSE: this code is derived from the division code, which is transcribed code from
        // GCC, which is licensed under GPL

        @Child private CHypot chypot;
        @Child protected Multiply mult;
        @Child protected CPow2 cpow2;

        protected void ensurePowKNodes() {
            // all or nothing: checking just one is sufficient
            if (mult == null) {
                CompilerDirectives.transferToInterpreter();
                mult = insert(new Multiply());
                cpow2 = insert(new CPow2());
            }
        }

        protected void ensurePow2() {
            if (cpow2 == null) {
                CompilerDirectives.transferToInterpreter();
                cpow2 = insert(new CPow2());
            }
        }

        private void ensureChypot() {
            if (chypot == null) {
                CompilerDirectives.transferToInterpreter();
                chypot = insert(new CHypot());
            }
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag == 0.0) {
                CompilerDirectives.transferToInterpreter();
                if (rightReal == 0.0) {
                    return replace(new Pow0()).op(leftReal, leftImag, rightReal, rightImag);
                } else if (rightReal == 1.0) {
                    return replace(new Pow1()).op(leftReal, leftImag, rightReal, rightImag);
                } else if (rightReal == 2.0) {
                    return replace(new Pow2()).op(leftReal, leftImag, rightReal, rightImag);
                } else if (rightReal < 0.0) {
                    return replace(new PowNegative()).op(leftReal, leftImag, rightReal, rightImag);
                } else {
                    return replace(new PowK()).op(leftReal, leftImag, rightReal, rightImag);
                }
            }
            return powc(leftReal, leftImag, rightReal, rightImag);
        }

        protected RComplex powk(double leftReal, double leftImag, int k) {
            RComplex x = RDataFactory.createComplex(leftReal, leftImag);
            RComplex z = RDataFactory.createComplex(1.0, 0.0);

            int kk = k;
            while (kk > 0) {
                if ((kk & 1) != 0) {
                    // "z = z * X"
                    z = mult.op(z.getRealPart(), z.getImaginaryPart(), x.getRealPart(), x.getImaginaryPart());
                    if (kk == 1) {
                        break;
                    }
                }
                kk = kk / 2;
                // "X = X * X"
                x = cpow2.cpow2(x.getRealPart(), x.getImaginaryPart());
            }

            return z;
        }

        protected RComplex powc(double leftReal, double leftImag, double rightReal, double rightImag) {
            ensureChypot();
            double zr = chypot.chypot(leftReal, leftImag);
            double zi = Math.atan2(leftImag, leftReal);
            double theta = zi * rightReal;
            zr = Math.log(zr);
            theta += zr * rightImag;
            double rho = Math.exp(zr * rightReal - zi * rightImag);

            return RDataFactory.createComplex(rho * Math.cos(theta), rho * Math.sin(theta));
        }

        // The code for chypot was transcribed from FastR:
        // after libgcc2's x86 hypot - note the sign of NaN below (what GNU-R uses)
        // note that Math.hypot in Java is _very_ slow as it tries to be more precise

        private static class CHypot extends Node {

            @CompilerDirectives.CompilationFinal private boolean everSeenInfinite = false;

            public double chypot(double real, double imag) {
                double res = Math.sqrt(real * real + imag * imag);
                if (!isFinite(real) || !isFinite(imag)) {
                    if (!everSeenInfinite) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        everSeenInfinite = true;
                    }
                    if (Double.isInfinite(real) || Double.isInfinite(imag)) {
                        res = Double.POSITIVE_INFINITY;
                    } else if (Double.isNaN(imag)) {
                        res = imag;
                    } else {
                        res = real;
                    }
                }
                return res;
            }

        }

        private static class CPow2 extends Node {

            @CompilerDirectives.CompilationFinal boolean everSeenNaN = false;

            public RComplex cpow2(double cre, double cim) {
                double cre2 = cre * cre;
                double cim2 = cim * cim;
                double crecim = cre * cim;
                double real = cre2 - cim2;
                double imag = 2 * crecim;
                if (Double.isNaN(real) && Double.isNaN(imag)) {
                    if (!everSeenNaN) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        everSeenNaN = true;
                    }
                    boolean recalc = false;
                    double ra = cre;
                    double rb = cim;
                    if (Double.isInfinite(ra) || Double.isInfinite(rb)) {
                        ra = convertInf(ra);
                        rb = convertInf(rb);
                        recalc = true;
                    }
                    if (!recalc && (Double.isInfinite(cre2) || Double.isInfinite(cim2) || Double.isInfinite(crecim))) {
                        ra = convertNaN(ra);
                        rb = convertNaN(rb);
                        recalc = true;
                    }
                    if (recalc) {
                        real = Double.POSITIVE_INFINITY * (ra * ra - rb * rb);
                        imag = Double.POSITIVE_INFINITY * (ra * rb);
                    }
                }
                return RDataFactory.createComplex(real, imag);
            }

        }

    }

    private static final class PowK extends Pow {

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag != 0.0 || ((int) rightReal) != rightReal) {
                CompilerDirectives.transferToInterpreter();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            ensurePowKNodes();
            return powk(leftReal, leftImag, (int) rightReal);
        }

    }

    private static final class Pow0 extends Pow {

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag != 0.0 || rightReal != 0.0) {
                CompilerDirectives.transferToInterpreter();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            return RDataFactory.createComplex(1.0, 0.0);
        }

    }

    private static final class Pow1 extends Pow {

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag != 0.0 || rightReal != 1.0) {
                CompilerDirectives.transferToInterpreter();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            return RDataFactory.createComplex(leftReal, leftImag);
        }

    }

    private static final class Pow2 extends Pow {

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag != 0.0 || rightReal != 2.0) {
                CompilerDirectives.transferToInterpreter();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            ensurePow2();
            return cpow2.cpow2(leftReal, leftImag);
        }

    }

    private static final class PowNegative extends Pow {

        @Child private Pow pow = new PowK();
        @Child protected CReciprocal creciprocal = new CReciprocal();

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag != 0.0 || rightReal >= 0.0) {
                CompilerDirectives.transferToInterpreter();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            RComplex r = pow.op(leftReal, leftImag, -rightReal, 0.0); // x^(-k)
            return creciprocal.creciprocal(r);
        }

        private static class CReciprocal extends Node {

            @CompilerDirectives.CompilationFinal private boolean everSeenNaN = false;

            public RComplex creciprocal(RComplex c) {
                double cre = c.getRealPart();
                double cim = c.getImaginaryPart();
                double real;
                double imag;

                if (Math.abs(cre) < Math.abs(cim)) {
                    double ratio = cre / cim;
                    double denom = (cre * ratio) + cim;
                    real = ratio / denom;
                    imag = -1 / denom;
                } else {
                    double ratio = cim / cre;
                    double denom = (cim * ratio) + cre;
                    real = 1 / denom;
                    imag = -ratio / denom;
                }
                if (Double.isNaN(real) && Double.isNaN(imag)) {
                    if (!everSeenNaN) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        everSeenNaN = true;
                    }
                    if (cre == 0.0 && cim == 0.0) {
                        real = Math.copySign(Double.POSITIVE_INFINITY, cre);
                        imag = Math.copySign(Double.NaN, cre);
                    } else if (Double.isInfinite(cre) || Double.isInfinite(cim)) {
                        double rc = convertInf(cre);
                        double rd = convertInf(cim);
                        real = 0.0 * rc;
                        imag = 0.0 * (-rd);
                    }
                }
                return RDataFactory.createComplex(real, imag);
            }

        }

    }

    private static final class PowFull extends Pow {

        @Override
        public double op(double a, double b) {
            // LICENSE: transcribed code from GNU R, which is licensed under GPL

            // NOTE: Math.pow (which uses FDLIBM) is very slow, the version written in assembly in
            // GLIBC (SSE2 optimized) is about 2x faster

            // arithmetic.c (GNU R)
            if (b == 2.0D) {
                return a * a;
            }
            if (a == 1.0D || b == 0.0D) {
                return 1;
            }
            if (a == 0.0D) {
                if (b > 0.0D) {
                    return 0.0D;
                }
                if (b < 0.0D) {
                    return Double.POSITIVE_INFINITY;
                }
                return b;  // NA or NaN
            }
            if (isFinite(a) && isFinite(b)) {
                return Math.pow(a, b);
            }
            if (isNAorNaN(a) || isNAorNaN(b)) {
                // NA check was before, so this can only mean NaN
                return a + b;
            }
            if (!isFinite(a)) {
                if (a > 0) { // Inf ^ y
                    if (b < 0) {
                        return 0;
                    }
                    return Double.POSITIVE_INFINITY;
                } else if (isFinite(b) && b == Math.floor(b)) { // (-Inf) ^ n
                    if (b < 0) {
                        return 0;
                    }
                    return fmod(b, 2) != 0 ? a : -a;
                }
            }
            if (!isFinite(b)) {
                if (a >= 0) {
                    if (b > 0) {
                        return (a >= 1) ? Double.POSITIVE_INFINITY : 0;
                    }
                    return (a < 1) ? Double.POSITIVE_INFINITY : 0;
                }
            }
            return Double.NaN;
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag == 0.0 && ((int) rightReal) == rightReal) {
                ensurePowKNodes();
                return powk(leftReal, leftImag, (int) rightReal);
            } else {
                return powc(leftReal, leftImag, rightReal, rightImag);
            }
        }

    }

    private static class Max extends BinaryArithmetic {

        public Max() {
            super(true, true, true);
        }

        @Override
        public int op(int left, int right) {
            return Math.max(left, right);
        }

        @Override
        public double op(double left, double right) {
            return Math.max(left, right);
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            throw new UnsupportedOperationException("illegal type 'complex' of argument");
        }
    }

    private static class Min extends BinaryArithmetic {

        public Min() {
            super(true, true, true);
        }

        @Override
        public int op(int left, int right) {
            return Math.min(left, right);
        }

        @Override
        public double op(double left, double right) {
            return Math.min(left, right);
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            throw new UnsupportedOperationException("illegal type 'complex' of argument");
        }
    }

}
