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
import com.oracle.truffle.r.runtime.RRuntime;
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
 */
public final class VectorReuse extends Node {

    @Child private VectorAccess access;

    private final boolean isShareableClass;
    private final boolean isTempOrNonShared;
    private final boolean needsTemporary;
    protected final boolean isGeneric;
    protected final Class<? extends RAbstractContainer> clazz;
    @CompilationFinal private ValueProfile copiedValueProfile;

    public VectorReuse(RAbstractContainer vector, boolean needsTemporary, boolean isGeneric) {
        this.isShareableClass = isGeneric ? false : RSharingAttributeStorage.isShareable(vector);
        this.clazz = isGeneric ? null : vector.getClass();
        this.needsTemporary = needsTemporary;
        this.isGeneric = isGeneric;
        this.isTempOrNonShared = isShareableClass && isTempOrNonShared(vector);
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
        RSharingAttributeStorage vector = cast(value);
        return needsTemporary ? vector.isTemporary() == isTempOrNonShared : !vector.isShared() == isTempOrNonShared;
    }

    @TruffleBoundary
    private static RAbstractContainer copyVector(RAbstractContainer vector) {
        return vector.copy();
    }

    private boolean isTempOrNonShared(RAbstractContainer vector) {
        return needsTemporary ? ((RSharingAttributeStorage) vector).isTemporary() : !((RSharingAttributeStorage) vector).isShared();
    }

    @SuppressWarnings("unchecked")
    public <T extends RAbstractContainer> T getResult(T vector) {
        RAbstractContainer result;
        if (isGeneric) {
            if (RSharingAttributeStorage.isShareable(vector) && isTempOrNonShared(vector)) {
                result = vector;
            } else {
                result = copyVector(vector);
            }
        } else {
            if (!isShareableClass || !isTempOrNonShared) {
                result = cast(vector).copy();
            } else {
                result = cast(vector);
            }
        }
        return (T) result;
    }

    @SuppressWarnings("unchecked")
    public <T extends RAbstractContainer> T getMaterializedResult(T vector) {
        RAbstractContainer result;
        if (isGeneric) {
            if (RSharingAttributeStorage.isShareable(vector) && isTempOrNonShared(vector)) {
                result = vector.materialize();
            } else {
                result = copyVector(vector).materialize();
            }
        } else {
            if (!isShareableClass || !isTempOrNonShared) {
                if (!RRuntime.isMaterializedVector(vector) && RAbstractVector.class.isAssignableFrom(clazz)) {
                    // materialization of non RMaterializedVector subclasses
                    // create a copy in materialize already
                    result = cast(vector).materialize();
                    assert result != vector : result.getClass().getSimpleName() + " " + vector.getClass().getSimpleName();
                } else {
                    result = profileCopiedValue(cast(vector).copy()).materialize();
                }
            } else {
                result = cast(vector).materialize();
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
