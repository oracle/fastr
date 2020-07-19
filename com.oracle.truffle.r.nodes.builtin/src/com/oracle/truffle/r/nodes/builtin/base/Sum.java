/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode.ReduceSemantics;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RDispatch.SUMMARY_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_SUMMARY;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;
import static com.oracle.truffle.r.runtime.context.FastROptions.FullPrecisionSum;

/**
 * Sum has combine semantics (TBD: exactly?) and uses a reduce operation on the resulting array.
 */
@RBuiltin(name = "sum", kind = PRIMITIVE, parameterNames = {"...", "na.rm"}, dispatch = SUMMARY_GROUP_GENERIC, behavior = PURE_SUMMARY)
@ImportStatic(AltrepUtilities.class)
public abstract class Sum extends RBuiltinNode.Arg2 {

    private static final ReduceSemantics semantics = new ReduceSemantics(0, 0.0, true, null, null, true, false, false);

    @Child private UnaryArithmeticReduceNode reduce = UnaryArithmeticReduceNodeGen.create(semantics, BinaryArithmetic.ADD);

    static {
        Casts casts = new Casts(Sum.class);
        casts.arg("na.rm").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    protected static boolean isRDoubleVector(Object value) {
        return value instanceof RDoubleVector;
    }

    protected boolean fullPrecision() {
        return RContext.getInstance().getOption(FullPrecisionSum);
    }

    @Child private MiscRFFI.ExactSumNode exactSumNode;

    @Specialization(guards = {"fullPrecision()", "args.getLength() == 1", "isRDoubleVector(args.getArgument(0))", "naRm == cachedNaRm"})
    protected double sumLengthOneRDoubleVector(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean naRm,
                    @Cached("create()") GetReadonlyData.Double vectorToArrayNode,
                    @Cached("naRm") boolean cachedNaRm,
                    @Cached("create()") VectorLengthProfile lengthProfile,
                    @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
                    @Cached("create()") NACheck na,
                    @Cached("createBinaryProfile()") ConditionProfile needsExactSumProfile) {
        RDoubleVector vector = (RDoubleVector) args.getArgument(0);
        int length = lengthProfile.profile(vector.getLength());

        if (needsExactSumProfile.profile(length >= 3)) {
            if (exactSumNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                exactSumNode = (MiscRFFI.ExactSumNode) insert((Node) MiscRFFI.ExactSumNode.create());
            }
            return exactSumNode.execute(vectorToArrayNode.execute(vector), !vector.isComplete(), cachedNaRm);
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

    /**
     * The behavior of this specialization for altrep instances is currently copied from GNU-R (summary.c) and is
     * as follows:
     * <ol>
     *     <li>
     *         If sum gets one altrep instance argument, it dispatches to Sum altrep method.
     *     </li>
     *     <li>
     *         If sum gets more instances (both altrep instances and "normal" instances) it processess them with
     *         <code>ITERATE_BY_REGION</code> and does not dispatch to Sum altrep method at all.
     *     </li>
     * </ol>
     *
     * Note that this behavior is same for S4 instances - when there is just one S4 instance argument, sum dispatches
     * to corresponding method, if there are more S4 instances, no dispatching is done.
     */
    @Specialization(replaces = "sumLengthOneRDoubleVector",
            guards = {"args.getLength() == 1", "isAltrep(args.getArgument(0))", "hasSumMethodRegistered(args.getArgument(0))"})
    protected Object sumLengthOneAltrep(RArgsValuesAndNames args, boolean naRm,
                                        @Cached AltrepRFFI.SumNode sumNode) {
        return sumNode.execute(args.getArgument(0), naRm);
    }

    @Specialization(replaces = {"sumLengthOneRDoubleVector", "sumLengthOneAltrep"}, guards = "args.getLength() == 1")
    protected Object sumLengthOne(RArgsValuesAndNames args, boolean naRm) {
        return reduce.executeReduce(args.getArgument(0), naRm, false);
    }

    @Specialization(replaces = {"sumLengthOneRDoubleVector", "sumLengthOneAltrep", "sumLengthOne"})
    protected Object sum(RArgsValuesAndNames args, boolean naRm,
                    @Cached("create()") Combine combine) {
        return reduce.executeReduce(combine.executeCombine(args, false), naRm, false);
    }
}
