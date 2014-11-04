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

import java.util.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public final class RList extends RVector implements RAbstractVector {

    private Object[] data;

    private static final String[] implicitClassHrDyn = {"", RType.List.getName()};

    @CompilationFinal public String elementNamePrefix;

    @SuppressWarnings("unused")
    RList(Object[] data, boolean isComplete, int[] dims, Object names) {
        super(false, data.length, dims, names);
        this.data = data;
    }

    private RList(Object[] data, boolean isComplete, int[] dims) {
        this(data, isComplete, dims, null);
    }

    @Override
    protected RList internalCopy() {
        return new RList(Arrays.copyOf(data, data.length), this.isComplete(), null);
    }

    @Override
    protected RVector internalDeepCopy() {
        // TOOD: only used for nested list updates, but still could be made faster (through a
        // separate AST node?)
        RList listCopy = new RList(Arrays.copyOf(data, data.length), this.isComplete(), null);
        for (int i = 0; i < listCopy.getLength(); i++) {
            Object el = listCopy.getDataAt(i);
            if (el instanceof RVector) {
                Object elCopy = ((RVector) el).deepCopy();
                listCopy.updateDataAt(i, elCopy, null);
            }
        }
        return listCopy;
    }

    @Override
    protected int internalGetLength() {
        return data.length;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return Arrays.toString(data);
    }

    @Override
    protected boolean internalVerify() {
        // TODO: Implement String + NA
        return true;
    }

    /**
     * Intended for external calls where a copy is not needed. WARNING: think carefully before using
     * this method rather than {@link #getDataCopy()}.
     */
    public Object[] getDataWithoutCopying() {
        return data;
    }

    public Object[] getDataCopy() {
        Object[] copy = new Object[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    /**
     * Return vector data (copying if necessary) that's guaranteed not to be shared with any other
     * vector instance (but maybe non-temporary in terms of vector's sharing mode).
     *
     * @return vector data
     */
    public Object[] getDataNonShared() {
        return isShared() ? getDataCopy() : getDataWithoutCopying();

    }

    /**
     * Return vector data (copying if necessary) that's guaranteed to be "fresh" (temporary in terms
     * of vector sharing mode).
     *
     * @return vector data
     */
    public Object[] getDataTemp() {
        return isTemporary() ? getDataWithoutCopying() : getDataCopy();
    }

    public Object getDataAt(int i) {
        return data[i];
    }

    @Override
    protected String getDataAtAsString(int index) {
        return RRuntime.toString(getDataAt(index));
    }

    public RList updateDataAt(int i, Object right, @SuppressWarnings("unused") NACheck rightNACheck) {
        assert !this.isShared();
        data[i] = right;
        return this;
    }

    @Override
    public RList updateDataAtAsObject(int i, Object o, NACheck naCheck) {
        return updateDataAt(i, o, naCheck);

    }

    @Override
    public RList createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createList(new Object[newLength]);
    }

    @Override
    public void transferElementSameType(int toIndex, RAbstractVector fromVector, int fromIndex) {
        RList other = (RList) fromVector;
        data[toIndex] = other.data[fromIndex];
    }

    public Class<?> getElementClass() {
        return Object.class;
    }

    @Override
    public RList copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createList(data, newDimensions);
    }

    @TruffleBoundary
    public Object getNameAt(int index) {
        if (names != null && names != RNull.instance) {
            String name = ((RStringVector) names).getDataAt(index);
            if (name == RRuntime.STRING_NA) {
                return "$" + RRuntime.NA_HEADER;
            } else if (name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE)) {
                return "[[" + Integer.toString(index + 1) + "]]";
            } else if (name.matches("^[a-zA-Z.][a-zA-Z0-9_.]*$")) {
                return "$" + name;
            } else {
                return "$`" + name + "`";
            }
        } else {
            return "[[" + Integer.toString(index + 1) + "]]";
        }
    }

    @Override
    public RVector materialize() {
        return this;
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return this.getDataAt(index);
    }

    private Object[] createResizedData(int size, boolean fillNA) {
        assert !this.isShared();
        return copyResizedData(size, fillNA);
    }

    private Object[] copyResizedData(int size, boolean fillNA) {
        Object[] newData = Arrays.copyOf(data, size);
        return resizeData(newData, this.data, this.getLength(), fillNA);
    }

    private static Object[] resizeData(Object[] newData, Object[] oldData, int oldDataLength, boolean fillNA) {
        if (newData.length > oldDataLength) {
            if (fillNA) {
                for (int i = oldDataLength; i < newData.length; ++i) {
                    newData[i] = RRuntime.LOGICAL_NA;
                }
            } else {
                for (int i = oldData.length, j = 0; i < newData.length; ++i, j = Utils.incMod(j, oldData.length)) {
                    newData[i] = oldData[j];
                }
            }
        }
        return newData;
    }

    @Override
    public RList copyResized(int size, boolean fillNA) {
        return RDataFactory.createList(copyResizedData(size, fillNA));
    }

    @Override
    protected void resizeInternal(int size) {
        this.data = createResizedData(size, true);
    }

    @Override
    protected RStringVector getImplicitClassHr() {
        return getClassHierarchyHelper(new String[]{RType.List.getName()}, implicitClassHrDyn);
    }
}
