/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RVector;

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

    RVector copyResized(int size, boolean fillNA);

    RAbstractVector copyWithNewDimensions(int[] newDimensions);

    RVector copyResizedWithDimensions(int[] newDimensions, boolean fillNA);

    RAbstractVector copyDropAttributes();

    RVector createEmptySameType(int newLength, boolean newIsComplete);

    RVector materialize();

    boolean isMatrix();

    boolean isArray();

    boolean checkCompleteness();

    /**
     * Casts a vector to another {@link RType}. If a safe cast to the target {@link RType} is not
     * supported <code>null</code> is returned. Instead of materializing the cast for each index the
     * implementation may decide to just wrap the original vector with a closure. This method is
     * optimized for invocation with a compile-time constant {@link RType}.
     *
     * @see RType#getPrecedence()
     */
    default RAbstractVector castSafe(RType type, @SuppressWarnings("unused") ConditionProfile isNAProfile) {
        if (type == getRType()) {
            return this;
        } else {
            return null;
        }
    }

    void setComplete(boolean complete);

    void setNA(Object store, int index);

}
