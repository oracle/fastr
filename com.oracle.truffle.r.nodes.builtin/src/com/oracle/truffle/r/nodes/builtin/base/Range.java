/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
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
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

@RBuiltin(name = "range", kind = PRIMITIVE, parameterNames = {"...", "na.rm", "finite"}, dispatch = SUMMARY_GROUP_GENERIC, behavior = PURE)
public abstract class Range extends RBuiltinNode.Arg3 {

    /*
     * Note: the GNU-R implementation forces promises and then just calls range.default. It seems
     * safe to implement our version of range.default here, as the RCallNode would itself dispatch
     * to "range.default" if the dispatching object has some class and otherwise to the builtin.
     * This means that if there is a chance that "c", "max" and other primitives used in
     * "range.default" would dispatch to some crazy S3 overloads, the control flow should not reach
     * to this builtin anyway.
     */

    private static final ReduceSemantics minSemantics = new ReduceSemantics(RRuntime.INT_MAX_VALUE, Double.POSITIVE_INFINITY, false, RError.Message.NO_NONMISSING_MIN,
                    RError.Message.NO_NONMISSING_MIN_NA, null, false, true, true);
    private static final ReduceSemantics maxSemantics = new ReduceSemantics(RRuntime.INT_MIN_VALUE, Double.NEGATIVE_INFINITY, false, RError.Message.NO_NONMISSING_MAX,
                    RError.Message.NO_NONMISSING_MAX_NA, null, false, true, true);

    static {
        Casts casts = new Casts(Range.class);
        casts.arg("na.rm").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("finite").asLogicalVector().findFirst().map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE};
    }

    @Specialization(guards = {"args.getLength() == 1", "isAtomicVector(args.getArgument(0))"})
    protected RAbstractVector rangeLengthOne(RArgsValuesAndNames args, boolean naRm, boolean finite,
                    @Cached("createMinReduce()") UnaryArithmeticReduceNode minReduce,
                    @Cached("createMaxReduce()") UnaryArithmeticReduceNode maxReduce) {
        Object min = minReduce.executeReduce(args.getArgument(0), naRm || finite, finite);
        Object max = maxReduce.executeReduce(args.getArgument(0), naRm || finite, finite);
        return createResult(min, max);
    }

    @Specialization(replaces = "rangeLengthOne")
    protected RAbstractVector range(RArgsValuesAndNames args, boolean naRm, boolean finite,
                    @Cached("createMinReduce()") UnaryArithmeticReduceNode minReduce,
                    @Cached("createMaxReduce()") UnaryArithmeticReduceNode maxReduce,
                    @Cached("create()") Combine combine) {
        Object combined = combine.executeCombine(args, true);
        Object min = minReduce.executeReduce(combined, naRm || finite, finite);
        Object max = maxReduce.executeReduce(combined, naRm || finite, finite);
        return createResult(min, max);
    }

    private static RAbstractVector createResult(Object min, Object max) {
        if (min instanceof Integer) {
            return RDataFactory.createIntVector(new int[]{(Integer) min, (Integer) max}, false);
        } else if (min instanceof Double) {
            return RDataFactory.createDoubleVector(new double[]{(Double) min, (Double) max}, false);
        } else {
            return RDataFactory.createStringVector(new String[]{(String) min, (String) max}, false);
        }
    }

    protected static boolean isAtomicVector(Object vec) {
        return vec instanceof RAbstractAtomicVector;
    }

    protected UnaryArithmeticReduceNode createMinReduce() {
        return UnaryArithmeticReduceNodeGen.create(minSemantics, BinaryArithmetic.MIN);
    }

    protected UnaryArithmeticReduceNode createMaxReduce() {
        return UnaryArithmeticReduceNodeGen.create(maxSemantics, BinaryArithmetic.MAX);
    }
}
