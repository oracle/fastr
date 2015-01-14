/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin(name = "mean", kind = INTERNAL, parameterNames = {"x"})
@GenerateNodeFactory
public abstract class Mean extends RBuiltinNode {

    private final BranchProfile emptyProfile = BranchProfile.create();

    public abstract Object executeDouble(VirtualFrame frame, RDoubleVector x);

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();
    @Child private BinaryArithmetic div = BinaryArithmetic.DIV.create();

    @Specialization
    protected double mean(RAbstractDoubleVector x) {
        controlVisibility();
        if (x.getLength() == 0) {
            emptyProfile.enter();
            return Double.NaN;
        }
        double sum = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); ++k) {
            sum = add.op(sum, x.getDataAt(k));
        }
        return div.op(sum, x.getLength());
    }

    @Specialization
    protected double mean(RAbstractIntVector x) {
        controlVisibility();
        if (x.getLength() == 0) {
            emptyProfile.enter();
            return Double.NaN;
        }
        double sum = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); ++k) {
            sum = add.op(sum, x.getDataAt(k));
        }
        return div.op(sum, x.getLength());
    }

    @Specialization
    protected double mean(RAbstractLogicalVector x) {
        controlVisibility();
        if (x.getLength() == 0) {
            emptyProfile.enter();
            return Double.NaN;
        }
        double sum = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); ++k) {
            sum = add.op(sum, x.getDataAt(k));
        }
        return div.op(sum, x.getLength());
    }

    @Specialization
    protected RComplex mean(RAbstractComplexVector x) {
        controlVisibility();
        if (x.getLength() == 0) {
            emptyProfile.enter();
            return RDataFactory.createComplex(Double.NaN, Double.NaN);
        }
        RComplex sum = x.getDataAt(0);
        RComplex comp;
        for (int k = 1; k < x.getLength(); ++k) {
            comp = x.getDataAt(k);
            sum = add.op(sum.getRealPart(), sum.getImaginaryPart(), comp.getRealPart(), comp.getImaginaryPart());
        }
        return div.op(sum.getRealPart(), sum.getImaginaryPart(), x.getLength(), 0);
    }
}
