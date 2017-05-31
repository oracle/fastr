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
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_SUMMARY;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

@RBuiltin(name = "mean", kind = INTERNAL, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC, behavior = PURE_SUMMARY)
public abstract class Mean extends RBuiltinNode.Arg1 {

    private final BranchProfile emptyProfile = BranchProfile.create();

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.createOperation();
    @Child private BinaryArithmetic div = BinaryArithmetic.DIV.createOperation();

    static {
        Casts.noCasts(Mean.class);
    }

    @Specialization
    protected double mean(RAbstractDoubleVector x) {
        if (x.getLength() == 0) {
            emptyProfile.enter();
            return Double.NaN;
        }
        double sum = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            sum = add.op(sum, x.getDataAt(k));
        }
        return div.op(sum, x.getLength());
    }

    @Specialization
    protected double mean(RAbstractIntVector x) {
        if (x.getLength() == 0) {
            emptyProfile.enter();
            return Double.NaN;
        }
        double sum = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            sum = add.op(sum, x.getDataAt(k));
        }
        return div.op(sum, x.getLength());
    }

    @Specialization
    protected double mean(RAbstractLogicalVector x) {
        if (x.getLength() == 0) {
            emptyProfile.enter();
            return Double.NaN;
        }
        double sum = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            sum = add.op(sum, x.getDataAt(k));
        }
        return div.op(sum, x.getLength());
    }

    @Specialization
    protected RComplex mean(RAbstractComplexVector x) {
        if (x.getLength() == 0) {
            emptyProfile.enter();
            return RDataFactory.createComplex(Double.NaN, Double.NaN);
        }
        RComplex sum = x.getDataAt(0);
        RComplex comp;
        for (int k = 1; k < x.getLength(); k++) {
            comp = x.getDataAt(k);
            sum = add.op(sum.getRealPart(), sum.getImaginaryPart(), comp.getRealPart(), comp.getImaginaryPart());
        }
        return div.op(sum.getRealPart(), sum.getImaginaryPart(), x.getLength(), 0);
    }
}
