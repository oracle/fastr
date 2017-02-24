/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.ApproxNodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.ApproxTestNodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function2_1NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function2_2NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function3_1NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function3_2NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function4_1NodeGen;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodesFactory.Function4_2NodeGen;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function2_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function3_2;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_1;
import com.oracle.truffle.r.runtime.nmath.MathFunctions.Function4_2;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

// inspired by arithmetic.c

public final class StatsFunctionsNodes {
    @CompilationFinal private static final RDouble DUMMY_VECTOR = RDouble.valueOf(1);

    private StatsFunctionsNodes() {
        // private
    }

    static final class StatFunctionProfiles {
        final BranchProfile nan = BranchProfile.create();
        final NACheck aCheck = NACheck.create();
        final NACheck bCheck = NACheck.create();
        final NACheck cCheck = NACheck.create();
        final NACheck dCheck = NACheck.create();
        final ConditionProfile copyAttrsFromA = ConditionProfile.createBinaryProfile();
        final ConditionProfile copyAttrsFromB = ConditionProfile.createBinaryProfile();
        final ConditionProfile copyAttrsFromC = ConditionProfile.createBinaryProfile();
        final ConditionProfile copyAttrsFromD = ConditionProfile.createBinaryProfile();
        final VectorLengthProfile resultVectorLengthProfile = VectorLengthProfile.create();
        final LoopConditionProfile loopConditionProfile = LoopConditionProfile.createCountingProfile();

        public static StatFunctionProfiles create() {
            return new StatFunctionProfiles();
        }
    }

    private static RAbstractDoubleVector evaluate4(Node node, Function4_2 function, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RAbstractDoubleVector d, boolean x,
                    boolean y, StatFunctionProfiles profiles, UnaryCopyAttributesNode copyAttributesNode) {
        int aLength = a.getLength();
        int bLength = b.getLength();
        int cLength = c.getLength();
        int dLength = d.getLength();
        if (aLength == 0 || bLength == 0 || cLength == 0 || dLength == 0) {
            return RDataFactory.createEmptyDoubleVector();
        }
        int length = profiles.resultVectorLengthProfile.profile(Math.max(aLength, Math.max(bLength, Math.max(cLength, dLength))));
        RBaseNode.reportWork(node, length);
        double[] result = new double[length];

        boolean complete = true;
        boolean nans = false;
        profiles.aCheck.enable(a);
        profiles.bCheck.enable(b);
        profiles.cCheck.enable(c);
        profiles.dCheck.enable(d);
        profiles.loopConditionProfile.profileCounted(length);
        for (int i = 0; profiles.loopConditionProfile.inject(i < length); i++) {
            double aValue = a.getDataAt(i % aLength);
            double bValue = b.getDataAt(i % bLength);
            double cValue = c.getDataAt(i % cLength);
            double dValue = d.getDataAt(i % dLength);
            double value;
            if (Double.isNaN(aValue) || Double.isNaN(bValue) || Double.isNaN(cValue) || Double.isNaN(dValue)) {
                profiles.nan.enter();
                if (profiles.aCheck.check(aValue) || profiles.bCheck.check(bValue) || profiles.cCheck.check(cValue) || profiles.cCheck.check(dValue)) {
                    value = RRuntime.DOUBLE_NA;
                    complete = false;
                } else {
                    value = Double.NaN;
                }
            } else {
                value = function.evaluate(aValue, bValue, cValue, dValue, x, y);
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
        } else if (profiles.copyAttrsFromD.profile((dLength == length))) {
            copyAttributesNode.execute(resultVec, d);
        }

        return resultVec;
    }

    private static void castBoolean(Casts casts, int index, byte defaultValue) {
        // defensively we map missing to the default values
        casts.arg(index).asLogicalVector().findFirst(defaultValue).map(toBoolean());
    }

    private static void castDoubleVec(Casts casts, int index) {
        casts.arg(index).mustBe(numericValue(), Message.NON_NUMERIC_MATH).asDoubleVector();
    }

    public abstract static class Function3_2Node extends RExternalBuiltinNode.Arg5 {
        private final Function3_2 function;

        public Function3_2Node(Function3_2 function) {
            this.function = function;
        }

        public static Function3_2Node create(Function3_2 function) {
            return Function3_2NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function3_2Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castDoubleVec(casts, 2);
            castBoolean(casts, 3, RRuntime.LOGICAL_TRUE);
            castBoolean(casts, 4, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, boolean x, boolean y,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, c, DUMMY_VECTOR, x, y, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function4_1Node extends RExternalBuiltinNode.Arg5 {
        private final Function4_1 function;

        public Function4_1Node(Function4_1 function) {
            this.function = function;
        }

        public static Function4_1Node create(Function4_1 function) {
            return Function4_1NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function4_1Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castDoubleVec(casts, 2);
            castDoubleVec(casts, 3);
            castBoolean(casts, 4, RRuntime.LOGICAL_TRUE);
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RAbstractDoubleVector d, boolean x,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, c, d, x, false /* dummy */, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function4_2Node extends RExternalBuiltinNode.Arg6 {
        private final Function4_2 function;

        public Function4_2Node(Function4_2 function) {
            this.function = function;
        }

        public static Function4_2Node create(Function4_2 function) {
            return Function4_2NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function4_2Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castDoubleVec(casts, 2);
            castDoubleVec(casts, 3);
            castBoolean(casts, 4, RRuntime.LOGICAL_TRUE);
            castBoolean(casts, 5, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RAbstractDoubleVector d, boolean x, boolean y,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, c, d, x, y, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function3_1Node extends RExternalBuiltinNode.Arg4 {
        private final Function3_1 function;

        public Function3_1Node(Function3_1 function) {
            this.function = function;
        }

        public static Function3_1Node create(Function3_1 function) {
            return Function3_1NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function3_1Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castDoubleVec(casts, 2);
            castBoolean(casts, 3, RRuntime.LOGICAL_TRUE);
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, boolean x,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, c, DUMMY_VECTOR, x, false /* dummy */, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function2_1Node extends RExternalBuiltinNode.Arg3 {
        private final Function2_1 function;

        public Function2_1Node(Function2_1 function) {
            this.function = function;
        }

        public static Function2_1Node create(Function2_1 function) {
            return Function2_1NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function2_1Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castBoolean(casts, 2, RRuntime.LOGICAL_TRUE);
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, boolean x,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, DUMMY_VECTOR, DUMMY_VECTOR, x, false /* dummy */, profiles, copyAttributesNode);
        }
    }

    public abstract static class Function2_2Node extends RExternalBuiltinNode.Arg4 {
        private final Function2_2 function;

        public Function2_2Node(Function2_2 function) {
            this.function = function;
        }

        public static Function2_2Node create(Function2_2 function) {
            return Function2_2NodeGen.create(function);
        }

        static {
            Casts casts = new Casts(Function2_2Node.class);
            castDoubleVec(casts, 0);
            castDoubleVec(casts, 1);
            castBoolean(casts, 2, RRuntime.LOGICAL_TRUE);
            castBoolean(casts, 3, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractDoubleVector a, RAbstractDoubleVector b, boolean x, boolean y,
                        @Cached("create()") StatFunctionProfiles profiles,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            return evaluate4(this, function, a, b, DUMMY_VECTOR, DUMMY_VECTOR, x, y, profiles, copyAttributesNode);
        }
    }

    public abstract static class ApproxTest extends RExternalBuiltinNode.Arg4 {
        public static ApproxTest create() {
            return ApproxTestNodeGen.create();
        }

        static {
            Casts casts = new Casts(ApproxTest.class);
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
                        throw error(RError.Message.GENERIC, "approx(): invalid f value");
                    }
                    break;
                default:
                    throw error(RError.Message.GENERIC, "approx(): invalid interpolation method");
            }

            for (int i = 0; i < nx; i++) {
                if (RRuntime.isNAorNaN(x.getDataAt(i)) || RRuntime.isNAorNaN(y.getDataAt(i))) {
                    throw error(RError.Message.GENERIC, ("approx(): attempted to interpolate NA values"));
                }
            }

            return RNull.instance;
        }
    }

    public abstract static class Approx extends RExternalBuiltinNode.Arg7 {
        private static final NACheck naCheck = NACheck.create();

        public static Approx create() {
            return ApproxNodeGen.create();
        }

        static {
            Casts casts = new Casts(Approx.class);
            casts.arg(0).mustBe(instanceOf(RDoubleVector.class));
            casts.arg(1).mustBe(instanceOf(RDoubleVector.class));
            casts.arg(2).mustBe(missingValue().not()).mapIf(nullValue(), emptyDoubleVector()).asDoubleVector();
            casts.arg(3).asIntegerVector().findFirst();
            casts.arg(4).asDoubleVector().findFirst();
            casts.arg(5).asDoubleVector().findFirst();
            casts.arg(6).asDoubleVector().findFirst();
        }

        @Specialization
        protected RDoubleVector approx(RDoubleVector x, RDoubleVector y, RAbstractDoubleVector v, int method, double yl, double yr, double f) {
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
