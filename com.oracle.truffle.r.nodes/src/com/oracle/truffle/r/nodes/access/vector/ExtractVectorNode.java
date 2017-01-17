/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class ExtractVectorNode extends Node {

    protected static final int CACHE_LIMIT = 5;

    private final ElementAccessMode mode;
    private final boolean recursive;
    private final boolean ignoreRecursive;

    @Child private BoxPrimitiveNode boxVector = BoxPrimitiveNode.create();
    @Child private BoxPrimitiveNode boxExact = BoxPrimitiveNode.create();
    @Child private BoxPrimitiveNode boxDropdimensions = BoxPrimitiveNode.create();

    ExtractVectorNode(ElementAccessMode mode, boolean recursive, boolean ignoreRecursive) {
        this.mode = mode;
        this.recursive = recursive;
        this.ignoreRecursive = ignoreRecursive;
    }

    public ElementAccessMode getMode() {
        return mode;
    }

    public final Object applyAccessField(VirtualFrame frame, Object vector, String singlePosition) {
        return apply(frame, vector, new Object[]{singlePosition}, RLogical.valueOf(false), RMissing.instance);
    }

    public final Object apply(VirtualFrame frame, Object vector, Object[] positions, Object exact, Object dropDimensions) {
        return execute(frame, boxVector.execute(vector), positions, boxExact.execute(exact), boxDropdimensions.execute(dropDimensions));
    }

    public static ExtractVectorNode create(ElementAccessMode accessMode, boolean ignoreRecursive) {
        return ExtractVectorNodeGen.create(accessMode, false, ignoreRecursive);
    }

    static ExtractVectorNode createRecursive(ElementAccessMode accessMode) {
        return ExtractVectorNodeGen.create(accessMode, true, false);
    }

    protected abstract Object execute(VirtualFrame frame, Object vector, Object[] positions, Object exact, Object dropDimensions);

    protected Node createForeignRead(Object[] positions) {
        if (positions.length != 1) {
            throw RError.error(this, RError.Message.GENERIC, "Invalid number positions for foreign access.");
        }
        return Message.READ.createNode();
    }

    protected static boolean isForeignObject(TruffleObject object) {
        return RRuntime.isForeignObject(object);
    }

    protected static FirstStringNode createFirstString() {
        return FirstStringNode.createWithError(RError.Message.GENERIC, "Cannot corce position to character for foreign access.");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isForeignObject(object)", "positions.length == cachedLength"})
    protected Object accessField(VirtualFrame frame, TruffleObject object, Object[] positions, Object exact, Object dropDimensions,
                    @Cached("createForeignRead(positions)") Node foreignRead,
                    @Cached("positions.length") int cachedLength,
                    @Cached("create()") CastStringNode castNode,
                    @Cached("createFirstString()") FirstStringNode firstString,
                    @Cached("createClassProfile()") ValueProfile positionProfile) {
        Object position = positionProfile.profile(positions[0]);
        try {
            if (position instanceof Integer) {
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{((int) position) - 1});
            } else if (position instanceof Double) {
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{((double) position) - 1});
            } else if (position instanceof String) {
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{position});
            } else if (position instanceof RAbstractStringVector) {
                String string = firstString.executeString(castNode.execute(position));
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{string});
            } else if (position instanceof RAbstractDoubleVector) {
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{((RAbstractDoubleVector) position).getDataAt(0) - 1});
            } else if (position instanceof RAbstractIntVector) {
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{((RAbstractIntVector) position).getDataAt(0) - 1});
            } else {
                throw RError.error(this, RError.Message.GENERIC, "invalid index during foreign access");
            }
        } catch (InteropException e) {
            throw RError.interopError(RError.findParentRBase(this), e, object);
        }
    }

    @Specialization(guards = {"cached != null", "cached.isSupported(vector, positions)"})
    protected Object doExtractSameDimensions(VirtualFrame frame, RAbstractVector vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("createRecursiveCache(vector, positions)") RecursiveExtractSubscriptNode cached) {
        return cached.apply(frame, vector, positions, exact, dropDimensions);
    }

    @Specialization(guards = {"cached != null", "cached.isSupported(vector, positions)"})
    protected Object doExtractRecursive(VirtualFrame frame, RAbstractListVector vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("createRecursiveCache(vector, positions)") RecursiveExtractSubscriptNode cached) {
        return cached.apply(frame, vector, positions, exact, dropDimensions);
    }

    protected RecursiveExtractSubscriptNode createRecursiveCache(Object vector, Object[] positions) {
        if (isRecursiveSubscript(vector, positions)) {
            return RecursiveExtractSubscriptNode.create((RAbstractListVector) vector, positions[0]);
        }
        return null;
    }

    private boolean isRecursiveSubscript(Object vector, Object[] positions) {
        return !recursive && !ignoreRecursive && mode.isSubscript() && vector instanceof RAbstractListVector && positions.length == 1;
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"cached != null", "cached.isSupported(vector, positions, exact, dropDimensions)"})
    protected Object doExtractDefaultCached(Object vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("createDefaultCache(getThis(), vector, positions, exact, dropDimensions)") CachedExtractVectorNode cached) {
        assert !isRecursiveSubscript(vector, positions);
        return cached.apply(vector, positions, null, exact, dropDimensions);
    }

    protected static CachedExtractVectorNode createDefaultCache(ExtractVectorNode node, Object vector, Object[] positions, Object exact, Object dropDimensions) {
        return new CachedExtractVectorNode(node.getMode(), (RTypedValue) vector, positions, (RTypedValue) exact, (RTypedValue) dropDimensions, node.recursive);
    }

    @Specialization(contains = "doExtractDefaultCached")
    @TruffleBoundary
    protected Object doExtractDefaultGeneric(Object vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("new(createDefaultCache(getThis(), vector, positions, exact, dropDimensions))") GenericVectorExtractNode generic) {
        return generic.get(this, vector, positions, exact, dropDimensions).apply(vector, positions, null, exact, dropDimensions);
    }

    // TODO hack until Truffle-DSL supports this.
    protected ExtractVectorNode getThis() {
        return this;
    }

    protected static final class GenericVectorExtractNode extends TruffleBoundaryNode {

        @Child private CachedExtractVectorNode cached;

        public GenericVectorExtractNode(CachedExtractVectorNode cachedOperation) {
            this.cached = insert(cachedOperation);
        }

        public CachedExtractVectorNode get(ExtractVectorNode node, Object vector, Object[] positions, Object exact, Object dropDimensions) {
            CompilerAsserts.neverPartOfCompilation();
            if (!cached.isSupported(vector, positions, exact, dropDimensions)) {
                cached = cached.replace(createDefaultCache(node, vector, positions, exact, dropDimensions));
            }
            return cached;
        }
    }
}
