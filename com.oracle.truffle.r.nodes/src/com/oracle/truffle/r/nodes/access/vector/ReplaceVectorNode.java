/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.profile.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Syntax node for element writes.
 */
public abstract class ReplaceVectorNode extends Node {

    /*
     * TODO remove this as soon as the new vector access nodes are the default.
     */
    public static final boolean USE_NODE = System.getProperty("newVectorAccess") != null ? System.getProperty("newVectorAccess").equals("true") : false;

    protected static final int CACHE_LIMIT = 5;

    private final ElementAccessMode mode;
    private final boolean recursive;

    @Child private BoxPrimitiveNode boxVector = BoxPrimitiveNode.create();
    @Child private BoxPrimitiveNode boxValue = BoxPrimitiveNode.create();

    public ReplaceVectorNode(ElementAccessMode mode, boolean recursive) {
        this.mode = mode;
        this.recursive = recursive;
    }

    public final Object apply(Object vector, Object[] positions, Object value) {
        return execute(boxVector.execute(vector), positions, boxValue.execute(value));
    }

    protected abstract Object execute(Object vector, Object[] positions, Object value);

    public static ReplaceVectorNode create(ElementAccessMode mode) {
        return ReplaceVectorNodeGen.create(mode, false);
    }

    static ReplaceVectorNode createRecursive(ElementAccessMode mode) {
        return ReplaceVectorNodeGen.create(mode, true);
    }

    @SuppressWarnings("unchecked")
    protected Class<? extends RTypedValue> getInvalidVectorType(Object vector) {
        if (vector instanceof REnvironment || vector instanceof RFunction) {
            return (Class<? extends RTypedValue>) vector.getClass();
        }
        return null;
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"cached != null", "cached.isSupported(vector, positions)"})
    protected Object doRecursive(RAbstractListVector vector, Object[] positions, Object value,  //
                    @Cached("createRecursiveCache(vector, positions)") RecursiveReplaceSubscriptNode cached) {
        return cached.apply(vector, positions, value);
    }

    protected RecursiveReplaceSubscriptNode createRecursiveCache(Object vector, Object[] positions) {
        if (isRecursiveSubscript(vector, positions)) {
            return RecursiveReplaceSubscriptNode.create((RAbstractListVector) vector, positions[0]);
        }
        return null;
    }

    private boolean isRecursiveSubscript(Object vector, Object[] positions) {
        return !recursive && mode.isSubscript() && vector instanceof RAbstractListVector && positions.length == 1;
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"cached != null", "cached.isSupported(vector, positions, value)"})
    protected Object doReplaceCached(Object vector, Object[] positions, Object value,  //
                    @Cached("createDefaultCached(getThis(), vector, positions, value)") CachedReplaceVectorNode cached) {
        assert !isRecursiveSubscript(vector, positions);
        return cached.apply(vector, positions, value);
    }

    protected static CachedReplaceVectorNode createDefaultCached(ReplaceVectorNode node, Object vector, Object[] positions, Object value) {
        return new CachedReplaceVectorNode(node.mode, (RTypedValue) vector, positions, (RTypedValue) value, true, node.recursive);
    }

    public ElementAccessMode getMode() {
        return mode;
    }

    @Specialization(contains = "doReplaceCached")
    @TruffleBoundary
    protected Object doReplaceDefaultGeneric(Object vector, Object[] positions, Object value,  //
                    @Cached("new(createDefaultCached(getThis(), vector, positions, value))") GenericVectorReplaceNode generic) {
        return generic.get(this, vector, positions, value).apply(vector, positions, value);
    }

    // TODO hack until Truffle-DSL supports this.
    protected ReplaceVectorNode getThis() {
        return this;
    }

    protected static final class GenericVectorReplaceNode extends TruffleBoundaryNode {

        @Child private CachedReplaceVectorNode cached;

        public GenericVectorReplaceNode(CachedReplaceVectorNode cachedOperation) {
            this.cached = insert(cachedOperation);
        }

        public CachedReplaceVectorNode get(ReplaceVectorNode node, Object vector, Object[] positions, Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (!cached.isSupported(vector, positions, value)) {
                cached = cached.replace(createDefaultCached(node, vector, positions, value));
            }
            return cached;
        }
    }
}
