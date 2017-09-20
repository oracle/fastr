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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

abstract class RToVectorClosure implements RAbstractVector {

    /** If {@code true}, attributes should be preserved when materializing. */
    protected final boolean keepAttributes;

    protected RToVectorClosure(boolean keepAttributes) {
        this.keepAttributes = keepAttributes;
    }

    public abstract RAbstractVector getVector();

    @Override
    public int getLength() {
        return getVector().getLength();
    }

    @Override
    public EmptyInternalStore getInternalStore() {
        return EmptyInternalStore.INSTANCE;
    }

    @Override
    public final RAbstractContainer resize(int size) {
        return getVector().resize(size);
    }

    @Override
    public final void setComplete(boolean complete) {
        this.getVector().setComplete(complete);
    }

    @Override
    public final boolean isComplete() {
        return getVector().isComplete();
    }

    @Override
    public final boolean hasDimensions() {
        return getVector().hasDimensions();
    }

    @Override
    public final int[] getDimensions() {
        return getVector().getDimensions();
    }

    @Override
    public final void setDimensions(int[] newDimensions) {
        getVector().setDimensions(newDimensions);
    }

    @Override
    public RStringVector getNames() {
        return getVector().getNames();
    }

    @Override
    public final void setNames(RStringVector newNames) {
        getVector().setNames(newNames);
    }

    @Override
    public final RList getDimNames() {
        return getVector().getDimNames();
    }

    @Override
    public final void setDimNames(RList newDimNames) {
        getVector().setDimNames(newDimNames);
    }

    @Override
    public final Object getRowNames() {
        return getVector().getRowNames();
    }

    @Override
    public final void setRowNames(RAbstractVector rowNames) {
        getVector().setRowNames(rowNames);
    }

    @Override
    public final DynamicObject initAttributes() {
        return getVector().initAttributes();
    }

    @Override
    public final void initAttributes(DynamicObject newAttributes) {
        getVector().initAttributes(newAttributes);
    }

    @Override
    public final DynamicObject getAttributes() {
        return getVector().getAttributes();
    }

    @Override
    public final RAbstractVector copy() {
        return materialize().copy();
    }

    @Override
    public final RVector<?> copyResized(int size, boolean fillNA) {
        return materialize().copyResized(size, fillNA);
    }

    @Override
    public final RVector<?> copyResizedWithDimensions(int[] newDimensions, boolean fillNA) {
        return materialize().copyResizedWithDimensions(newDimensions, fillNA);
    }

    @Override
    public final RAbstractVector copyDropAttributes() {
        return materialize().copyDropAttributes();
    }

    @Override
    public final boolean isMatrix() {
        return getVector().isMatrix();
    }

    @Override
    public final boolean isArray() {
        return getVector().isArray();
    }

    @Override
    public final RTypedValue getNonShared() {
        return getVector().getNonShared();
    }

    @Override
    public int getTypedValueInfo() {
        return getVector().getTypedValueInfo();
    }

    @Override
    public void setTypedValueInfo(int value) {
        getVector().setTypedValueInfo(value);
    }

    @Override
    public boolean isS4() {
        return false;
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttrs) {
        // Closures are trimmed to use a concrete type in order to avoid polymorphism. Therefore,
        // first materialize and then cast and do not create a closure over a closure.
        return materialize().castSafe(type, isNAProfile, keepAttrs);
    }
}
