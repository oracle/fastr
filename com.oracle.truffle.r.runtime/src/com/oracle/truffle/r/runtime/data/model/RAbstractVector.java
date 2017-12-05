/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

/**
 * When implementing, make sure to invoke related {@link MemoryCopyTracer} methods.
 */
public interface RAbstractVector extends RAbstractContainer {

    /**
     * Creates a copy of the vector. This copies all of the contained data as well. If the data in
     * the vector is to be updated upon copying, the corresponding {@code copyResetData()} method
     * should be used.
     */
    RAbstractVector copy();

    default RAbstractVector deepCopy() {
        return copy();
    }

    RVector<?> copyResized(int size, boolean fillNA);

    // TODO: this does not say anything about reference counting? It seems that its used wrongly
    // w.r.t. reference counting.
    RAbstractVector copyWithNewDimensions(int[] newDimensions);

    RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA);

    RAbstractVector copyDropAttributes();

    RVector<?> createEmptySameType(int newLength, boolean newIsComplete);

    @Override
    RVector<?> materialize();

    boolean isMatrix();

    boolean isArray();

    /**
     * Casts a vector to another {@link RType}. If a safe cast to the target {@link RType} is not
     * supported <code>null</code> is returned. Instead of materializing the cast for each index the
     * implementation may decide to just wrap the original vector with a closure. This method is
     * optimized for invocation with a compile-time constant {@link RType}.
     *
     * @see #castSafe(RType, ConditionProfile, boolean)
     */
    default RAbstractVector castSafe(RType type, ConditionProfile isNAProfile) {
        return castSafe(type, isNAProfile, false);
    }

    /**
     * Casts a vector to another {@link RType}. If a safe cast to the target {@link RType} is not
     * supported <code>null</code> is returned. Instead of materializing the cast for each index the
     * implementation may decide to just wrap the original vector with a closure. This method is
     * optimized for invocation with a compile-time constant {@link RType}.
     *
     * @param type
     * @param isNAProfile
     * @param keepAttributes If {@code true}, the cast itself will keep the attributes. This is,
     *            however, a rather slow operation and you should set this to {@code false} and use
     *            nodes for copying attributes if possible.
     *
     * @see RType#getPrecedence()
     */
    default RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        if (type == getRType()) {
            return this;
        } else {
            return null;
        }
    }

    void setComplete(boolean complete);

    /**
     * Verifies the integrity of the vector, mainly whether a vector that claims to be
     * {@link #isComplete()} contains NA values.
     */
    static boolean verify(RAbstractVector vector) {
        CompilerAsserts.neverPartOfCompilation();
        VectorAccess access = vector.slowPathAccess();
        assert access.getType().isVector();
        if (!access.getType().isAtomic()) {
            // check non-atomic vectors for nullness
            try (SequentialIterator iter = access.access(vector)) {
                while (access.next(iter)) {
                    assert access.getListElement(iter) != null : "element " + iter.getIndex() + " of vector " + vector + " is null";
                }
            }
        } else if (access.getType() == RType.List) {
            assert !vector.isComplete();
        }
        if (vector.isComplete()) {
            // check all vectors for completeness
            try (SequentialIterator iter = access.access(vector)) {
                while (access.next(iter)) {
                    assert !access.isNA(iter) : "element " + iter.getIndex() + " of vector " + vector + " is NA";
                }
            }
        }
        return true;
    }
}
