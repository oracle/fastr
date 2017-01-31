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
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

@RBuiltin(name = "max", kind = PRIMITIVE, parameterNames = {"...", "na.rm"}, dispatch = SUMMARY_GROUP_GENERIC, behavior = PURE)
public abstract class Max extends RBuiltinNode {

    private static final ReduceSemantics semantics = new ReduceSemantics(RRuntime.INT_MIN_VALUE, Double.NEGATIVE_INFINITY, false, RError.Message.NO_NONMISSING_MAX,
                    RError.Message.NO_NONMISSING_MAX_NA, false, true);

    @Child private UnaryArithmeticReduceNode reduce = UnaryArithmeticReduceNodeGen.create(semantics, BinaryArithmetic.MAX);

    static {
        Casts casts = new Casts(Max.class);
        casts.arg("na.rm").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    @Specialization(guards = "args.getLength() == 1")
    protected Object maxLengthOne(RArgsValuesAndNames args, boolean naRm) {
        return reduce.executeReduce(args.getArgument(0), naRm, false);
    }

    @Specialization(replaces = "maxLengthOne")
    protected Object max(RArgsValuesAndNames args, boolean naRm,
                    @Cached("create()") Combine combine) {
        return reduce.executeReduce(combine.executeCombine(args, false), naRm, false);
    }
}
