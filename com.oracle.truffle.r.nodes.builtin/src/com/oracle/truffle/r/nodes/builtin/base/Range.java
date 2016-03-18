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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode.ReduceSemantics;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

/**
 * Sum has combine semantics (TBD: exactly?) and uses a reduce operation on the resulting array.
 */
@RBuiltin(name = "range", kind = PRIMITIVE, parameterNames = {"...", "na.rm", "finite"})
public abstract class Range extends RBuiltinNode {

    private static final ReduceSemantics minSemantics = new ReduceSemantics(RRuntime.INT_MAX_VALUE, Double.POSITIVE_INFINITY, false, RError.Message.NO_NONMISSING_MIN,
                    RError.Message.NO_NONMISSING_MIN_NA, false, true);
    private static final ReduceSemantics maxSemantics = new ReduceSemantics(RRuntime.INT_MIN_VALUE, Double.NEGATIVE_INFINITY, false, RError.Message.NO_NONMISSING_MAX,
                    RError.Message.NO_NONMISSING_MAX_NA, false, true);

    @Child private UnaryArithmeticReduceNode minReduce = UnaryArithmeticReduceNodeGen.create(minSemantics, BinaryArithmetic.MIN);
    @Child private UnaryArithmeticReduceNode maxReduce = UnaryArithmeticReduceNodeGen.create(maxSemantics, BinaryArithmetic.MAX);

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.firstBoolean(1).firstBoolean(2);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE};
    }

    @Specialization(guards = "args.getLength() == 1")
    protected RVector rangeLengthOne(RArgsValuesAndNames args, boolean naRm, boolean finite) {
        Object min = minReduce.executeReduce(args.getArgument(0), naRm, finite);
        Object max = maxReduce.executeReduce(args.getArgument(0), naRm, finite);
        return createResult(min, max);
    }

    private static RVector createResult(Object min, Object max) {
        if (min instanceof Integer) {
            return RDataFactory.createIntVector(new int[]{(Integer) min, (Integer) max}, false);
        } else {
            return RDataFactory.createDoubleVector(new double[]{(Double) min, (Double) max}, false);
        }
    }

    @Specialization(contains = "rangeLengthOne")
    protected RVector range(RArgsValuesAndNames args, boolean naRm, boolean finite, //
                    @Cached("create()") Combine combine) {
        Object combined = combine.executeCombine(args);
        Object min = minReduce.executeReduce(combined, naRm, finite);
        Object max = maxReduce.executeReduce(combined, naRm, finite);
        return createResult(min, max);
    }
}
