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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "is.na", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class IsNA extends RBuiltinNode {

    @Child private IsNA recursiveIsNA;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    private Object isNARecursive(Object o) {
        if (recursiveIsNA == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            recursiveIsNA = insert(IsNANodeGen.create(new RNode[1], null, null));
        }
        return recursiveIsNA.execute(o);
    }

    public abstract Object execute(Object o);

    @Specialization
    protected byte isNA(int value) {
        controlVisibility();
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected RLogicalVector isNA(RAbstractIntVector vector) {
        controlVisibility();
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = RRuntime.asLogical(RRuntime.isNA(vector.getDataAt(i)));
        }
        return RDataFactory.createLogicalVector(resultVector, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames(attrProfiles));
    }

    @Specialization
    protected byte isNA(double value) {
        controlVisibility();
        return RRuntime.asLogical(RRuntime.isNAorNaN(value));
    }

    @Specialization
    protected RLogicalVector isNA(RAbstractDoubleVector vector) {
        controlVisibility();
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = RRuntime.asLogical(RRuntime.isNAorNaN(vector.getDataAt(i)));
        }
        return RDataFactory.createLogicalVector(resultVector, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames(attrProfiles));
    }

    @Specialization
    protected RLogicalVector isNA(RComplexVector vector) {
        controlVisibility();
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            RComplex complex = vector.getDataAt(i);
            resultVector[i] = RRuntime.asLogical(RRuntime.isNAorNaN(complex.getRealPart()) || RRuntime.isNAorNaN(complex.getImaginaryPart()));
        }
        return RDataFactory.createLogicalVector(resultVector, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames(attrProfiles));
    }

    @Specialization
    protected byte isNA(String value) {
        controlVisibility();
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected RLogicalVector isNA(RStringVector vector) {
        controlVisibility();
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = RRuntime.asLogical(RRuntime.isNA(vector.getDataAt(i)));
        }
        return RDataFactory.createLogicalVector(resultVector, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames(attrProfiles));
    }

    @Specialization
    protected RLogicalVector isNA(RList list) {
        controlVisibility();
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
        controlVisibility();
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected RLogicalVector isNA(RLogicalVector vector) {
        controlVisibility();
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = (RRuntime.isNA(vector.getDataAt(i)) ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE);
        }
        return RDataFactory.createLogicalVector(resultVector, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames(attrProfiles));
    }

    @Specialization
    protected byte isNA(RComplex value) {
        controlVisibility();
        return RRuntime.asLogical(RRuntime.isNA(value));
    }

    @Specialization
    protected RLogicalVector isNA(RFactor value) {
        return isNA(value.getVector());
    }

    @Specialization
    protected byte isNA(@SuppressWarnings("unused") RRaw value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected RLogicalVector isNA(RRawVector vector) {
        controlVisibility();
        byte[] resultVector = new byte[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            resultVector[i] = RRuntime.LOGICAL_FALSE;
        }
        return RDataFactory.createLogicalVector(resultVector, RDataFactory.COMPLETE_VECTOR, vector.getDimensions(), vector.getNames(attrProfiles));
    }

    @Specialization
    protected RLogicalVector isNA(RNull value) {
        controlVisibility();
        RError.warning(this, RError.Message.IS_NA_TO_NON_VECTOR, value.getRType().getName());
        return RDataFactory.createEmptyLogicalVector();
    }

    @Specialization
    protected byte isNA(RLanguage value) {
        controlVisibility();
        RError.warning(this, RError.Message.IS_NA_TO_NON_VECTOR, value.getRType().getName());
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isNA(RFunction value) {
        controlVisibility();
        RError.warning(this, RError.Message.IS_NA_TO_NON_VECTOR, value.getRType().getName());
        return RRuntime.LOGICAL_FALSE;
    }
}
