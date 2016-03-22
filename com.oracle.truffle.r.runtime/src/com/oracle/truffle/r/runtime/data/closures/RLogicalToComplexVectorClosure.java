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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;

final class RLogicalToComplexVectorClosure extends RToComplexVectorClosure implements RAbstractComplexVector {

    RLogicalToComplexVectorClosure(RAbstractLogicalVector vector) {
        super(vector);
    }

    @Override
    public RComplex getDataAt(int index) {
        byte data = ((RAbstractLogicalVector) vector).getDataAt(index);
        double real;
        double imaginary;
        if (!vector.isComplete() && RRuntime.isNA(data)) {
            real = RRuntime.COMPLEX_NA_REAL_PART;
            imaginary = RRuntime.COMPLEX_NA_IMAGINARY_PART;
        } else {
            real = data;
            imaginary = 0;
        }
        return RDataFactory.createComplex(real, imaginary);
    }
}
