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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RDispatch.SUMMARY_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode.ReduceSemantics;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

@RBuiltin(name = "range", kind = PRIMITIVE, parameterNames = {"...", "na.rm", "finite"}, dispatch = SUMMARY_GROUP_GENERIC, behavior = PURE)
public abstract class Range extends RBuiltinNode.Arg3 {

    private static final ReduceSemantics minSemantics = new ReduceSemantics(RRuntime.INT_MAX_VALUE, Double.POSITIVE_INFINITY, false, RError.Message.NO_NONMISSING_MIN,
                    RError.Message.NO_NONMISSING_MIN_NA, false, true);
    private static final ReduceSemantics maxSemantics = new ReduceSemantics(RRuntime.INT_MIN_VALUE, Double.NEGATIVE_INFINITY, false, RError.Message.NO_NONMISSING_MAX,
                    RError.Message.NO_NONMISSING_MAX_NA, false, true);

    @Child private UnaryArithmeticReduceNode minReduce = UnaryArithmeticReduceNodeGen.create(minSemantics, BinaryArithmetic.MIN);
    @Child private UnaryArithmeticReduceNode maxReduce = UnaryArithmeticReduceNodeGen.create(maxSemantics, BinaryArithmetic.MAX);

    static {
        Casts casts = new Casts(Range.class);
        casts.arg("na.rm").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("finite").asLogicalVector().findFirst().map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE};
    }

    @Specialization(guards = "args.getLength() == 1")
    protected RVector<?> rangeLengthOne(RArgsValuesAndNames args, boolean naRm, boolean finite) {
        Object min = minReduce.executeReduce(args.getArgument(0), naRm, finite);
        Object max = maxReduce.executeReduce(args.getArgument(0), naRm, finite);
        return createResult(min, max);
    }

    private static RVector<?> createResult(Object min, Object max) {
        if (min instanceof Integer) {
            return RDataFactory.createIntVector(new int[]{(Integer) min, (Integer) max}, false);
        } else if (min instanceof Double) {
            return RDataFactory.createDoubleVector(new double[]{(Double) min, (Double) max}, false);
        } else {
            return RDataFactory.createStringVector(new String[]{(String) min, (String) max}, false);
        }
    }

    @Specialization(replaces = "rangeLengthOne")
    protected RVector<?> range(RArgsValuesAndNames args, boolean naRm, boolean finite,
                    @Cached("create()") Combine combine) {
        Object combined = combine.executeCombine(args, false);
        Object min = minReduce.executeReduce(combined, naRm, finite);
        Object max = maxReduce.executeReduce(combined, naRm, finite);
        return createResult(min, max);
    }
}
