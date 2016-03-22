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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

@RBuiltin(name = "complex", kind = INTERNAL, parameterNames = {"length.out", "real", "imaginary"})
public abstract class Complex extends RBuiltinNode {

    private static final RDoubleVector ZERO = RDataFactory.createDoubleVectorFromScalar(0.0);

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(0);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "resultEmpty(lengthOut, realAbsVec, imaginaryAbsVec)")
    protected RComplexVector complexEmpty(int lengthOut, RAbstractDoubleVector realAbsVec, RAbstractDoubleVector imaginaryAbsVec) {
        return RDataFactory.createEmptyComplexVector();
    }

    @Specialization(guards = "!resultEmpty(lengthOut, realAbsVec, imaginaryAbsVec)")
    protected RComplexVector complex(int lengthOut, RAbstractDoubleVector realAbsVec, RAbstractDoubleVector imaginaryAbsVec) {
        controlVisibility();
        RDoubleVector real = checkLength(realAbsVec);
        RDoubleVector imaginary = checkLength(imaginaryAbsVec);
        int realLength = real.getLength();
        int imaginaryLength = imaginary.getLength();
        int length = Math.max(Math.max(realLength, imaginaryLength), lengthOut);
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        double[] data = new double[length << 1];
        for (int i = 0; i < data.length; i += 2) {
            data[i] = real.getDataAt((i >> 1) % realLength);
            data[i + 1] = imaginary.getDataAt((i >> 1) % imaginaryLength);
            if (RRuntime.isNA(data[i]) || RRuntime.isNA(data[i + 1])) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
        }
        return RDataFactory.createComplexVector(data, complete);
    }

    private static RDoubleVector checkLength(RAbstractDoubleVector v) {
        if (v.getLength() == 0) {
            return ZERO;
        } else {
            return v.materialize();
        }
    }

    protected static boolean resultEmpty(int lengthOut, RAbstractDoubleVector realAbsVec, RAbstractDoubleVector imaginaryAbsVec) {
        return lengthOut == 0 && realAbsVec.getLength() == 0 && imaginaryAbsVec.getLength() == 0;
    }
}
