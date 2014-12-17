/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.Lapply.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * The {@code vapply} builtin. The closure definition for {@code vapply} is
 * {@code unction(X, FUN, FUN.VALUE, ...,  USE.NAMES = TRUE)}. The {@code .Internal} call in
 * {@code sapply.R} is {@code .Internal(vapply(X, FUN, FUN.VALUE, USE.NAMES))}. I.e., the "..." is
 * not passed even though, for correct operation, any extra arguments must be passed to {@code FUN}.
 * Compare with similar code for {@code lapply} which does pass the "...". In order for FastR to use
 * the same version of {@code sapply.R} as GnuR, we have to define the specialization signature
 * without the "...", which means we have to fish the optional arguments out of the frame for the
 * closure.
 *
 * TODO Set dimnames on result if necessary.
 */
@RBuiltin(name = "vapply", kind = INTERNAL, parameterNames = {"X", "FUN", "FUN.VALUE", "USE.NAMES"})
public abstract class VApply extends RBuiltinNode {

    private final ValueProfile funValueProfile = ValueProfile.createClassProfile();
    private final ConditionProfile useNamesProfile = ConditionProfile.createBinaryProfile();

    @Child private GeneralLApplyNode doApply = new GeneralLApplyNode();

    @Specialization
    protected Object vapply(VirtualFrame frame, RAbstractVector vec, RFunction fun, Object funValue, byte useNames) {
        controlVisibility();
        RArgsValuesAndNames optionalArgs = (RArgsValuesAndNames) RArguments.getArgument(frame, 3);
        return delegateToLapply(frame, vec, fun, funValue, useNames, optionalArgs);
    }

    private RVector delegateToLapply(VirtualFrame frame, RAbstractVector vec, RFunction fun, Object funValueArg, byte useNames, RArgsValuesAndNames optionalArgs) {
        RVector vecMat = vec.materialize();
        Object[] applyResult = doApply.execute(frame, vecMat, fun, optionalArgs);

        RVector result = null;
        boolean applyResultZeroLength = applyResult.length == 0;
        Object funValue = funValueProfile.profile(funValueArg);
        if (funValue instanceof Integer) {
            int[] data = applyResultZeroLength ? new int[0] : convertInt(applyResult);
            result = RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValue instanceof Double) {
            double[] data = applyResultZeroLength ? new double[0] : convertDouble(applyResult);
            result = RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValue instanceof Byte) {
            byte[] data = applyResultZeroLength ? new byte[0] : convertByte(applyResult);
            result = RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValue instanceof String) {
            String[] data = applyResultZeroLength ? new String[0] : convertString(applyResult);
            result = RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValue instanceof RComplex) {
            double[] data = applyResultZeroLength ? new double[0] : convertComplex(applyResult);
            result = RDataFactory.createComplexVector(data, RDataFactory.COMPLETE_VECTOR);
        } else {
            assert false;
        }

        if (useNamesProfile.profile(RRuntime.fromLogical(useNames))) {
            Object names = vecMat.getNames();
            Object newNames = null;
            if (names != RNull.instance) {
                newNames = names;
            } else if (vecMat instanceof RStringVector) {
                newNames = ((RStringVector) vecMat).copy();
            }
            if (newNames != null) {
                result.setNames(newNames);
            }
        }
        return result;
    }

    @TruffleBoundary
    private static double[] convertDouble(Object[] values) {
        double[] newArray = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            newArray[i] = (double) values[i];
        }
        return newArray;
    }

    @TruffleBoundary
    private static int[] convertInt(Object[] values) {
        int[] newArray = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            newArray[i] = (int) values[i];
        }
        return newArray;
    }

    @TruffleBoundary
    private static byte[] convertByte(Object[] values) {
        byte[] newArray = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            newArray[i] = (byte) values[i];
        }
        return newArray;
    }

    @TruffleBoundary
    private static String[] convertString(Object[] values) {
        String[] newArray = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            newArray[i] = (String) values[i];
        }
        return newArray;
    }

    @TruffleBoundary
    private static double[] convertComplex(Object[] values) {
        double[] newArray = new double[values.length * 2];
        for (int i = 0; i < values.length; i++) {
            int index = i << 1;
            newArray[index] = ((RComplex) values[i]).getRealPart();
            newArray[index + 1] = ((RComplex) values[i]).getImaginaryPart();
        }
        return newArray;
    }

}
