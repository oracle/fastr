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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode.ReduceSemantics;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNodeGen;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Sum has combine semantics (TBD: exactly?) and uses a reduce operation on the resulting array.
 */
@RBuiltin(name = "sum", kind = PRIMITIVE, parameterNames = {"...", "na.rm"}, dispatch = SUMMARY_GROUP_GENERIC, behavior = PURE)
public abstract class Sum extends RBuiltinNode {

    protected static final boolean FULL_PRECISION = FastROptions.FullPrecisionSum.getBooleanValue();

    private static final ReduceSemantics semantics = new ReduceSemantics(0, 0.0, true, null, null, Message.INTEGER_OVERFLOW_USE_SUM_NUMERIC, true, false);

    @Child private UnaryArithmeticReduceNode reduce = UnaryArithmeticReduceNodeGen.create(semantics, BinaryArithmetic.ADD);

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("na.rm").allowNull().asLogicalVector().findFirst().map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    protected static boolean isRDoubleVector(Object value) {
        return value instanceof RDoubleVector;
    }

    @Specialization(guards = {"FULL_PRECISION", "args.getLength() == 1", "isRDoubleVector(args.getArgument(0))", "naRm == cachedNaRm"})
    protected double sumLengthOneRDoubleVector(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean naRm,
                    @Cached("naRm") boolean cachedNaRm,
                    @Cached("create()") VectorLengthProfile lengthProfile,
                    @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                    @Cached("create()") NACheck na,
                    @Cached("createBinaryProfile()") ConditionProfile needsExactSumProfile) {
        RDoubleVector vector = (RDoubleVector) args.getArgument(0);
        int length = lengthProfile.profile(vector.getLength());

        if (needsExactSumProfile.profile(length >= 3)) {
            return RFFIFactory.getRFFI().getMiscRFFI().exactSum(vector.getDataWithoutCopying(), !vector.isComplete(), cachedNaRm);
        } else {
            na.enable(vector);
            loopProfile.profileCounted(length);
            double sum = 0;
            for (int i = 0; loopProfile.inject(i < length); i++) {
                double value = vector.getDataAt(i);
                if (na.check(value)) {
                    if (!cachedNaRm) {
                        return RRuntime.DOUBLE_NA;
                    }
                } else {
                    sum += value;
                }
            }
            return sum;
        }
    }

    @Specialization(contains = "sumLengthOneRDoubleVector", guards = "args.getLength() == 1")
    protected Object sumLengthOne(RArgsValuesAndNames args, boolean naRm) {
        return reduce.executeReduce(args.getArgument(0), naRm, false);
    }

    @Specialization(contains = {"sumLengthOneRDoubleVector", "sumLengthOne"})
    protected Object sum(RArgsValuesAndNames args, boolean naRm, //
                    @Cached("create()") Combine combine) {
        return reduce.executeReduce(combine.executeCombine(args, false), naRm, false);
    }
}
