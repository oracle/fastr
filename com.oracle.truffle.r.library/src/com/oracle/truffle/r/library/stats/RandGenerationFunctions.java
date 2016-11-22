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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_UNNAMED_ARGUMENTS;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.library.stats.RandGenerationFunctionsFactory.ConvertToLengthNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.rng.RRNG;

public final class RandGenerationFunctions {
    public static final double ERR_NA = RRuntime.DOUBLE_NA;
    private static final RDouble DUMMY_VECTOR = RDouble.valueOf(1);

    private RandGenerationFunctions() {
        // static class
    }

    // inspired by the DEFRAND{X}_REAL and DEFRAND{X}_INT macros in GnuR

    public interface RandFunction3_Int {
        int evaluate(double a, double b, double c);
    }

    public interface RandFunction2_Int extends RandFunction3_Int {
        @Override
        default int evaluate(double a, double b, double c) {
            return evaluate(a, b);
        }

        int evaluate(double a, double b);
    }

    public interface RandFunction2_Double {
        /**
         * If returns {@code false}, {@link #evaluate(double, double)} will not be invoked.
         */
        boolean isValid(double a, double b);

        /**
         * Is guaranteed to be preceded by invocation of {@link #isValid(double, double)} with the
         * same arguments.
         */
        double evaluate(double a, double b);
    }

    public abstract static class RandFunction2_DoubleAdapter implements RandFunction2_Double {
        @Override
        public boolean isValid(double a, double b) {
            return true;
        }
    }

    static final class RandGenerationProfiles {
        final BranchProfile nanResult = BranchProfile.create();
        final BranchProfile errResult = BranchProfile.create();
        final BranchProfile nan = BranchProfile.create();
        final NACheck aCheck = NACheck.create();
        final NACheck bCheck = NACheck.create();
        final NACheck cCheck = NACheck.create();
        final VectorLengthProfile resultVectorLengthProfile = VectorLengthProfile.create();
        final LoopConditionProfile loopConditionProfile = LoopConditionProfile.createCountingProfile();

        public static RandGenerationProfiles create() {
            return new RandGenerationProfiles();
        }
    }

    private static RAbstractIntVector evaluate3Int(Node node, RandFunction3_Int function, int lengthIn, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c,
                    RandGenerationProfiles profiles) {
        int length = profiles.resultVectorLengthProfile.profile(lengthIn);
        int aLength = a.getLength();
        int bLength = b.getLength();
        int cLength = c.getLength();
        if (aLength == 0 || bLength == 0 || cLength == 0) {
            profiles.nanResult.enter();
            RError.warning(SHOW_CALLER, RError.Message.NAN_PRODUCED);
            int[] nansResult = new int[length];
            Arrays.fill(nansResult, RRuntime.INT_NA);
            return RDataFactory.createIntVector(nansResult, false);
        }

        RNode.reportWork(node, length);
        boolean complete = true;
        boolean nans = false;
        profiles.aCheck.enable(a);
        profiles.bCheck.enable(b);
        profiles.cCheck.enable(c);
        int[] result = new int[length];
        RRNG.getRNGState();
        for (int i = 0; profiles.loopConditionProfile.inject(i < length); i++) {
            double aValue = a.getDataAt(i % aLength);
            double bValue = b.getDataAt(i % bLength);
            double cValue = c.getDataAt(i % cLength);
            int value;
            if (Double.isNaN(aValue) || Double.isNaN(bValue) || Double.isNaN(cValue)) {
                profiles.nan.enter();
                value = RRuntime.INT_NA;
                if (profiles.aCheck.check(aValue) || profiles.bCheck.check(bValue) || profiles.cCheck.check(cValue)) {
                    complete = false;
                }
            } else {
                value = function.evaluate(aValue, bValue, cValue);
                if (Double.isNaN(value)) {
                    profiles.nan.enter();
                    nans = true;
                }
            }
            result[i] = value;
        }
        RRNG.putRNGState();
        if (nans) {
            RError.warning(SHOW_CALLER, RError.Message.NAN_PRODUCED);
        }
        return RDataFactory.createIntVector(result, complete);
    }

    private static RAbstractDoubleVector evaluate2Double(Node node, RandFunction2_Double function, int length, RAbstractDoubleVector a, RAbstractDoubleVector b, RandGenerationProfiles profiles) {
        int aLength = a.getLength();
        int bLength = b.getLength();
        if (aLength == 0 || bLength == 0) {
            profiles.nanResult.enter();
            RError.warning(SHOW_CALLER, RError.Message.NAN_PRODUCED);
            return createVectorOf(length, RRuntime.DOUBLE_NA);
        }

        RNode.reportWork(node, length);
        boolean complete = true;
        boolean nans = false;
        profiles.aCheck.enable(a);
        profiles.bCheck.enable(b);
        double[] result = new double[length];
        RRNG.getRNGState();
        for (int i = 0; i < length; i++) {
            double aValue = a.getDataAt(i % aLength);
            double bValue = b.getDataAt(i % bLength);
            double value;
            if (Double.isNaN(aValue) || Double.isNaN(bValue)) {
                profiles.nan.enter();
                value = RRuntime.INT_NA;
                if (profiles.aCheck.check(aValue) || profiles.bCheck.check(bValue)) {
                    complete = false;
                }
            } else {
                if (!function.isValid(aValue, bValue)) {
                    profiles.errResult.enter();
                    RError.warning(SHOW_CALLER, RError.Message.NA_PRODUCED);
                    return createVectorOf(length, Double.NaN);
                }
                value = function.evaluate(aValue, bValue);
                if (Double.isNaN(value)) {
                    profiles.nan.enter();
                    nans = true;
                }
            }
            result[i] = value;
        }
        RRNG.putRNGState();
        if (nans) {
            RError.warning(SHOW_CALLER, RError.Message.NAN_PRODUCED);
        }
        return RDataFactory.createDoubleVector(result, complete);
    }

    private static RAbstractDoubleVector createVectorOf(int length, double element) {
        double[] nansResult = new double[length];
        Arrays.fill(nansResult, element);
        return RDataFactory.createDoubleVector(nansResult, false);
    }

    /**
     * Converts given value to actual length that should be used as length of the output vector. The
     * argument must be cast using {@link #addLengthCast(CastBuilder)}. Using this node allows us to
     * avoid casting of long vectors to integers, if we only need to know their length.
     */
    protected abstract static class ConvertToLength extends Node {
        public abstract int execute(RAbstractVector value);

        @Specialization(guards = "vector.getLength() == 1")
        public int lengthOne(RAbstractVector vector,
                        @Cached("createNonPreserving()") CastIntegerNode castNode,
                        @Cached("create()") BranchProfile seenNA) {
            int result = ((RAbstractIntVector) castNode.execute(vector)).getDataAt(0);
            if (RRuntime.isNA(result)) {
                seenNA.enter();
                throw RError.error(SHOW_CALLER, INVALID_UNNAMED_ARGUMENTS);
            }
            return result;
        }

        @Specialization(guards = "vector.getLength() != 1")
        public int notSingle(RAbstractVector vector) {
            return vector.getLength();
        }

        private static void addLengthCast(CastBuilder casts) {
            casts.arg(0).defaultError(SHOW_CALLER, INVALID_UNNAMED_ARGUMENTS).mustBe(abstractVectorValue()).asVector();
        }
    }

    public abstract static class Function3_IntNode extends RExternalBuiltinNode.Arg4 {
        private final RandFunction3_Int function;
        @Child private ConvertToLength convertToLength = ConvertToLengthNodeGen.create();

        protected Function3_IntNode(RandFunction3_Int function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            ConvertToLength.addLengthCast(casts);
            casts.arg(1).asDoubleVector();
            casts.arg(2).asDoubleVector();
            casts.arg(3).asDoubleVector();
        }

        @Specialization
        protected RAbstractIntVector evaluate(RAbstractVector length, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c,
                        @Cached("create()") RandGenerationProfiles profiles) {
            return evaluate3Int(this, function, convertToLength.execute(length), a, b, c, profiles);
        }
    }

    public abstract static class Function2_IntNode extends RExternalBuiltinNode.Arg3 {
        private final RandFunction2_Int function;
        @Child private ConvertToLength convertToLength = ConvertToLengthNodeGen.create();

        protected Function2_IntNode(RandFunction2_Int function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            ConvertToLength.addLengthCast(casts);
            casts.arg(1).asDoubleVector();
            casts.arg(2).asDoubleVector();
        }

        @Specialization
        protected RAbstractIntVector evaluate(RAbstractVector length, RAbstractDoubleVector a, RAbstractDoubleVector b,
                        @Cached("create()") RandGenerationProfiles profiles) {
            return evaluate3Int(this, function, convertToLength.execute(length), a, b, DUMMY_VECTOR, profiles);
        }
    }

    public abstract static class Function2_DoubleNode extends RExternalBuiltinNode.Arg3 {
        private final RandFunction2_Double function;
        @Child private ConvertToLength convertToLength = ConvertToLengthNodeGen.create();

        protected Function2_DoubleNode(RandFunction2_Double function) {
            this.function = function;
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            ConvertToLength.addLengthCast(casts);
            casts.arg(1).asDoubleVector();
            casts.arg(2).asDoubleVector();
        }

        @Specialization
        protected RAbstractDoubleVector evaluate(RAbstractVector length, RAbstractDoubleVector a, RAbstractDoubleVector b,
                        @Cached("create()") RandGenerationProfiles profiles) {
            return evaluate2Double(this, function, convertToLength.execute(length), a, b, profiles);
        }
    }
}
