/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Node that should be used whenever you want to alter some vector: if the vector is shared, then it
 * creates a copy, otherwise it returns the vector itself. It does not increment the reference count
 * of the result in either case, but that is typically handled by write variable node, put container
 * element node or by put attribute node.
 *
 * This node can be configured to copy all non-temporary vectors, i.e. only temporary vectors will
 * be reused, or to copy all shared vectors, i.e. non-shared and temporary vectors will be reused.
 * 
 * Reusing non-shared vectors is only correct in builtins that have replacement form, e.g.
 * {@code names<-}. Builtins are normally not allowed to modify their arguments (unless temporary),
 * but this is a hack also used in GNU-R that avoids creating a copy in {@code names(a) <- val},
 * which get rewritten to {@code a <- `names<-`(a, val)}.
 *
 * This node specializes its instances for the input vector type, but also for the input vector
 * reference count creating separate specializations for the cases where copying is necessary and
 * where the input vector can be reused.
 */
public final class VectorReuse extends Node {

    @Child private VectorAccess access;

    private final boolean isShareableClass;
    private final boolean isTempOrNonShared; // is this instance specialized to no-copying (true) or
                                             // copying the input
    private final boolean needsTemporary; // can we reuse only temporary vectors or also non-shared
    protected final boolean isGeneric;
    protected final Class<? extends RAbstractContainer> clazz;
    @CompilationFinal private ValueProfile copiedValueProfile;
    @Child private AbstractContainerLibrary containerLib;
    @Child private CopyWithAttributes copyWithAttributesNode;
    @Child private AbstractContainerLibrary copyResultContainerLib;

    public VectorReuse(RAbstractContainer vector, boolean needsTemporary, boolean isGeneric) {
        this.isShareableClass = isGeneric ? false : RSharingAttributeStorage.isShareable(vector);
        this.clazz = isGeneric ? null : vector.getClass();
        this.needsTemporary = needsTemporary;
        this.isGeneric = isGeneric;
        this.isTempOrNonShared = isShareableClass && isTempOrNonShared(vector);
        this.containerLib = isGeneric ? AbstractContainerLibrary.getFactory().getUncached() : AbstractContainerLibrary.getFactory().create(vector);
    }

    protected RAbstractContainer cast(RAbstractContainer value) {
        return clazz.cast(value);
    }

    // TODO: this access is used to write into the result, but there is no guarantee that the result
    // is materialized, i.e. writeable, vector
    public VectorAccess access(RAbstractVector result) {
        return isGeneric ? result.slowPathAccess() : access;
    }

    public boolean supports(RAbstractContainer value) {
        assert !isGeneric : "cannot call 'supports' on generic vector reuse";
        if (value.getClass() != clazz) {
            return false;
        }
        if (access == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            access = insert(isTempOrNonShared ? value.access() : VectorAccess.createNew(value.getRType()));
        }
        if (!isShareableClass) {
            return true;
        }
        if (isTempOrNonShared && !access.supports(value)) {
            return false;
        }
        if (!containerLib.accepts(value)) {
            return false;
        }
        RSharingAttributeStorage vector = cast(value);
        return needsTemporary ? vector.isTemporary() == isTempOrNonShared : !vector.isShared() == isTempOrNonShared;
    }

    @TruffleBoundary
    private static RAbstractContainer copyVectorSlowPath(RAbstractContainer vector) {
        return vector.copy();
    }

    @TruffleBoundary
    private static RAbstractContainer materializeSlowPath(RAbstractContainer vector) {
        return vector.materialize();
    }

    private boolean isTempOrNonShared(RAbstractContainer vector) {
        return needsTemporary ? vector.isTemporary() : !vector.isShared();
    }

    @SuppressWarnings("unchecked")
    public <T extends RAbstractContainer> T getResult(T vector) {
        RAbstractContainer result;
        if (isGeneric) {
            if (RSharingAttributeStorage.isShareable(vector) && isTempOrNonShared(vector)) {
                result = vector;
            } else {
                result = copyVectorSlowPath(vector);
            }
        } else {
            if (!isShareableClass || !isTempOrNonShared) {
                result = copyWithAttributes(cast(vector));
            } else {
                result = cast(vector);
            }
        }
        return (T) result;
    }

    private <T extends RAbstractContainer> RAbstractContainer reuse(T vector) {
        RAbstractContainer result = vector;
        if (!isTempOrNonShared || (isGeneric && isTempOrNonShared(vector))) {
            result = copyWithAttributes(vector);
        }
        return result;
    }

    /**
     * Should be used in cases where the data of the resulting vector will be written.
     */
    @SuppressWarnings("unchecked")
    public <T extends RAbstractContainer> T getMaterializedDataResult(T vector) {
        RAbstractContainer result = reuse(vector);
        containerLib.materializeData(vector);
        return (T) result;
    }

    /**
     * Should be used in cases where the attributes of the resulting vector will be written.
     */
    public <T extends RAbstractContainer> T getMaterializedAttributesResult(T vector) {
        // TODO: when all vectors can have attributes will be just: return reuse(vector)
        return getMaterializedResult(vector);
    }

    /**
     * Should be used in cases where both the attributes and data of the resulting vector will be
     * written.
     */
    @SuppressWarnings("unchecked")
    public <T extends RAbstractContainer> T getMaterializedResult(T vector) {
        RAbstractContainer result = vector;
        if (isGeneric) {
            if (!RSharingAttributeStorage.isShareable(vector) || !isTempOrNonShared(vector)) {
                result = copyVectorSlowPath(vector);
            }
            result = materializeSlowPath(result);
        } else {
            if (!isShareableClass || !isTempOrNonShared) {
                if (!containerLib.isMaterialized(vector) && RAbstractVector.class.isAssignableFrom(clazz)) {
                    // materialization of non RMaterializedVector subclasses
                    // create a copy in materialize already
                    result = containerLib.materialize(cast(vector));
                    assert result != vector : result.getClass().getSimpleName() + " " + vector.getClass().getSimpleName();
                } else {
                    RAbstractContainer vectorCopy = profileCopiedValue(copyWithAttributes(cast(vector)));
                    result = getCopyResultContainerLib().materialize(vectorCopy);
                }
            } else {
                result = containerLib.materialize(cast(vector));
            }
        }
        return (T) result;
    }

    private RAbstractContainer profileCopiedValue(RAbstractContainer vec) {
        if (copiedValueProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            copiedValueProfile = ValueProfile.createClassProfile();
        }
        return copiedValueProfile.profile(vec);
    }

    public AbstractContainerLibrary getCopyResultContainerLib() {
        if (copyResultContainerLib == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            copyResultContainerLib = insert(AbstractContainerLibrary.getFactory().createDispatched(DSLConfig.getCacheSize(1)));
        }
        return copyResultContainerLib;
    }

    public RAbstractContainer copyWithAttributes(RAbstractContainer container) {
        if (copyWithAttributesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            copyWithAttributesNode = insert(CopyWithAttributes.create());
        }
        return copyWithAttributesNode.execute(containerLib, container);
    }

    public static VectorReuse createTemporaryGeneric() {
        return new VectorReuse(null, true, true);
    }

    public static VectorReuse createNonSharedGeneric() {
        return new VectorReuse(null, false, true);
    }

    public static VectorReuse createTemporary(RAbstractContainer vector) {
        return new VectorReuse(vector, true, false);
    }

    public static VectorReuse createNonShared(RAbstractContainer vector) {
        return new VectorReuse(vector, false, false);
    }
}
