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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.binary.ColonNodeFactory.ColonCastNodeFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@NodeChildren({@NodeChild("left"), @NodeChild("right")})
public abstract class ColonNode extends RNode implements VisibilityController {

    @CreateCast({"left", "right"})
    public RNode createCast(RNode child) {
        return ColonCastNodeFactory.create(child);
    }

    @Specialization(order = 1, guards = "isSmaller")
    public RIntSequence colonAscending(int left, int right) {
        controlVisibility();
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(order = 2, guards = "!isSmaller")
    public RIntSequence colonDescending(int left, int right) {
        controlVisibility();
        return RDataFactory.createDescendingRange(left, right);
    }

    @Specialization(order = 3, guards = "isSmaller")
    public RIntSequence colonAscending(int left, double right) {
        controlVisibility();
        return RDataFactory.createAscendingRange(left, (int) right);
    }

    @Specialization(order = 4, guards = "!isSmaller")
    public RIntSequence colonDescending(int left, double right) {
        controlVisibility();
        return RDataFactory.createDescendingRange(left, (int) right);
    }

    @Specialization(order = 5, guards = "isSmaller")
    public RDoubleSequence colonAscending(double left, int right) {
        controlVisibility();
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(order = 6, guards = "!isSmaller")
    public RDoubleSequence colonDescending(double left, int right) {
        controlVisibility();
        return RDataFactory.createDescendingRange(left, right);
    }

    @Specialization(order = 7, guards = "isSmaller")
    public RDoubleSequence colonAscending(double left, double right) {
        controlVisibility();
        return RDataFactory.createAscendingRange(left, right);
    }

    @Specialization(order = 8, guards = "!isSmaller")
    public RDoubleSequence colonDescending(double left, double right) {
        controlVisibility();
        return RDataFactory.createDescendingRange(left, right);
    }

    public static ColonNode create(RNode left, RNode right) {
        return ColonNodeFactory.create(left, right);
    }

    public static ColonNode create(SourceSection src, RNode left, RNode right) {
        ColonNode cn = create(left, right);
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

        @Specialization(guards = "isIntValue")
        public int doDoubleToInt(double operand) {
            return (int) operand;
        }

        @Specialization(guards = "!isIntValue")
        public double doDouble(double operand) {
            return operand;
        }

        @Specialization
        public int doSequence(RIntSequence sequence) {
            // TODO: Produce warning
            return sequence.getStart();
        }

        @Specialization
        public int doSequence(RIntVector vector) {
            // TODO: Produce warning
            return vector.getDataAt(0);
        }

        @Specialization(guards = "isFirstIntValue")
        public int doDoubleVectorFirstIntValue(RDoubleVector vector) {
            return (int) vector.getDataAt(0);
        }

        @Specialization(guards = "!isFirstIntValue")
        public double doDoubleVector(RDoubleVector vector) {
            return vector.getDataAt(0);
        }

        @Specialization
        public int doInt(int operand) {
            return operand;
        }

        @Specialization
        public byte doBoolean(byte operand) {
            return operand;
        }

        public static boolean isIntValue(double d) {
            return (((int) d)) == d;
        }

        public static boolean isFirstIntValue(RDoubleVector d) {
            return (((int) d.getDataAt(0))) == d.getDataAt(0);
        }
    }
}
