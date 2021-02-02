/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.access.vector.AccessForeignObjectNode.ReadPositionsNode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNodeGen.ExtractSingleNameNodeGen;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.objects.GetS4DataSlot;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({RRuntime.class, DSLConfig.class})
public abstract class ExtractVectorNode extends RBaseNode {

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
        return apply(vector, new Object[]{singlePosition}, RDataFactory.createLogicalVectorFromScalar(false), RMissing.instance);
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

    // Note: the factory that creates cached decides whether given positions are recursive and
    // returns null if not. RAbstractContainer is not foreign by definition.
    @Specialization(guards = {"cached != null", "cached.isSupported(vector, positions)"}, limit = "getCacheSize(3)")
    protected Object doRecursive(RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("createRecursiveCacheOrNull(vector, positions)") RecursiveExtractSubscriptNode cached) {
        assert isRecursiveSubscript(vector, positions);
        return cached.apply(vector, positions, exact, dropDimensions);
    }

    protected RecursiveExtractSubscriptNode createRecursiveCacheOrNull(Object x, Object[] positions) {
        if (isRecursiveSubscript(x, positions)) {
            return RecursiveExtractSubscriptNode.create((RAbstractContainer) x, positions[0]);
        }
        return null;
    }

    protected static boolean isForeignObject(Object object) {
        return RRuntime.isForeignObject(object);
    }

    private boolean isRecursiveSubscript(Object vector, Object[] positions) {
        return !recursive && !ignoreRecursive && mode.isSubscript() && (vector instanceof RAbstractListVector || vector instanceof RPairList) && positions.length == 1;
    }

    // Note: the factory that creates cached decides whether given positions are not recursive and
    // returns null they are. RAbstractContainer is not foreign by definition.
    @Specialization(limit = "getCacheSize(5)", guards = {"cached != null", "cached.isSupported(vector, positions, exact, dropDimensions)"})
    protected Object doExtractDefaultCached(RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions,  //
                    @Cached("createDefaultCacheOrNull(getThis(), vector, positions, exact, dropDimensions)") CachedExtractVectorNode cached) {
        assert !isRecursiveSubscript(vector, positions);
        return cached.apply(vector, positions, null, exact, dropDimensions);
    }

    protected CachedExtractVectorNode createDefaultCacheOrNull(ExtractVectorNode node, RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions) {
        if (!isRecursiveSubscript(vector, positions)) {
            return createDefaultCache(node, vector, positions, exact, dropDimensions);
        }
        return null;
    }

    protected static CachedExtractVectorNode createDefaultCache(ExtractVectorNode node, RAbstractContainer vector, Object[] positions, Object exact, Object dropDimensions) {
        return new CachedExtractVectorNode(node.getMode(), vector, positions, (RBaseObject) exact, (RBaseObject) dropDimensions, node.recursive);
    }

    // Note: RAbstractContainer is not foreign by definition.
    @Specialization(replaces = {"doExtractDefaultCached", "doRecursive"})
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
            return obj == null ? RNull.instance : promiseHelper.checkVisibleEvaluate(null, obj);
        }
        throw error(RError.Message.WRONG_ARGS_SUBSET_ENV);
    }

    @Specialization
    protected Object doExtractS4Object(RS4Object obj, Object[] positions, Object exact, Object dropDimensions,
                    @Cached("createEnvironment()") GetS4DataSlot getS4DataSlotNode,
                    @Cached("create(mode, True)") ExtractVectorNode recursiveExtract) {
        RBaseObject dataSlot = getS4DataSlotNode.executeObject(obj);
        if (dataSlot == RNull.instance) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.OP_NOT_DEFINED_FOR_S4_CLASS, "$");
        }
        return recursiveExtract.execute(dataSlot, positions, exact, dropDimensions);
    }

    @ImportStatic(DSLConfig.class)
    abstract static class ExtractSingleName extends Node {

        public abstract String execute(Object value);

        public static ExtractSingleName createExtractName() {
            return ExtractSingleNameNodeGen.create();
        }

        @Specialization
        protected static String extract(String value) {
            return value;
        }

        @Specialization(limit = "getVectorAccessCacheSize()")
        protected static String extractCached(RStringVector value,
                        @CachedLibrary("value.getData()") VectorDataLibrary lib) {
            Object data = value.getData();
            if (lib.getLength(data) == 1) {
                return lib.getStringAt(data, 0);
            }
            return null;
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

    @Specialization(guards = {"isForeignObject(object)"})
    protected Object accessFieldByVectorPositions(TruffleObject object, Object[] positions, @SuppressWarnings("unused") Object exact, @SuppressWarnings("unused") Object dropDimensions,
                    @Cached("create()") ReadPositionsNode readElements) {
        return readElements.execute(object, positions);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object access(Object object, Object[] positions, Object exact, Object dropDimensions) {
        CompilerDirectives.transferToInterpreter();
        throw error(RError.Message.OBJECT_NOT_SUBSETTABLE, Predef.getTypeName(object));
    }
}
