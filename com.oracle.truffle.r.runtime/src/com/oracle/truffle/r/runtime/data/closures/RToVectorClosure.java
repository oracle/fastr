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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

abstract class RToVectorClosure implements RAbstractVector {

    protected final RAbstractVector vector;

    RToVectorClosure(RAbstractVector vector) {
        this.vector = vector;
    }

    @Override
    public int getLength() {
        return vector.getLength();
    }

    @Override
    public Void getInternalStore() {
        return null;
    }

    @Override
    public final RAbstractContainer resize(int size) {
        return vector.resize(size);
    }

    @Override
    public final void setComplete(boolean complete) {
        this.vector.setComplete(complete);
    }

    @Override
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
    public RStringVector getNames() {
        return vector.getNames();
    }

    @Override
    public final void setNames(RStringVector newNames) {
        vector.setNames(newNames);
    }

    @Override
    public final RList getDimNames() {
        return vector.getDimNames();
    }

    @Override
    public final void setDimNames(RList newDimNames) {
        vector.setDimNames(newDimNames);
    }

    @Override
    public final Object getRowNames() {
        return vector.getRowNames();
    }

    @Override
    public final void setRowNames(RAbstractVector rowNames) {
        vector.setRowNames(rowNames);
    }

    @Override
    public final DynamicObject initAttributes() {
        return vector.initAttributes();
    }

    @Override
    public final void initAttributes(DynamicObject newAttributes) {
        vector.initAttributes(newAttributes);
    }

    @Override
    public final DynamicObject getAttributes() {
        return vector.getAttributes();
    }

    @Override
    public final RAbstractVector copy() {
        RAbstractVector result = vector.copy();
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public final RVector<?> copyResized(int size, boolean fillNA) {
        RVector<?> result = vector.copyResized(size, fillNA);
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public final RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        // TODO support for higher dimensions
        assert newDimensions.length == 2;
        RVector<?> result = copyResized(newDimensions[0] * newDimensions[1], fillNA);
        result.setDimensions(newDimensions);
        return result;
    }

    @Override
    public final RAbstractVector copyDropAttributes() {
        RAbstractVector result = vector.copyDropAttributes();
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public final boolean isMatrix() {
        return vector.isMatrix();
    }

    @Override
    public final boolean isArray() {
        return vector.isArray();
    }

    @Override
    public final RStringVector getClassHierarchy() {
        return vector.getClassHierarchy();
    }

    @Override
    public RStringVector getImplicitClass() {
        return vector.getImplicitClass();
    }

    @Override
    public final RTypedValue getNonShared() {
        return vector.getNonShared();
    }

    @Override
    public final RShareable materializeToShareable() {
        return vector.materialize();
    }

    @Override
    public int getTypedValueInfo() {
        return vector.getTypedValueInfo();
    }

    @Override
    public void setTypedValueInfo(int value) {
        vector.setTypedValueInfo(value);
    }

    @Override
    public boolean isS4() {
        return false;
    }
}
