/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;

/**
 * The base class for list-like objects: {@link com.oracle.truffle.r.runtime.data.RExpression} and
 * {@link com.oracle.truffle.r.runtime.data.RList}. It can be useful in situations where the same
 * algorithm applies to those two data structures.
 * 
 * Note on sharing mode for list elements: by default the sharing state of elements in a list can be
 * inconsistent, e.g. a list referenced by one local variable may contain temporary vectors, or list
 * references by more variables (shared list) may contain non-shared elements. The sharing state of
 * the elements should be made consistent on reading from the list by the callers! When we read from
 * shared list, we make the element shared. When we read from non-shared list, we make the element
 * at least non-shared. There is no possible way, how a list can contain a non-shared element not
 * owned by it, given that any element of some other list must be first read into variable (the
 * extraction from list makes it at least non-shared, then the write makes it shared) and only then
 * it can be put inside another list. This is however not true for internal code, which may read
 * data from a list and then put it into another list, in such case it is responsibility of the code
 * to increment the refcount of such data. Consult also the documentation of
 * {@code ExtractListElement}, which is a node that can extract an element of a list or abstract
 * vector and put it in the consistent sharing state.
 */
public abstract class RAbstractListBaseVector extends RAbstractVector {

    public RAbstractListBaseVector(boolean complete) {
        super(complete);
    }

    @Override
    public Object getDataAtAsObject(Object store, int i) {
        return getDataAt(i);
    }

    public abstract Object getDataAt(int index);

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RAbstractVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createList(newLength);
    }

    @Override
    public Object getInternalManagedData() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object getDataCopy() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public Object getReadonlyData() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void setLength(int l) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public int getTrueLength() {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void setTrueLength(int l) {
        throw RInternalError.shouldNotReachHere();
    }
}
