/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * The {@code} .Internal part of the {@code array} function. The R code may alter the arguments
 * before calling {@code .Internal}.
 *
 * <pre>
 * array <- function(data = NA, dim = length(data), dimnames = NULL) { .Internal.array(data, dim, dimnames) }
 * </pre>
 *
 * TODO complete. This is sufficient for the b25 benchmark use.
 */
@RBuiltin(name = "array", kind = INTERNAL)
public abstract class Array extends RBuiltinNode {

    @CreateCast({"arguments"})
    public RNode[] createCastDimensions(RNode[] children) {
        RNode dimsVector = CastToVectorNodeFactory.create(children[1], false, false, false, false);
        return new RNode[]{children[0], CastIntegerNodeFactory.create(dimsVector, false, false, false), children[2]};
    }

    private int dimDataHelper(RAbstractIntVector dim, int[] dimData) {
        int totalLength = 1;
        int seenNegative = 0;
        for (int i = 0; i < dim.getLength(); i++) {
            dimData[i] = dim.getDataAt(i);
            if (dimData[i] < 0) {
                seenNegative++;
            }
            totalLength *= dimData[i];
        }
        if (seenNegative == dim.getLength() && seenNegative != 0) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getDimsContainNegativeValues(getEncapsulatingSourceSection());
        } else if (seenNegative > 0) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getNegativeLengthVectorsNotAllowed(getEncapsulatingSourceSection());
        }
        return totalLength;
    }

    @Specialization
    public RIntVector doArray(RAbstractIntVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        int[] data = new int[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createIntVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    public RDoubleVector doArray(RAbstractDoubleVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        double[] data = new double[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createDoubleVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    public RLogicalVector doArray(RAbstractLogicalVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        byte[] data = new byte[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createLogicalVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    public RStringVector doArray(RAbstractStringVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        String[] data = new String[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createStringVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    public RComplexVector doArray(RAbstractComplexVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        double[] data = new double[totalLength << 2];
        int ind = 0;
        for (int i = 0; i < totalLength; i++) {
            RComplex d = vec.getDataAt(i % vec.getLength());
            data[ind++] = d.getRealPart();
            data[ind++] = d.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    public RRawVector doArray(RAbstractRawVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        byte[] data = new byte[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength()).getValue();
        }
        return RDataFactory.createRawVector(data, dimData);
    }

    @Specialization
    public RList doArray(RList vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        controlVisibility();
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        Object[] data = new Object[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createList(data, dimData);
    }

}
