/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

public final class RFactor implements RShareable, RAbstractContainer {

    private RIntVector vector;

    private final boolean ordered;

    public RFactor(RIntVector vector, boolean ordered) {
        this.vector = vector;
        this.ordered = ordered;
    }

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
    public void markNonTemporary() {
        vector.markNonTemporary();
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
    public RVector makeShared() {
        return vector.makeShared();
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
    public RVector materializeNonSharedVector() {
        if (isShared()) {
            vector = (RIntVector) vector.copy();
            vector.markNonTemporary();
        }
        return vector;
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
    public RList getDimNames() {
        return vector.getDimNames();
    }

    @Override
    public void setDimNames(RList newDimNames) {
        vector.setDimNames(newDimNames);
    }

    @Override
    public Object getRowNames(RAttributeProfiles attrProfiles) {
        return vector.getRowNames(attrProfiles);
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
    public boolean isObject(RAttributeProfiles attrProfiles) {
        return true;
    }

    public RAttributes initAttributes() {
        return vector.initAttributes();
    }

    @Override
    public RShareable materializeToShareable() {
        return this;
    }

    public void setLevels(Object newLevels) {
        vector.setAttr(RRuntime.LEVELS_ATTR_KEY, newLevels);
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr) {
        return RVector.setVectorClassAttr(vector, classAttr, null, this);
    }

    public RVector getLevels(RAttributeProfiles attrProfiles) {
        return (RVector) vector.getAttr(attrProfiles, RRuntime.LEVELS_ATTR_KEY);
    }

    public int getNLevels(RAttributeProfiles attrProfiles) {
        RVector levels = getLevels(attrProfiles);
        return levels == null ? 0 : levels.getLength();
    }
}
