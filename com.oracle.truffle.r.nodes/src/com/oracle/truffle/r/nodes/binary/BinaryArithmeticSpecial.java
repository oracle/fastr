/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.binary.BinaryArithmeticSpecialNodeGen.IntegerBinaryArithmeticSpecialNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.BinaryArithmeticFactory;

/**
 * Fast-path for scalar values: these cannot have any class attribute. Note: we intentionally use
 * empty type system to avoid conversions to vector types. Some binary operations have simple NA
 * handling, which is replicated here, others (notably pow and mul) throw
 * {@link RSpecialFactory#throwFullCallNeeded()} on NA.
 */
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
@NodeChild(value = "arguments", type = RNode[].class)
public abstract class BinaryArithmeticSpecial extends RNode {
    private final boolean handleNA;
    @Child private BinaryArithmetic operation;

    public BinaryArithmeticSpecial(BinaryArithmetic operation, boolean handleNA) {
        this.operation = operation;
        this.handleNA = handleNA;
    }

    public static RSpecialFactory createSpecialFactory(final BinaryArithmeticFactory opFactory) {
        final boolean handleNA = !(opFactory == BinaryArithmetic.POW || opFactory == BinaryArithmetic.MOD);
        boolean handleIntegers = !(opFactory == BinaryArithmetic.POW || opFactory == BinaryArithmetic.DIV);
        if (handleIntegers) {
            return (signature, arguments) -> signature.getNonNullCount() == 0 && arguments.length == 2
                            ? IntegerBinaryArithmeticSpecialNodeGen.create(opFactory.create(), handleNA, arguments) : null;
        } else {
            return (signature, arguments) -> signature.getNonNullCount() == 0 && arguments.length == 2 ? BinaryArithmeticSpecialNodeGen.create(opFactory.create(), handleNA, arguments)
                            : null;
        }
    }

    @Specialization
    protected double doDoubles(double left, double right) {
        if (RRuntime.isNA(left) || RRuntime.isNA(right)) {
            checkFullCallNeededOnNA();
            return RRuntime.DOUBLE_NA;
        }
        return getOperation().op(left, right);
    }

    @Fallback
    @SuppressWarnings("unused")
    protected void doFallback(Object left, Object right) {
        throw RSpecialFactory.throwFullCallNeeded();
    }

    protected BinaryArithmetic getOperation() {
        return operation;
    }

    protected void checkFullCallNeededOnNA() {
        if (!handleNA) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw RSpecialFactory.throwFullCallNeeded();
        }
    }

    /**
     * Adds integers handling.
     */
    abstract static class IntegerBinaryArithmeticSpecial extends BinaryArithmeticSpecial {

        IntegerBinaryArithmeticSpecial(BinaryArithmetic op, boolean handleNA) {
            super(op, handleNA);
        }

        @Specialization
        public int doIntegers(int left, int right, @Cached("createBinaryProfile()") ConditionProfile naProfile) {
            if (naProfile.profile(RRuntime.isNA(left) || RRuntime.isNA(right))) {
                checkFullCallNeededOnNA();
                return RRuntime.INT_NA;
            }
            return getOperation().op(left, right);
        }

        @Specialization
        public double doIntDouble(int left, double right, @Cached("createBinaryProfile()") ConditionProfile naProfile) {
            if (naProfile.profile(RRuntime.isNA(left) || RRuntime.isNA(right))) {
                checkFullCallNeededOnNA();
                return RRuntime.DOUBLE_NA;
            }
            return getOperation().op(left, right);
        }

        @Specialization
        public double doDoubleInt(double left, int right, @Cached("createBinaryProfile()") ConditionProfile naProfile) {
            if (naProfile.profile(RRuntime.isNA(left) || RRuntime.isNA(right))) {
                checkFullCallNeededOnNA();
                return RRuntime.DOUBLE_NA;
            }
            return getOperation().op(left, right);
        }
    }
}
