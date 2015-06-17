/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class RToVectorClosure implements RAbstractVector {

    protected final RAbstractVector vector;

    public RToVectorClosure(RAbstractVector vector) {
        this.vector = vector;
    }

    public int getLength() {
        return vector.getLength();
    }

    @Override
    public final RAbstractContainer resize(int size) {
        return vector.resize(size);
    }

    public final void setComplete(boolean complete) {
        this.vector.setComplete(complete);
    }

    public final boolean isComplete() {
        return vector.isComplete();
    }

    @Override
    public final boolean hasDimensions() {
        return vector.hasDimensions();
    }

    @Override
    public final int[] getDimensions() {
        return vector.getDimensions();
    }

    @Override
    public final void setDimensions(int[] newDimensions) {
        vector.setDimensions(newDimensions);
    }

    @Override
    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        return vector.getNames(attrProfiles);
    }

    @Override
    public final void setNames(RStringVector newNames) {
        vector.setNames(newNames);
    }

    @Override
    public final RList getDimNames(RAttributeProfiles attrProfiles) {
        return vector.getDimNames(attrProfiles);
    }

    @Override
    public final void setDimNames(RList newDimNames) {
        vector.setDimNames(newDimNames);
    }

    @Override
    public final Object getRowNames(RAttributeProfiles attrProfiles) {
        return vector.getRowNames(attrProfiles);
    }

    @Override
    public final void setRowNames(RAbstractVector rowNames) {
        vector.setRowNames(rowNames);
    }

    @Override
    public final RAttributes initAttributes() {
        return vector.initAttributes();
    }

    @Override
    public final RAttributes getAttributes() {
        return vector.getAttributes();
    }

    @Override
    public final RAbstractVector copy() {
        return vector.copy();
    }

    @Override
    public final RVector copyResized(int size, boolean fillNA) {
        return vector.copyResized(size, fillNA);
    }

    @Override
    public final RVector copyResizedWithDimensions(int[] newDimensions) {
        // TODO support for higher dimensions
        assert newDimensions.length == 2;
        RVector result = copyResized(newDimensions[0] * newDimensions[1], false);
        result.setDimensions(newDimensions);
        return result;
    }

    @Override
    public final RAbstractVector copyDropAttributes() {
        return vector.copyDropAttributes();
    }

    public final boolean isMatrix() {
        return vector.isMatrix();
    }

    public final boolean isArray() {
        return vector.isArray();
    }

    public final RStringVector getClassHierarchy() {
        return vector.getClassHierarchy();
    }

    public final boolean isObject(RAttributeProfiles attrProfiles) {
        return vector.isObject(attrProfiles);
    }

    @Override
    public final RAbstractVector materializeNonShared() {
        return (RAbstractVector) vector.materializeNonShared();
    }

    @Override
    public final RShareable materializeToShareable() {
        return vector.materialize();
    }

    @Override
    public final void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        throw RInternalError.shouldNotReachHere();
    }

}
