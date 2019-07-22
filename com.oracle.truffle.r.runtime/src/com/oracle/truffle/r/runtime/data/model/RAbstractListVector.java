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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import java.util.Arrays;

/**
 * Note: lists must not contain {@code null} values.
 */
public abstract class RAbstractListVector extends RAbstractListBaseVector {

    public RAbstractListVector(boolean complete) {
        super(complete);
    }

    @Override
    public RType getRType() {
        return RType.List;
    }

    @Override
    public RList materialize() {
        RList materialized = RDataFactory.createList(getDataCopy());
        copyAttributes(materialized);
        return materialized;
    }

    @CompilerDirectives.TruffleBoundary
    private void copyAttributes(RList materialized) {
        materialized.copyAttributesFrom(this);
    }

    @Override
    protected RList internalCopyResized(int size, boolean fillNA, int[] dimensions) {
        return RDataFactory.createList(copyResizedData(size, fillNA), dimensions);
    }

    private Object[] copyResizedData(int size, boolean fillNA) {
        Object[] localData = getReadonlyData();
        Object[] newData = Arrays.copyOf(localData, size);
        return resizeData(newData, localData, this.getLength(), fillNA);
    }

    @Override
    public Object[] getDataCopy() {
        int length = getLength();
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++) {
            result[i] = getDataAt(i);
        }
        return result;
    }

}
