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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.closures.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "seq.int", kind = SUBSTITUTE, parameterNames = {"from", "to", "by", "length.out", "along.with"})
// Implement in R, but seq.int is PRIMITIVE (and may have to contain most, if not all, of the code
// below)
@SuppressWarnings("unused")
public abstract class Seq extends RBuiltinNode {
    private final ConditionProfile lengthProfile1 = ConditionProfile.createBinaryProfile();
    private final ConditionProfile lengthProfile2 = ConditionProfile.createBinaryProfile();
    private final BranchProfile error = BranchProfile.create();

    @Child private Seq seqRecursive;

    protected abstract Object execute(Object start, Object to, Object stride, Object lengthOut, Object alongWith);

    private Object seqRecursive(Object start, Object to, Object stride, Object lengthOut, Object alongWith) {
        if (seqRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            seqRecursive = insert(SeqNodeGen.create(new RNode[5], null, null));
        }
        return seqRecursive.execute(start, to, stride, lengthOut, alongWith);
    }

    private void validateParam(int v, String vName) {
        if (RRuntime.isNA(v)) {
            error.enter();
            throw RError.error(this, RError.Message.CANNOT_BE_INVALID, vName);
        }
    }

    private void validateParam(double v, String vName) {
        if (RRuntime.isNAorNaN(v) || Double.isInfinite(v)) {
            error.enter();
            throw RError.error(this, RError.Message.CANNOT_BE_INVALID, vName);
        }
    }

    private void validateParams(RAbstractIntVector start, RAbstractIntVector to) {
        if (start != null) {
            validateParam(start.getDataAt(0), "from");
        }
        if (to != null) {
            validateParam(start.getDataAt(0), "to");
        }
    }

    private void validateParams(RAbstractDoubleVector start, RAbstractIntVector to) {
        if (start != null) {
            validateParam(start.getDataAt(0), "from");
        }
        if (to != null) {
            validateParam(start.getDataAt(0), "to");
        }
    }

    private void validateParams(RAbstractIntVector start, RAbstractDoubleVector to) {
        if (start != null) {
            validateParam(start.getDataAt(0), "from");
        }
        if (to != null) {
            validateParam(start.getDataAt(0), "to");
        }
    }

    private void validateParams(double start, double to) {
        validateParam(start, "from");
        validateParam(start, "to");
    }

    private void validateParams(RAbstractDoubleVector start, RAbstractDoubleVector to) {
        if (start != null) {
            validateParam(start.getDataAt(0), "from");
        }
        if (to != null) {
            validateParam(start.getDataAt(0), "to");
        }
    }

    private void validateParams(RAbstractLogicalVector start, RAbstractLogicalVector to) {
        if (start != null) {
            validateParam(RRuntime.logical2int(start.getDataAt(0)), "from");
        }
        if (to != null) {
            validateParam(RRuntime.logical2int(start.getDataAt(0)), "to");
        }
    }

    private RDoubleVector getVectorWithComputedStride(double start, double to, double lengthOut, boolean ascending) {
        validateParams(start, to);
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

    private RIntVector getVectorWithComputedStride(int start, int to, double lengthOut, boolean ascending) {
        validateParams(start, to);
        int length = (int) Math.ceil(lengthOut);
        if (lengthProfile1.profile(length == 1)) {
            return RDataFactory.createIntVector(new int[]{start}, RDataFactory.COMPLETE_VECTOR);
        } else if (lengthProfile2.profile(length == 2)) {
            return RDataFactory.createIntVector(new int[]{start, to}, RDataFactory.COMPLETE_VECTOR);
        } else {
            int[] data = new int[length];
            data[0] = start;
            int newStride = (to - start) / (length - 1);
            if (!ascending) {
                newStride = -newStride;
            }
            for (int i = 1; i < length - 1; i++) {
                data[i] = start + (i * newStride);
            }
            data[length - 1] = to;
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    // int vector start, missing to

    @Specialization(guards = "!startEmpty(start)")
    protected RIntSequence seqFromOneArg(RAbstractIntVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        validateParam(start.getDataAt(0), "to");
        controlVisibility();
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createIntSequence(1, 1, start.getLength());
    }

    @Specialization(guards = "startEmpty(start)")
    protected RIntVector seqFromOneArgEmpty(RAbstractIntVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "!lengthZero(lengthOut)"})
    protected RDoubleSequence seq(RAbstractIntVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), 1, lengthOut);
    }

    @Specialization(guards = {"startLengthOne(start)", "!lengthZero(lengthOut)"})
    protected RDoubleSequence seq(RAbstractIntVector start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), 1, (int) Math.ceil(lengthOut));
    }

    @Specialization(guards = {"startLengthOne(start)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractIntVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractIntVector start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createEmptyIntVector();
    }

    // int vector start, int vector to

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    protected int seqZero(RAbstractIntVector start, RAbstractIntVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RIntSequence seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createIntSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RIntSequence seq(RAbstractIntVector start, RAbstractIntVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        validateParams(start, to);
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), stride, Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1);
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RDoubleSequence seq(RAbstractIntVector start, RAbstractIntVector to, double stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride)) + 1);
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!lengthZero(lengthOut)"})
    protected RIntVector seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), RRuntime.int2double(lengthOut), ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!lengthZero(lengthOut)"})
    protected RIntVector seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), lengthOut, ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "lengthZeroAlong(alongWith)"})
    protected RIntVector seq(RAbstractIntVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "!lengthZeroAlong(alongWith)"})
    protected RDoubleSequence seqLengthZero(RAbstractIntVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, alongWith.getLength());
    }

    // int vector start, double vector to

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    protected int seqZero(RAbstractIntVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RIntSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createIntSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RDoubleSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        int length = (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) / stride;
        if (start.getDataAt(0) + length * stride == to.getDataAt(0)) {
            length++;
        }
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, length);
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RDoubleSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, double stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        int length = (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) / stride);
        if (start.getDataAt(0) + length * stride == to.getDataAt(0)) {
            length++;
        }
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, length);
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!lengthZero(lengthOut)"})
    protected RDoubleVector seq(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return getVectorWithComputedStride(RRuntime.int2double(start.getDataAt(0)), to.getDataAt(0), lengthOut, ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!lengthZero(lengthOut)"})
    protected RDoubleVector seq(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return getVectorWithComputedStride(RRuntime.int2double(start.getDataAt(0)), to.getDataAt(0), lengthOut, ascending(start, to));
    }

    // double vector start, missing to

    @Specialization(guards = "!startEmpty(start)")
    protected RDoubleSequence seqFromOneArg(RAbstractDoubleVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createDoubleSequence(1, 1, start.getLength());
    }

    @Specialization(guards = "startEmpty(start)")
    protected RIntVector seqFromOneArgEmpty(RAbstractDoubleVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "!lengthZero(lengthOut)"})
    protected RDoubleSequence seqLengthZero(RAbstractDoubleVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, lengthOut);
    }

    @Specialization(guards = {"startLengthOne(start)", "!lengthZero(lengthOut)"})
    protected RDoubleSequence seqLengthZero(RAbstractDoubleVector start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, (int) Math.ceil(lengthOut));
    }

    @Specialization(guards = {"startLengthOne(start)", "lengthZero(lengthOut)"})
    protected RIntVector seq(RAbstractDoubleVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "lengthZero(lengthOut)"})
    protected RIntVector seq(RAbstractDoubleVector start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "lengthZeroAlong(alongWith)"})
    protected RIntVector seq(RAbstractDoubleVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "!lengthZeroAlong(alongWith)"})
    protected RDoubleSequence seqLengthZero(RAbstractDoubleVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        validateParam(start.getDataAt(0), "from");
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, alongWith.getLength());
    }

    // double vector start, int vector to

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    protected int seqZero(RAbstractDoubleVector start, RAbstractIntVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RAbstractVector seq(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        if ((int) start.getDataAt(0) == start.getDataAt(0)) {
            return RDataFactory.createIntSequence((int) start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
        } else {
            return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
        }
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractIntVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1);
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractIntVector to, double stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1);
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!lengthZero(lengthOut)"})
    protected RDoubleVector seq(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return getVectorWithComputedStride(start.getDataAt(0), RRuntime.int2double(to.getDataAt(0)), lengthOut, ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!lengthZero(lengthOut)"})
    protected RDoubleVector seq(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return getVectorWithComputedStride(start.getDataAt(0), RRuntime.int2double(to.getDataAt(0)), lengthOut, ascending(start, to));
    }

    // double vector start, double vector to

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    protected double seq(RAbstractDoubleVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return 0;
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RAbstractVector seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        if ((int) start.getDataAt(0) == start.getDataAt(0) && (int) to.getDataAt(0) == to.getDataAt(0)) {
            return RDataFactory.createIntSequence((int) start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
        } else {
            return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
        }
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, int stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1));
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    protected RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, double stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride) + 1));
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!lengthZero(lengthOut)"})
    protected RDoubleVector seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), RRuntime.int2double(lengthOut), ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!lengthZero(lengthOut)"})
    protected RDoubleVector seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), lengthOut, ascending(start, to));
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "lengthZero(lengthOut)"})
    protected RIntVector seqLengthZero(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParams(start, to);
        return RDataFactory.createEmptyIntVector();
    }

    // logical vectors

    private final NACheck naCheck = NACheck.create();

    @Specialization
    protected Object seq(RAbstractLogicalVector start, RAbstractLogicalVector to, Object stride, Object lengthOut, Object alongWith) {
        return seqRecursive(RClosures.createLogicalToDoubleVector(start), RClosures.createLogicalToDoubleVector(to), stride, lengthOut, alongWith);
    }

    @Specialization(guards = "!isLogical(to)")
    protected Object seq(RAbstractLogicalVector start, RAbstractVector to, Object stride, Object lengthOut, Object alongWith) {
        return seqRecursive(RClosures.createLogicalToDoubleVector(start), to, stride, lengthOut, alongWith);
    }

    @Specialization(guards = "!isLogical(start)")
    protected Object seq(RAbstractVector start, RAbstractLogicalVector to, Object stride, Object lengthOut, Object alongWith) {
        return seqRecursive(start, RClosures.createLogicalToDoubleVector(to), stride, lengthOut, alongWith);
    }

    @Specialization
    protected Object seq(RAbstractLogicalVector start, RMissing to, Object stride, Object lengthOut, Object alongWith) {
        return seqRecursive(RClosures.createLogicalToDoubleVector(start), to, stride, lengthOut, alongWith);
    }

    @Specialization
    protected Object seq(RMissing start, RAbstractLogicalVector to, Object stride, Object lengthOut, Object alongWith) {
        return seqRecursive(start, RClosures.createLogicalToDoubleVector(to), stride, lengthOut, alongWith);
    }

    protected boolean isLogical(RAbstractVector v) {
        return v.getElementClass() == RLogical.class;
    }

    // @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    // protected double seq(RAbstractLogicalVector start, RAbstractLogicalVector to, Object stride,
    // RMissing lengthOut, RMissing alongWith) {
    // controlVisibility();
    // return 0;
    // }
    //
    // @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "!zero(start, to)"})
    // protected RIntSequence seq(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing
    // stride, RMissing lengthOut, RMissing alongWith) {
    // controlVisibility();
    // validateParams(start, to);
    // return RDataFactory.createIntSequence(RRuntime.logical2int(start.getDataAt(0)),
    // ascending(start,
    // to) ? 1 : -1,
    // Math.abs(RRuntime.logical2int(to.getDataAt(0)) - RRuntime.logical2int(start.getDataAt(0))) +
    // 1);
    // }
    //
    // @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)",
    // "!lengthZero(lengthOut)"})
    // protected RDoubleVector seq(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing
    // stride, int lengthOut, RMissing alongWith) {
    // controlVisibility();
    // validateParams(start, to);
    // return getVectorWithComputedStride(RRuntime.logical2double(start.getDataAt(0)),
    // RRuntime.logical2double(to.getDataAt(0)), RRuntime.int2double(lengthOut), ascending(start,
    // to));
    // }
    //
    // @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)",
    // "!lengthZero(lengthOut)"})
    // protected RDoubleVector seq(RAbstractLogicalVector start, RAbstractLogicalVector to, RMissing
    // stride, double lengthOut, RMissing alongWith) {
    // controlVisibility();
    // validateParams(start, to);
    // return getVectorWithComputedStride(RRuntime.logical2double(start.getDataAt(0)),
    // RRuntime.logical2double(to.getDataAt(0)), lengthOut, ascending(start, to));
    // }
    //
    // @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)",
    // "lengthZero(lengthOut)"})
    // protected RIntVector seqLengthZero(RAbstractLogicalVector start, RAbstractLogicalVector to,
    // RMissing stride, double lengthOut, RMissing alongWith) {
    // controlVisibility();
    // validateParams(start, to);
    // return RDataFactory.createEmptyIntVector();
    // }
    //
    // @Specialization(guards = "!startEmpty(start)")
    // protected RIntSequence seqFromOneArg(RAbstractLogicalVector start, RMissing to, RMissing
    // stride,
    // RMissing lengthOut, RMissing alongWith) {
    // controlVisibility();
    // validateParam(RRuntime.logical2int(start.getDataAt(0)), "to");
    // // GNU R really does that (take the length of start to create a sequence)
    // return RDataFactory.createIntSequence(1, 1, start.getLength());
    // }
    //
    // @Specialization(guards = "startEmpty(start)")
    // protected RIntVector seqFromOneArgEmpty(RAbstractLogicalVector start, RMissing to, RMissing
    // stride, RMissing lengthOut, RMissing alongWith) {
    // return RDataFactory.createEmptyIntVector();
    // }

    @Specialization(guards = "lengthZero(lengthOut)")
    protected RIntVector seqLengthZero(RMissing start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "!lengthZero(lengthOut)")
    protected RIntSequence seq(RMissing start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, lengthOut);
    }

    @Specialization(guards = "lengthZero(lengthOut)")
    protected RIntVector seqLengthZero(RMissing start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "!lengthZero(lengthOut)")
    protected RIntSequence seq(RMissing start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, (int) Math.ceil(lengthOut));
    }

    @Specialization(guards = "lengthZeroAlong(alongWith)")
    protected RIntVector seqLengthZeroAlong(RMissing start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = "!lengthZeroAlong(alongWith)")
    protected RIntSequence seq(RMissing start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, alongWith.getLength());
    }

    @Specialization(guards = {"toLengthOne(to)", "positiveLengthOut(lengthOut)"})
    protected RDoubleSequence seq(RMissing start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(to.getDataAt(0), "to");
        return RDataFactory.createDoubleSequence(to.getDataAt(0) - lengthOut + 1, 1, lengthOut);
    }

    @Specialization(guards = {"toLengthOne(to)", "positiveLengthOut(lengthOut)"})
    protected RDoubleSequence seq(RMissing start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(to.getDataAt(0), "to");
        final int intLength = (int) Math.ceil(lengthOut);
        return RDataFactory.createDoubleSequence(to.getDataAt(0) - intLength + 1, 1, intLength);
    }

    @Specialization(guards = {"toLengthOne(to)", "positiveLengthOut(lengthOut)"})
    protected RDoubleSequence seq(RMissing start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        controlVisibility();
        validateParam(to.getDataAt(0), "to");
        return RDataFactory.createDoubleSequence(to.getDataAt(0) - lengthOut + 1, 1, (int) Math.ceil(lengthOut));
    }

    @Specialization(guards = "toLengthOne(to)")
    protected Object seq(RMissing start, RAbstractVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        controlVisibility();
        return seqRecursive(1.0, to, stride, lengthOut, alongWith);
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

    protected boolean startLengthOne(RAbstractVector start) {
        if (start.getLength() != 1) {
            throw RError.error(this, RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean toLengthOne(RAbstractVector to) {
        if (to.getLength() != 1) {
            throw RError.error(this, RError.Message.MUST_BE_SCALAR, "to");
        }
        return true;
    }

    protected boolean lengthZero(int lengthOut) {
        return lengthOut == 0;
    }

    protected boolean lengthZero(double lengthOut) {
        return lengthOut == 0;
    }

    protected boolean lengthZeroAlong(RAbstractVector alongWith) {
        return alongWith.getLength() == 0;
    }

    protected boolean positiveLengthOut(int lengthOut) {
        if (lengthOut < 0) {
            throw RError.error(this, RError.Message.MUST_BE_POSITIVE, "length.out");
        }
        return true;
    }

    protected boolean positiveLengthOut(double lengthOut) {
        if (lengthOut < 0) {
            throw RError.error(this, RError.Message.MUST_BE_POSITIVE, "length.out");
        }
        return true;
    }
}
