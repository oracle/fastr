/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

public final class RDataFrame implements RShareable, RAbstractContainer {

    private RVector vector;

    public RDataFrame(RVector vector) {
        this.vector = vector;
    }

    public RVector getVector() {
        return vector;
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
    public RDataFrame copy() {
        return RDataFactory.createDataFrame(vector.copy());
    }

    @Override
    public RAttributes getAttributes() {
        return vector.getAttributes();
    }

    @Override
    public int[] getDimensions() {
        return vector.getDimensions();
    }

    @Override
    public Class<?> getElementClass() {
        return RVector.class;
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
    public Object getNames() {
        return vector.getNames();
    }

    @Override
    public RList getDimNames() {
        return vector.getDimNames();
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
}
