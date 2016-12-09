/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// inspired by arithmetic.c

public final class StatsFunctions {
    @CompilationFinal private static final RDouble DUMMY_VECTOR = RDouble.valueOf(1);

    private StatsFunctions() {
        // private
    }

    public interface Function3_2 {
        double evaluate(double a, double b, double c, boolean x, boolean y);
    }

    public interface Function3_1 extends Function3_2 {
        @Override
        default double evaluate(double a, double b, double c, boolean x, boolean y) {
            return evaluate(a, b, c, x);
        }

        double evaluate(double a, double b, double c, boolean x);
    }

    public interface Function2_1 extends Function3_2 {
        @Override
        default double evaluate(double a, double b, double c, boolean x, boolean y) {
            return evaluate(a, b, x);
        }

        double evaluate(double a, double b, boolean x);
    }

    public interface Function2_2 extends Function3_2 {
        @Override
        default double evaluate(double a, double b, double c, boolean x, boolean y) {
            return evaluate(a, b, x, y);
        }

        double evaluate(double a, double b, boolean x, boolean y);
    }

    static final class StatFunctionProfiles {
        final BranchProfile nan = BranchProfile.create();
        final NACheck aCheck = NACheck.create();
        final NACheck bCheck = NACheck.create();
        final NACheck cCheck = NACheck.create();
        final ConditionProfile copyAttrsFromA = ConditionProfile.createBinaryProfile();
        final ConditionProfile copyAttrsFromB = ConditionProfile.createBinaryProfile();
        final ConditionProfile copyAttrsFromC = ConditionProfile.createBinaryProfile();
        final VectorLengthProfile resultVectorLengthProfile = VectorLengthProfile.create();
        final LoopConditionProfile loopConditionProfile = LoopConditionProfile.createCountingProfile();

        public static StatFunctionProfiles create() {
            return new StatFunctionProfiles();
        }
    }

    private static RAbstractDoubleVector evaluate3(Node node, Function3_2 function, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, boolean x, boolean y,
                    StatFunctionProfiles profiles, UnaryCopyAttributesNode copyAttributesNode) {
        int aLength = a.getLength();
        int bLength = b.getLength();
        int cLength = c.getLength();
        if (aLength == 0 || bLength == 0 || cLength == 0) {
            return RDataFactory.createEmptyDoubleVector();
        }
        int length = profiles.resultVectorLengthProfile.profile(Math.max(aLength, Math.max(bLength, cLength)));
        RNode.reportWork(node, length);
        double[] result = new double[length];

        boolean complete = true;
        boolean nans = false;
        profiles.aCheck.enable(a);
        profiles.bCheck.enable(b);
        profiles.cCheck.enable(c);
        profiles.loopConditionProfile.profileCounted(length);
        for (int i = 0; profiles.loopConditionProfile.inject(i < length); i++) {
            double aValue = a.getDataAt(i % aLength);
            double bValue = b.getDataAt(i % bLength);
            double cValue = c.getDataAt(i % cLength);
            double value;
            if (Double.isNaN(aValue) || Double.isNaN(bValue) || Double.isNaN(cValue)) {
                profiles.nan.enter();
                if (profiles.aCheck.check(aValue) || profiles.bCheck.check(bValue) || profiles.cCheck.check(cValue)) {
                    value = RRuntime.DOUBLE_NA;
                    complete = false;
                } else {
                    value = Double.NaN;
                }
            } else {
                value = function.evaluate(aValue, bValue, cValue, x, y);
                if (Double.isNaN(value)) {
                    profiles.nan.enter();
                    nans = true;
                }
            }
            result[i] = value;
        }
        if (nans) {
            RError.warning(RError.SHOW_CALLER, RError.Message.NAN_PRODUCED);
        }
        RDoubleVector resultVec = RDataFactory.createDoubleVector(result, complete);

        // copy attributes if necessary:
        if (profiles.copyAttrsFromA.profile(aLength == length)) {
            copyAttributesNode.execute(resultVec, a);
        } else if (profiles.copyAttrsFromB.profile(bLength == length)) {
            copyAttributesNode.execute(resultVec, b);
        } else if (profiles.copyAttrsFromC.profile(cLength == length)) {
            copyAttributesNode.execute(resultVec, c);
        }

        return resultVec;
    }

    public abstract static class Function3_2Node extends RExternalBuiltinNode.Arg5 {
        private final Function3_2 function;

        public Function3_2Node(Function3_2 function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg(0).asDoubleVector();
            casts.arg(1).asDoubleVector();
            casts.arg(2).asDoubleVector();
            casts.arg(3).asLogicalVector().findFirst().map(toBoolean());
            casts.arg(4).asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, boolean x, boolean y,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate3(this, function, a, b, c, x, y, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function3_1Node extends RExternalBuiltinNode.Arg4 {
        private final Function3_1 function;

        public Function3_1Node(Function3_1 function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg(0).asDoubleVector();
            casts.arg(1).asDoubleVector();
            casts.arg(2).asDoubleVector();
            casts.arg(3).asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, boolean x,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate3(this, function, a, b, c, x, false /* dummy */, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function2_1Node extends RExternalBuiltinNode.Arg3 {
        private final Function2_1 function;

        public Function2_1Node(Function2_1 function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg(0).asDoubleVector();
            casts.arg(1).asDoubleVector();
            casts.arg(2).asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, boolean x,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate3(this, function, a, b, DUMMY_VECTOR, x, false /* dummy */, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function2_2Node extends RExternalBuiltinNode.Arg4 {
        private final Function2_2 function;

        public Function2_2Node(Function2_2 function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg(0).asDoubleVector();
            casts.arg(1).asDoubleVector();
            casts.arg(2).asLogicalVector().findFirst().map(toBoolean());
            casts.arg(3).asLogicalVector().findFirst().map(toBoolean());
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, boolean x, boolean y,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate3(this, function, a, b, DUMMY_VECTOR, x, y, profiles, copyAttributesNode);
        }
    }

    public abstract static class ApproxTest extends RExternalBuiltinNode.Arg4 {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg(2).asIntegerVector().findFirst();
            casts.arg(3).asDoubleVector().findFirst();
        }

        @Specialization
        protected RNull approxtest(RAbstractDoubleVector x, RAbstractDoubleVector y, int method, double f) {
            int nx = x.getLength();
            switch (method) {
                case 1:
                    break;
                case 2:
                    if (!RRuntime.isFinite(f) || f < 0.0 || f > 1.0) {
                        throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "approx(): invalid f value");
                    }
                    break;
                default:
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, "approx(): invalid interpolation method");
            }

            for (int i = 0; i < nx; i++) {
                if (RRuntime.isNAorNaN(x.getDataAt(i)) || RRuntime.isNAorNaN(y.getDataAt(i))) {
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, ("approx(): attempted to interpolate NA values"));
                }
            }

            return RNull.instance;
        }
    }

    public abstract static class Approx extends RExternalBuiltinNode.Arg7 {
        private static final NACheck naCheck = NACheck.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg(2).asDoubleVector();
            casts.arg(3).asIntegerVector().findFirst();
            casts.arg(4).asDoubleVector().findFirst();
            casts.arg(5).asDoubleVector().findFirst();
            casts.arg(6).asDoubleVector().findFirst();
        }

        @Specialization
        protected RDoubleVector approx(RDoubleVector x, RDoubleVector y, RDoubleVector v, int method, double yl, double yr, double f) {
            int nx = x.getLength();
            int nout = v.getLength();
            double[] yout = new double[nout];
            ApprMeth apprMeth = new ApprMeth();

            apprMeth.f2 = f;
            apprMeth.f1 = 1 - f;
            apprMeth.kind = method;
            apprMeth.ylow = yl;
            apprMeth.yhigh = yr;
            naCheck.enable(true);
            for (int i = 0; i < nout; i++) {
                double xouti = v.getDataAt(i);
                yout[i] = RRuntime.isNAorNaN(xouti) ? xouti : approx1(xouti, x.getDataWithoutCopying(), y.getDataWithoutCopying(), nx, apprMeth);
                naCheck.check(yout[i]);
            }
            return RDataFactory.createDoubleVector(yout, naCheck.neverSeenNA());
        }

        private static class ApprMeth {
            double ylow;
            double yhigh;
            double f1;
            double f2;
            int kind;
        }

        private static double approx1(double v, double[] x, double[] y, int n,
                        ApprMeth apprMeth) {
            /* Approximate y(v), given (x,y)[i], i = 0,..,n-1 */
            int i;
            int j;
            int ij;

            if (n == 0) {
                return RRuntime.DOUBLE_NA;
            }

            i = 0;
            j = n - 1;

            /* handle out-of-domain points */
            if (v < x[i]) {
                return apprMeth.ylow;
            }
            if (v > x[j]) {
                return apprMeth.yhigh;
            }

            /* find the correct interval by bisection */
            while (i < j - 1) { /* x[i] <= v <= x[j] */
                ij = (i + j) / 2; /* i+1 <= ij <= j-1 */
                if (v < x[ij]) {
                    j = ij;
                } else {
                    i = ij;
                }
                /* still i < j */
            }
            /* provably have i == j-1 */

            /* interpolation */

            if (v == x[j]) {
                return y[j];
            }
            if (v == x[i]) {
                return y[i];
            }
            /* impossible: if(x[j] == x[i]) return y[i]; */

            if (apprMeth.kind == 1) { /* linear */
                return y[i] + (y[j] - y[i]) * ((v - x[i]) / (x[j] - x[i]));
            } else { /* 2 : constant */
                return (apprMeth.f1 != 0.0 ? y[i] * apprMeth.f1 : 0.0) + (apprMeth.f2 != 0.0 ? y[j] * apprMeth.f2 : 0.0);
            }
        }/* approx1() */

    }
}
