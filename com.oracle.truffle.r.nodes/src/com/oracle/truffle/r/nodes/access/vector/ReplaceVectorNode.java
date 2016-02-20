/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Syntax node for element writes.
 */
public abstract class ReplaceVectorNode extends Node {

    protected static final int CACHE_LIMIT = 5;

    private final ElementAccessMode mode;
    private final boolean recursive;
    private final boolean ignoreRecursive;

    @Child private BoxPrimitiveNode boxVector = BoxPrimitiveNode.create();
    @Child private BoxPrimitiveNode boxValue = BoxPrimitiveNode.create();

    public ReplaceVectorNode(ElementAccessMode mode, boolean recursive, boolean ignoreRecursive) {
        this.mode = mode;
        this.recursive = recursive;
        this.ignoreRecursive = ignoreRecursive;
    }

    public final Object apply(VirtualFrame frame, Object vector, Object[] positions, Object value) {
        return execute(frame, boxVector.execute(vector), positions, boxValue.execute(value));
    }

    protected abstract Object execute(VirtualFrame frame, Object vector, Object[] positions, Object value);

    public static ReplaceVectorNode create(ElementAccessMode mode, boolean ignoreRecursive) {
        return ReplaceVectorNodeGen.create(mode, false, ignoreRecursive);
    }

    static ReplaceVectorNode createRecursive(ElementAccessMode mode) {
        return ReplaceVectorNodeGen.create(mode, true, false);
    }

    @SuppressWarnings("unchecked")
    protected Class<? extends RTypedValue> getInvalidVectorType(Object vector) {
        if (vector instanceof REnvironment || vector instanceof RFunction) {
            return (Class<? extends RTypedValue>) vector.getClass();
        }
        return null;
    }

    protected Node createForeignWrite(Object[] positions) {
        if (positions.length != 1) {
            throw RError.error(this, RError.Message.GENERIC, "Invalid number positions for foreign access.");
        }
        return Message.WRITE.createNode();
    }

    protected static boolean isForeignObject(TruffleObject object) {
        return RRuntime.isForeignObject(object);
    }

    protected FirstStringNode createFirstString() {
        return FirstStringNode.createWithError(RError.Message.GENERIC, "Cannot corce position to character for foreign access.");
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isForeignObject(object)", "positions.length == cachedLength"})
    protected Object accessField(VirtualFrame frame, TruffleObject object, Object[] positions, Object value, //
                    @Cached("createForeignWrite(positions)") Node foreignRead, //
                    @Cached("positions.length") int cachedLength, //
                    @Cached("create()") CastStringNode castNode, @Cached("createFirstString()") FirstStringNode firstString) {

        Object writtenValue = value;
        if (writtenValue instanceof RInteger) {
            writtenValue = ((RInteger) writtenValue).getValue();
        } else if (writtenValue instanceof RDouble) {
            writtenValue = ((RDouble) writtenValue).getValue();
        }
        Object position = positions[0];
        try {
            if (position instanceof String || position instanceof Double || position instanceof Integer) {
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{position, writtenValue});
            } else if (position instanceof RAbstractStringVector) {
                String string = firstString.executeString(castNode.execute(position));
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{string, writtenValue});
            } else if (position instanceof RAbstractDoubleVector) {
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{((RAbstractDoubleVector) position).getDataAt(0), writtenValue});
            } else if (position instanceof RAbstractIntVector) {
                return ForeignAccess.send(foreignRead, frame, object, new Object[]{((RAbstractIntVector) position).getDataAt(0), writtenValue});
            } else {
                throw RError.error(this, RError.Message.GENERIC, "invalid index during foreign access");
            }
        } catch (InteropException e) {
            throw RError.interopError(RError.findParentRBase(this), e);
        }
    }

    @Specialization(limit = "CACHE_LIMIT", guards = {"cached != null", "cached.isSupported(vector, positions)"})
    protected Object doRecursive(VirtualFrame frame, RAbstractListVector vector, Object[] positions, Object value,  //
                    @Cached("createRecursiveCache(vector, positions)") RecursiveReplaceSubscriptNode cached) {
        return cached.apply(frame, vector, positions, value);
    }

    protected RecursiveReplaceSubscriptNode createRecursiveCache(Object vector, Object[] positions) {
        if (isRecursiveSubscript(vector, positions)) {
            return RecursiveReplaceSubscriptNode.create((RAbstractListVector) vector, positions[0]);
        }
        return null;
    }

    private boolean isRecursiveSubscript(Object vector, Object[] positions) {
        return !recursive && !ignoreRecursive && mode.isSubscript() && vector instanceof RAbstractListVector && positions.length == 1;
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
