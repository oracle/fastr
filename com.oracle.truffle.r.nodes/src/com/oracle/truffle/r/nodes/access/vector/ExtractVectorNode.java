/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNodeGen.ExtractSingleNameNodeGen;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.objects.GetS4DataSlot;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R.ForeignArrayData;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({RRuntime.class, com.oracle.truffle.api.interop.Message.class})
public abstract class ExtractVectorNode extends RBaseNode {

    protected static final int CACHE_LIMIT = 5;

    protected final ElementAccessMode mode;
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

    public final Object applyAccessField(Object vector, String singlePosition) {
        return apply(vector, new Object[]{singlePosition}, RLogical.valueOf(false), RMissing.instance);
    }

    public final Object apply(Object vector, Object[] positions, Object exact, Object dropDimensions) {
        return execute(boxVector.execute(vector), positions, boxExact.execute(exact), boxDropdimensions.execute(dropDimensions));
    }

    public static ExtractVectorNode create(ElementAccessMode accessMode, boolean ignoreRecursive) {
        return ExtractVectorNodeGen.create(accessMode, false, ignoreRecursive);
    }

    static ExtractVectorNode createRecursive(ElementAccessMode accessMode) {
        return ExtractVectorNodeGen.create(accessMode, true, false);
    }

    protected abstract Object execute(Object vector, Object[] positions, Object exact, Object dropDimensions);

    @Specialization(guards = {"cached != null", "cached.isSupported(vector, positions)"}, limit = "3")
    protected Object doRecursive(RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("createRecursiveCache(vector, positions)") RecursiveExtractSubscriptNode cached) {
        return cached.apply(vector, positions, exact, dropDimensions);
    }

    protected RecursiveExtractSubscriptNode createRecursiveCache(Object x, Object[] positions) {
        if (isRecursiveSubscript(x, positions)) {
            return RecursiveExtractSubscriptNode.create((RAbstractContainer) x, positions[0]);
        }
        return null;
    }

    protected static boolean isForeignObject(Object object) {
        return RRuntime.isForeignObject(object);
    }

    protected static FirstStringNode createFirstString() {
        return FirstStringNode.createWithError(RError.Message.GENERIC, "Cannot coerce position to character for foreign access.");
    }

    protected boolean positionsByVector(Object[] positions) {
        return positions.length == 1 && positions[0] instanceof RAbstractVector && ((RAbstractVector) positions[0]).getLength() > 1;
    }

    private boolean isRecursiveSubscript(Object vector, Object[] positions) {
        return !recursive && !ignoreRecursive && mode.isSubscript() && (vector instanceof RAbstractListVector || vector instanceof RPairList) && positions.length == 1;
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"!isForeignObject(vector)", "cached != null", "cached.isSupported(vector, positions, exact, dropDimensions)"})
    protected Object doExtractDefaultCached(RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("createDefaultCache(getThis(), vector, positions, exact, dropDimensions)") CachedExtractVectorNode cached) {
        assert !isRecursiveSubscript(vector, positions);
        return cached.apply(vector, positions, null, exact, dropDimensions);
    }

    protected static CachedExtractVectorNode createDefaultCache(ExtractVectorNode node, RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions) {
        assert !(vector instanceof REnvironment);
        return new CachedExtractVectorNode(node.getMode(), vector, positions, (RTypedValue) exact, (RTypedValue) dropDimensions, node.recursive);
    }

    @Specialization(replaces = {"doExtractDefaultCached", "doRecursive"}, guards = {"!isForeignObject(vector)"})
    @TruffleBoundary
    protected Object doExtractDefaultGeneric(RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("create()") GenericVectorExtractNode generic) {
        if (isRecursiveSubscript(vector, positions)) {
            return generic.getRecursive(vector, positions).apply(vector, positions, exact, dropDimensions);
        } else {
            return generic.get(this, vector, positions, exact, dropDimensions).apply(vector, positions, null, exact, dropDimensions);
        }
    }

    @Specialization
    protected Object doExtractEnvironment(REnvironment env, Object[] positions, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") Object dropDimensions,
                    @Cached("createExtractName()") ExtractSingleName extractName,
                    @Cached("new()") PromiseCheckHelperNode promiseHelper) {
        if (mode.isSubset()) {
            throw error(RError.Message.OBJECT_NOT_SUBSETTABLE, RType.Environment.getName());
        }
        String name = positions.length == 1 ? extractName.execute(positions[0]) : null;
        if (name != null) {
            Object obj = env.get(name);
            return obj == null ? RNull.instance : promiseHelper.checkEvaluate(null, obj);
        }
        throw error(RError.Message.WRONG_ARGS_SUBSET_ENV);
    }

    @Specialization
    protected Object doExtractS4Object(RS4Object obj, Object[] positions, Object exact, Object dropDimensions,
                    @Cached("createEnvironment()") GetS4DataSlot getS4DataSlotNode,
                    @Cached("create(mode, True)") ExtractVectorNode recursiveExtract) {
        RTypedValue dataSlot = getS4DataSlotNode.executeObject(obj);
        if (dataSlot == RNull.instance) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.OP_NOT_DEFINED_FOR_S4_CLASS, "$");
        }
        return recursiveExtract.execute(dataSlot, positions, exact, dropDimensions);
    }

    abstract static class ExtractSingleName extends Node {

        public abstract String execute(Object value);

        public static ExtractSingleName createExtractName() {
            return ExtractSingleNameNodeGen.create();
        }

        @Specialization
        protected static String extract(String value) {
            return value;
        }

        @Specialization(guards = "access.supports(value)")
        protected static String extractCached(RAbstractStringVector value,
                        @Cached("value.access()") VectorAccess access) {
            try (RandomIterator iter = access.randomAccess(value)) {
                if (access.getLength(iter) == 1) {
                    return access.getString(iter, 0);
                }
            }
            return null;
        }

        @Specialization(replaces = "extractCached")
        @TruffleBoundary
        protected static String extractGeneric(RAbstractStringVector value) {
            return extractCached(value, value.slowPathAccess());
        }

        @Fallback
        protected static String extractFallback(@SuppressWarnings("unused") Object value) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doExtractRNull(RNull vector, Object[] positions, Object exact, Object dropDimensions) {
        return RNull.instance;
    }

    // TODO hack until Truffle-DSL supports this.
    protected ExtractVectorNode getThis() {
        return this;
    }

    protected static final class GenericVectorExtractNode extends TruffleBoundaryNode {

        @Child private CachedExtractVectorNode cached;
        @Child private RecursiveExtractSubscriptNode cachedRecursive;

        public static GenericVectorExtractNode create() {
            return new GenericVectorExtractNode();
        }

        public RecursiveExtractSubscriptNode getRecursive(RAbstractContainer vector, Object[] positions) {
            CompilerAsserts.neverPartOfCompilation();
            if (cachedRecursive == null || !cachedRecursive.isSupported(vector, positions)) {
                cachedRecursive = insert(RecursiveExtractSubscriptNode.create(vector, positions[0]));
            }
            return cachedRecursive;
        }

        public CachedExtractVectorNode get(ExtractVectorNode node, RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions) {
            CompilerAsserts.neverPartOfCompilation();
            if (cached == null || !cached.isSupported(vector, positions, exact, dropDimensions)) {
                cached = insert(createDefaultCache(node, vector, positions, exact, dropDimensions));
            }
            return cached;
        }
    }

    @Specialization(guards = {"isForeignObject(object)", "positionsByVector(positions)"})
    protected Object accessFieldByVectorPositions(TruffleObject object, Object[] positions, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") Object dropDimensions,
                    @Cached("createReadElement()") ReadElementNode readElement,
                    @Cached("IS_NULL.createNode()") Node isNullNode,
                    @Cached("IS_BOXED.createNode()") Node isBoxedNode,
                    @Cached("UNBOX.createNode()") Node unboxNode,
                    @Cached("create()") Foreign2R foreign2RNode) {

        RAbstractVector vec = (RAbstractVector) positions[0];
        ForeignArrayData arrayData = new ForeignArrayData();

        try {
            for (int i = 0; i < vec.getLength(); i++) {
                Object res = readElement.execute(vec.getDataAtAsObject(i), object);
                arrayData.add(res, () -> isNullNode, () -> isBoxedNode, () -> unboxNode, () -> foreign2RNode);
            }
            return ForeignArray2R.asAbstractVector(arrayData);
        } catch (InteropException e) {
            CompilerDirectives.transferToInterpreter();
            throw RError.interopError(RError.findParentRBase(this), e, object);
        }
    }

    @Specialization(guards = {"isForeignObject(object)", "!positionsByVector(positions)"})
    protected Object accessField(TruffleObject object, Object[] positions, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") Object dropDimensions,
                    @Cached("createReadElement()") ReadElementNode readElement,
                    @Cached("createClassProfile()") ValueProfile positionProfile,
                    @Cached("create()") VectorLengthProfile lengthProfile,
                    @Cached("create()") Foreign2R foreign2RNode) {
        Object[] pos = positionProfile.profile(positions);
        if (pos.length == 0) {
            throw error(RError.Message.GENERIC, "No positions for foreign access.");
        }
        try {
            Object result = object;
            for (int i = 0; i < lengthProfile.profile(pos.length); i++) {
                result = readElement.execute(pos[i], (TruffleObject) result);
                assert !(pos.length > 1 && i < pos.length - 1) || result instanceof TruffleObject;
            }
            return foreign2RNode.execute(result);
        } catch (InteropException | NoSuchFieldError e) {
            CompilerDirectives.transferToInterpreter();
            throw RError.interopError(RError.findParentRBase(this), e, object);
        }
    }

    static ReadElementNode createReadElement() {
        return new ReadElementNode();
    }

    abstract static class AccessElementNode extends RBaseNode {

        @Child private Node hasSizeNode;
        @Child private CastStringNode castNode;
        @Child private FirstStringNode firstString;

        private final ConditionProfile isIntProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isDoubleProfile = ConditionProfile.createBinaryProfile();

        protected final Object extractPosition(Object position) {
            Object pos = position;
            if (isIntProfile.profile(pos instanceof Integer)) {
                pos = ((int) pos) - 1;
            } else if (isDoubleProfile.profile(pos instanceof Double)) {
                pos = ((double) pos) - 1;
            } else if (pos instanceof RAbstractDoubleVector) {
                RAbstractDoubleVector vector = (RAbstractDoubleVector) pos;
                if (vector.getLength() == 0) {
                    throw error(RError.Message.GENERIC, "invalid index during foreign access");
                }
                pos = vector.getDataAt(0) - 1;
            } else if (pos instanceof RAbstractIntVector) {
                RAbstractIntVector vector = (RAbstractIntVector) pos;
                if (vector.getLength() == 0) {
                    throw error(RError.Message.GENERIC, "invalid index during foreign access");
                }
                pos = vector.getDataAt(0) - 1;
            } else if (pos instanceof RAbstractStringVector) {
                if (castNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    castNode = insert(CastStringNode.create());
                }
                if (firstString == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    firstString = insert(createFirstString());
                }
                pos = firstString.executeString(castNode.doCast(pos));
            } else if (!(pos instanceof String)) {
                throw error(RError.Message.GENERIC, "invalid index during foreign access");
            }
            return pos;
        }

        protected final boolean hasSize(TruffleObject object) {
            if (hasSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasSizeNode = insert(com.oracle.truffle.api.interop.Message.HAS_SIZE.createNode());
            }
            return ForeignAccess.sendHasSize(hasSizeNode, object);
        }
    }

    static final class ReadElementNode extends AccessElementNode {

        @Child private Node foreignRead = com.oracle.truffle.api.interop.Message.READ.createNode();
        @Child private Node classForeignRead;
        @Child private Node keyInfoNode;

        public Object execute(Object position, TruffleObject object) throws InteropException {
            Object pos = extractPosition(position);
            if (keyInfoNode == null) {
                try {
                    return ForeignAccess.sendRead(foreignRead, object, pos);
                } catch (InteropException e) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    keyInfoNode = insert(com.oracle.truffle.api.interop.Message.KEY_INFO.createNode());
                }
            }
            int info = ForeignAccess.sendKeyInfo(keyInfoNode, object, pos);
            if (KeyInfo.isReadable(info) || hasSize(object)) {
                return ForeignAccess.sendRead(foreignRead, object, pos);
            } else if (pos instanceof String && !KeyInfo.isExisting(info) && JavaInterop.isJavaObject(Object.class, object)) {
                if (classForeignRead == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    classForeignRead = insert(com.oracle.truffle.api.interop.Message.READ.createNode());
                }
                return ForeignAccess.sendRead(classForeignRead, toJavaClass(object), pos);
            }
            CompilerDirectives.transferToInterpreter();
            throw error(RError.Message.GENERIC, "invalid index/identifier during foreign access: " + pos);
        }
    }

    @TruffleBoundary
    private static TruffleObject toJavaClass(TruffleObject obj) {
        return JavaInterop.toJavaClass(obj);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object access(Object object, Object[] positions, Object exact, Object dropDimensions) {
        CompilerDirectives.transferToInterpreter();
        throw error(RError.Message.OBJECT_NOT_SUBSETTABLE, Predef.getTypeName(object));
    }
}
