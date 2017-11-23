/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

@RBuiltin(name = "array", kind = INTERNAL, parameterNames = {"data", "dim", "dimnames"}, behavior = PURE)
public abstract class Array extends RBuiltinNode.Arg3 {

    @Child private UpdateDimNames updateDimNames;

    // it's OK for the following method to update dimnames in-place as the container is "fresh"
    private void updateDimNames(RAbstractContainer container, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesNodeGen.create());
        }
        updateDimNames.executeRAbstractContainer(container, o);
    }

    static {
        Casts casts = new Casts(Array.class);
        casts.arg("data").defaultError(RError.Message.MUST_BE_VECTOR_BUT_WAS, "data", typeName()).mustBe(abstractVectorValue());
        casts.arg("dim").defaultError(RError.Message.CANNOT_BE_LENGTH, "dims", 0).mustNotBeNull().asIntegerVector().mustBe(notEmpty());
        casts.arg("dimnames").defaultError(RError.Message.DIMNAMES_LIST).allowNull().mustBe(instanceOf(RList.class));
    }

    @Specialization(guards = {"dataAccess.supports(data)", "dimAccess.supports(dim)"})
    protected RAbstractVector arrayCached(RAbstractVector data, RAbstractIntVector dim, Object dimNames,
                    @Cached("data.access()") VectorAccess dataAccess,
                    @Cached("dim.access()") VectorAccess dimAccess,
                    @Cached("createNew(dataAccess.getType())") VectorAccess resultAccess,
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("createBinaryProfile()") ConditionProfile isEmpty,
                    @Cached("create()") VectorFactory factory) {
        // extract dimensions and compute total length
        int[] dimArray;
        int totalLength = 1;
        boolean negativeDims = false;
        try (SequentialIterator dimIter = dimAccess.access(dim)) {
            dimArray = new int[dimAccess.getLength(dimIter)];
            while (dimAccess.next(dimIter)) {
                int dimValue = dimAccess.getInt(dimIter);
                if (dimValue < 0) {
                    negativeDims = true;
                }
                totalLength *= dimValue;
                dimArray[dimIter.getIndex()] = dimValue;
            }
        }
        if (totalLength < 0) {
            throw error(RError.Message.NEGATIVE_LENGTH_VECTORS_NOT_ALLOWED);
        } else if (negativeDims) {
            throw error(RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
        }

        RAbstractVector result = factory.createUninitializedVector(dataAccess.getType(), totalLength, dimArray, null, null);

        try (SequentialIterator resultIter = resultAccess.access(result); SequentialIterator dataIter = dataAccess.access(data)) {
            if (isEmpty.profile(dataAccess.getLength(dataIter) == 0)) {
                if (dataAccess.getType() == RType.Character) {
                    // character vectors are initialized with "" instead of NA
                    while (resultAccess.next(resultIter)) {
                        resultAccess.setString(resultIter, "");
                    }
                    result.setComplete(true);
                } else {
                    while (resultAccess.next(resultIter)) {
                        resultAccess.setNA(resultIter);
                    }
                    result.setComplete(false);
                }
            } else {
                while (resultAccess.next(resultIter)) {
                    dataAccess.nextWithWrap(dataIter);
                    resultAccess.setFromSameType(resultIter, dataAccess, dataIter);
                }
                result.setComplete(!dataAccess.na.isEnabled());
            }
        }

        // dimensions are set as a separate step so they are checked for validity
        if (hasDimNames.profile(dimNames instanceof RList)) {
            updateDimNames(result, dimNames);
        } else {
            assert dimNames instanceof RNull;
        }
        return result;
    }

    @Specialization(replaces = "arrayCached")
    @TruffleBoundary
    protected RAbstractVector arrayGeneric(RAbstractVector data, RAbstractIntVector dim, Object dimNames,
                    @Cached("createBinaryProfile()") ConditionProfile hasDimNames,
                    @Cached("createBinaryProfile()") ConditionProfile isEmpty,
                    @Cached("create()") VectorFactory factory) {
        VectorAccess dataAccess = data.slowPathAccess();
        return arrayCached(data, dim, dimNames, dataAccess, dim.slowPathAccess(), VectorAccess.createSlowPathNew(dataAccess.getType()), hasDimNames, isEmpty, factory);
    }
}
