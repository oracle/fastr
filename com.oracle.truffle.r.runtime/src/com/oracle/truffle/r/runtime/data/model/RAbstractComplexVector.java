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

import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;

public abstract class RAbstractComplexVector extends RAbstractAtomicVector {

    public RAbstractComplexVector(boolean complete) {
        super(complete);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    public abstract RComplex getDataAt(int index);

    @Override
    public abstract RComplexVector materialize();

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, RComplex value) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public void setDataAt(Object store, int index, double value) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public double getComplexPartAt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RType getRType() {
        return RType.Complex;
    }

    @Override
    public double[] getDataTemp() {
        return (double[]) super.getDataTemp();
    }

    @Override
    public Object getReadonlyData() {
        return getDataCopy();
    }

    @Override
    public double[] getDataCopy() {
        int length = getLength();
        double[] result = new double[length << 1];
        for (int i = 0; i < length; i++) {
            RComplex c = getDataAt(i);
            result[i * 2] = c.getRealPart();
            result[i * 2 + 1] = c.getImaginaryPart();
        }
        return result;
    }

    @Override
    public Object getInternalManagedData() {
        return null;
    }

    @Override
    public final RComplexVector createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createComplexVector(new double[newLength << 1], newIsComplete);
    }

}
