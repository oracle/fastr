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
package com.oracle.truffle.r.runtime.data;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromListAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

/**
 * A note on the RList complete flag {@link RAbstractVector#isComplete() } - it is always
 * initialized with <code>false</code> in {@link RListBase#RListBase(java.lang.Object[])} and never
 * expected to change.
 */
public final class RList extends RListBase implements RAbstractListVector {

    public String elementNamePrefix;

    RList(Object[] data) {
        super(data);
    }

    RList(Object[] data, int[] dims, RStringVector names, RList dimNames) {
        super(data, dims, names, dimNames);
    }

    @Override
    public RList materialize() {
        return this;
    }

    @Override
    protected RList internalCopy() {
        return new RList(Arrays.copyOf(data, data.length), getDimensionsInternal(), null, null);
    }

    @TruffleBoundary
    private int[] getDimensionsInternal() {
        return getDimensions();
    }

    @Override
    protected RList internalDeepCopy() {
        // TOOD: only used for nested list updates, but still could be made faster (through a
        // separate AST node?)
        RList listCopy = new RList(Arrays.copyOf(data, data.length), getDimensionsInternal(), null, null);
        for (int i = 0; i < listCopy.getLength(); i++) {
            Object el = listCopy.getDataAt(i);
            if (el instanceof RVector) {
                Object elCopy = ((RVector<?>) el).deepCopy();
                listCopy.updateDataAt(i, elCopy, null);
            }
        }
        return listCopy;
    }

    @Override
    public RList createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createList(newLength);
    }

    @Override
    public RList copyWithNewDimensions(int[] newDimensions) {
        return RDataFactory.createList(data, newDimensions);
    }

    @Override
    protected RList internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        return RDataFactory.createList(copyResizedData(size, fillNA), dimensions);
    }

    private static final class FastPathAccess extends FastPathFromListAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        public RType getType() {
            return RType.List;
        }

        @Override
        protected Object getListElement(Object store, int index) {
            return ((Object[]) store)[index];
        }

        @Override
        protected void setListElement(Object store, int index, Object value) {
            ((Object[]) store)[index] = value;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromListAccess SLOW_PATH_ACCESS = new SlowPathFromListAccess() {
        @Override
        public RType getType() {
            return RType.List;
        }

        @Override
        protected Object getListElement(Object store, int index) {
            return ((RList) store).data[index];
        }

        @Override
        protected void setListElement(Object store, int index, Object value) {
            ((RList) store).data[index] = value;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
