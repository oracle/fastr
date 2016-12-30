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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.library.stats.RandGenerationFunctionsFactory.ConvertToLengthNodeGen;
import com.oracle.truffle.r.library.stats.RandGenerationFunctionsFactory.RandFunction1NodeGen;
import com.oracle.truffle.r.library.stats.RandGenerationFunctionsFactory.RandFunction2NodeGen;
import com.oracle.truffle.r.library.stats.RandGenerationFunctionsFactory.RandFunction3NodeGen;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.rng.RRNG;
import com.oracle.truffle.r.runtime.rng.RRNG.NormKind;
import com.oracle.truffle.r.runtime.rng.RandomNumberGenerator;

/**
 * Contains infrastructure for R external functions implementing generation of a random value from
 * given random value distribution. To implement such external function, implement one of:
 * {@link RandFunction3_Double}, {@link RandFunction2_Double} or {@link RandFunction1_Double}.
 */
public final class RandGenerationFunctions {
    @CompilationFinal private static final RDouble DUMMY_VECTOR = RDouble.valueOf(1);

    private RandGenerationFunctions() {
        // static class
    }

    public static final class RandomNumberProvider {
        final RandomNumberGenerator generator;
        final NormKind normKind;

        public RandomNumberProvider(RandomNumberGenerator generator, NormKind normKind) {
            this.generator = generator;
            this.normKind = normKind;
        }

        public static RandomNumberProvider fromCurrentRNG() {
            return new RandomNumberProvider(RRNG.currentGenerator(), RRNG.currentNormKind());
        }

        protected boolean isSame(RandomNumberProvider other) {
            return this.generator == other.generator && this.normKind == other.normKind;
        }

        public double unifRand() {
            return generator.genrandDouble();
        }

        public double normRand() {
            return SNorm.normRand(generator, normKind);
        }

        public double expRand() {
            return SExp.expRand(generator);
        }
    }

    // inspired by the DEFRAND{X}_REAL and DEFRAND{X}_INT macros in GnuR

    public abstract static class RandFunction3_Double extends Node {
        public abstract double execute(double a, double b, double c, RandomNumberProvider rand);
    }

    public abstract static class RandFunction2_Double extends RandFunction3_Double {
        public abstract double execute(double a, double b, RandomNumberProvider rand);

        @Override
        public final double execute(double a, double b, double c, RandomNumberProvider rand) {
            return execute(a, b, rand);
        }
    }

    public abstract static class RandFunction1_Double extends RandFunction2_Double {
        public abstract double execute(double a, RandomNumberProvider rand);

        @Override
        public final double execute(double a, double b, RandomNumberProvider rand) {
            return execute(a, rand);
        }
    }

    /**
     * Converts given value to actual length that should be used as length of the output vector. The
     * argument must be cast using {@link #addLengthCast(CastBuilder)}. Using this node allows us to
     * avoid casting of long vectors to integers if we only need to know their length.
     */
    protected abstract static class ConvertToLength extends Node {
        public abstract int execute(RAbstractVector value);

        @Specialization(guards = "vector.getLength() == 1")
        public int lengthOne(RAbstractVector vector,
                        @Cached("createNonPreserving()") CastIntegerNode castNode,
                        @Cached("create()") BranchProfile seenNA) {
            int result = ((RAbstractIntVector) castNode.execute(vector)).getDataAt(0);
            if (RRuntime.isNA(result) || result < 0) {
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

    /**
     * Executor node handles the validation, the loop over all vector elements, the creation of the
     * result vector, and similar. The random function is provided as implementation of
     * {@link RandFunction3_Double}.
     */
    @TypeSystemReference(EmptyTypeSystemFlatLayout.class)
    protected abstract static class RandFunctionExecutorBase extends Node {
        static final class RandGenerationNodeData {
            final BranchProfile nanResult = BranchProfile.create();
            final BranchProfile nan = BranchProfile.create();
            final VectorLengthProfile resultVectorLengthProfile = VectorLengthProfile.create();
            final LoopConditionProfile loopConditionProfile = LoopConditionProfile.createCountingProfile();

            public static RandGenerationNodeData create() {
                return new RandGenerationNodeData();
            }
        }

        public abstract Object execute(RAbstractVector length, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RandomNumberProvider rand);

        @Child private ConvertToLength convertToLength = ConvertToLengthNodeGen.create();

        @Specialization(guards = {"randCached.isSame(rand)"})
        protected final Object evaluateWithCached(RAbstractVector lengthVec, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c,
                        @SuppressWarnings("unused") RandomNumberProvider rand,
                        @Cached("rand") RandomNumberProvider randCached,
                        @Cached("create()") RandGenerationNodeData nodeData) {
            return evaluateWrapper(lengthVec, a, b, c, randCached, nodeData);
        }

        @Specialization(contains = "evaluateWithCached")
        protected final Object evaluateFallback(RAbstractVector lengthVec, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RandomNumberProvider rand,
                        @Cached("create()") RandGenerationNodeData nodeData) {
            return evaluateWrapper(lengthVec, a, b, c, rand, nodeData);
        }

        private Object evaluateWrapper(RAbstractVector lengthVec, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RandomNumberProvider rand,
                        RandGenerationNodeData nodeData) {
            int length = nodeData.resultVectorLengthProfile.profile(convertToLength.execute(lengthVec));
            RNode.reportWork(this, length);
            return evaluate(length, a, b, c, nodeData, rand);
        }

        @SuppressWarnings("unused")
        Object evaluate(int length, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RandGenerationNodeData nodeData, RandomNumberProvider randProvider) {
            // DSL generates code for this class too, with abstract method it would not compile
            throw RInternalError.shouldNotReachHere("must be overridden");
        }

        static void putRNGState() {
            // Note: we call putRNGState only if we actually changed the state, i.e. called random
            // number generation. We do not need to getRNGState() because the parent wrapper node
            // should do that for us
            RRNG.putRNGState();
        }

        static void showNAWarning() {
            RError.warning(SHOW_CALLER, RError.Message.NA_PRODUCED);
        }
    }

    @TypeSystemReference(EmptyTypeSystemFlatLayout.class)
    protected abstract static class RandFunctionIntExecutorNode extends RandFunctionExecutorBase {
        @Child private RandFunction3_Double function;

        protected RandFunctionIntExecutorNode(RandFunction3_Double function) {
            this.function = function;
        }

        @Override
        protected RAbstractIntVector evaluate(int length, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RandGenerationNodeData nodeData,
                        RandomNumberProvider randProvider) {
            int aLength = a.getLength();
            int bLength = b.getLength();
            int cLength = c.getLength();
            if (aLength == 0 || bLength == 0 || cLength == 0) {
                nodeData.nanResult.enter();
                showNAWarning();
                int[] nansResult = new int[length];
                Arrays.fill(nansResult, RRuntime.INT_NA);
                return RDataFactory.createIntVector(nansResult, false);
            }

            boolean nans = false;
            int[] result = new int[length];
            nodeData.loopConditionProfile.profileCounted(length);
            for (int i = 0; nodeData.loopConditionProfile.inject(i < length); i++) {
                double aValue = a.getDataAt(i % aLength);
                double bValue = b.getDataAt(i % bLength);
                double cValue = c.getDataAt(i % cLength);
                double value = function.execute(aValue, bValue, cValue, randProvider);
                if (Double.isNaN(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                    nodeData.nan.enter();
                    nans = true;
                    result[i] = RRuntime.INT_NA;
                } else {
                    result[i] = (int) value;
                }
            }
            putRNGState();
            if (nans) {
                showNAWarning();
            }
            return RDataFactory.createIntVector(result, !nans);
        }
    }

    @TypeSystemReference(EmptyTypeSystemFlatLayout.class)
    protected abstract static class RandFunctionDoubleExecutorNode extends RandFunctionExecutorBase {
        @Child private RandFunction3_Double function;

        protected RandFunctionDoubleExecutorNode(RandFunction3_Double function) {
            this.function = function;
        }

        @Override
        protected RAbstractDoubleVector evaluate(int length, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c, RandGenerationNodeData nodeData,
                        RandomNumberProvider randProvider) {
            int aLength = a.getLength();
            int bLength = b.getLength();
            int cLength = c.getLength();
            if (aLength == 0 || bLength == 0 || cLength == 0) {
                nodeData.nanResult.enter();
                showNAWarning();
                double[] nansResult = new double[length];
                Arrays.fill(nansResult, RRuntime.DOUBLE_NA);
                return RDataFactory.createDoubleVector(nansResult, false);
            }

            boolean nans = false;
            double[] result;
            result = new double[length];
            nodeData.loopConditionProfile.profileCounted(length);
            for (int i = 0; nodeData.loopConditionProfile.inject(i < length); i++) {
                double aValue = a.getDataAt(i % aLength);
                double bValue = b.getDataAt(i % bLength);
                double cValue = c.getDataAt(i % cLength);
                double value = function.execute(aValue, bValue, cValue, randProvider);
                if (Double.isNaN(value) || RRuntime.isNA(value)) {
                    nodeData.nan.enter();
                    nans = true;
                }
                result[i] = value;
            }
            putRNGState();
            if (nans) {
                showNAWarning();
            }
            return RDataFactory.createDoubleVector(result, !nans);
        }
    }

    public abstract static class RandFunction3Node extends RExternalBuiltinNode.Arg4 {
        @Child private RandFunctionExecutorBase inner;

        protected RandFunction3Node(RandFunctionExecutorBase inner) {
            this.inner = inner;
        }

        public static RandFunction3Node createInt(RandFunction3_Double function) {
            return RandFunction3NodeGen.create(RandGenerationFunctionsFactory.RandFunctionIntExecutorNodeGen.create(function));
        }

        // Note: for completeness of the API
        @SuppressWarnings("unused")
        public static RandFunction3Node createDouble(RandFunction3_Double function) {
            return RandFunction3NodeGen.create(RandGenerationFunctionsFactory.RandFunctionDoubleExecutorNodeGen.create(function));
        }

        @Override
        protected final void createCasts(CastBuilder casts) {
            ConvertToLength.addLengthCast(casts);
            casts.arg(1).asDoubleVector();
            casts.arg(2).asDoubleVector();
            casts.arg(3).asDoubleVector();
        }

        @Specialization
        protected Object evaluate(RAbstractVector length, RAbstractDoubleVector a, RAbstractDoubleVector b, RAbstractDoubleVector c) {
            RRNG.getRNGState();
            return inner.execute(length, a, b, c, RandomNumberProvider.fromCurrentRNG());
        }
    }

    public abstract static class RandFunction2Node extends RExternalBuiltinNode.Arg3 {
        @Child private RandFunctionExecutorBase inner;

        protected RandFunction2Node(RandFunctionExecutorBase inner) {
            this.inner = inner;
        }

        public static RandFunction2Node createInt(RandFunction2_Double function) {
            return RandFunction2NodeGen.create(RandGenerationFunctionsFactory.RandFunctionIntExecutorNodeGen.create(function));
        }

        public static RandFunction2Node createDouble(RandFunction2_Double function) {
            return RandFunction2NodeGen.create(RandGenerationFunctionsFactory.RandFunctionDoubleExecutorNodeGen.create(function));
        }

        @Override
        protected final void createCasts(CastBuilder casts) {
            ConvertToLength.addLengthCast(casts);
            casts.arg(1).asDoubleVector();
            casts.arg(2).asDoubleVector();
        }

        @Specialization
        protected Object evaluate(RAbstractVector length, RAbstractDoubleVector a, RAbstractDoubleVector b) {
            RRNG.getRNGState();
            return inner.execute(length, a, b, DUMMY_VECTOR, RandomNumberProvider.fromCurrentRNG());
        }
    }

    public abstract static class RandFunction1Node extends RExternalBuiltinNode.Arg2 {
        @Child private RandFunctionExecutorBase inner;

        protected RandFunction1Node(RandFunctionExecutorBase inner) {
            this.inner = inner;
        }

        public static RandFunction1Node createInt(RandFunction1_Double function) {
            return RandFunction1NodeGen.create(RandGenerationFunctionsFactory.RandFunctionIntExecutorNodeGen.create(function));
        }

        public static RandFunction1Node createDouble(RandFunction1_Double function) {
            return RandFunction1NodeGen.create(RandGenerationFunctionsFactory.RandFunctionDoubleExecutorNodeGen.create(function));
        }

        @Override
        protected final void createCasts(CastBuilder casts) {
            ConvertToLength.addLengthCast(casts);
            casts.arg(1).asDoubleVector();
        }

        @Specialization
        protected Object evaluate(RAbstractVector length, RAbstractDoubleVector a) {
            RRNG.getRNGState();
            return inner.execute(length, a, DUMMY_VECTOR, DUMMY_VECTOR, RandomNumberProvider.fromCurrentRNG());
        }
    }
}
