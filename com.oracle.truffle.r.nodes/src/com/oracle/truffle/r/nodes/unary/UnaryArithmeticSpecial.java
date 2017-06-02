/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;
import com.oracle.truffle.r.runtime.ops.UnaryArithmeticFactory;

/**
 * Fast-path for scalar values: these cannot have any class attribute. Note: we intentionally use
 * empty type system to avoid conversions to vector types.
 */
@ImportStatic(RType.class)
@NodeChild(value = "operand", type = RNode.class)
public abstract class UnaryArithmeticSpecial extends RNode {

    private final UnaryArithmeticFactory unaryFactory;

    @Child protected UnaryArithmetic operation;

    protected UnaryArithmeticSpecial(UnaryArithmeticFactory unaryFactory) {
        this.unaryFactory = unaryFactory;
        this.operation = unaryFactory.createOperation();
    }

    public static RSpecialFactory createSpecialFactory(UnaryArithmeticFactory unaryFactory) {
        return (signature, arguments, inReplacement) -> signature.getNonNullCount() == 0 && arguments.length == 1
                        ? UnaryArithmeticSpecialNodeGen.create(unaryFactory, arguments[0]) : null;
    }

    @Specialization
    protected double doDoubles(double operand,
                    @Cached("createBinaryProfile()") ConditionProfile naProfile) {
        if (naProfile.profile(RRuntime.isNA(operand))) {
            return operand;
        }
        return getOperation().op(operand);
    }

    protected UnaryArithmeticNode createFull() {
        return UnaryArithmeticNodeGen.create(unaryFactory);
    }

    @Specialization(guards = "operation.getMinPrecedence() == Integer")
    public int doIntegers(int operand,
                    @Cached("createBinaryProfile()") ConditionProfile naProfile) {
        if (naProfile.profile(RRuntime.isNA(operand))) {
            return RRuntime.INT_NA;
        }
        return getOperation().op(operand);
    }

    @Specialization(guards = "operation.getMinPrecedence() == Double")
    public double doIntegersDouble(int operand,
                    @Cached("createBinaryProfile()") ConditionProfile naProfile) {
        if (naProfile.profile(RRuntime.isNA(operand))) {
            return RRuntime.INT_NA;
        }
        return getOperation().op((double) operand);
    }

    @Specialization
    protected Object doFallback(Object operand,
                    @Cached("create()") BoxPrimitiveNode boxPrimitive,
                    @Cached("createFull()") UnaryArithmeticNode unary) {
        return unary.execute(boxPrimitive.execute(operand));
    }

    protected UnaryArithmetic getOperation() {
        return operation;
    }
}
