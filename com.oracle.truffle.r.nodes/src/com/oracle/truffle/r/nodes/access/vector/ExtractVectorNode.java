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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.java.JavaInterop;
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
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@ImportStatic({RRuntime.class, com.oracle.truffle.api.interop.Message.class})
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

    protected static boolean isForeignObject(TruffleObject object) {
        return RRuntime.isForeignObject(object);
    }

    protected static FirstStringNode createFirstString() {
        return FirstStringNode.createWithError(RError.Message.GENERIC, "Cannot coerce position to character for foreign access.");
    }

    @Specialization(guards = {"isForeignObject(object)", "positions.length == cachedLength"})
    protected Object accessField(TruffleObject object, Object[] positions, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") Object dropDimensions,
                    @Cached("READ.createNode()") Node foreignRead,
                    @Cached("KEY_INFO.createNode()") Node keyInfoNode,
                    @Cached("positions.length") @SuppressWarnings("unused") int cachedLength,
                    @Cached("create()") CastStringNode castNode,
                    @Cached("createFirstString()") FirstStringNode firstString,
                    @Cached("createClassProfile()") ValueProfile positionProfile,
                    @Cached("IS_NULL.createNode()") Node isNullNode,
                    @Cached("IS_BOXED.createNode()") Node isBoxedNode,
                    @Cached("UNBOX.createNode()") Node unboxNode) {
        if (positions.length == 0) {
            throw RError.error(this, RError.Message.GENERIC, "No positions for foreign access.");
        }
        positions = positionProfile.profile(positions);
        try {
            try {
                // TODO implicite unboxing ok? method calls seem to behave this way
                Object result = object;
                for (int i = 0; i < positions.length; i++) {
                    result = send(positions[i], foreignRead, keyInfoNode, (TruffleObject) result, firstString, castNode);
                    if (positions.length > 1 && i < positions.length - 1) {
                        assert result instanceof TruffleObject;
                    }
                }
                return unbox(result, isNullNode, isBoxedNode, unboxNode);
            } catch (UnknownIdentifierException | NoSuchFieldError e) {
                throw RError.interopError(RError.findParentRBase(this), e, object);
            }
        } catch (InteropException e) {
            throw RError.interopError(RError.findParentRBase(this), e, object);
        }
    }

    private Object unbox(Object obj, Node isNullNode, Node isBoxedNode, Node unboxNode) throws UnsupportedMessageException {
        if (RRuntime.isForeignObject(obj)) {
            if (ForeignAccess.sendIsNull(isNullNode, (TruffleObject) obj)) {
                return RNull.instance;
            }
            Boolean isBoxed = (Boolean) ForeignAccess.sendIsBoxed(isBoxedNode, (TruffleObject) obj);
            if (isBoxed) {
                return RRuntime.java2R(ForeignAccess.sendUnbox(unboxNode, (TruffleObject) obj));
            }
        }
        return RRuntime.java2R(obj);
    }

    private Object send(Object position, Node foreignRead, Node keyInfoNode, TruffleObject object, FirstStringNode firstString, CastStringNode castNode) throws RError, InteropException {
        if (position instanceof Integer) {
            position = ((int) position) - 1;
        } else if (position instanceof Double) {
            position = ((double) position) - 1;
        } else if (position instanceof RAbstractDoubleVector) {
            position = ((RAbstractDoubleVector) position).getDataAt(0) - 1;
        } else if (position instanceof RAbstractIntVector) {
            position = ((RAbstractIntVector) position).getDataAt(0) - 1;
        } else if (position instanceof RAbstractStringVector) {
            position = firstString.executeString(castNode.doCast(position));
        } else if (!(position instanceof String)) {
            throw RError.error(this, RError.Message.GENERIC, "invalid index during foreign access");
        }

        int info = ForeignAccess.sendKeyInfo(keyInfoNode, object, position);
        if (KeyInfo.isReadable(info)) {
            return ForeignAccess.sendRead(foreignRead, object, position);
        } else if (position instanceof String && !KeyInfo.isExisting(info) && JavaInterop.isJavaObject(Object.class, object)) {
            TruffleObject clazz = JavaInterop.toJavaClass(object);
            info = ForeignAccess.sendKeyInfo(keyInfoNode, clazz, position);
            if (KeyInfo.isReadable(info)) {
                return ForeignAccess.sendRead(foreignRead, clazz, position);
            }
        }
        throw RError.error(this, RError.Message.GENERIC, "invalid index/identifier during foreign access: " + position);
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

    @Specialization(replaces = "doExtractDefaultCached")
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
