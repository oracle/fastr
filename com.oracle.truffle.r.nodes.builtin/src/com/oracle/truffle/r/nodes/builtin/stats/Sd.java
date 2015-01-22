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
package com.oracle.truffle.r.nodes.builtin.stats;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin(name = "sd", kind = SUBSTITUTE, parameterNames = {"x", "na.rm"})
// TODO Implement in R
public abstract class Sd extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
    }

    @Child private Mean mean = MeanFactory.create(new RNode[2], getBuiltin(), getSuppliedArgsNames());
    @Child private Sqrt sqrt = SqrtFactory.create(new RNode[1], getBuiltin(), getSuppliedArgsNames());
    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();
    @Child private BinaryArithmetic sub = BinaryArithmetic.SUBTRACT.create();
    @Child private BinaryArithmetic pow = BinaryArithmetic.POW.create();
    @Child private BinaryArithmetic div = BinaryArithmetic.DIV.create();

    @Specialization
    @SuppressWarnings("unused")
    protected double sd(VirtualFrame frame, RDoubleVector x, byte narm) {
        controlVisibility();

        double xmean = (double) mean.executeDouble(frame, x);
        double distSum = 0.0;
        for (int i = 0; i < x.getLength(); ++i) {
            distSum = add.op(distSum, pow.op(sub.op(x.getDataAt(i), xmean), 2));
        }
        return sqrt.sqrt(div.op(distSum, x.getLength() - 1));
    }

    @Specialization
    protected double sd(VirtualFrame frame, RDoubleVector x, RArgsValuesAndNames rargs) {
        if (rargs.getValues().length > 0) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.unimplemented();
        }
        return sd(frame, x, RRuntime.LOGICAL_FALSE);
    }
}
