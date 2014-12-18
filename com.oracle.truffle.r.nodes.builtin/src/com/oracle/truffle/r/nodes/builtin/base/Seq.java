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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.SUBSTITUTE;

@RBuiltin(name = "seq", aliases = {"seq.int"}, kind = SUBSTITUTE, parameterNames = {"from", "to", "by", "length.out", "along.with"})
// Implement in R, but seq.int is PRIMITIVE (and may have to contain most, if not all, of the code
// below)
@SuppressWarnings("unused")
public abstract class Seq extends RBuiltinNode {
    private final ConditionProfile lengthProfile1 = ConditionProfile.createBinaryProfile();
    private final ConditionProfile lengthProfile2 = ConditionProfile.createBinaryProfile();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance),
                        ConstantNode.create(RMissing.instance)};
    }

    @TruffleBoundary
    private RDoubleVector getVectorWithComputedStride(double start, double to, double lengthOut, boolean ascending) {
        int length = (int) Math.ceil(lengthOut);
        if (lengthProfile1.profile(length == 1)) {
            return RDataFactory.createDoubleVector(new double[]{start}, RDataFactory.COMPLETE_VECTOR);
        } else if (lengthProfile2.profile(length == 2)) {
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

    @Specialization(guards = {"startLengthOne", "toLengthOne", "zero"})
    protected int seqZero(VirtualFrame frame, RAbstractIntVector start, RAbstractIntVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "zero"})
    protected int seqZer0(RAbstractDoubleVector start, RAbstractIntVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "zero"})
    protected int seqZero(RAbstractIntVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "zero"})
    protected double seq(RAbstractDoubleVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RIntSequence seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RIntSequence seq(RAbstractIntVector start, RAbstractIntVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), stride, Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1);
    }

    @Specialization(guards = {"startLengthOne", "!lengthZero"})
    protected RDoubleSequence seq(RAbstractIntVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), 1, lengthOut);
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    protected RDoubleVector seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(RRuntime.int2double(start.getDataAt(0)), RRuntime.int2double(to.getDataAt(0)), RRuntime.int2double(lengthOut), ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    protected RDoubleVector seq(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(RRuntime.logical2double(start.getDataAt(0)), RRuntime.logical2double(to.getDataAt(0)), RRuntime.int2double(lengthOut), ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "lengthZero"})
    protected RIntVector seqLengthZero(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RDoubleSequence seq(RAbstractIntVector start, RAbstractIntVector to, double stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride)) + 1);
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    protected RDoubleVector seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(RRuntime.int2double(start.getDataAt(0)), RRuntime.int2double(to.getDataAt(0)), lengthOut, ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    protected RDoubleVector seq(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(RRuntime.logical2double(start.getDataAt(0)), RRuntime.logical2double(to.getDataAt(0)), lengthOut, ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "lengthZero"})
    protected RIntVector seqLengthZero(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RDoubleSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RDoubleSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        int length = (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) / stride;
        if (start.getDataAt(0) + length * stride == to.getDataAt(0)) {
            length++;
        }
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, length);
    }

    @Specialization(guards = "!startEmpty")
    protected RIntSequence seqFromOneArg(RAbstractIntVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createIntSequence(1, 1, start.getLength());
    }

    @Specialization(guards = "startEmpty")
    protected RIntVector seqFromOneArgEmpty(RAbstractIntVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "!startEmpty")
    protected RIntSequence seqFromOneArg(RAbstractLogicalVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createIntSequence(1, 1, start.getLength());
    }

    @Specialization(guards = "startEmpty")
    protected RIntVector seqFromOneArgEmpty(RAbstractLogicalVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractIntVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1);
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    protected RDoubleVector seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), RRuntime.int2double(lengthOut), ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "lengthZero"})
    protected RIntVector seqLengthZero(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!zero"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, double stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "!lengthZero"})
    protected RDoubleVector seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), lengthOut, ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne", "toLengthOne", "lengthZero"})
    protected RIntVector seqLengthZero(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "!startEmpty")
    protected RDoubleSequence seqFromOneArg(RAbstractDoubleVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createDoubleSequence(1, 1, start.getLength());
    }

    @Specialization(guards = "startEmpty")
    protected RIntVector seqFromOneArgEmpty(RAbstractDoubleVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "lengthZero")
    protected RIntVector seqLengthZero(RMissing start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "!lengthZero")
    protected RIntSequence seq(RMissing start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, lengthOut);
    }

    @Specialization(guards = "lengthZero")
    protected RIntVector seqLengthZero(RMissing start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "!lengthZero")
    protected RIntSequence seq(RMissing start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, (int) Math.ceil(lengthOut));
    }

    @Specialization(guards = "lengthZeroAlong")
    protected RIntVector seqLengthZeroAlong(RMissing start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "!lengthZeroAlong")
    protected RIntSequence seq(RMissing start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, alongWith.getLength());
    }

    @Specialization(guards = {"toLengthOne", "positiveLengthOut"})
    public RIntSequence seq(RMissing start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(to.getDataAt(0) - lengthOut + 1, 1, lengthOut);
    }

    @Specialization(guards = {"toLengthOne", "positiveLengthOut"})
    public RIntSequence seq(RMissing start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        final int intLength = (int) Math.ceil(lengthOut);
        return RDataFactory.createIntSequence(to.getDataAt(0) - intLength + 1, 1, intLength);
    }

    @Specialization(guards = {"toLengthOne", "positiveLengthOut"})
    public RDoubleSequence seq(RMissing start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(to.getDataAt(0) - lengthOut + 1, 1, (int) Math.ceil(lengthOut));
    }

    @Specialization(guards = {"startLengthOne", "lengthZero"})
    protected RDoubleVector seq(RAbstractDoubleVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = {"startLengthOne", "lengthZeroAlong"})
    protected RDoubleVector seq(RAbstractDoubleVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = {"startLengthOne", "!lengthZero"})
    protected RDoubleSequence seqLengthZero(RAbstractDoubleVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, lengthOut);
    }

    @Specialization(guards = {"startLengthOne", "!lengthZeroAlong"})
    protected RDoubleSequence seqLengthZero(RAbstractDoubleVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
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

    protected boolean startLengthOne(RAbstractIntVector start) {
        if (start.getLength() != 1) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean startLengthOne(RAbstractLogicalVector start) {
        if (start.getLength() != 1) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean startLengthOne(RAbstractDoubleVector start) {
        if (start.getLength() != 1) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean toLengthOne(Object start, RAbstractVector to) {
        if (to.getLength() != 1) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "to");
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

    protected boolean positiveLengthOut(Object start, Object to, Object stride, int lengthOut, Object v) {
        if (lengthOut < 0) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_POSITIVE, "length.out");
        }
        return true;
    }

    protected boolean positiveLengthOut(Object start, Object to, Object stride, double lengthOut, Object v) {
        if (lengthOut < 0) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_POSITIVE, "length.out");
        }
        return true;
    }
}
