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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode.ExtractSingleName;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.objects.GetS4DataSlot;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RLanguage.RepType;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Syntax node for element writes.
 */
@ImportStatic({RRuntime.class, com.oracle.truffle.api.interop.Message.class})
public abstract class ReplaceVectorNode extends RBaseNode {

    protected static final int CACHE_LIMIT = 5;

    protected final ElementAccessMode mode;
    private final boolean recursive;
    protected final boolean ignoreRecursive;

    @Child private BoxPrimitiveNode boxVector = BoxPrimitiveNode.create();
    @Child private BoxPrimitiveNode boxValue = BoxPrimitiveNode.create();

    ReplaceVectorNode(ElementAccessMode mode, boolean recursive, boolean ignoreRecursive) {
        this.mode = mode;
        this.recursive = recursive;
        this.ignoreRecursive = ignoreRecursive;
    }

    public final Object apply(Object vector, Object[] positions, Object value) {
        return execute(boxVector.execute(vector), positions, boxValue.execute(value));
    }

    protected abstract Object execute(Object vector, Object[] positions, Object value);

    public static ReplaceVectorNode create(ElementAccessMode mode, boolean ignoreRecursive) {
        return ReplaceVectorNodeGen.create(mode, false, ignoreRecursive);
    }

    protected static ReplaceVectorNode createRecursive(ElementAccessMode mode) {
        return ReplaceVectorNodeGen.create(mode, true, false);
    }

    private boolean isRecursiveSubscript(Object vector, Object[] positions) {
        return !recursive && !ignoreRecursive && mode.isSubscript() && vector instanceof RAbstractListVector && positions.length == 1;
    }

    protected RecursiveReplaceSubscriptNode createRecursiveCache(Object vector, Object[] positions) {
        if (isRecursiveSubscript(vector, positions)) {
            return RecursiveReplaceSubscriptNode.create((RAbstractListVector) vector, positions[0]);
        }
        return null;
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"!isForeignObject(vector)", "cached != null", "cached.isSupported(vector, positions)"})
    protected Object doRecursive(RAbstractListVector vector, Object[] positions, Object value,  //
                    @Cached("createRecursiveCache(vector, positions)") RecursiveReplaceSubscriptNode cached) {
        return cached.apply(vector, positions, value);
    }

    protected CachedReplaceVectorNode createDefaultCached(RAbstractVector vector, Object[] positions, Object value) {
        if (vector instanceof RAbstractListVector && isRecursiveSubscript(vector, positions)) {
            return null;
        }
        return new CachedReplaceVectorNode(mode, vector, positions, value.getClass(), RRuntime.isForeignObject(value) ? RType.TruffleObject : ((RTypedValue) value).getRType(), true,
                        recursive, CachedReplaceVectorNode.isValueLengthGreaterThanOne(value));
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"!isForeignObject(vector)", "cached != null", "cached.isSupported(vector, positions, value)"})
    protected Object doReplaceCached(RAbstractVector vector, Object[] positions, Object value,  //
                    @Cached("createDefaultCached(vector, positions, value)") CachedReplaceVectorNode cached) {
        assert !isRecursiveSubscript(vector, positions);
        return cached.apply(vector, positions, value);
    }

    @Specialization
    @TruffleBoundary
    protected Object doReplaceEnvironment(REnvironment env, Object[] positions, Object value,
                    @Cached("createExtractName()") ExtractSingleName extractName) {
        if (mode.isSubset()) {
            throw error(RError.Message.OBJECT_NOT_SUBSETTABLE, RType.Environment.getName());
        }
        String name = positions.length == 1 ? extractName.execute(positions[0]) : null;
        if (name != null) {
            try {
                env.put(name, value instanceof RScalarVector ? ((RScalarVector) value).getDataAtAsObject(0) : value);
            } catch (PutException ex) {
                throw error(ex);
            }
        }
        return env;
    }

    @Specialization
    @TruffleBoundary
    protected Object doReplaceS4Object(RS4Object obj, Object[] positions, Object value,
                    @Cached("createEnvironment()") GetS4DataSlot getS4DataSlotNode,
                    @Cached("create(mode, ignoreRecursive)") ReplaceVectorNode recursiveReplace) {
        RTypedValue dataSlot = getS4DataSlotNode.executeObject(obj);
        if (dataSlot == RNull.instance) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.NO_METHOD_ASSIGNING_SUBSET_S4);
        }
        // No need to update the data slot, the value is env and they have reference semantics.
        recursiveReplace.execute(dataSlot, positions, value);
        return obj;
    }

    protected ReplaceVectorNode createForContainerTypes() {
        return ReplaceVectorNodeGen.create(mode, false, false);
    }

    @Specialization
    protected Object doReplacementNull(@SuppressWarnings("unused") RNull vector, Object[] positions, Object value,
                    @Cached("createForContainerTypes()") ReplaceVectorNode replace) {
        /*
         * Replacing inside a variable containing NULL is quite inconsistent, we try to emulate the
         * behavior as good as possible.
         */
        if (value == RNull.instance) {
            return RNull.instance;
        }
        RType type;
        switch (mode) {
            case FIELD_SUBSCRIPT:
                type = RType.List;
                break;
            case SUBSCRIPT:
                if (value instanceof RAbstractAtomicVector && ((RAbstractAtomicVector) value).getLength() == 1) {
                    type = ((RAbstractAtomicVector) value).getRType();
                } else {
                    type = RType.List;
                }
                break;
            case SUBSET:
                if (value instanceof RAbstractAtomicVector) {
                    if (((RAbstractAtomicVector) value).getLength() == 0) {
                        return RNull.instance;
                    } else {
                        type = ((RAbstractAtomicVector) value).getRType();
                    }
                } else {
                    type = RType.List;
                }
                break;
            default:
                throw RInternalError.shouldNotReachHere();
        }
        return replace.execute(type.getEmpty(), positions, value);
    }

    @Specialization
    @TruffleBoundary
    protected Object doReplacementLanguage(RLanguage vector, Object[] positions, Object value,
                    @Cached("createForContainerTypes()") ReplaceVectorNode replace) {
        RepType repType = RContext.getRRuntimeASTAccess().getRepType(vector);
        RList result = RContext.getRRuntimeASTAccess().asList(vector);
        DynamicObject attrs = vector.getAttributes();
        if (attrs != null && !attrs.isEmpty()) {
            result.initAttributes(RAttributesLayout.copy(attrs));
        }
        result = (RList) replace.execute(result, positions, value);
        return RContext.getRRuntimeASTAccess().createLanguageFromList(result, repType);
    }

    @Specialization
    @TruffleBoundary
    protected Object doReplacementPairList(RPairList vector, Object[] positions, Object value,
                    @Cached("createForContainerTypes()") ReplaceVectorNode replace) {
        return replace.execute(vector.toRList(), positions, value);
    }

    protected static GenericVectorReplaceNode createGeneric() {
        return new GenericVectorReplaceNode();
    }

    @Specialization(replaces = {"doReplaceCached", "doRecursive"}, guards = "!isForeignObject(vector)")
    @TruffleBoundary
    protected Object doReplaceDefaultGeneric(RAbstractVector vector, Object[] positions, Object value,  //
                    @Cached("createGeneric()") GenericVectorReplaceNode generic) {
        if (vector instanceof RAbstractListVector && isRecursiveSubscript(vector, positions)) {
            return generic.getRecursive(this, vector, positions).apply(vector, positions, value);
        } else {
            return generic.get(this, vector, positions, value).apply(vector, positions, value);
        }
    }

    protected static final class GenericVectorReplaceNode extends TruffleBoundaryNode {

        @Child private RecursiveReplaceSubscriptNode cachedRecursive;
        @Child private CachedReplaceVectorNode cached;

        private RecursiveReplaceSubscriptNode getRecursive(ReplaceVectorNode node, Object vector, Object[] positions) {
            CompilerAsserts.neverPartOfCompilation();
            if (cachedRecursive == null || !cachedRecursive.isSupported(vector, positions)) {
                cachedRecursive = insert(node.createRecursiveCache(vector, positions));
            }
            return cachedRecursive;
        }

        private CachedReplaceVectorNode get(ReplaceVectorNode node, RAbstractVector vector, Object[] positions, Object value) {
            CompilerAsserts.neverPartOfCompilation();
            if (cached == null || !cached.isSupported(vector, positions, value)) {
                cached = insert(node.createDefaultCached(vector, positions, value));
            }
            return cached;
        }
    }

    protected FirstStringNode createFirstString() {
        return FirstStringNode.createWithError(RError.Message.GENERIC, "Cannot corce position to character for foreign access.");
    }

    @Specialization(guards = {"isForeignObject(object)", "positions.length == cachedLength"})
    protected Object accessField(TruffleObject object, Object[] positions, Object value,
                    @Cached("WRITE.createNode()") Node foreignWrite,
                    @Cached("READ.createNode()") Node foreignRead,
                    @Cached("KEY_INFO.createNode()") Node keyInfoNode,
                    @Cached("HAS_SIZE.createNode()") Node hasSizeNode,
                    @SuppressWarnings("unused") @Cached("positions.length") int cachedLength,
                    @Cached("create()") CastStringNode castNode,
                    @Cached("createFirstString()") FirstStringNode firstString,
                    @Cached("create()") R2Foreign r2Foreign) {
        Object writtenValue = value;
        try {
            TruffleObject result = object;
            for (int i = 0; i < positions.length - 1; i++) {
                result = (TruffleObject) ExtractVectorNode.read(this, positions[i], foreignRead, keyInfoNode, hasSizeNode, result, firstString, castNode);
            }
            write(positions[positions.length - 1], foreignWrite, keyInfoNode, hasSizeNode, result, writtenValue, firstString, castNode, r2Foreign);
            return object;
        } catch (InteropException e) {
            throw RError.interopError(RError.findParentRBase(this), e, object);
        }
    }

    @Specialization(guards = {"isForeignObject(object)"}, replaces = "accessField")
    protected Object accessFieldGeneric(TruffleObject object, Object[] positions, Object value,
                    @Cached("WRITE.createNode()") Node foreignWrite,
                    @Cached("READ.createNode()") Node foreignRead,
                    @Cached("KEY_INFO.createNode()") Node keyInfoNode,
                    @Cached("HAS_SIZE.createNode()") Node hasSizeNode,
                    @Cached("create()") CastStringNode castNode,
                    @Cached("createFirstString()") FirstStringNode firstString,
                    @Cached("create()") R2Foreign r2Foreign) {
        return accessField(object, positions, value, foreignWrite, foreignRead, keyInfoNode, hasSizeNode, positions.length, castNode, firstString, r2Foreign);
    }

    private void write(Object position, Node foreignWrite, Node keyInfoNode, Node hasSizeNode, TruffleObject object, Object writtenValue, FirstStringNode firstString, CastStringNode castNode,
                    R2Foreign r2Foreign)
                    throws InteropException, RError {
        Object pos = position;
        if (pos instanceof Integer) {
            pos = ((Integer) pos) - 1;
        } else if (pos instanceof Double) {
            pos = ((Double) pos) - 1;
        } else if (pos instanceof RAbstractDoubleVector) {
            pos = ((RAbstractDoubleVector) pos).getDataAt(0) - 1;
        } else if (pos instanceof RAbstractIntVector) {
            pos = ((RAbstractIntVector) pos).getDataAt(0) - 1;
        } else if (pos instanceof RAbstractStringVector) {
            String string = firstString.executeString(castNode.doCast(pos));
            pos = string;
        } else if (!(pos instanceof String)) {
            throw error(RError.Message.GENERIC, "invalid index during foreign access");
        }

        int info = ForeignAccess.sendKeyInfo(keyInfoNode, object, pos);
        if (KeyInfo.isWritable(info) || ForeignAccess.sendHasSize(hasSizeNode, object) ||
                        (pos instanceof String && !JavaInterop.isJavaObject(Object.class, object))) {
            ForeignAccess.sendWrite(foreignWrite, object, pos, r2Foreign.execute(writtenValue));
            return;
        } else if (pos instanceof String && !KeyInfo.isExisting(info) && JavaInterop.isJavaObject(Object.class, object)) {
            TruffleObject clazz = toJavaClass(object);
            info = ForeignAccess.sendKeyInfo(keyInfoNode, clazz, pos);
            if (KeyInfo.isWritable(info)) {
                ForeignAccess.sendWrite(foreignWrite, clazz, pos, r2Foreign.execute(writtenValue));
                return;
            }
        }
        throw error(RError.Message.GENERIC, "invalid index/identifier during foreign access: " + pos);
    }

    @TruffleBoundary
    private static TruffleObject toJavaClass(TruffleObject obj) {
        return JavaInterop.toJavaClass(obj);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object access(Object object, Object[] positions, Object value) {
        CompilerDirectives.transferToInterpreter();
        throw error(RError.Message.OBJECT_NOT_SUBSETTABLE, Predef.typeName().apply(object));
    }
}
