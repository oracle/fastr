/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.binary.ColonNodeFactory.ColonCastNodeFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeChildren({@NodeChild("left"), @NodeChild("right")})
public abstract class ColonNode extends RNode implements VisibilityController {

    private final BranchProfile naCheckErrorProfile = BranchProfile.create();

    @CreateCast({"left", "right"})
    public RNode createCast(RNode child) {
        ColonCastNode ccn = ColonCastNodeFactory.create(child);
        ccn.assignSourceSection(getSourceSection());
        return ccn;
    }

    private void naCheck(boolean na) {
        if (na) {
            naCheckErrorProfile.enter();
            throw RError.error(getSourceSection(), RError.Message.NA_OR_NAN);
        }
    }

    @Specialization(guards = "isSmaller")
    protected RIntSequence colonAscending(int left, int right) {
        controlVisibility();
        naCheck(RRuntime.isNA(left) || RRuntime.isNA(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "!isSmaller")
    protected RIntSequence colonDescending(int left, int right) {
        controlVisibility();
        naCheck(RRuntime.isNA(left) || RRuntime.isNA(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    @Specialization(guards = "isSmaller")
    protected RIntSequence colonAscending(int left, double right) {
        controlVisibility();
        naCheck(RRuntime.isNA(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createAscendingRange(left, (int) right);
    }

    @Specialization(guards = "!isSmaller")
    protected RIntSequence colonDescending(int left, double right) {
        controlVisibility();
        naCheck(RRuntime.isNA(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createDescendingRange(left, (int) right);
    }

    @Specialization(guards = "isSmaller")
    protected RDoubleSequence colonAscending(double left, int right) {
        controlVisibility();
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNA(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "!isSmaller")
    protected RDoubleSequence colonDescending(double left, int right) {
        controlVisibility();
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNA(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    @Specialization(guards = "isSmaller")
    protected RDoubleSequence colonAscending(double left, double right) {
        controlVisibility();
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(guards = "!isSmaller")
    protected RDoubleSequence colonDescending(double left, double right) {
        controlVisibility();
        naCheck(RRuntime.isNAorNaN(left) || RRuntime.isNAorNaN(right));
        return RDataFactory.createDescendingRange(left, right);
    }

    public static ColonNode create(SourceSection src, RNode left, RNode right) {
        ColonNode cn = ColonNodeFactory.create(left, right);
        cn.assignSourceSection(src);
        return cn;
    }

    public static boolean isSmaller(double left, double right) {
        return left <= right;
    }

    public static boolean isSmaller(double left, int right) {
        return left <= right;
    }

    public static boolean isSmaller(int left, double right) {
        return left <= right;
    }

    public static boolean isSmaller(int left, int right) {
        return left <= right;
    }

    @NodeChild("operand")
    public abstract static class ColonCastNode extends RNode {

        private final ConditionProfile lengthGreaterOne = ConditionProfile.createBinaryProfile();

        @Specialization(guards = "isIntValue")
        protected int doDoubleToInt(double operand) {
            return (int) operand;
        }

        @Specialization(guards = "!isIntValue")
        protected double doDouble(double operand) {
            return operand;
        }

        @Specialization
        protected int doSequence(RIntSequence sequence) {
            if (lengthGreaterOne.profile(sequence.getLength() > 1)) {
                RError.warning(getEncapsulatingSourceSection(), RError.Message.ONLY_FIRST_USED, sequence.getLength());
            }
            return sequence.getStart();
        }

        @Specialization
        protected int doSequence(RIntVector vector) {
            if (lengthGreaterOne.profile(vector.getLength() > 1)) {
                RError.warning(getEncapsulatingSourceSection(), RError.Message.ONLY_FIRST_USED, vector.getLength());
            }
            return vector.getDataAt(0);
        }

        @Specialization(guards = "isFirstIntValue")
        protected int doDoubleVectorFirstIntValue(RDoubleVector vector) {
            if (lengthGreaterOne.profile(vector.getLength() > 1)) {
                RError.warning(getEncapsulatingSourceSection(), RError.Message.ONLY_FIRST_USED, vector.getLength());
            }
            return (int) vector.getDataAt(0);
        }

        @Specialization(guards = "!isFirstIntValue")
        protected double doDoubleVector(RDoubleVector vector) {
            if (lengthGreaterOne.profile(vector.getLength() > 1)) {
                RError.warning(getEncapsulatingSourceSection(), RError.Message.ONLY_FIRST_USED, vector.getLength());
            }
            return vector.getDataAt(0);
        }

        @Specialization
        protected int doInt(int operand) {
            return operand;
        }

        @Specialization
        protected int doBoolean(byte operand) {
            return RRuntime.logical2int(operand);
        }

        public static boolean isIntValue(double d) {
            return (((int) d)) == d;
        }

        public static boolean isFirstIntValue(RDoubleVector d) {
            return (((int) d.getDataAt(0))) == d.getDataAt(0);
        }
    }
}
