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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@ImportStatic(RRuntime.class)
@RBuiltin(name = "is.na", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class IsNA extends RBuiltinNode.Arg1 {

    @Child private IsNA recursiveIsNA;
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
    @Child private GetDimAttributeNode getDimsNode = GetDimAttributeNode.create();
    @Child private SetDimNamesAttributeNode setDimNamesNode = SetDimNamesAttributeNode.create();
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();

    private final ConditionProfile nullDimNamesProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts.noCasts(IsNA.class);
    }

    private Object isNARecursive(Object o) {
        if (recursiveIsNA == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveIsNA = insert(IsNANodeGen.create());
        }
        return recursiveIsNA.execute(o);
    }

    public abstract Object execute(Object o);

    @Specialization
    protected byte isNA(int value) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected RLogicalVector isNA(RAbstractIntVector vector) {
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = RRuntime.asLogical(RRuntime.isNA(vector.getDataAt(i)));
        }
        return createResult(resultVector, vector);
    }

    @Specialization
    protected byte isNA(double value) {
        return RRuntime.asLogical(RRuntime.isNAorNaN(value));
    }

    @Specialization
    protected RLogicalVector isNA(RAbstractDoubleVector vector) {
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = RRuntime.asLogical(RRuntime.isNAorNaN(vector.getDataAt(i)));
        }
        return createResult(resultVector, vector);
    }

    @Specialization
    protected RLogicalVector isNA(RComplexVector vector) {
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            RComplex complex = vector.getDataAt(i);
            resultVector[i] = RRuntime.asLogical(RRuntime.isNA(complex));
        }
        return createResult(resultVector, vector);
    }

    @Specialization
    protected byte isNA(String value) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected RLogicalVector isNA(RStringVector vector) {
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = RRuntime.asLogical(RRuntime.isNA(vector.getDataAt(i)));
        }
        return createResult(resultVector, vector);
    }

    @Specialization
    protected RLogicalVector isNA(RList list) {
        byte[] resultVector = new byte[list.getLength()];
        for (int i = 0; i < list.getLength(); i++) {
            Object result = isNARecursive(list.getDataAt(i));
            byte isNAResult;
            if (result instanceof Byte) {
                isNAResult = (Byte) result;
            } else if (result instanceof RLogicalVector) {
                RLogicalVector vector = (RLogicalVector) result;
                // result is false unless that element is a length-one atomic vector
                // and the single element of that vector is regarded as NA
                isNAResult = (vector.getLength() == 1) ? vector.getDataAt(0) : RRuntime.LOGICAL_FALSE;
            } else {
                throw fail("unhandled return type in isNA(list)");
            }
            resultVector[i] = isNAResult;
        }
        return RDataFactory.createLogicalVector(resultVector, RDataFactory.COMPLETE_VECTOR);
    }

    @TruffleBoundary
    private static UnsupportedOperationException fail(String message) {
        throw new UnsupportedOperationException(message);
    }

    @Specialization
    protected byte isNA(byte value) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected RLogicalVector isNA(RLogicalVector vector) {
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = (RRuntime.isNA(vector.getDataAt(i)) ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE);
        }
        return createResult(resultVector, vector);
    }

    @Specialization
    protected byte isNA(RComplex value) {
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RRaw value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected RLogicalVector isNA(RRawVector vector) {
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = RRuntime.LOGICAL_FALSE;
        }
        return createResult(resultVector, vector);
    }

    @Specialization
    protected RLogicalVector isNA(RNull value) {
        warning(RError.Message.IS_NA_TO_NON_VECTOR, value.getRType().getName());
        return RDataFactory.createEmptyLogicalVector();
    }

    @Specialization(guards = "isForeignObject(obj)")
    protected byte isNA(@SuppressWarnings("unused") TruffleObject obj) {
        return RRuntime.LOGICAL_FALSE;
    }

    // Note: all the primitive values have specialization, so we can only get RTypedValue in
    // fallback
    @Fallback
    protected byte isNA(Object value) {
        warning(RError.Message.IS_NA_TO_NON_VECTOR, ((RTypedValue) value).getRType().getName());
        return RRuntime.LOGICAL_FALSE;
    }

    private RLogicalVector createResult(byte[] data, RAbstractVector originalVector) {
        RLogicalVector result = RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR, getDimsNode.getDimensions(originalVector), getNamesNode.getNames(originalVector));
        RList dimNames = getDimNamesNode.getDimNames(originalVector);
        if (nullDimNamesProfile.profile(dimNames != null)) {
            setDimNamesNode.setDimNames(result, dimNames);
        }
        return result;
    }
}
