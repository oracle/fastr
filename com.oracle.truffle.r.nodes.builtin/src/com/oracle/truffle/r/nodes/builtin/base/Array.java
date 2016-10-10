/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullConstant;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

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
@RBuiltin(name = "array", kind = INTERNAL, parameterNames = {"data", "dim", "dimnames"}, behavior = PURE)
public abstract class Array extends RBuiltinNode {

    @Child private UpdateDimNames updateDimNames;

    // it's OK for the following method to update dimnames in-place as the container is "fresh"
    private void updateDimNames(RAbstractContainer container, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesNodeGen.create(null));
        }
        updateDimNames.executeRAbstractContainer(container, o);
    }

    private String argType(Object arg) {
        return ((RTypedValue) arg).getRType().getName();
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        Function<Object, Object> argType = this::argType;
        casts.arg("data").defaultError(RError.SHOW_CALLER, RError.Message.MUST_BE_VECTOR_BUT_WAS, "data", argType).mustNotBeNull().mustBe(abstractVectorValue());
        casts.arg("dim").defaultError(RError.SHOW_CALLER, RError.Message.CANNOT_BE_LENGTH, "dims", 0).mustNotBeNull().asIntegerVector().mustBe(notEmpty());
        casts.arg("dimnames").allowNull().shouldBe(instanceOf(RList.class), RError.SHOW_CALLER,
                        RError.Message.GENERIC, "non-list dimnames are disregarded; will be an error in R 3.3.0").mapIf(
                                        instanceOf(RList.class).not(), nullConstant());
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
            throw RError.error(this, RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
        } else if (seenNegative > 0) {
            throw RError.error(this, RError.Message.NEGATIVE_LENGTH_VECTORS_NOT_ALLOWED);
        }
        return totalLength;
    }

    private RIntVector doArrayInt(RAbstractIntVector vec, RAbstractIntVector dim) {
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        int[] data = new int[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createIntVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    protected RIntVector doArrayNoDimNames(RAbstractIntVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        return doArrayInt(vec, dim);
    }

    @Specialization
    protected RIntVector doArray(RAbstractIntVector vec, RAbstractIntVector dim, RList dimnames) {
        RIntVector ret = doArrayInt(vec, dim);
        updateDimNames(ret, dimnames);
        return ret;
    }

    private RDoubleVector doArrayDouble(RAbstractDoubleVector vec, RAbstractIntVector dim) {
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        double[] data = new double[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createDoubleVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    protected RDoubleVector doArrayNoDimNames(RAbstractDoubleVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        return doArrayDouble(vec, dim);
    }

    @Specialization
    protected RDoubleVector doArray(RAbstractDoubleVector vec, RAbstractIntVector dim, RList dimnames) {
        RDoubleVector ret = doArrayDouble(vec, dim);
        updateDimNames(ret, dimnames);
        return ret;
    }

    private RLogicalVector doArrayLogical(RAbstractLogicalVector vec, RAbstractIntVector dim) {
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        byte[] data = new byte[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createLogicalVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    protected RLogicalVector doArrayNoDimNames(RAbstractLogicalVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        return doArrayLogical(vec, dim);
    }

    @Specialization
    protected RLogicalVector doArray(RAbstractLogicalVector vec, RAbstractIntVector dim, RList dimnames) {
        RLogicalVector ret = doArrayLogical(vec, dim);
        updateDimNames(ret, dimnames);
        return ret;
    }

    private RStringVector doArrayString(RAbstractStringVector vec, RAbstractIntVector dim) {
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        String[] data = new String[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createStringVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    protected RStringVector doArrayNoDimNames(RAbstractStringVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        return doArrayString(vec, dim);
    }

    @Specialization
    protected RStringVector doArray(RAbstractStringVector vec, RAbstractIntVector dim, RList dimnames) {
        RStringVector ret = doArrayString(vec, dim);
        updateDimNames(ret, dimnames);
        return ret;
    }

    private RComplexVector doArrayComplex(RAbstractComplexVector vec, RAbstractIntVector dim) {
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        double[] data = new double[totalLength << 1];
        int ind = 0;
        for (int i = 0; i < totalLength; i++) {
            RComplex d = vec.getDataAt(i % vec.getLength());
            data[ind++] = d.getRealPart();
            data[ind++] = d.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(data, vec.isComplete(), dimData);
    }

    @Specialization
    protected RComplexVector doArrayNoDimNames(RAbstractComplexVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        return doArrayComplex(vec, dim);
    }

    @Specialization
    protected RComplexVector doArray(RAbstractComplexVector vec, RAbstractIntVector dim, RList dimnames) {
        RComplexVector ret = doArrayComplex(vec, dim);
        updateDimNames(ret, dimnames);
        return ret;
    }

    private RRawVector doArrayRaw(RAbstractRawVector vec, RAbstractIntVector dim) {
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        byte[] data = new byte[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength()).getValue();
        }
        return RDataFactory.createRawVector(data, dimData);
    }

    @Specialization
    protected RRawVector doArrayNoDimNames(RAbstractRawVector vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        return doArrayRaw(vec, dim);
    }

    @Specialization
    protected RRawVector doArray(RAbstractRawVector vec, RAbstractIntVector dim, RList dimnames) {
        RRawVector ret = doArrayRaw(vec, dim);
        updateDimNames(ret, dimnames);
        return ret;
    }

    private RList doArrayList(RList vec, RAbstractIntVector dim) {
        int[] dimData = new int[dim.getLength()];
        int totalLength = dimDataHelper(dim, dimData);
        Object[] data = new Object[totalLength];
        for (int i = 0; i < totalLength; i++) {
            data[i] = vec.getDataAt(i % vec.getLength());
        }
        return RDataFactory.createList(data, dimData);
    }

    @Specialization
    protected RList doArrayNoDimeNames(RList vec, RAbstractIntVector dim, @SuppressWarnings("unused") RNull dimnames) {
        return doArrayList(vec, dim);
    }

    @Specialization
    protected RList doArray(RList vec, RAbstractIntVector dim, RList dimnames) {
        RList ret = doArrayList(vec, dim);
        updateDimNames(ret, dimnames);
        return ret;
    }
}
