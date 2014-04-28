/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime.rng;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ffi.*;
import com.oracle.truffle.r.runtime.rng.mm.*;
import com.oracle.truffle.r.runtime.rng.mt.*;

/**
 * Facade class to the R random number generators, (see src/main/RNG.c in GnuR). The individual
 * generators are implemented in their own class. Currently there is only one implemented,
 * {@link MersenneTwister}.
 *
 * The fact that the R programmer can set {@code .Random.seed} explicitly, as opposed to the
 * recommended approach of calling {@code set.seed}, is something of a pain as it changes the
 * {@link Kind}, the {@link NormKind} and the actual seeds all in one go and in a totally
 * uncontrolled way, which then has to be checked. Currently we do not support reading it, although
 * we do create/update it, primarily as a debugging aid.
 */
public class RRNG {
    /**
     * The standard kinds provided by GnuR, where the ordinal value corresponds to the argument to
     * {@link RRNG#setSeed}.
     */
    public enum Kind {
        WICHMANN_HILL,
        MARSAGLIA_MULTICARRY {
            @Override
            Kind setGenerator() {
                generator = new MarsagliaMulticarry();
                return this;
            }
        },
        SUPER_DUPER,
        MERSENNE_TWISTER {
            @Override
            Kind setGenerator() {
                generator = new MersenneTwister();
                return this;
            }
        },
        KNUTH_TAOCP,
        USER_UNIF,
        KNUTH_TAOCP2,
        LECUYER_CMRG;

        static final Kind[] VALUES = values();

        Generator generator;

        Kind setGenerator() {
            assert false;
            return this;
        }

    }

    public enum NormKind {
        BUGGY_KINDERMAN_RAMAGE,
        AHRENS_DIETER,
        BOX_MULLER,
        USER_NORM,
        INVERSION,
        KINDERMAN_RAMAGE;

        static final NormKind[] VALUES = values();

    }

    public static final int NO_KIND_CHANGE = -1;
    public static final Integer RESET = null;
    private static final Kind DEFAULT_KIND = Kind.MERSENNE_TWISTER;
    private static final NormKind DEFAULT_NORM_KIND = NormKind.INVERSION;
    private static final String RANDOM_SEED = ".Random.seed";
    public static final double I2_32M1 = 2.3283064365386963e-10;

    /**
     * The interface that a random number generator must implement.
     */
    public interface Generator {
        void init(int seed);

        void fixupSeeds(boolean initial);

        int[] getSeeds();

        double genrandDouble();
    }

    /**
     * R Errors and warnings are possible during operations like {@link #setSeed}, which are all
     * passed back to the associated builtin.
     */
    public static class RNGException extends Exception {
        private static final long serialVersionUID = 1L;
        private boolean isError;

        RNGException(String msg, boolean isError) {
            super(msg);
            this.isError = isError;
        }

        public boolean isError() {
            return isError;
        }
    }

    /**
     * The current {@link Generator}.
     */
    private static Kind currentKind;
    private static NormKind currentNormKind;

    public static void initialize() {
        int seed = timeToSeed();
        currentNormKind = DEFAULT_NORM_KIND;
        initGenerator(DEFAULT_KIND.setGenerator(), seed);
    }

    public static Generator get() {
        return currentKind.generator;
    }

    /**
     *
     * @param frame frame associated with {@code set.seed} function (our caller)
     * @param seed {@link #RESET} to reset, else new seed
     * @param kindAsInt {@link #NO_KIND_CHANGE} for no change, else ordinal value of new
     *            {@link Kind}.
     * @param normKindAsInt {@link #NO_KIND_CHANGE} for no change, else ordinal value of new
     *            {@link NormKind}.
     */
    public static void setSeed(VirtualFrame frame, Integer seed, int kindAsInt, int normKindAsInt) throws RNGException {
        int newSeed = seed == RESET ? timeToSeed() : seed;
        Kind kind = currentKind;
        if (kindAsInt != NO_KIND_CHANGE) {
            kind = setKind(kindAsInt);
        }
        if (normKindAsInt != NO_KIND_CHANGE) {
            setNormKind(normKindAsInt);
        }
        initGenerator(kind, newSeed);
        updateDotRandomSeed(frame);
    }

    public static double fixup(double x) {
        /* transcribed from GNU R, RNG.c (fixup) */
        /* ensure 0 and 1 are never returned */
        if (x <= 0.0) {
            return 0.5 * I2_32M1;
        }
        // removed fixup for 1.0 since x is in [0,1).
        // TODO Since GnuR does include this is should we not for compatibility (probably).
        // if ((1.0 - x) <= 0.0) return 1.0 - 0.5 * I2_32M1;
        return x;
    }

    private static Kind setKind(int kindAsInt) throws RNGException {
        if (kindAsInt < 0 || kindAsInt >= Kind.VALUES.length) {
            throw new RNGException("unimplemented RNG kind " + kindAsInt, true);
        }
        return Kind.VALUES[kindAsInt];

    }

    @SuppressWarnings("unused")
    private static NormKind setNormKind(int normKindAsInt) throws RNGException {
        assert false;
        return null;
    }

    private static void initGenerator(Kind kind, int aSeed) {
        // Initial scrambling, common to all, from RNG_Init in src/main/RNG.c
        int seed = aSeed;
        for (int i = 0; i < 50; i++) {
            seed = (69069 * seed + 1);
        }
        if (kind.generator == null) {
            kind.setGenerator();
        }
        kind.generator.init(seed);
        currentKind = kind;
    }

    @SuppressWarnings("unused")
    private static Object getDotRandomSeed(VirtualFrame frame) {
        // TODO try to find .Random.seed in R_GlobalEnv
        return null;
    }

    private static void updateDotRandomSeed(@SuppressWarnings("unused") VirtualFrame frame) {
        int[] seeds = currentKind.generator.getSeeds();
        int[] data = new int[seeds.length + 1];
        data[0] = currentKind.ordinal() + 100 * currentNormKind.ordinal();
        for (int i = 0; i < seeds.length; i++) {
            data[i + 1] = seeds[i];
        }
        RIntVector vector = RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        // TODO set this properly using "frame"
        try {
            REnvironment.globalEnv().put(RANDOM_SEED, vector);
        } catch (REnvironment.PutException ex) {
            // should never happen
        }
    }

    /**
     * Create a random integer.
     */
    public static Integer timeToSeed() {
        int pid = RFFIFactory.getRFFI().getBaseRFFI().getpid();
        int millis = (int) (System.currentTimeMillis() & 0xFFFFFFFFL);
        return (millis << 16) ^ pid;
    }
}
