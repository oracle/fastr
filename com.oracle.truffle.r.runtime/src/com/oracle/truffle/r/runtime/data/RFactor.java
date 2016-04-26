/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class RFactor implements RShareable, RAbstractContainer {

    private final RIntVector vector;

    private final boolean ordered;

    public RFactor(RIntVector vector, boolean ordered) {
        this.vector = vector;
        this.ordered = ordered;
    }

    @Override
    public int[] getInternalStore() {
        return vector.getInternalStore();
    }

    @Override
    public RType getRType() {
        return RType.Integer;
    }

    public RIntVector getVector() {
        return vector;
    }

    public boolean isOrdered() {
        return ordered;
    }

    @Override
    public boolean isComplete() {
        return vector.isComplete();
    }

    @Override
    public int getLength() {
        return vector.getLength();
    }

    @Override
    public RAbstractContainer resize(int size) {
        return vector.resize(size);
    }

    @Override
    public boolean isTemporary() {
        return vector.isTemporary();
    }

    @Override
    public boolean isShared() {
        return vector.isShared();
    }

    @Override
    public void incRefCount() {
        vector.incRefCount();
    }

    @Override
    public void decRefCount() {
        vector.decRefCount();
    }

    @Override
    public boolean isSharedPermanent() {
        return vector.isSharedPermanent();
    }

    @Override
    public void makeSharedPermanent() {
        vector.makeSharedPermanent();
    }

    @Override
    public RShareable getNonShared() {
        RIntVector newVector = (RIntVector) vector.getNonShared();
        return newVector == vector ? this : RDataFactory.createFactor(newVector, ordered);
    }

    @Override
    public RFactor copy() {
        return RDataFactory.createFactor((RIntVector) vector.copy(), ordered);
    }

    @Override
    public RAttributes getAttributes() {
        return vector.getAttributes();
    }

    @Override
    public boolean hasDimensions() {
        return vector.hasDimensions();
    }

    @Override
    public int[] getDimensions() {
        return vector.getDimensions();
    }

    @Override
    public void setDimensions(int[] newDimensions) {
        vector.setDimensions(newDimensions);
    }

    @Override
    public Class<?> getElementClass() {
        return RFactor.class;
    }

    @Override
    public RFactor materializeNonShared() {
        RVector v = vector.materializeNonShared();
        return vector != v ? RDataFactory.createFactor((RIntVector) v, ordered) : this;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return vector.getDataAtAsObject(index);
    }

    @Override
    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        return vector.getNames(attrProfiles);
    }

    @Override
    public void setNames(RStringVector newNames) {
        vector.setNames(newNames);
    }

    @Override
    public RList getDimNames(RAttributeProfiles attrProfiles) {
        return vector.getDimNames();
    }

    @Override
    public void setDimNames(RList newDimNames) {
        vector.setDimNames(newDimNames);
    }

    @Override
    public Object getRowNames(RAttributeProfiles attrProfiles) {
        return vector.getRowNames();
    }

    @Override
    public void setRowNames(RAbstractVector rowNames) {
        vector.setRowNames(rowNames);
    }

    @Override
    public RStringVector getClassHierarchy() {
        return vector.getClassHierarchy();
    }

    @Override
    public RStringVector getImplicitClass() {
        return vector.getImplicitClass();
    }

    @Override
    public boolean isObject(RAttributeProfiles attrProfiles) {
        return true;
    }

    @Override
    public RAttributes initAttributes() {
        return vector.initAttributes();
    }

    @Override
    public void initAttributes(RAttributes newAttributes) {
        vector.initAttributes(newAttributes);
    }

    @Override
    public void setAttr(String name, Object value) {
        vector.setAttr(name, value);
    }

    @Override
    public Object getAttr(RAttributeProfiles attrProfiles, String name) {
        return vector.getAttr(attrProfiles, name);
    }

    @Override
    public RAttributes resetAllAttributes(boolean nullify) {
        return vector.resetAllAttributes(nullify);
    }

    @Override
    public RShareable materializeToShareable() {
        return this;
    }

    public void setLevels(Object newLevels) {
        vector.setAttr(RRuntime.LEVELS_ATTR_KEY, newLevels);
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr, boolean convertToInt) {
        return vector.setClassAttr(classAttr, convertToInt);
    }

    public RVector getLevels(RAttributeProfiles attrProfiles) {
        Object attr = vector.getAttr(attrProfiles, RRuntime.LEVELS_ATTR_KEY);
        if (attr instanceof RVector) {
            return (RVector) attr;
        } else {
            // Scalar, must convert
            return (RVector) RRuntime.asAbstractVector(attr);
        }
    }

    public int getNLevels(RAttributeProfiles attrProfiles) {
        RVector levels = getLevels(attrProfiles);
        return levels == null ? 0 : levels.getLength();
    }

    @Override
    public int getGPBits() {
        return vector.getGPBits();
    }

    @Override
    public void setGPBits(int value) {
        vector.setGPBits(value);
    }
}
