/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.AccessForeignObjectNode.WriteElementsNode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode.ExtractSingleName;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.objects.GetS4DataSlot;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RScalarVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Syntax node for element writes.
 */

@ImportStatic({RRuntime.class, com.oracle.truffle.api.interop.Message.class, DSLConfig.class})
public abstract class ReplaceVectorNode extends RBaseNode {

    protected final ElementAccessMode mode;
    private final boolean recursive;
    protected final boolean ignoreRecursive;
    protected final boolean ignoreRefCount;

    @Child private BoxPrimitiveNode boxVector = BoxPrimitiveNode.create();
    @Child private BoxPrimitiveNode boxValue = BoxPrimitiveNode.create();

    ReplaceVectorNode(ElementAccessMode mode, boolean recursive, boolean ignoreRecursive, boolean ignoreRefCount) {
        this.mode = mode;
        this.recursive = recursive;
        this.ignoreRecursive = ignoreRecursive;
        this.ignoreRefCount = ignoreRefCount;
    }

    public final Object apply(Object vector, Object[] positions, Object value) {
        return execute(boxVector.execute(vector), positions, boxValue.execute(value));
    }

    protected abstract Object execute(Object vector, Object[] positions, Object value);

    public static ReplaceVectorNode create(ElementAccessMode mode, boolean ignoreRecursive) {
        return ReplaceVectorNodeGen.create(mode, false, ignoreRecursive, false);
    }

    protected static ReplaceVectorNode createRecursive(ElementAccessMode mode) {
        return ReplaceVectorNodeGen.create(mode, true, false, false);
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

    @Specialization(limit = "getCacheSize(5)", guards = {"!isForeignObject(vector)", "cached != null", "cached.isSupported(vector, positions)"})
    protected Object doRecursive(RAbstractListVector vector, Object[] positions, Object value,  //
                    @Cached("createRecursiveCache(vector, positions)") RecursiveReplaceSubscriptNode cached) {
        return cached.apply(vector, positions, value);
    }

    protected CachedReplaceVectorNode createDefaultCached(RAbstractVector vector, Object[] positions, Object value) {
        if (vector instanceof RAbstractListVector && isRecursiveSubscript(vector, positions)) {
            return null;
        }
        return new CachedReplaceVectorNode(mode, vector, positions, value.getClass(), RRuntime.isForeignObject(value) ? RType.TruffleObject : ((RTypedValue) value).getRType(), true,
                        recursive, CachedReplaceVectorNode.isValueLengthGreaterThanOne(value), ignoreRefCount);
    }

    @Specialization(limit = "getCacheSize(5)", guards = {"!isForeignObject(vector)", "cached != null", "cached.isSupported(vector, positions, value)"})
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
        return ReplaceVectorNodeGen.create(mode, false, false, false);
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
    protected Object doReplacementPairList(RPairList vector, Object[] positions, Object value,
                    @Cached("createForContainerTypes()") ReplaceVectorNode replace,
                    @Cached("createBinaryProfile()") ConditionProfile isLanguage) {
        RList result = vector.toRList();
        result = (RList) replace.execute(result, positions, value);
        // whether the result is list or pairlist depends on mode and the type of the pairlist
        return mode != ElementAccessMode.SUBSET || isLanguage.profile(vector.isLanguage()) ? RPairList.asPairList(result, vector.getType()) : result;
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
            RecursiveReplaceSubscriptNode current = cachedRecursive;
            if (current == null || !current.isSupported(vector, positions)) {
                return cachedRecursive = insert(node.createRecursiveCache(vector, positions));
            }
            return current;
        }

        private CachedReplaceVectorNode get(ReplaceVectorNode node, RAbstractVector vector, Object[] positions, Object value) {
            CompilerAsserts.neverPartOfCompilation();
            CachedReplaceVectorNode current = cached;
            if (current == null || !current.isSupported(vector, positions, value)) {
                return cached = insert(node.createDefaultCached(vector, positions, value));
            }
            return current;
        }
    }

    @Specialization(guards = {"isForeignObject(object)"})
    protected Object accessField(TruffleObject object, Object[] positions, Object value,
                    @Cached("create()") WriteElementsNode writeNode) {
        return writeNode.execute(object, positions, value);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object access(Object object, Object[] positions, Object value) {
        CompilerDirectives.transferToInterpreter();
        throw error(RError.Message.OBJECT_NOT_SUBSETTABLE, Predef.getTypeName(object));
    }
}
