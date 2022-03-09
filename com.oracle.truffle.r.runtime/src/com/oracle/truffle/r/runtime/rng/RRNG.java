/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates
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
package com.oracle.truffle.r.runtime.rng;

import static com.oracle.truffle.r.runtime.rng.RRNG.SampleKind.REJECTION;
import static com.oracle.truffle.r.runtime.rng.RRNG.SampleKind.ROUNDING;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.nmath.RandomFunctions.RandomNumberProvider;
import com.oracle.truffle.r.runtime.rng.mm.MarsagliaMulticarry;
import com.oracle.truffle.r.runtime.rng.mt.MersenneTwister;
import com.oracle.truffle.r.runtime.rng.user.UserRNG;

/**
 * Facade class to the R random number generators, (see src/main/RNG.c in GnuR). The individual
 * generators are implemented in their own class. Currently there are only two implemented, the
 * default, {@link MersenneTwister} and {@link MarsagliaMulticarry}.
 *
 * The fact that the R programmer can set {@code .Random.seed} explicitly, as opposed to the
 * recommended approach of calling {@code set.seed}, is something of a pain as it changes the
 * {@link Kind}, the {@link NormKind} and the actual seeds all in one go and in a totally
 * uncontrolled way, which then has to be checked.
 *
 * Important note: make sure to invoke {@link #getRNGState()} before invoking any other methods from
 * this class and to invoke {@link #putRNGState()} when done witch random number generation.
 */
public class RRNG {

    private static final double LOG2 = Math.log(2);

    private static final double U = 33554432.0;
    private static final double MAX_INT = Integer.MAX_VALUE;

    /**
     * The standard kinds provided by GnuR, where the ordinal value corresponds to the argument to
     * {@link RRNG#doSetSeed}.
     */
    public enum Kind {
        WICHMANN_HILL(),
        MARSAGLIA_MULTICARRY(MarsagliaMulticarry::new),
        SUPER_DUPER(),
        MERSENNE_TWISTER(MersenneTwister::new),
        KNUTH_TAOCP(),
        USER_UNIF(UserRNG::new),
        KNUTH_TAOCP2(),
        LECUYER_CMRG();

        @CompilationFinal(dimensions = 1) static final Kind[] VALUES = values();

        private final Supplier<RandomNumberGenerator> createFunction;

        Kind() {
            // this RNG kind is not available
            this.createFunction = null;
        }

        Kind(Supplier<RandomNumberGenerator> create) {
            this.createFunction = create;
        }

        public RandomNumberGenerator create() {
            RandomNumberGenerator rng = createFunction.get();
            assert rng.getKind() == this;
            return rng;
        }

        public boolean isAvailable() {
            return createFunction != null;
        }
    }

    public enum NormKind {
        BUGGY_KINDERMAN_RAMAGE,
        AHRENS_DIETER,
        BOX_MULLER,
        USER_NORM,
        INVERSION,
        KINDERMAN_RAMAGE;

        @CompilationFinal(dimensions = 1) static final NormKind[] VALUES = values();
    }

    public enum SampleKind {
        ROUNDING,
        REJECTION;

        @CompilationFinal(dimensions = 1) static final SampleKind[] VALUES = values();
    }

    public static final int NO_KIND_CHANGE = -2; // internal value
    public static final int DEFAULT_KIND_CHANGE = -1; // comes from RNG.R
    public static final Integer SAME_SEED = null;
    private static final Kind DEFAULT_KIND = Kind.MERSENNE_TWISTER;
    private static final NormKind DEFAULT_NORM_KIND = NormKind.INVERSION;
    private static final SampleKind DEFAULT_SAMPLE_KIND = SampleKind.REJECTION;
    public static final String RANDOM_SEED = ".Random.seed";
    private static final double UINT_MAX = (double) Integer.MAX_VALUE * 2;

    public static final class ContextStateImpl implements RContext.ContextState {
        private RandomNumberGenerator currentGenerator;
        private final RandomNumberGenerator[] allGenerators;
        private NormKind currentNormKind;
        private SampleKind currentSampleKind;
        private WeakReference<ActiveBinding> dotRandomSeedBinding;

        /**
         * Stores the current RNG seed. The type is Object because the user may assign an arbitrary
         * value to variable {@value RRNG#RANDOM_SEED}. Allowed types are therefore any R value, an
         * {@code int[]} or {@code null}.
         */
        private Object currentSeeds = null;

        private ContextStateImpl() {
            this.currentNormKind = DEFAULT_NORM_KIND;
            this.currentSampleKind = REJECTION;
            this.allGenerators = new RandomNumberGenerator[Kind.VALUES.length];
        }

        @Override
        public RContext.ContextState initialize(RContext context) {
            int seed = timeToSeed();
            RandomNumberGenerator rng = DEFAULT_KIND.create();
            initGenerator(rng, seed);
            this.currentGenerator = rng;
            this.allGenerators[rng.getKind().ordinal()] = rng;
            return this;
        }

        /*
         * Similar to GNUR's RNGkind function.
         */
        @TruffleBoundary
        void updateCurrentGenerator(RandomNumberGenerator newRng, boolean saveState) {
            this.allGenerators[newRng.getKind().ordinal()] = newRng;
            if (saveState) {
                getRNGState();  // updates this.currentGenerator
                // Check if the what will be soon previous generator is not corrupted, otherwise
                // generate the seed again for our newRng
                double u = unifRand();
                if (u < 0.0 || u > 1.0) {
                    RError.warning(RError.NO_CALLER, RError.Message.GENERIC, "someone corrupted the random-number generator: re-initializing");
                    initGenerator(newRng, timeToSeed());
                } else {
                    initGenerator(newRng, (int) (u * UINT_MAX));
                }
                this.currentGenerator = newRng;
                putRNGState();
            } else {
                this.currentGenerator = newRng;
            }
        }

        void switchCurrentGenerator(Kind kind) {
            if (kind != currentGenerator.getKind()) {
                if (allGenerators[kind.ordinal()] != null) {
                    currentGenerator = allGenerators[kind.ordinal()];
                } else {
                    RandomNumberGenerator rng = kind.create();
                    initGenerator(rng, timeToSeed());
                    updateCurrentGenerator(rng, false);
                }
            }
        }

        /*
         * Similar to GNUR's Norm_kind function.
         */
        @TruffleBoundary
        void updateCurrentNormKind(NormKind normKind, boolean saveState) {
            currentNormKind = normKind;
            if (saveState) {
                putRNGState();
            }
        }

        /*
         * Similar to GNUR's Sample_kind function.
         */
        @TruffleBoundary
        void updateCurrentSampleKind(SampleKind sampleKind, boolean saveState) {
            currentSampleKind = sampleKind;
            if (saveState) {
                putRNGState();
            }
        }

        public static ContextStateImpl newContextState() {
            return new ContextStateImpl();
        }

        public void initializeDotRandomSeed(RContext context) {
            assert REnvironment.globalEnv() != null;
            RFunction fun = context.lookupBuiltin(".fastr.set.seed");
            ActiveBinding dotRandomSeed = new ActiveBinding(RType.Any, fun, true);
            Frame frame = REnvironment.globalEnv().getFrame();
            int frameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frame.getFrameDescriptor(), RRNG.RANDOM_SEED);
            FrameSlotChangeMonitor.setActiveBinding(frame, frameIndex, dotRandomSeed, false, null);
            dotRandomSeedBinding = new WeakReference<>(dotRandomSeed);
        }

        public void setCurrentSeeds(Object seeds) {
            ActiveBinding activeBinding = dotRandomSeedBinding.get();
            if (activeBinding != null) {
                activeBinding.setInitialized(true);
            }
            this.currentSeeds = seeds;
        }

        public Object getCurrentSeeds() {
            return this.currentSeeds;
        }

    }

    private static ContextStateImpl getContextState() {
        return RContext.getInstance().stateRNG;
    }

    private static ContextStateImpl getContextState(RContext ctx) {
        return ctx.stateRNG;
    }

    public static int currentKindAsInt() {
        return getContextState().currentGenerator.getKind().ordinal();
    }

    public static int currentNormKindAsInt() {
        return getContextState().currentNormKind.ordinal();
    }

    public static int currentSampleKindAsInt() {
        return getContextState().currentSampleKind.ordinal();
    }

    public static Kind currentKind() {
        return getContextState().currentGenerator.getKind();
    }

    public static RandomNumberGenerator currentGenerator() {
        return getContextState().currentGenerator;
    }

    public static RandomNumberGenerator currentGenerator(RContext ctx) {
        return getContextState(ctx).currentGenerator;
    }

    public static NormKind currentNormKind() {
        return getContextState().currentNormKind;
    }

    public static NormKind currentNormKind(RContext ctx) {
        return getContextState(ctx).currentNormKind;
    }

    public static SampleKind currentSampleKind() {
        return getContextState().currentSampleKind;
    }

    /**
     * Ask the current generator for a random double. (cf. {@code unif_rand} in RNG.c.
     */
    public static double unifRand() {
        return currentGenerator().genrandDouble();
    }

    private static double ru() {
        return (Math.floor(U * RRNG.unifRand()) + RRNG.unifRand()) / U;
    }

    private static double unifIndex0(double dn) {
        double cut = MAX_INT;

        switch (RRNG.currentKind()) {
            case KNUTH_TAOCP:
            case USER_UNIF:
            case KNUTH_TAOCP2:
                cut = 33554431.0; /* 2^25 - 1 */
                break;
            default:
                break;
        }

        double u = dn > cut ? ru() : RRNG.unifRand();
        return Math.floor(dn * u);
    }

    private static double rbits(RandomNumberProvider rand, int bits) {
        // The following code is transcribed from GNU R src/main/RNG.c lines 851-861 in
        // function do_sample.

        // generate a random non-negative integer < 2 ^ bits in 16 bit chunks
        long v = 0;
        for (int n = 0; n <= bits; n += 16) {
            double ru = rand != null ? rand.unifRand() : unifRand();
            int v1 = (int) Math.floor(ru * 65536);
            v = 65536 * v + v1;
        }
        long one64 = 1L;
        // mask out the bits in the result that are not needed
        return v & ((one64 << bits) - 1);
    }

    public static double unifIndex(double dn) {
        return unifIndex(null, dn);
    }

    public static double unifIndex(RandomNumberProvider rand, double dn) {
        if (RRNG.currentSampleKind() == ROUNDING) {
            return unifIndex0(dn);
        }

        // rejection sampling from integers below the next larger power of two
        if (dn <= 0) {
            return 0.0;
        }
        int bits = (int) Math.ceil(Math.log(dn) / LOG2);
        double dv;
        do {
            dv = rbits(rand, bits);
        } while (dn <= dv);
        return dv;
    }

    /**
     * Set the seed and optionally the RNG kind and norm kind.
     *
     * @param seed new seed
     * @param kindAsInt {@link #NO_KIND_CHANGE} for no change, else ordinal value of new
     *            {@link Kind}.
     * @param normKindAsInt {@link #NO_KIND_CHANGE} for no change, else ordinal value of new
     *            {@link NormKind}.
     */
    @TruffleBoundary
    public static void doSetSeed(int seed, int kindAsInt, int normKindAsInt, int sampleKindAsInt) {
        getRNGKind(RNull.instance);
        changeKindsAndInitGenerator(seed, kindAsInt, normKindAsInt, sampleKindAsInt);
        putRNGState();
    }

    /**
     * Set the kind and optionally the norm kind, called from R builtin {@code RNGkind}. GnuR
     * chooses the new seed from the previous RNG.
     */
    public static void doRNGKind(int kindAsInt, int normKindAsInt, int sampleKindAsInt) {
        getRNGKind(RNull.instance);
        changeKindsAndInitGenerator(SAME_SEED, kindAsInt, normKindAsInt, sampleKindAsInt);
    }

    @TruffleBoundary
    private static void changeKindsAndInitGenerator(Integer newSeed, int kindAsInt, int normKindAsInt, int sampleKindAsInt) {
        RandomNumberGenerator rng;
        if (kindAsInt != NO_KIND_CHANGE) {
            if (kindAsInt == DEFAULT_KIND_CHANGE) {
                rng = DEFAULT_KIND.create();
            } else {
                rng = intToKind(kindAsInt).create();
            }
            getContextState().updateCurrentGenerator(rng, true);
        }

        // re-read unconditionally since it may change in updateCurrentGenerator
        rng = getContextState().currentGenerator;

        if (!Objects.equals(newSeed, SAME_SEED)) {
            initGenerator(rng, newSeed);
        }

        if (normKindAsInt != NO_KIND_CHANGE) {
            if (normKindAsInt == DEFAULT_KIND_CHANGE) {
                getContextState().updateCurrentNormKind(DEFAULT_NORM_KIND, true);
            } else {
                getContextState().updateCurrentNormKind(intToNormKind(normKindAsInt), true);
            }
        } // otherwise the current one stays

        if (sampleKindAsInt != NO_KIND_CHANGE) {
            if (sampleKindAsInt == DEFAULT_KIND_CHANGE) {
                getContextState().updateCurrentSampleKind(DEFAULT_SAMPLE_KIND, true);
            } else {
                getContextState().updateCurrentSampleKind(intToSampleKind(sampleKindAsInt), true);
            }
        } // otherwise the current one stays
    }

    private static Kind intToKind(int kindAsInt) {
        if (kindAsInt < 0 || kindAsInt >= Kind.VALUES.length || !Kind.VALUES[kindAsInt].isAvailable()) {
            throw RError.error(RError.NO_CALLER, RError.Message.RNG_NOT_IMPL_KIND, kindAsInt);
        }
        return Kind.VALUES[kindAsInt];

    }

    private static NormKind intToNormKind(int normKindAsInt) {
        return NormKind.VALUES[normKindAsInt];
    }

    private static SampleKind intToSampleKind(int sampleKindAsInt) {
        return SampleKind.VALUES[sampleKindAsInt];
    }

    private static void initGenerator(RandomNumberGenerator generator, int aSeed) {
        // Initial scrambling, common to all, from RNG_Init in src/main/RNG.c
        int seed = aSeed;
        for (int i = 0; i < 50; i++) {
            seed = (69069 * seed + 1);
        }
        generator.init(seed);
    }

    @TruffleBoundary
    private static Object getDotRandomSeed() {
        return RContext.getInstance().stateRNG.getCurrentSeeds();
    }

    /**
     * Create a random integer.
     */
    public static Integer timeToSeed() {
        int pid = (int) BaseRFFI.GetpidRootNode.create().getCallTarget().call();
        int millis = (int) (System.currentTimeMillis() & 0xFFFFFFFFL);
        return (millis << 16) ^ pid;
    }

    private static void randomize(Kind kind) {
        RandomNumberGenerator rng = kind.create();
        initGenerator(rng, timeToSeed());
        getContextState().updateCurrentGenerator(rng, false);
    }

    private static void handleInvalidSeed() {
        randomize(DEFAULT_KIND);
        getContextState().updateCurrentNormKind(DEFAULT_NORM_KIND, false);
        getContextState().updateCurrentSampleKind(DEFAULT_SAMPLE_KIND, false);
    }

    /**
     * Sets the current generator according to the flag under index 0 of given vector
     * {@code seedsObj}.
     */
    private static void getRNGKind(Object seedsObj) {
        Object seeds = seedsObj;
        if (seeds == RNull.instance) {
            seeds = getDotRandomSeed();
        }
        if (seeds == null) {
            return;
        }
        int tmp;
        if (seeds instanceof Integer) {
            tmp = (int) seeds;
        } else if (seeds instanceof RIntVector) {
            RIntVector seedsVec = (RIntVector) seeds;
            tmp = seedsVec.getLength() == 0 ? RRuntime.INT_NA : seedsVec.getDataAt(0);
        } else if (seeds instanceof int[]) {
            int[] seedsArr = (int[]) seeds;
            tmp = seedsArr.length == 0 ? RRuntime.INT_NA : seedsArr[0];
        } else {
            // allow RMissing to indicate that ".Random.seed" has not been initialized yet
            if (seeds != RMissing.instance) {
                RError.warning(RError.SHOW_CALLER, RError.Message.SEED_TYPE, RRuntime.getRTypeName(seeds));
            }
            handleInvalidSeed();
            return;
        }
        if (tmp == RRuntime.INT_NA || tmp < 0 || tmp > 11000) {
            assert seeds != RMissing.instance;
            String type = seeds instanceof RBaseObject ? ((RBaseObject) seeds).getRType().getName() : "unknown";
            RError.warning(RError.NO_CALLER, RError.Message.SEED_TYPE, type);
            handleInvalidSeed();
            return;
        }

        int kindIntValue = tmp % 100;
        if (kindIntValue >= Kind.VALUES.length) {
            RError.warning(RError.NO_CALLER, RError.Message.GENERIC, "'.Random.seed[1]' is not a valid RNG kind so ignored");
            handleInvalidSeed();
            return;
        }
        Kind newKind = intToKind(kindIntValue);

        int normKindIntValue = (tmp % 10000) / 100;
        int sampleKindIntValue = (tmp / 10000);
        if (normKindIntValue >= NormKind.VALUES.length || sampleKindIntValue > REJECTION.ordinal()) {
            RError.warning(RError.NO_CALLER, RError.Message.GENERIC, "'.Random.seed[1]' is not a valid Normal type, so ignored");
            handleInvalidSeed();
            return;
        }
        NormKind newNormKind = intToNormKind(normKindIntValue);
        SampleKind newSampleKind = intToSampleKind(sampleKindIntValue);

        // TODO: GNU R checks if user-supplied generator is available, but it seems like we would
        // not be able to initialize the user RNG at all if it wasn't

        getContextState().switchCurrentGenerator(newKind);
        getContextState().updateCurrentNormKind(newNormKind, false);
        getContextState().updateCurrentSampleKind(newSampleKind, false);
    }

    /**
     * Loads the state of RNG from global environment variable {@code .Random.seed}. This should be
     * invoked before any random numbers generation as the user may have directly changed the
     * {@code .Random.seed} variable and the current generator and/or its seed should be updated.
     */
    @TruffleBoundary
    public static void getRNGState() {
        Object seedsObj = getDotRandomSeed();
        if (seedsObj == null) {
            randomize(currentKind());
        } else {
            getRNGKind(seedsObj);
            int[] seeds;
            if (seedsObj instanceof Integer) {
                seeds = new int[]{(int) seedsObj};
            } else if (seedsObj instanceof RIntVector) {
                RIntVector seedsVec = (RIntVector) seedsObj;
                seeds = seedsVec.getReadonlyData();
                if (seeds == currentGenerator().getSeeds()) {
                    // Optimization: if the array instance has not changed, then the .Random.seed
                    // variable was not changed, nor materialized to a native mirror, we still hold
                    // the same reference to its underlying data. Note: setISeed is potentially
                    // doing some clean-up of the data, so it may not be O(1) operation and it's
                    // good to avoid it.
                    return;
                }
            } else if (seedsObj instanceof int[]) {
                seeds = (int[]) seedsObj;
                if (seeds == currentGenerator().getSeeds()) {
                    // no change of the .Random.seed variable
                    return;
                }
            } else {
                // seedsObj is not valid, which should have been reported and fixed in getRNGKind
                return;
            }

            if (seeds.length > 1 && seeds.length < (currentGenerator().getNSeed() + 1)) {
                throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, "'.Random.seed' has wrong length");
            }
            if (seeds.length == 1 && currentKind() != Kind.USER_UNIF) {
                randomize(currentKind());
            } else {
                currentGenerator().setISeed(seeds);
            }
        }
    }

    /**
     * Saves the state of RNG into global environment under {@code .Random.seed}. This should be
     * invoked after any random numbers generation.
     */
    @TruffleBoundary
    public static void putRNGState() {
        int[] seeds = currentGenerator().getSeeds();
        seeds[0] = currentKind().ordinal() + 100 * currentNormKind().ordinal() + 10000 * currentSampleKind().ordinal();
        RContext.getInstance().stateRNG.setCurrentSeeds(seeds);
    }
}
