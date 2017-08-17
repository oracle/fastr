/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.rng;

import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
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
 * uncontrolled way, which then has to be checked. Currently we do not support reading it, although
 * we do create/update it when the seed/kind is changed, primarily as a debugging aid. N.B. GnuR
 * updates it on <i>every</i> random number generation!
 *
 * Important note: make sure to invoke {@link #getRNGState()} before invoking any other methods from
 * this class and to invoke {@link #putRNGState()} when done witch random number generation.
 */
public class RRNG {
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

    public static final int NO_KIND_CHANGE = -2; // internal value
    public static final int DEFAULT_KIND_CHANGE = -1; // comes from RNG.R
    public static final Integer SAME_SEED = null;
    private static final Kind DEFAULT_KIND = Kind.MERSENNE_TWISTER;
    private static final NormKind DEFAULT_NORM_KIND = NormKind.INVERSION;
    public static final String RANDOM_SEED = ".Random.seed";
    private static final double UINT_MAX = (double) Integer.MAX_VALUE * 2;

    public static final class ContextStateImpl implements RContext.ContextState {
        private RandomNumberGenerator currentGenerator;
        private final RandomNumberGenerator[] allGenerators;
        private NormKind currentNormKind;
        public Object currentSeeds;

        private ContextStateImpl() {
            this.currentNormKind = DEFAULT_NORM_KIND;
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

        public static ContextStateImpl newContextState() {
            return new ContextStateImpl();
        }
    }

    private static ContextStateImpl getContextState() {
        return RContext.getInstance().stateRNG;
    }

    public static int currentKindAsInt() {
        return getContextState().currentGenerator.getKind().ordinal();
    }

    public static int currentNormKindAsInt() {
        return getContextState().currentNormKind.ordinal();
    }

    private static Kind currentKind() {
        return getContextState().currentGenerator.getKind();
    }

    public static RandomNumberGenerator currentGenerator() {
        return getContextState().currentGenerator;
    }

    public static NormKind currentNormKind() {
        return getContextState().currentNormKind;
    }

    /**
     * Ask the current generator for a random double. (cf. {@code unif_rand} in RNG.c.
     */
    public static double unifRand() {
        return currentGenerator().genrandDouble();
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
    public static void doSetSeed(int seed, int kindAsInt, int normKindAsInt) {
        getRNGKind(RNull.instance);
        changeKindsAndInitGenerator(seed, kindAsInt, normKindAsInt);
        putRNGState();
    }

    /**
     * Set the kind and optionally the norm kind, called from R builtin {@code RNGkind}. GnuR
     * chooses the new seed from the previous RNG.
     */
    public static void doRNGKind(int kindAsInt, int normKindAsInt) {
        getRNGKind(RNull.instance);
        changeKindsAndInitGenerator(SAME_SEED, kindAsInt, normKindAsInt);
    }

    @TruffleBoundary
    private static void changeKindsAndInitGenerator(Integer newSeed, int kindAsInt, int normKindAsInt) {
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

        if (newSeed != SAME_SEED) {
            initGenerator(rng, newSeed);
        }

        if (normKindAsInt != NO_KIND_CHANGE) {
            if (normKindAsInt == DEFAULT_KIND_CHANGE) {
                getContextState().updateCurrentNormKind(DEFAULT_NORM_KIND, true);
            } else {
                getContextState().updateCurrentNormKind(intToNormKind(normKindAsInt), true);
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
        return RContext.getInstance().stateRNG.currentSeeds;
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
            assert seeds != RMissing.instance;
            assert seeds instanceof RTypedValue;
            RError.warning(RError.NO_CALLER, RError.Message.SEED_TYPE, ((RTypedValue) seeds).getRType().getName());
            handleInvalidSeed();
            return;
        }
        if (tmp == RRuntime.INT_NA || tmp < 0 || tmp > 1000) {
            String type = seeds instanceof RTypedValue ? ((RTypedValue) seeds).getRType().getName() : "unknown";
            RError.warning(RError.NO_CALLER, RError.Message.SEED_TYPE, type);
            handleInvalidSeed();
            return;
        }

        int normKindIntValue = tmp / 100;
        if (normKindIntValue >= NormKind.VALUES.length) {
            RError.warning(RError.NO_CALLER, RError.Message.GENERIC, "'.Random.seed[1]' is not a valid Normal type, so ignored");
            handleInvalidSeed();
            return;
        }
        NormKind newNormKind = intToNormKind(normKindIntValue);

        int kindIntValue = tmp % 100;
        if (kindIntValue >= Kind.VALUES.length) {
            RError.warning(RError.NO_CALLER, RError.Message.GENERIC, "'.Random.seed[1]' is not a valid RNG kind so ignored");
            handleInvalidSeed();
            return;
        }
        Kind newKind = intToKind(kindIntValue);

        // TODO: GNU R checks if user-supplied generator is available, but it seems like we would
        // not be able to initialize the user RNG at all if it wasn't

        getContextState().switchCurrentGenerator(newKind);
        getContextState().updateCurrentNormKind(newNormKind, false);
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
                seeds = seedsVec.getDataWithoutCopying();
                if (seeds == currentGenerator().getSeeds()) {
                    // no change of the .Random.seed variable
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
        seeds[0] = currentKind().ordinal() + 100 * currentNormKind().ordinal();
        RContext.getInstance().stateRNG.currentSeeds = seeds;
    }
}
