/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime;

/**
 * Class that should eventually contain all DSL (and AST rewriting) related constants.
 */
public final class DSLConfig {
    private static final double DSL_CACHE_SIZE_FACTOR = FastROptions.DSLCacheSizeFactor.getNonNegativeDoubleValue();

    /**
     * Some DSL {@code limit}s must be set to either constant {@code 1} or constant {@code 0},
     * otherwise the DSL compiler will fail. To allow these to be still configured, we set the limit
     * to {@code 1} and use this final field as a guard.
     */
    public static final boolean LIMIT_1_GUARD = FastROptions.DSLCacheSizeFactor.getNonNegativeDoubleValue() != 0;

    private DSLConfig() {
        // only static methods
    }

    /**
     * This method should be used to set any cache size that is used to create specialized variants
     * of vector access nodes like {@link com.oracle.truffle.r.runtime.data.nodes.VectorAccess}.
     */
    public static int getVectorAccessCacheSize() {
        return getCacheSize(3);
    }

    /**
     * This method should be used to set any cache size that is used to create variants of vector
     * access nodes like {@link com.oracle.truffle.r.runtime.data.nodes.VectorAccess} when based on
     * an abstract vector type like e.g.
     * {@link com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector}.
     */
    public static int getGenericVectorAccessCacheSize() {
        return getCacheSize(8);
    }

    /**
     * This method should be used to set any cache size that can be configured, i.e. it does not
     * matter how large the cache is and it can even be zero. If used, make sure that there is more
     * generic specialization available.
     */
    public static int getCacheSize(int suggestedSize) {
        return (int) (suggestedSize * DSL_CACHE_SIZE_FACTOR);
    }
}
