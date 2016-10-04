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
package com.oracle.truffle.r.nodes.binary;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.binary.ColonNodeGen.ColonCastNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = ":", kind = PRIMITIVE, parameterNames = {"", ""}, behavior = PURE)
public abstract class ColonNode extends RBuiltinNode {

    private final BranchProfile naCheckErrorProfile = BranchProfile.create();
    private final NACheck leftNA = NACheck.create();
    private final NACheck rightNA = NACheck.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        // These casts should not be custom, but they are very hard to get right in a generic way.
        // Also, the proper warnings cannot be produced at the moment.
        casts.arg(0).defaultError(this, Message.ARGUMENT_LENGTH_0).customNode(() -> ColonCastNodeGen.create());
        casts.arg(1).defaultError(this, Message.ARGUMENT_LENGTH_0).customNode(() -> ColonCastNodeGen.create());
    }

    private void naCheck(boolean na) {
        if (na) {
            naCheckErrorProfile.enter();
            throw RError.error(this, RError.Message.NA_OR_NAN);
        }
    }

    @Specialization(guards = "left <= right")
    protected RIntSequence colonAscending(int left, int right) {
        leftNA.enable(left);
        rightNA.enable(right);
        naCheck(leftNA.check(left) || rightNA.check(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "left > right")
    protected RIntSequence colonDescending(int left, int right) {
        leftNA.enable(left);
        rightNA.enable(right);
        naCheck(leftNA.check(left) || rightNA.check(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    @Specialization(guards = "asDouble(left) <= right")
    protected RIntSequence colonAscending(int left, double right) {
        leftNA.enable(left);
        naCheck(leftNA.check(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createAscendingRange(left, (int) right);
    }

    @Specialization(guards = "asDouble(left) > right")
    protected RIntSequence colonDescending(int left, double right) {
        leftNA.enable(left);
        naCheck(leftNA.check(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createDescendingRange(left, (int) right);
    }

    @Specialization(guards = "left <= asDouble(right)")
    protected RDoubleSequence colonAscending(double left, int right) {
        rightNA.enable(right);
        naCheck(RRuntime.isNAorNaN(left) || rightNA.check(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "left > asDouble(right)")
    protected RDoubleSequence colonDescending(double left, int right) {
        rightNA.enable(right);
        naCheck(RRuntime.isNAorNaN(left) || rightNA.check(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    @Specialization(guards = "left <= right")
    protected RDoubleSequence colonAscending(double left, double right) {
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "left > right")
    protected RDoubleSequence colonDescending(double left, double right) {
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    protected static double asDouble(int intValue) {
        return intValue;
    }

    abstract static class ColonCastNode extends CastNode {

        private final ConditionProfile lengthGreaterOne = ConditionProfile.createBinaryProfile();
        private final ConditionProfile lengthEqualsZero = ConditionProfile.createBinaryProfile();

        @Specialization(guards = "isIntValue(operand)")
        protected int doDoubleToInt(double operand) {
            return (int) operand;
        }

        @Specialization
        protected int doInt(int operand) {
            return operand;
        }

        @Specialization(guards = "!isIntValue(operand)")
        protected double doDouble(double operand) {
            return operand;
        }

        private void checkLength(int length) {
            if (lengthGreaterOne.profile(length > 1)) {
                RError.warning(this, RError.Message.ONLY_FIRST_USED, length);
            }
            if (lengthEqualsZero.profile(length == 0)) {
                throw RError.error(this, Message.ARGUMENT_LENGTH_0);
            }
        }

        @Specialization
        protected int doSequence(RAbstractIntVector vector) {
            checkLength(vector.getLength());
            return vector.getDataAt(0);
        }

        @Specialization(guards = "isFirstIntValue(vector)")
        protected int doDoubleVectorFirstIntValue(RAbstractDoubleVector vector) {
            checkLength(vector.getLength());
            return (int) vector.getDataAt(0);
        }

        @Specialization(guards = "!isFirstIntValue(vector)")
        protected double doDoubleVector(RAbstractDoubleVector vector) {
            checkLength(vector.getLength());
            return vector.getDataAt(0);
        }

        @Specialization
        protected int doBoolean(byte operand) {
            return RRuntime.logical2int(operand);
        }

        @Specialization
        protected int doString(RAbstractStringVector vector) {
            checkLength(vector.getLength());
            String val = vector.getDataAt(0);
            if (RRuntime.isNA(val)) {
                throw RError.error(this, RError.Message.NA_OR_NAN);
            }
            // TODO it might be a double or complex string
            int result = RRuntime.string2intNoCheck(val);
            if (RRuntime.isNA(result)) {
                RError.warning(this, RError.Message.NA_INTRODUCED_COERCION);
                throw RError.error(this, RError.Message.NA_OR_NAN);
            }
            return result;
        }

        @Fallback
        @TruffleBoundary
        protected int doOther(@SuppressWarnings("unused") Object value) {
            throw RError.error(this, Message.ARGUMENT_LENGTH_0);
        }

        protected static boolean isIntValue(double d) {
            return (((int) d)) == d;
        }

        protected static boolean isFirstIntValue(RAbstractDoubleVector d) {
            return d.getLength() > 0 && (((int) d.getDataAt(0))) == d.getDataAt(0);
        }
    }
}
