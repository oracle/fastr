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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class RExpression implements RShareable, RAbstractContainer {

    private static final RStringVector implicitClassHeader = RDataFactory.createStringVectorFromScalar(RType.Expression.getClazz());

    private final RList data;

    public RExpression(RList data) {
        this.data = data;
    }

    @Override
    public RType getRType() {
        return RType.Expression;
    }

    public RList getList() {
        return data;
    }

    public Object getDataAt(int index) {
        return data.getDataAt(index);
    }

    @Override
    public RAttributes initAttributes() {
        return data.initAttributes();
    }

    @Override
    public final void initAttributes(RAttributes newAttributes) {
        data.initAttributes(newAttributes);
    }

    @Override
    public final void setAttr(String name, Object value) {
        data.setAttr(name, value);
    }

    @Override
    public Object getAttr(RAttributeProfiles attrProfiles, String name) {
        return data.getAttr(attrProfiles, name);
    }

    @Override
    public RAttributes getAttributes() {
        return data.getAttributes();
    }

    @Override
    public final void resetAllAttributes(boolean nullify) {
        data.resetAllAttributes(nullify);
    }

    @Override
    public boolean isComplete() {
        return data.isComplete();
    }

    @Override
    public int getLength() {
        return data.getLength();
    }

    @Override
    public RAbstractContainer resize(int size) {
        return data.resize(size);
    }

    @Override
    public boolean hasDimensions() {
        return data.hasDimensions();
    }

    @Override
    public int[] getDimensions() {
        return data.getDimensions();
    }

    @Override
    public void setDimensions(int[] newDimensions) {
        data.setDimensions(newDimensions);
    }

    @Override
    public Class<?> getElementClass() {
        return RExpression.class;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return data.getDataAtAsObject(index);
    }

    @Override
    public RStringVector getNames(RAttributeProfiles attrProfiles) {
        return data.getNames(attrProfiles);
    }

    @Override
    public void setNames(RStringVector newNames) {
        data.setNames(newNames);
    }

    @Override
    public RList getDimNames(RAttributeProfiles attrProfiles) {
        return data.getDimNames();
    }

    @Override
    public void setDimNames(RList newDimNames) {
        data.setDimNames(newDimNames);
    }

    @Override
    public Object getRowNames(RAttributeProfiles attrProfiles) {
        return data.getRowNames();
    }

    @Override
    public void setRowNames(RAbstractVector rowNames) {
        data.setRowNames(rowNames);
    }

    @Override
    public final RStringVector getClassHierarchy() {
        RStringVector v = (RStringVector) data.getAttr(RRuntime.CLASS_ATTR_KEY);
        if (v == null) {
            return getImplicitClass();
        } else {
            return v;
        }
    }

    @Override
    public RStringVector getImplicitClass() {
        return implicitClassHeader;
    }

    @Override
    public boolean isObject(RAttributeProfiles attrProfiles) {
        return false;
    }

    @Override
    public boolean isTemporary() {
        return data.isTemporary();
    }

    @Override
    public boolean isShared() {
        return data.isShared();
    }

    @Override
    public void incRefCount() {
        data.incRefCount();
    }

    @Override
    public void decRefCount() {
        data.decRefCount();
    }

    @Override
    public boolean isSharedPermanent() {
        return data.isSharedPermanent();
    }

    @Override
    public RShareable makeSharedPermanent() {
        data.makeSharedPermanent();
        return this;
    }

    @Override
    public RShareable getNonShared() {
        RList newData = (RList) data.getNonShared();
        return newData == data ? this : RDataFactory.createExpression(newData);
    }

    @Override
    public RExpression copy() {
        return RDataFactory.createExpression((RList) data.copy());
    }

    @Override
    public RShareable materializeToShareable() {
        return this;
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr) {
        return data.setClassAttr(classAttr);
    }

    @Override
    public String toString() {
        return String.format("RExpression(data=%s)", data);
    }

    @Override
    public int getTypedValueInfo() {
        return data.getTypedValueInfo();
    }

    @Override
    public void setTypedValueInfo(int value) {
        data.setTypedValueInfo(value);
    }
}
