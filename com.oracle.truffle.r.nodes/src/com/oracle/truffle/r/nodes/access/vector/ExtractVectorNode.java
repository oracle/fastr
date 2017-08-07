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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.interop.Foreign2RNodeGen;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R.CollectedElements;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({RRuntime.class, com.oracle.truffle.api.interop.Message.class})
public abstract class ExtractVectorNode extends RBaseNode {

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

    protected static boolean isForeignObject(Object object) {
        return RRuntime.isForeignObject(object);
    }

    protected static FirstStringNode createFirstString() {
        return FirstStringNode.createWithError(RError.Message.GENERIC, "Cannot coerce position to character for foreign access.");
    }

    protected static Foreign2R createForeign2RNode() {
        return Foreign2RNodeGen.create();
    }

    protected boolean positionsByVector(Object[] positions) {
        return positions.length == 1 && positions[0] instanceof RAbstractVector && ((RAbstractVector) positions[0]).getLength() > 1;
    }

    private boolean isRecursiveSubscript(Object vector, Object[] positions) {
        return !recursive && !ignoreRecursive && mode.isSubscript() && vector instanceof RAbstractListVector && positions.length == 1;
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"!isForeignObject(vector)", "cached != null", "cached.isSupported(vector, positions, exact, dropDimensions)"})
    protected Object doExtractDefaultCached(Object vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("createDefaultCache(getThis(), vector, positions, exact, dropDimensions)") CachedExtractVectorNode cached) {
        assert !isRecursiveSubscript(vector, positions);
        return cached.apply(vector, positions, null, exact, dropDimensions);
    }

    protected static CachedExtractVectorNode createDefaultCache(ExtractVectorNode node, Object vector, Object[] positions, Object exact, Object dropDimensions) {
        return new CachedExtractVectorNode(node.getMode(), (RTypedValue) vector, positions, (RTypedValue) exact, (RTypedValue) dropDimensions, node.recursive);
    }

    @Specialization(replaces = "doExtractDefaultCached", guards = "!isForeignObject(vector)")
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

    @Specialization(guards = {"isForeignObject(object)", "positionsByVector(positions)"})
    protected Object accessFieldByVectorPositions(TruffleObject object, Object[] positions, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") Object dropDimensions,
                    @Cached("READ.createNode()") Node foreignRead,
                    @Cached("KEY_INFO.createNode()") Node keyInfoNode,
                    @Cached("HAS_SIZE.createNode()") Node hasSizeNode,
                    @Cached("create()") CastStringNode castNode,
                    @Cached("createFirstString()") FirstStringNode firstString,
                    @Cached("IS_NULL.createNode()") Node isNullNode,
                    @Cached("IS_BOXED.createNode()") Node isBoxedNode,
                    @Cached("UNBOX.createNode()") Node unboxNode,
                    @Cached("createForeign2RNode()") Foreign2R foreign2RNode) {

        RAbstractVector vec = (RAbstractVector) positions[0];
        CollectedElements ce = new CollectedElements();

        try {
            for (int i = 0; i < vec.getLength(); i++) {
                Object res = read(this, vec.getDataAtAsObject(i), foreignRead, keyInfoNode, hasSizeNode, object, firstString, castNode);
                if (RRuntime.isForeignObject(res)) {
                    if (ForeignAccess.sendIsNull(isNullNode, (TruffleObject) res)) {
                        res = RNull.instance;
                    }
                    if (ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) res)) {
                        res = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) res);
                    }
                }
                ce.getTypeCheck().checkForeign(res);
                ce.getElements().add(foreign2RNode.execute(res));
            }
            return ForeignArray2R.asAbstractVector(ce);
        } catch (InteropException | NoSuchFieldError e) {
            throw RError.interopError(RError.findParentRBase(this), e, object);
        }
    }

    @Specialization(guards = {"isForeignObject(object)", "!positionsByVector(positions)", "positions.length == cachedLength"})
    protected Object accessField(TruffleObject object, Object[] positions, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") Object dropDimensions,
                    @Cached("READ.createNode()") Node foreignRead,
                    @Cached("KEY_INFO.createNode()") Node keyInfoNode,
                    @Cached("HAS_SIZE.createNode()") Node hasSizeNode,
                    @Cached("positions.length") @SuppressWarnings("unused") int cachedLength,
                    @Cached("create()") CastStringNode castNode,
                    @Cached("createFirstString()") FirstStringNode firstString,
                    @Cached("createClassProfile()") ValueProfile positionProfile,
                    @Cached("IS_NULL.createNode()") Node isNullNode,
                    @Cached("IS_BOXED.createNode()") Node isBoxedNode,
                    @Cached("UNBOX.createNode()") Node unboxNode,
                    @Cached("createForeign2RNode()") Foreign2R foreign2RNode) {
        Object[] pos = positionProfile.profile(positions);
        if (pos.length == 0) {
            throw error(RError.Message.GENERIC, "No positions for foreign access.");
        }
        try {
            // TODO implicite unboxing ok? method calls seem to behave this way
            Object result = object;
            for (int i = 0; i < pos.length; i++) {
                result = read(this, pos[i], foreignRead, keyInfoNode, hasSizeNode, (TruffleObject) result, firstString, castNode);
                if (pos.length > 1 && i < pos.length - 1) {
                    assert result instanceof TruffleObject;
                }
            }
            return unbox(result, isNullNode, isBoxedNode, unboxNode, foreign2RNode);
        } catch (InteropException | NoSuchFieldError e) {
            throw RError.interopError(RError.findParentRBase(this), e, object);
        }
    }

    private static Object unbox(Object obj, Node isNullNode, Node isBoxedNode, Node unboxNode, Foreign2R foreign2RNode) throws UnsupportedMessageException {
        if (RRuntime.isForeignObject(obj)) {
            if (ForeignAccess.sendIsNull(isNullNode, (TruffleObject) obj)) {
                return RNull.instance;
            }
            if (ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) obj)) {
                return foreign2RNode.execute(ForeignAccess.sendUnbox(unboxNode, (TruffleObject) obj));
            }
        }
        return foreign2RNode.execute(obj);
    }

    public static Object read(RBaseNode caller, Object positions, Node foreignRead, Node keyInfoNode, Node hasSizeNode, TruffleObject object, FirstStringNode firstString, CastStringNode castNode)
                    throws RError, InteropException {
        Object pos = positions;
        if (pos instanceof Integer) {
            pos = ((int) pos) - 1;
        } else if (pos instanceof Double) {
            pos = ((double) pos) - 1;
        } else if (pos instanceof RAbstractDoubleVector) {
            pos = ((RAbstractDoubleVector) pos).getDataAt(0) - 1;
        } else if (pos instanceof RAbstractIntVector) {
            pos = ((RAbstractIntVector) pos).getDataAt(0) - 1;
        } else if (pos instanceof RAbstractStringVector) {
            pos = firstString.executeString(castNode.doCast(pos));
        } else if (!(pos instanceof String)) {
            throw caller.error(RError.Message.GENERIC, "invalid index during foreign access");
        }

        int info = ForeignAccess.sendKeyInfo(keyInfoNode, object, pos);
        if (KeyInfo.isReadable(info) || ForeignAccess.sendHasSize(hasSizeNode, object)) {
            return ForeignAccess.sendRead(foreignRead, object, pos);
        } else if (pos instanceof String && !KeyInfo.isExisting(info) && JavaInterop.isJavaObject(Object.class, object)) {
            TruffleObject clazz = toJavaClass(object);
            info = ForeignAccess.sendKeyInfo(keyInfoNode, clazz, pos);
            if (KeyInfo.isReadable(info)) {
                return ForeignAccess.sendRead(foreignRead, clazz, pos);
            }
        }
        throw caller.error(RError.Message.GENERIC, "invalid index/identifier during foreign access: " + pos);
    }

    @TruffleBoundary
    private static TruffleObject toJavaClass(TruffleObject obj) {
        return JavaInterop.toJavaClass(obj);
    }
}
