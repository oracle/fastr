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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

public final class RDataFrame implements RShareable, RAbstractContainer {

    private final RVector vector;

    public RDataFrame(RVector vector) {
        this.vector = vector;
    }

    public RType getRType() {
        return RType.List;
    }

    public RVector getVector() {
        return vector;
    }

    @Override
    public boolean isComplete() {
        return vector.isComplete();
    }

    @Override
    public int getLength() {
        if (vector instanceof RList) {
            return vector.getLength();
        } else {
            return vector.getLength() == 0 ? 0 : 1;
        }
    }

    @Override
    public RAbstractContainer resize(int size) {
        if (vector instanceof RList || size == 0) {
            return vector.resize(size);
        } else {
            RVector v = RDataFactory.createList();
            v.resize(size);
            v.setNames(vector.getNames());
            if (vector.getLength() == 0) {
                v.updateDataAtAsObject(0, vector, null);
            }
            return v;
        }
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
    public RDataFrame copy() {
        return RDataFactory.createDataFrame(vector.copy());
    }

    @Override
    public RAttributes getAttributes() {
        return vector.getAttributes();
    }

    @Override
    public boolean hasDimensions() {
        Utils.nyi("data frame's dimensions need to be obtained using builtins");
        return false;
    }

    @Override
    public int[] getDimensions() {
        Utils.nyi("data frame's dimensions need to be obtained using builtins");
        return null;
    }

    @Override
    public void setDimensions(int[] newDimensions) {
        // this may be strange, considering that getDimensions() must be obtained using builtins,
        // but this sets the explicit attribute that is listed with other data frame attributes
        vector.setDimensions(newDimensions);
    }

    @Override
    public Class<?> getElementClass() {
        return RDataFrame.class;
    }

    @Override
    public RDataFrame materializeNonShared() {
        RVector v = vector.materializeNonShared();
        return vector != v ? RDataFactory.createDataFrame(v) : this;
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
        Utils.nyi("data frame's dimnames needs to be obtained using builtins");
        return null;
    }

    @Override
    public void setDimNames(RList newDimNames) {
        vector.setDimNames(newDimNames);
    }

    @Override
    public Object getRowNames() {
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
    public boolean isObject(RAttributeProfiles attrProfiles) {
        return true;
    }

    @Override
    public RAttributes initAttributes() {
        return vector.initAttributes();
    }

    @Override
    public final void setAttr(String name, Object value) {
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

    public int getElementIndexByName(RAttributeProfiles attrProfiles, String name) {
        return vector.getElementIndexByName(attrProfiles, name);
    }

    public int getElementIndexByNameInexact(RAttributeProfiles attrProfiles, String name) {
        return vector.getElementIndexByNameInexact(attrProfiles, name);
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr, boolean convertToInt) {
        return vector.setClassAttr(classAttr, convertToInt);
    }

}
