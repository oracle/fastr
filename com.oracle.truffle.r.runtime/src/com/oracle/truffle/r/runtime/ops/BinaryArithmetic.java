/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2012-2013, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.ops;

import static com.oracle.truffle.r.runtime.RDispatch.OPS_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.RRuntime.INT_NA;
import static com.oracle.truffle.r.runtime.RRuntime.isFinite;
import static com.oracle.truffle.r.runtime.RRuntime.isNAorNaN;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.nmath.RMath;

/*
 * Logic derived from GNU-R, Purdue FastR and gcc.
 */
/**
 * All methods must be invoked with non-NA values.
 */
public abstract class BinaryArithmetic extends Operation {

    /* Fake RBuiltins to unify the binary operations */

    @RBuiltin(name = "+", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class AddBuiltin {
    }

    @RBuiltin(name = "-", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class SubtractBuiltin {
    }

    @RBuiltin(name = "/", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class DivBuiltin {
    }

    @RBuiltin(name = "%/%", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class IntegerDivBuiltin {
    }

    @RBuiltin(name = "%%", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class ModBuiltin {
    }

    @RBuiltin(name = "*", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class MultiplyBuiltin {
    }

    @RBuiltin(name = "^", kind = PRIMITIVE, parameterNames = {"", ""}, alwaysSplit = true, dispatch = OPS_GROUP_GENERIC, behavior = PURE)
    public static class PowBuiltin {
    }

    public static final BinaryArithmeticFactory ADD = Add::new;
    public static final BinaryArithmeticFactory SUBTRACT = Subtract::new;
    public static final BinaryArithmeticFactory MULTIPLY = Multiply::new;
    public static final BinaryArithmeticFactory INTEGER_DIV = IntegerDiv::new;
    public static final BinaryArithmeticFactory DIV = Div::new;
    public static final BinaryArithmeticFactory MOD = Mod::new;
    public static final BinaryArithmeticFactory POW = Pow::new;
    public static final BinaryArithmeticFactory MAX = Max::new;
    public static final BinaryArithmeticFactory MIN = Min::new;

    public static final BinaryArithmeticFactory[] ALL = {ADD, SUBTRACT, MULTIPLY, INTEGER_DIV, DIV, MOD, POW, MAX, MIN};

    private final boolean supportsIntResult;

    private BinaryArithmetic(boolean commutative, boolean associative, boolean supportsInt) {
        super(commutative, associative);
        this.supportsIntResult = supportsInt;
    }

    public final boolean isSupportsIntResult() {
        return supportsIntResult;
    }

    public boolean introducesNA() {
        return false;
    }

    public abstract String opName();

    public abstract int op(int left, int right);

    public abstract double op(double left, double right);

    public abstract RComplex op(double leftReal, double leftImag, double rightReal, double rightImag);

    public abstract String op(String left, String right);

    private static double convertInf(double d) {
        // This code is transcribed from Purdue FastR.
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
        public String opName() {
            return "+";
        }

        @Override
        public int op(int left, int right) {
            int r = left + right;
            // TODO: not using ExactMath because of perf problems
            if (((left ^ r) & (right ^ r)) < 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                replace(new AddOverflow());
                return INT_NA;
            }
            return r;
        }

        @Override
        public double op(double left, double right) {
            return left + right;
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            return RDataFactory.createComplex(op(leftReal, rightReal), op(leftImag, rightImag));
        }

        @Override
        public String op(String left, String right) {
            throw error(RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    private static final class AddOverflow extends Add {

        @Override
        public boolean introducesNA() {
            return true;
        }

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
        public String opName() {
            return "-";
        }

        @Override
        public int op(int left, int right) {
            int r = left - right;
            // TODO: not using ExactMath because of perf problems
            if (((left ^ right) & (left ^ r)) < 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                replace(new SubtractOverflow());
                return INT_NA;
            }
            return r;
        }

        @Override
        public double op(double left, double right) {
            return left - right;
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            return RDataFactory.createComplex(op(leftReal, rightReal), op(leftImag, rightImag));
        }

        @Override
        public String op(String left, String right) {
            throw error(RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    private static final class SubtractOverflow extends Subtract {

        @Override
        public boolean introducesNA() {
            return true;
        }

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
        public String opName() {
            return "*";
        }

        @Override
        public int op(int left, int right) {
            long r = (long) left * (long) right;
            // TODO: not using ExactMath because of perf problems
            if ((int) r != r) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                replace(new MultiplyOverflow());
                return INT_NA;
            }
            return (int) r;
        }

        @Override
        public final double op(double left, double right) {
            return left * right;
        }

        // The code for complex multiplication is transcribed from Purdue FastR:
        // LICENSE: this code is derived from the multiplication code, which is transcribed code
        // from GCC, which is licensed under GPL

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            double[] res = new double[2];
            double[] interm = new double[4];
            complexMult(leftReal, leftImag, rightReal, rightImag, res, interm);
            /*
             * LStadler: removed the special code for handling NaNs in the result, since it works
             * fine (and the tests are broken either way). to revive it, get it from the history.
             */
            return RDataFactory.createComplex(res[0], res[1]);
        }

        private void complexMult(double leftReal, double leftImag, double rightReal, double rightImag, double[] res, double[] interm) {
            interm[0] = op(leftReal, rightReal);
            interm[1] = op(leftImag, rightImag);
            interm[2] = op(leftImag, rightReal);
            interm[3] = op(leftReal, rightImag);
            res[0] = interm[0] - interm[1];
            res[1] = interm[2] + interm[3];
        }

        @Override
        public String op(String left, String right) {
            throw error(RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    private static final class MultiplyOverflow extends Multiply {

        @Override
        public boolean introducesNA() {
            return true;
        }

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

    public static class Div extends BinaryArithmetic {

        public Div() {
            super(false, false, false);
        }

        @Override
        public String opName() {
            return "/";
        }

        @Override
        public final int op(int left, int right) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public final double op(double left, double right) {
            return left / right;
        }

        // The code for complex division is transcribed from Purdue FastR:
        // LICENSE: transcribed code from GCC, which is licensed under GPL
        // libgcc2

        private final ConditionProfile everSeenNaN = ConditionProfile.createBinaryProfile();
        private final ConditionProfile needCopySign = ConditionProfile.createBinaryProfile();
        private final ConditionProfile leftInf = ConditionProfile.createBinaryProfile();
        private final ConditionProfile rightInf = ConditionProfile.createBinaryProfile();

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

            if (everSeenNaN.profile(Double.isNaN(real) && Double.isNaN(imag))) {
                if (needCopySign.profile(rightReal == 0.0 && rightImag == 0.0 && (!Double.isNaN(leftReal) || !Double.isNaN(leftImag)) && leftReal != 0.0 && rightReal != 0.0)) {
                    real = Math.copySign(Double.POSITIVE_INFINITY, rightReal) * leftReal;
                    imag = Math.copySign(Double.POSITIVE_INFINITY, rightReal) * leftImag;
                } else if (leftInf.profile((Double.isInfinite(leftReal) || Double.isInfinite(leftImag)) && isFinite(rightReal) && isFinite(rightImag))) {
                    double ra = convertInf(leftReal);
                    double rb = convertInf(leftImag);
                    real = Double.POSITIVE_INFINITY * (ra * rightReal + rb * rightImag);
                    imag = Double.POSITIVE_INFINITY * (rb * rightReal - ra * rightImag);
                } else if (rightInf.profile((Double.isInfinite(rightReal) || Double.isInfinite(rightImag)) && isFinite(leftReal) && isFinite(leftImag))) {
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

        @Override
        public String op(String left, String right) {
            throw error(RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    public static final class IntegerDiv extends BinaryArithmetic {

        public IntegerDiv() {
            super(false, false, true);
        }

        @Override
        public boolean introducesNA() {
            return true;
        }

        @Override
        public String opName() {
            return "%/%";
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
            throw error(RError.Message.UNIMPLEMENTED_COMPLEX);
        }

        @Override
        public String op(String left, String right) {
            throw error(RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    public static final class Mod extends BinaryArithmetic {

        @CompilationFinal private boolean tryInt = true;
        private final BranchProfile isNaProfile = BranchProfile.create();
        private final BranchProfile isZeroProfile = BranchProfile.create();
        private final ConditionProfile resultZeroProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile simpleProfile = ConditionProfile.createBinaryProfile();

        public Mod() {
            super(false, false, true);
        }

        @Override
        public boolean introducesNA() {
            return true;
        }

        @Override
        public String opName() {
            return "%%";
        }

        @Override
        public int op(int left, int right) {
            // LICENSE: transcribed code from GNU R, which is licensed under GPL
            if (right == 0) {
                isNaProfile.enter();
                return RRuntime.INT_NA;
            }
            if (left == 0) {
                isZeroProfile.enter();
                return 0;
            }
            int result = left % right;
            if (resultZeroProfile.profile(result == 0)) {
                return 0;
            }
            if (simpleProfile.profile((right > 0) == (result > 0))) {
                return result;
            } else {
                // in R, the result always has the same sign as the divisor
                return result + right;
            }
        }

        @Override
        public double op(double left, double right) {
            if (right == 0) {
                isNaProfile.enter();
                return Double.NaN;
            }
            if (tryInt) {
                int leftInt = (int) left;
                int rightInt = (int) right;
                if (left == leftInt && right == rightInt) {
                    return op(leftInt, rightInt);
                }
                CompilerDirectives.transferToInterpreterAndInvalidate();
                tryInt = false;
            }
            return RMath.fmod(left, right);
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            throw error(RError.Message.UNIMPLEMENTED_COMPLEX);
        }

        @Override
        public String op(String left, String right) {
            throw error(RError.Message.INVALID_TYPE_ARGUMENT, "character");
        }
    }

    public static class Pow extends BinaryArithmetic {

        private final ConditionProfile pow2Profile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile powIntegerProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile powIntegerPositiveProfile = ConditionProfile.createBinaryProfile();

        private static final int UNINITIALIZED = Integer.MIN_VALUE;
        private static final int GENERIC = Integer.MIN_VALUE + 1;
        @CompilationFinal private int cachedCastExponent = UNINITIALIZED;

        public Pow() {
            super(false, false, false);
        }

        @Override
        public String opName() {
            return "^";
        }

        @Override
        public int op(int left, int right) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public double op(double a, double b) {
            int castExponent = (int) b;

            // Special case with exponent always two.
            if (pow2Profile.profile(b == 2)) {
                return a * a;
            }

            // Special case with exponent always integer.
            if (powIntegerProfile.profile(castExponent == b)) {
                if (cachedCastExponent != GENERIC && cachedCastExponent != castExponent) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    if (castExponent == UNINITIALIZED) {
                        cachedCastExponent = GENERIC;
                    } else {
                        cachedCastExponent = cachedCastExponent == UNINITIALIZED ? castExponent : GENERIC;
                    }
                }
                if (cachedCastExponent == GENERIC) {
                    if (powIntegerPositiveProfile.profile(castExponent >= 0)) {
                        return positivePow(a, castExponent);
                    } else {
                        if (powIntegerPositiveProfile.profile(a == 0.0)) {
                            return Double.POSITIVE_INFINITY;
                        }
                        return 1 / positivePow(a, -castExponent);
                    }
                } else {
                    if (powIntegerPositiveProfile.profile(cachedCastExponent >= 0)) {
                        return positivePowUnrolled(a, cachedCastExponent);
                    } else {
                        if (powIntegerPositiveProfile.profile(a == 0.0)) {
                            return Double.POSITIVE_INFINITY;
                        }
                        return 1 / positivePowUnrolled(a, -cachedCastExponent);
                    }
                }
            }

            // Generic case with double exponent.
            if (isFinite(a) && isFinite(b)) {
                return Math.pow(a, b);
            }
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return replace(new PowFull()).op(a, b);
        }

        @ExplodeLoop
        private static double positivePowUnrolled(double operand, int castExponent) {
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

        // The code for complex pow is transcribed from Purdue FastR:
        // LICENSE: transcribed code from GNU R, which is licensed under GPL
        // LICENSE: transcribed code from GCC, which is licensed under GPL
        // LICENSE: transcribed code from GCC, which is licensed under GPL
        // libgcc2
        // LICENSE: this code is derived from the multiplication code, which is transcribed code
        // from GCC, which is licensed under GPL
        // LICENSE: this code is derived from the division code, which is transcribed code from
        // GCC, which is licensed under GPL

        @Child private CHypot chypot;
        @Child private Multiply mult;
        @Child protected CPow2 cpow2;

        protected void ensurePowKNodes() {
            // all or nothing: checking just one is sufficient
            if (mult == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                mult = insert(new Multiply());
            }
            ensurePow2();
        }

        protected void ensurePow2() {
            if (cpow2 == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cpow2 = insert(new CPow2());
            }
        }

        private void ensureChypot() {
            if (chypot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                chypot = insert(new CHypot());
            }
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag == 0.0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
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

        @Override
        @TruffleBoundary
        public String op(String left, String right) {
            throw new UnsupportedOperationException("illegal type 'String' of argument");
        }

        // The code for chypot was transcribed from Purdue FastR:
        // after libgcc2's x86 hypot - note the sign of NaN below (what GNU-R uses)
        // note that Math.hypot in Java is _very_ slow as it tries to be more precise

        public static class CHypot extends Node {

            private final ConditionProfile everSeenInfinite = ConditionProfile.createBinaryProfile();

            public double chypot(double real, double imag) {
                double res = Math.sqrt(real * real + imag * imag);
                if (everSeenInfinite.profile(!isFinite(real) || !isFinite(imag))) {
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

            private final ConditionProfile everSeenNaN = ConditionProfile.createBinaryProfile();
            private final ConditionProfile inf1 = ConditionProfile.createBinaryProfile();
            private final ConditionProfile inf2 = ConditionProfile.createBinaryProfile();

            public RComplex cpow2(double cre, double cim) {
                double cre2 = cre * cre;
                double cim2 = cim * cim;
                double crecim = cre * cim;
                double real = cre2 - cim2;
                double imag = 2 * crecim;
                if (everSeenNaN.profile(Double.isNaN(real) && Double.isNaN(imag))) {
                    boolean recalc = false;
                    double ra = cre;
                    double rb = cim;
                    if (inf1.profile(Double.isInfinite(ra) || Double.isInfinite(rb))) {
                        ra = convertInf(ra);
                        rb = convertInf(rb);
                        recalc = true;
                    }
                    if (inf2.profile(!recalc && (Double.isInfinite(cre2) || Double.isInfinite(cim2) || Double.isInfinite(crecim)))) {
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            return RDataFactory.createComplex(1.0, 0.0);
        }
    }

    private static final class Pow1 extends Pow {

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag != 0.0 || rightReal != 1.0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            return RDataFactory.createComplex(leftReal, leftImag);
        }
    }

    private static final class Pow2 extends Pow {

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag != 0.0 || rightReal != 2.0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            ensurePow2();
            return cpow2.cpow2(leftReal, leftImag);
        }
    }

    private static final class PowNegative extends Pow {

        @Child private Pow pow = new PowK();
        @Child private CReciprocal creciprocal = new CReciprocal();

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            if (rightImag != 0.0 || rightReal >= 0.0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new PowFull()).op(leftReal, leftImag, rightReal, rightImag);
            }
            RComplex r = pow.op(leftReal, leftImag, -rightReal, 0.0); // x^(-k)
            return creciprocal.creciprocal(r);
        }

        private static class CReciprocal extends Node {

            private final ConditionProfile everSeenNaN = ConditionProfile.createBinaryProfile();
            private final ConditionProfile zero = ConditionProfile.createBinaryProfile();
            private final ConditionProfile inf = ConditionProfile.createBinaryProfile();

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
                if (everSeenNaN.profile(Double.isNaN(real) && Double.isNaN(imag))) {
                    if (zero.profile(cre == 0.0 && cim == 0.0)) {
                        real = Math.copySign(Double.POSITIVE_INFINITY, cre);
                        imag = Math.copySign(Double.NaN, cre);
                    } else if (inf.profile(Double.isInfinite(cre) || Double.isInfinite(cim))) {
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
        public boolean introducesNA() {
            return true;
        }

        private final ConditionProfile pow2 = ConditionProfile.createBinaryProfile();
        private final ConditionProfile one = ConditionProfile.createBinaryProfile();
        private final ConditionProfile zero = ConditionProfile.createBinaryProfile();
        private final ConditionProfile finite = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nan = ConditionProfile.createBinaryProfile();
        private final ConditionProfile infiniteA = ConditionProfile.createBinaryProfile();
        private final ConditionProfile infiniteB = ConditionProfile.createBinaryProfile();

        @Override
        public double op(double a, double b) {
            // LICENSE: transcribed code from GNU R, which is licensed under GPL

            // NOTE: Math.pow (which uses FDLIBM) is very slow, the version written in assembly in
            // GLIBC (SSE2 optimized) is about 2x faster

            // arithmetic.c (GNU R)
            if (pow2.profile(b == 2.0D)) {
                return a * a;
            }
            if (one.profile(a == 1.0D || b == 0.0D)) {
                return 1;
            }
            if (zero.profile(a == 0.0D)) {
                if (b > 0.0D) {
                    return 0.0D;
                }
                if (b < 0.0D) {
                    return Double.POSITIVE_INFINITY;
                }
                return b;  // NA or NaN
            }
            if (finite.profile(isFinite(a) && isFinite(b))) {
                return Math.pow(a, b);
            }
            if (nan.profile(isNAorNaN(a) || isNAorNaN(b))) {
                // NA check was before, so this can only mean NaN
                return a + b;
            }
            if (infiniteA.profile(!isFinite(a))) {
                if (a > 0) { // Inf ^ y
                    if (b < 0) {
                        return 0;
                    }
                    return Double.POSITIVE_INFINITY;
                } else if (isFinite(b) && b == Math.floor(b)) { // (-Inf) ^ n
                    if (b < 0) {
                        return 0;
                    }
                    return RMath.fmod(b, 2) != 0 ? a : -a;
                }
            }
            if (infiniteB.profile(!isFinite(b))) {
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

        private final BranchProfile incomparableProfile = BranchProfile.create();
        private final BranchProfile zeroProfile = BranchProfile.create();
        private final ConditionProfile compareProfile = ConditionProfile.createBinaryProfile();

        Max() {
            super(true, true, true);
        }

        @Override
        public String opName() {
            return "max";
        }

        @Override
        public int op(int left, int right) {
            return Math.max(left, right);
        }

        @Override
        public double op(double left, double right) {
            // explicit checks, since Math.max uses a non-final static field
            if (left != left) {
                incomparableProfile.enter();
                return left;
            } else if (left == 0.0d && right == 0.0d && Double.doubleToRawLongBits(left) == Double.doubleToRawLongBits(-0.0d)) {
                zeroProfile.enter();
                return right;
            } else {
                return compareProfile.profile(left >= right) ? left : right;
            }
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            throw error(RError.Message.INVALID_TYPE_ARGUMENT, "complex");
        }

        @Override
        @TruffleBoundary
        public String op(String left, String right) {
            return left.compareTo(right) > 0 ? left : right;
        }
    }

    private static class Min extends BinaryArithmetic {

        private final BranchProfile incomparableProfile = BranchProfile.create();
        private final BranchProfile zeroProfile = BranchProfile.create();
        private final ConditionProfile compareProfile = ConditionProfile.createBinaryProfile();

        Min() {
            super(true, true, true);
        }

        @Override
        public String opName() {
            return "min";
        }

        @Override
        public int op(int left, int right) {
            return Math.min(left, right);
        }

        @Override
        public double op(double left, double right) {
            // explicit checks, since Math.min uses a non-final static field
            if (left != left) {
                incomparableProfile.enter();
                return left;
            } else if (left == 0.0d && right == 0.0d && Double.doubleToRawLongBits(right) == Double.doubleToRawLongBits(-0.0d)) {
                zeroProfile.enter();
                return right;
            } else {
                return compareProfile.profile(left <= right) ? left : right;
            }
        }

        @Override
        public RComplex op(double leftReal, double leftImag, double rightReal, double rightImag) {
            throw error(RError.Message.INVALID_TYPE_ARGUMENT, "complex");
        }

        @Override
        @TruffleBoundary
        public String op(String left, String right) {
            return left.compareTo(right) < 0 ? left : right;
        }
    }
}
