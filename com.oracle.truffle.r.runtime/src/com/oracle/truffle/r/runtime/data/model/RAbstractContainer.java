/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

public abstract class RAbstractContainer extends RSharingAttributeStorage {

    public abstract boolean isComplete();

    public abstract int getLength();

    public abstract void setLength(int l);

    public abstract int getTrueLength();

    public abstract void setTrueLength(int l);

    public abstract RAbstractContainer resize(int size);

    public abstract boolean hasDimensions();

    public abstract int[] getDimensions();

    public abstract void setDimensions(int[] newDimensions);

    public abstract RAbstractContainer materialize();

    public abstract Object getDataAtAsObject(int index);

    @Override
    public abstract RAbstractContainer copy();

    /**
     * Note: elements inside lists may be in inconsistent state reference counting wise. You may
     * need to put them into consistent state depending on what you use them for, consult the
     * documentation of {@code ExtractListElement}.
     */
    public Object getDataAtAsObject(@SuppressWarnings("unused") Object store, int index) {
        return getDataAtAsObject(index);
    }

    /**
     * Returns an object that could be passed to {@link #getDataAtAsObject(Object, int)} or type
     * specialized versions in concrete vector types. The {@code store} object should contain data
     * necessary for the vector to perform {@link #getDataAtAsObject(Object, int)} and similar
     * methods without any field loads. If {@code store} is saved into a local variable, then the
     * {@code getDataAsObject} overloads with {@code store} parameter do not have to load the
     * vector's fields, but instead read the necessary data from a local variable, which could be
     * beneficial when in loop.
     */
    public abstract Object getInternalStore();

    public RStringVector getNames() {
        CompilerAsserts.neverPartOfCompilation();
        return (RStringVector) getAttr(RRuntime.NAMES_ATTR_KEY);
    }

    /**
     * Sets names for the container, or removes them in case that <code>newNames</code> is
     * <code>null</code>.
     * 
     * @param newNames
     */
    public void setNames(RStringVector newNames) {
        CompilerAsserts.neverPartOfCompilation();
        setAttr(RRuntime.NAMES_ATTR_KEY, newNames);
    }

    public RList getDimNames() {
        CompilerAsserts.neverPartOfCompilation();
        return (RList) getAttr(RRuntime.DIMNAMES_ATTR_KEY);
    }

    public void setDimNames(RList newDimNames) {
        CompilerAsserts.neverPartOfCompilation();
        setAttr(RRuntime.DIMNAMES_ATTR_KEY, newDimNames);
    }

    public Object getRowNames() {
        CompilerAsserts.neverPartOfCompilation();
        return getAttr(RRuntime.ROWNAMES_ATTR_KEY);
    }

    public void setRowNames(RAbstractVector rowNames) {
        CompilerAsserts.neverPartOfCompilation();
        setAttr(RRuntime.ROWNAMES_ATTR_KEY, rowNames);
    }

    public abstract VectorAccess access();

    public abstract VectorAccess slowPathAccess();
}
