/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Implements a simple inline cache of specialized {@link CachedExtractVectorNode} instances. Allows
 * to use {@link CachedExtractVectorNode} in situations where the cached properties of the arguments
 * (e.g., their type) may change.
 */
public abstract class DispatchedCachedExtractVectorNode extends RBaseNode {
    protected final ElementAccessMode mode;
    protected final boolean recursive;

    public DispatchedCachedExtractVectorNode(ElementAccessMode mode, boolean recursive) {
        this.mode = mode;
        this.recursive = recursive;
    }

    public abstract Object execute(RAbstractContainer originalVector, Object[] originalPositions, PositionsCheckNode.PositionProfile[] originalProfiles, Object originalExact,
                    Object originalDropDimensions);

    @Specialization(guards = "cachedExtractVectorNode.isSupported(originalVector, originalPositions, originalExact, originalDropDimensions)", limit = "getCacheSize(3)")
    Object doCached(RAbstractContainer originalVector, Object[] originalPositions, PositionsCheckNode.PositionProfile[] originalProfiles, Object originalExact, Object originalDropDimensions,
                    @Cached("createCachedExtractNode(originalVector, originalPositions, originalExact, originalDropDimensions)") CachedExtractVectorNode cachedExtractVectorNode) {
        return cachedExtractVectorNode.apply(originalVector, originalPositions, originalProfiles, originalExact, originalDropDimensions);
    }

    @TruffleBoundary
    @Specialization(replaces = "doCached")
    Object doUncached(RAbstractContainer originalVector, Object[] originalPositions, PositionsCheckNode.PositionProfile[] originalProfiles, Object originalExact, Object originalDropDimensions,
                    @Cached("create()") GenericVectorExtractNode generic) {
        CachedExtractVectorNode genericExtract = generic.get(mode, recursive, originalVector, originalPositions, originalExact, originalDropDimensions);
        return doCached(originalVector, originalPositions, originalProfiles, originalExact, originalDropDimensions, genericExtract);
    }

    protected CachedExtractVectorNode createCachedExtractNode(RAbstractContainer originalVector, Object[] originalPositions, Object originalExact, Object originalDropDimensions) {
        return createCachedExtractVectorNode(mode, recursive, originalVector, originalPositions, originalExact, originalDropDimensions);
    }

    private static CachedExtractVectorNode createCachedExtractVectorNode(ElementAccessMode mode, boolean recursive, RAbstractContainer originalVector, Object[] originalPositions, Object originalExact,
                    Object originalDropDimensions) {
        return new CachedExtractVectorNode(mode, originalVector, originalPositions, (RBaseObject) originalExact, (RBaseObject) originalDropDimensions, recursive);
    }

    protected static final class GenericVectorExtractNode extends TruffleBoundaryNode {
        @Child private CachedExtractVectorNode cached;

        public static GenericVectorExtractNode create() {
            return new GenericVectorExtractNode();
        }

        public CachedExtractVectorNode get(ElementAccessMode mode, boolean recursive, RAbstractContainer originalVector, Object[] originalPositions, Object originalExact,
                        Object originalDropDimensions) {
            CompilerAsserts.neverPartOfCompilation();
            if (cached == null || !cached.isSupported(originalVector, originalPositions, originalExact, originalDropDimensions)) {
                cached = insert(createCachedExtractVectorNode(mode, recursive, originalVector, originalPositions, originalExact, originalDropDimensions));
            }
            return cached;
        }
    }
}
