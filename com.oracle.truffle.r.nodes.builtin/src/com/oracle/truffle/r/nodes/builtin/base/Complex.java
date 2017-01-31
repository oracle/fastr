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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "complex", kind = INTERNAL, parameterNames = {"length.out", "real", "imaginary"}, behavior = PURE)
public abstract class Complex extends RBuiltinNode {

    static {
        Casts casts = new Casts(Complex.class);
        casts.arg("length.out").asIntegerVector().findFirst(Message.INVALID_LENGTH);
        casts.arg("real").mapNull(emptyDoubleVector()).asDoubleVector();
        casts.arg("imaginary").mapNull(emptyDoubleVector()).asDoubleVector();
    }

    @Specialization
    protected RComplexVector complex(int lengthOut, RAbstractDoubleVector real, RAbstractDoubleVector imaginary,
                    @Cached("create()") NACheck realNA,
                    @Cached("create()") NACheck imaginaryNA,
                    @Cached("create()") VectorLengthProfile realLengthProfile,
                    @Cached("create()") VectorLengthProfile imaginaryLengthProfile,
                    @Cached("create()") VectorLengthProfile lengthProfile,
                    @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        int realLength = realLengthProfile.profile(real.getLength());
        int imaginaryLength = imaginaryLengthProfile.profile(imaginary.getLength());
        int length = lengthProfile.profile(Math.max(Math.max(lengthOut, realLength), imaginaryLength));
        double[] data = new double[length << 1];
        realNA.enable(real);
        imaginaryNA.enable(imaginary);
        loopProfile.profileCounted(length);
        for (int i = 0; loopProfile.inject(i < data.length); i += 2) {
            double realValue = realLength == 0 ? 0 : real.getDataAt((i >> 1) % realLength);
            double imaginaryValue = imaginaryLength == 0 ? 0 : imaginary.getDataAt((i >> 1) % imaginaryLength);
            data[i] = realValue;
            data[i + 1] = imaginaryValue;
            realNA.check(realValue);
            imaginaryNA.check(imaginaryValue);
        }
        return RDataFactory.createComplexVector(data, realNA.neverSeenNA() && imaginaryNA.neverSeenNA());
    }
}
