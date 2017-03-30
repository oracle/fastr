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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * In converting complex numbers to integers, this closure discards the imaginary parts.
 */
final class RAbstactVectorToListClosure extends RToVectorClosure implements RAbstractListVector {

    RAbstactVectorToListClosure(RAbstractVector vector) {
        super(vector);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return vector.getDataAtAsObject(index);
    }

    @Override
    public Object getDataAt(int index) {
        return vector.getDataAtAsObject(index);
    }

    @Override
    public RList materialize() {
        int length = getLength();
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++) {
            Object data = getDataAtAsObject(i);
            result[i] = data;
        }
        return RDataFactory.createList(result);
    }

    @Override
    public RAbstractVector copyWithNewDimensions(int[] newDimensions) {
        return materialize().copyWithNewDimensions(newDimensions);
    }

    @Override
    public RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createList(newLength);
    }
}
