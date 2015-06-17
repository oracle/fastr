/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.data.model.*;

public abstract class RScalarVector extends RScalar implements RAbstractVector {

    public abstract boolean isNA();

    public final RStringVector getClassHierarchy() {
        return RDataFactory.createStringVector(getRType().getName());
    }

    public final RStringVector getImplicitClass() {
        return RDataFactory.createStringVector(getRType().getName());
    }

    @Override
    public final RScalarVector copy() {
        return this;
    }

    public void setComplete(boolean complete) {
        // scalar vectors don't need this information.
        // it is always rechecked
    }

    public final boolean isComplete() {
        return !isNA();
    }

    public final int getLength() {
        return 1;
    }

    public RAbstractContainer resize(int size) {
        return materialize().resize(size);
    }

    public boolean hasDimensions() {
        return false;
    }

    public int[] getDimensions() {
        return null;
    }

    public void setDimensions(int[] newDimensions) {
        throw new UnsupportedOperationException();
    }

    public RAbstractContainer materializeNonShared() {
        return materialize().materializeNonShared();
    }

    public RShareable materializeToShareable() {
        return materialize().materializeToShareable();
    }

    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        return null;
    }

    public void setNames(RStringVector newNames) {
        throw new UnsupportedOperationException();
    }

    public RList getDimNames(RAttributeProfiles attrProfiles) {
        return null;
    }

    public void setDimNames(RList newDimNames) {
        throw new UnsupportedOperationException();
    }

    public Object getRowNames(RAttributeProfiles attrProfiles) {
        return null;
    }

    public void setRowNames(RAbstractVector rowNames) {
        throw new UnsupportedOperationException();
    }

    public boolean isObject(RAttributeProfiles attrProfiles) {
        return false;
    }

    public RAttributes initAttributes() {
        throw new UnsupportedOperationException();
    }

    public RAttributes getAttributes() {
        return null;
    }

    public RVector copyResized(int size, boolean fillNA) {
        return materialize().copyResized(size, fillNA);
    }

    public RAbstractVector copyWithNewDimensions(int[] newDimensions) {
        return materialize().copyWithNewDimensions(newDimensions);
    }

    public RVector copyResizedWithDimensions(int[] newDimensions) {
        return materialize().copyResizedWithDimensions(newDimensions);
    }

    public RAbstractVector copyDropAttributes() {
        return materialize().copyDropAttributes();
    }

    public RVector createEmptySameType(int newLength, boolean newIsComplete) {
        return materialize().createEmptySameType(newLength, newIsComplete);
    }

    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        throw new UnsupportedOperationException();
    }

    public boolean isMatrix() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public final boolean checkCompleteness() {
        return isComplete();
    }

}
