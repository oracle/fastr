/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class VectorReuse extends Node {

    @Child private VectorAccess access;

    private final boolean isShareableClass;
    private final boolean isTempOrNonShared;
    private final boolean needsTemporary;
    protected final boolean isGeneric;
    protected final Class<? extends RAbstractVector> clazz;

    public VectorReuse(RAbstractVector vector, boolean needsTemporary, boolean isGeneric) {
        this.isShareableClass = isGeneric ? false : vector instanceof RSharingAttributeStorage;
        this.clazz = isGeneric ? null : vector.getClass();
        this.needsTemporary = needsTemporary;
        this.isGeneric = isGeneric;
        this.isTempOrNonShared = isShareableClass && isTempOrNonShared(vector);
    }

    protected RAbstractVector cast(RAbstractVector value) {
        return clazz.cast(value);
    }

    public VectorAccess access(RAbstractVector result) {
        return isGeneric ? result.slowPathAccess() : access;
    }

    public boolean supports(RAbstractVector value) {
        assert !isGeneric : "cannot call 'supports' on generic vector reuse";
        RSharingAttributeStorage.verify(value);
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
        RSharingAttributeStorage vector = (RSharingAttributeStorage) cast(value);
        return needsTemporary ? vector.isTemporary() == isTempOrNonShared : !vector.isShared() == isTempOrNonShared;
    }

    @TruffleBoundary
    private static RAbstractVector copyVector(RAbstractVector vector) {
        return vector.copy();
    }

    private boolean isTempOrNonShared(RAbstractVector vector) {
        return needsTemporary ? ((RSharingAttributeStorage) vector).isTemporary() : !((RSharingAttributeStorage) vector).isShared();
    }

    @SuppressWarnings("unchecked")
    public <T extends RAbstractVector> T getResult(T vector) {
        RAbstractVector result;
        if (isGeneric) {
            RSharingAttributeStorage.verify(vector);
            if (vector instanceof RSharingAttributeStorage && isTempOrNonShared(vector)) {
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

    public static VectorReuse createTemporaryGeneric() {
        return new VectorReuse(null, true, true);
    }

    public static VectorReuse createNonSharedGeneric() {
        return new VectorReuse(null, false, true);
    }

    public static VectorReuse createTemporary(RAbstractVector vector) {
        return new VectorReuse(vector, true, false);
    }

    public static VectorReuse createNonShared(RAbstractVector vector) {
        return new VectorReuse(vector, false, false);
    }
}
