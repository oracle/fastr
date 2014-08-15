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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.UpdateArrayHelperNode.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "seq", aliases = {"seq.int"}, kind = SUBSTITUTE, parameterNames = {"from", "to", "by", "length.out", "along.with"})
// Implement in R, but seq.int is PRIMITIVE (and may have to contain most, if not all, of the code
// below)
@SuppressWarnings("unused")
public abstract class Seq extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance),
                        ConstantNode.create(RMissing.instance)};
    }

    private static RDoubleVector getVectorWithComputedStride(double start, double to, double lengthOut, boolean ascending) {
        int length = (int) Math.ceil(lengthOut);
        if (length == 1) {
            return RDataFactory.createDoubleVector(new double[]{start}, RDataFactory.COMPLETE_VECTOR);
        } else if (length == 2) {
            return RDataFactory.createDoubleVector(new double[]{start, to}, RDataFactory.COMPLETE_VECTOR);
        } else {
            double[] data = new double[length];
            data[0] = start;
            double newStride = (to - start) / (length - 1);
            if (!ascending) {
                newStride = -newStride;
            }
            for (int i = 1; i < length - 1; i++) {
                data[i] = start + (i * newStride);
            }
            data[length - 1] = to;
            return RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @Specialization(order = 0, guards = {"startLengthOne", "toLengthOne", "zero"})
    public int seqZero(VirtualFrame frame, RAbstractIntVector start, RAbstractIntVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 1, guards = {"startLengthOne", "toLengthOne", "zero"})
    public int seqZer0(RAbstractDoubleVector start, RAbstractIntVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 2, guards = {"startLengthOne", "toLengthOne", "zero"})
    public int seqZero(RAbstractIntVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 3, guards = {"startLengthOne", "toLengthOne", "zero"})
    public double seq(RAbstractDoubleVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 5, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RIntSequence seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
    }

    @Specialization(order = 10, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RIntSequence seq(RAbstractIntVector start, RAbstractIntVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), stride, Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1);
    }

    @Specialization(order = 12, guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    public RDoubleVector seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(RRuntime.int2double(start.getDataAt(0)), RRuntime.int2double(to.getDataAt(0)), RRuntime.int2double(lengthOut), ascending(start, to));
    }

    @Specialization(order = 13, guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    public RDoubleVector seq(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(RRuntime.logical2double(start.getDataAt(0)), RRuntime.logical2double(to.getDataAt(0)), RRuntime.int2double(lengthOut), ascending(start, to));
    }

    @Specialization(order = 14, guards = {"startLengthOne", "toLengthOne", "lengthZero"})
    public RIntVector seqLengthZero(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 15, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RDoubleSequence seq(RAbstractIntVector start, RAbstractIntVector to, double stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride)) + 1);
    }

    @Specialization(order = 17, guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    public RDoubleVector seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(RRuntime.int2double(start.getDataAt(0)), RRuntime.int2double(to.getDataAt(0)), lengthOut, ascending(start, to));
    }

    @Specialization(order = 18, guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    public RDoubleVector seq(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(RRuntime.logical2double(start.getDataAt(0)), RRuntime.logical2double(to.getDataAt(0)), lengthOut, ascending(start, to));
    }

    @Specialization(order = 19, guards = {"startLengthOne", "toLengthOne", "lengthZero"})
    public RIntVector seqLengthZero(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 31, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RDoubleSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
    }

    @Specialization(order = 40, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RDoubleSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        int length = (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) / stride;
        if (start.getDataAt(0) + length * stride == to.getDataAt(0)) {
            length++;
        }
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, length);
    }

    @Specialization(order = 45, guards = "!startEmpty")
    public RIntSequence seqFromOneArg(RAbstractIntVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createIntSequence(1, 1, start.getLength());
    }

    @Specialization(order = 46, guards = "startEmpty")
    public RIntVector seqFromOneArgEmpty(RAbstractIntVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 50, guards = "!startEmpty")
    public RIntSequence seqFromOneArg(RAbstractLogicalVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createIntSequence(1, 1, start.getLength());
    }

    @Specialization(order = 51, guards = "startEmpty")
    public RIntVector seqFromOneArgEmpty(RAbstractLogicalVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 55, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
    }

    @Specialization(order = 60, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractIntVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1);
    }

    @Specialization(order = 91, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
    }

    @Specialization(order = 100, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1));
    }

    @Specialization(order = 103, guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    public RDoubleVector seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), RRuntime.int2double(lengthOut), ascending(start, to));
    }

    @Specialization(order = 104, guards = {"startLengthOne", "toLengthOne", "lengthZero"})
    public RIntVector seqLengthZero(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 110, guards = {"startLengthOne", "toLengthOne", "!zero"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, double stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1));
    }

    @Specialization(order = 113, guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    public RDoubleVector seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), lengthOut, ascending(start, to));
    }

    @Specialization(order = 114, guards = {"startLengthOne", "toLengthOne", "lengthZero"})
    public RIntVector seqLengthZero(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 120, guards = "!startEmpty")
    public RDoubleSequence seqFromOneArg(RAbstractDoubleVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createDoubleSequence(1, 1, start.getLength());
    }

    @Specialization(order = 121, guards = "startEmpty")
    public RIntVector seqFromOneArgEmpty(RAbstractDoubleVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 150, guards = "lengthZero")
    public RIntVector seqLengthZero(RMissing start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 151, guards = "!lengthZero")
    public RIntSequence seq(RMissing start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, lengthOut);
    }

    @Specialization(order = 155, guards = "lengthZero")
    public RIntVector seqLengthZero(RMissing start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 156, guards = "!lengthZero")
    public RIntSequence seq(RMissing start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, (int) Math.ceil(lengthOut));
    }

    @Specialization(order = 160, guards = "lengthZeroAlong")
    public RIntVector LengthZero(RMissing start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(order = 161, guards = "!lengthZeroAlong")
    public RIntSequence seq(RMissing start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, alongWith.getLength());
    }

    @Specialization(order = 170, guards = {"startLengthOne", "lengthZero"})
    public RDoubleVector seq(RAbstractDoubleVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(order = 171, guards = {"startLengthOne", "lengthZeroAlong"})
    public RDoubleVector seq(RAbstractDoubleVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(order = 180, guards = {"startLengthOne", "!lengthZero"})
    public RDoubleSequence seqLengthZero(RAbstractDoubleVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, lengthOut);
    }

    @Specialization(order = 181, guards = {"startLengthOne", "!lengthZeroAlong"})
    public RDoubleSequence seqLengthZero(RAbstractDoubleVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, alongWith.getLength());
    }

    protected static boolean ascending(RAbstractIntVector start, RAbstractIntVector to) {
        return to.getDataAt(0) > start.getDataAt(0);
    }

    protected static boolean ascending(RAbstractLogicalVector start, RAbstractLogicalVector to) {
        return RRuntime.logical2int(to.getDataAt(0)) > RRuntime.logical2int(start.getDataAt(0));
    }

    protected static boolean ascending(RAbstractIntVector start, RAbstractDoubleVector to) {
        return to.getDataAt(0) > start.getDataAt(0);
    }

    protected static boolean ascending(RAbstractDoubleVector start, RAbstractIntVector to) {
        return to.getDataAt(0) > start.getDataAt(0);
    }

    protected static boolean ascending(RAbstractDoubleVector start, RAbstractDoubleVector to) {
        return to.getDataAt(0) > start.getDataAt(0);
    }

    protected static boolean zero(RAbstractIntVector start, RAbstractIntVector to) {
        return start.getDataAt(0) == 0 && to.getDataAt(0) == 0;
    }

    protected static boolean zero(RAbstractLogicalVector start, RAbstractLogicalVector to) {
        return start.getDataAt(0) == RRuntime.LOGICAL_FALSE && to.getDataAt(0) == RRuntime.LOGICAL_FALSE;
    }

    protected static boolean zero(RAbstractIntVector start, RAbstractDoubleVector to) {
        return start.getDataAt(0) == 0 && to.getDataAt(0) == 0;
    }

    protected static boolean zero(RAbstractDoubleVector start, RAbstractIntVector to) {
        return start.getDataAt(0) == 0 && to.getDataAt(0) == 0;
    }

    protected static boolean zero(RAbstractDoubleVector start, RAbstractDoubleVector to) {
        return start.getDataAt(0) == 0 && to.getDataAt(0) == 0;
    }

    protected boolean startEmpty(RAbstractVector start) {
        return start.getLength() == 0;
    }

    protected boolean startLengthOne(VirtualFrame frame, RAbstractIntVector start) {
        if (start.getLength() != 1) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean startLengthOne(VirtualFrame frame, RAbstractLogicalVector start) {
        if (start.getLength() != 1) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean startLengthOne(VirtualFrame frame, RAbstractDoubleVector start) {
        if (start.getLength() != 1) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean toLengthOne(VirtualFrame frame, RAbstractVector start, RAbstractVector to) {
        if (to.getLength() != 1) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "to");
        }
        return true;
    }

    protected boolean lengthZero(RAbstractVector start, Object to, Object stride, int lengthOut) {
        return lengthOut == 0;
    }

    protected boolean lengthZero(RAbstractVector start, RAbstractVector to, Object stride, int lengthOut) {
        return lengthOut == 0;
    }

    protected boolean lengthZero(RAbstractVector start, RAbstractVector to, Object stride, double lengthOut) {
        return lengthOut == 0;
    }

    protected boolean lengthZero(RMissing start, Object to, Object stride, int lengthOut) {
        return lengthOut == 0;
    }

    protected boolean lengthZero(Object start, Object to, Object stride, double lengthOut) {
        return lengthOut == 0;
    }

    protected boolean lengthZeroAlong(RAbstractVector start, Object to, Object stride, Object lengthOut, RAbstractVector v) {
        return v.getLength() == 0;
    }

    protected boolean lengthZeroAlong(Object start, Object to, Object stride, Object lengthOut, RAbstractVector v) {
        return v.getLength() == 0;
    }
}
