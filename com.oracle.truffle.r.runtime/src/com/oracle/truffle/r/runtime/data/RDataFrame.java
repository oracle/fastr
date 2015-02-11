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

    private RVector vector;

    public RDataFrame(RVector vector) {
        this.vector = vector;
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
        if (vector instanceof RList || vector.getLength() == 0) {
            return vector.getLength();
        } else {
            return 1;
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
    public Class<?> getElementClass() {
        return RDataFrame.class;
    }

    @Override
    public RVector materializeNonSharedVector() {
        if (isShared()) {
            vector = vector.copy();
            vector.markNonTemporary();
        }
        return vector;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return vector.getDataAtAsObject(index);
    }

    @Override
    public RStringVector getNames() {
        return vector.getNames();
    }

    @Override
    public RList getDimNames() {
        Utils.nyi("data frame's dimnames needs to be obtained using builtins");
        return null;
    }

    @Override
    public Object getRowNames() {
        return vector.getRowNames();
    }

    @Override
    public RStringVector getClassHierarchy() {
        return vector.getClassHierarchy();
    }

    @Override
    public boolean isObject() {
        return true;
    }

    public RAttributes initAttributes() {
        return vector.initAttributes();
    }

    @Override
    public RShareable materializeToShareable() {
        return this;
    }

    public int getElementIndexByName(String name) {
        return vector.getElementIndexByName(name);
    }

    public int getElementIndexByNameInexact(String name) {
        return vector.getElementIndexByNameInexact(name);
    }

    @Override
    public RAbstractContainer setClassAttr(RStringVector classAttr) {
        return RVector.setVectorClassAttr(vector, classAttr, this, null);
    }

}
