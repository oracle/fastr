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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode.ReduceSemantics;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RDispatch.SUMMARY_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_SUMMARY;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

@RBuiltin(name = "min", kind = PRIMITIVE, parameterNames = {"...", "na.rm"}, dispatch = SUMMARY_GROUP_GENERIC, behavior = PURE_SUMMARY)
@ImportStatic(AltrepUtilities.class)
public abstract class Min extends RBuiltinNode.Arg2 {

    private static final ReduceSemantics semantics = new ReduceSemantics(RRuntime.INT_MAX_VALUE, Double.POSITIVE_INFINITY, false, RError.Message.NO_NONMISSING_MIN, RError.Message.NO_NONMISSING_MIN_NA,
                    false, true, true);

    @Child private UnaryArithmeticReduceNode reduce = UnaryArithmeticReduceNodeGen.create(semantics, BinaryArithmetic.MIN);

    static {
        Casts casts = new Casts(Min.class);
        casts.arg("na.rm").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    @Specialization(guards = {"args.getLength() == 1", "!isAltrep(args.getArgument(0))"})
    protected Object minLengthOne(RArgsValuesAndNames args, boolean naRm) {
        return reduce.executeReduce(args.getArgument(0), naRm, false);
    }

    @Specialization(guards = {"args.getLength() == 1", "isAltrep(args.getArgument(0))", "hasMinMethodRegistered(args.getArgument(0))"})
    protected Object minLengthOneAltrep(RArgsValuesAndNames args, boolean naRm,
                                        @Cached AltrepRFFI.MinNode minNode) {
        return minNode.execute(args.getArgument(0), naRm);
    }

    @Specialization(replaces = {"minLengthOne", "minLengthOneAltrep"})
    protected Object min(RArgsValuesAndNames args, boolean naRm,
                    @Cached("create()") Combine combine) {
        return reduce.executeReduce(combine.executeCombine(args, false), naRm, false);
    }
}
