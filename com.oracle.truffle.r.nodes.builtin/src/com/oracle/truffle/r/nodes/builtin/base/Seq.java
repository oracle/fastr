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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.SUBSTITUTE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "seq.default", aliases = {"seq.int"}, kind = SUBSTITUTE, parameterNames = {"from", "to", "by", "length.out", "along.with"}, behavior = PURE)
// Implement in R, but seq.int is PRIMITIVE (and may have to contain most, if not all, of the code
// below)
@SuppressWarnings("unused")
public abstract class Seq extends RBuiltinNode {

    // TODO: warnings for non-scalar values of stride are quite weird (for now we simply assume that
    // stride is of length one)

    private final ConditionProfile lengthProfile1 = ConditionProfile.createBinaryProfile();
    private final ConditionProfile lengthProfile2 = ConditionProfile.createBinaryProfile();
    private final ConditionProfile topLengthProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile error = BranchProfile.create();

    @Child private Seq seqRecursive;

    protected abstract Object execute(Object start, Object to, Object stride, Object lengthOut, Object alongWith);

    private Object seqRecursive(Object start, Object to, Object stride, Object lengthOut, Object alongWith) {
        if (seqRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            seqRecursive = insert(SeqNodeGen.create());
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
        validateParam(to, "to");
    }

    private void validateParams(RAbstractDoubleVector start, RAbstractDoubleVector to) {
        if (start != null) {
            validateParam(start.getDataAt(0), "from");
        }
        if (to != null) {
            validateParam(start.getDataAt(0), "to");
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

    @Specialization
    protected RIntSequence seqFromOneArg(RAbstractListVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        // GNU R really does that (take the length of start to create a sequence)
        return RDataFactory.createIntSequence(1, 1, start.getLength());
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    protected int seqZero(RAbstractIntVector start, RAbstractIntVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        return 0;
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    protected int seqZero(RAbstractDoubleVector start, RAbstractIntVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        return 0;
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    protected int seqZero(RAbstractIntVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        return 0;
    }

    @Specialization(guards = {"startLengthOne(start)", "toLengthOne(to)", "zero(start, to)"})
    protected int seqZero(RAbstractDoubleVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        return 0;
    }

    // int vector start, missing to

    @Specialization
    protected RAbstractIntVector seqFromOneArg(RAbstractIntVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        if (topLengthProfile.profile(start.getLength() == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParam(start.getDataAt(0), "to");
            // GNU R really does that (take the length of start to create a sequence)
            return RDataFactory.createIntSequence(1, 1, start.getLength());
        }
    }

    @Specialization
    protected RAbstractVector seq(RAbstractIntVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        startLengthOne(start);
        validateParam(start.getDataAt(0), "from");
        if (topLengthProfile.profile(lengthOut == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), 1, lengthOut);
        }
    }

    @Specialization
    protected RAbstractVector seq(RAbstractIntVector start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        startLengthOne(start);
        validateParam(start.getDataAt(0), "from");
        if (topLengthProfile.profile(lengthOut == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), 1, (int) Math.ceil(lengthOut));
        }
    }

    // int vector start, int vector to

    @Specialization
    protected Object seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            return RDataFactory.createIntSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
        }
    }

    @Specialization
    protected Object seq(RAbstractIntVector start, RAbstractIntVector to, RAbstractIntVector stride, RMissing lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            return RDataFactory.createIntSequence(start.getDataAt(0), stride.getDataAt(0), Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride.getDataAt(0)) + 1);
        }
    }

    @Specialization
    protected Object seq(RAbstractIntVector start, RAbstractIntVector to, RAbstractDoubleVector stride, RMissing lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            return RDataFactory.createDoubleSequence(RRuntime.int2double(start.getDataAt(0)), stride.getDataAt(0), (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride.getDataAt(0))) + 1);
        }
    }

    @Specialization
    protected RIntVector seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        if (topLengthProfile.profile(lengthOut == 0)) {
            validateParams(start, to);
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParams(start, to);
            return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), RRuntime.int2double(lengthOut), ascending(start, to));
        }
    }

    @Specialization
    protected RIntVector seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        if (topLengthProfile.profile(lengthOut == 0)) {
            validateParams(start, to);
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParams(start, to);
            return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), lengthOut, ascending(start, to));
        }
    }

    @Specialization
    protected RAbstractVector seq(RAbstractIntVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        startLengthOne(start);
        if (topLengthProfile.profile(alongWith.getLength() == 0)) {
            validateParam(start.getDataAt(0), "from");
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParam(start.getDataAt(0), "from");
            return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, alongWith.getLength());
        }
    }

    // int vector start, double vector to

    @Specialization
    protected Object seq(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            return RDataFactory.createIntSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1);
        }
    }

    @Specialization
    protected Object seq(RAbstractIntVector start, RAbstractDoubleVector to, RAbstractIntVector stride, RMissing lengthOut, RMissing alongWith) {
        return seq(start, to, RClosures.createIntToDoubleVector(stride), lengthOut, alongWith);
    }

    @Specialization
    protected Object seq(RAbstractIntVector start, RAbstractDoubleVector to, RAbstractDoubleVector stride, RMissing lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            int length = (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) / stride.getDataAt(0));
            if (start.getDataAt(0) + length * stride.getDataAt(0) == to.getDataAt(0)) {
                length++;
            }
            return RDataFactory.createDoubleSequence(start.getDataAt(0), stride.getDataAt(0), length);
        }
    }

    @Specialization
    protected RAbstractVector seqLengthZero(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(lengthOut == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return getVectorWithComputedStride(RRuntime.int2double(start.getDataAt(0)), to.getDataAt(0), lengthOut, ascending(start, to));
        }
    }

    @Specialization
    protected RAbstractVector seqLengthZero(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(lengthOut == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return getVectorWithComputedStride(RRuntime.int2double(start.getDataAt(0)), to.getDataAt(0), lengthOut, ascending(start, to));
        }
    }

    // double vector start, missing to

    @Specialization
    protected RAbstractVector seqFromOneArg(RAbstractDoubleVector start, RMissing to, RMissing stride, RMissing lengthOut, RMissing alongWith) {
        if (topLengthProfile.profile(start.getLength() == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParam(start.getDataAt(0), "from");
            // GNU R really does that (take the length of start to create a sequence)
            return RDataFactory.createDoubleSequence(1, 1, start.getLength());
        }
    }

    @Specialization
    protected RAbstractVector seqStartLengthOne(RAbstractDoubleVector start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        startLengthOne(start);
        validateParam(start.getDataAt(0), "from");
        if (topLengthProfile.profile(lengthOut == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, lengthOut);
        }
    }

    @Specialization
    protected RAbstractVector seqStartLengthOne(RAbstractDoubleVector start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        startLengthOne(start);
        validateParam(start.getDataAt(0), "from");
        if (topLengthProfile.profile(lengthOut == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, (int) Math.ceil(lengthOut));
        }
    }

    @Specialization
    protected RAbstractVector seqStartLengthOne(RAbstractDoubleVector start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        startLengthOne(start);
        validateParam(start.getDataAt(0), "from");
        if (topLengthProfile.profile(alongWith.getLength() == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, alongWith.getLength());
        }
    }

    // double vector start, int vector to

    @Specialization
    protected Object seq(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut, RMissing alongWith,
                    @Cached("createBinaryProfile()") ConditionProfile intProfile) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            if (intProfile.profile((int) start.getDataAt(0) == start.getDataAt(0))) {
                return RDataFactory.createIntSequence((int) start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
            } else {
                return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
            }
        }
    }

    @Specialization
    protected Object seq(RAbstractDoubleVector start, RAbstractIntVector to, RAbstractIntVector stride, RMissing lengthOut, RMissing alongWith) {
        return seq(start, to, RClosures.createIntToDoubleVector(stride), lengthOut, alongWith);
    }

    @Specialization
    protected Object seq(RAbstractDoubleVector start, RAbstractIntVector to, RAbstractDoubleVector stride, RMissing lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            return RDataFactory.createDoubleSequence(start.getDataAt(0), stride.getDataAt(0), (int) Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride.getDataAt(0)) + 1);
        }
    }

    @Specialization
    protected RAbstractVector seqLengthZero(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        if (topLengthProfile.profile(lengthOut == 0)) {
            validateParams(start, to);
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParams(start, to);
            return getVectorWithComputedStride(start.getDataAt(0), RRuntime.int2double(to.getDataAt(0)), lengthOut, ascending(start, to));
        }
    }

    @Specialization
    protected RAbstractVector seqLengthZero(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        if (topLengthProfile.profile(lengthOut == 0)) {
            validateParams(start, to);
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParams(start, to);
            return getVectorWithComputedStride(start.getDataAt(0), RRuntime.int2double(to.getDataAt(0)), lengthOut, ascending(start, to));
        }
    }

    // double vector start, double vector to

    @Specialization
    protected Object seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut, RMissing alongWith, //
                    @Cached("createBinaryProfile()") ConditionProfile intProfile) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            if (intProfile.profile((int) start.getDataAt(0) == start.getDataAt(0) && (int) to.getDataAt(0) == to.getDataAt(0))) {
                return RDataFactory.createIntSequence((int) start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
            } else {
                return RDataFactory.createDoubleSequence(start.getDataAt(0), ascending(start, to) ? 1 : -1, (int) (Math.abs(to.getDataAt(0) - start.getDataAt(0)) + 1));
            }
        }
    }

    @Specialization
    protected Object seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RAbstractIntVector stride, RMissing lengthOut, RMissing alongWith) {
        return seq(start, to, RClosures.createIntToDoubleVector(stride), lengthOut, alongWith);
    }

    @Specialization
    protected Object seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RAbstractDoubleVector stride, RMissing lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        validateParams(start, to);
        if (topLengthProfile.profile(zero(start, to))) {
            return 0;
        } else {
            return RDataFactory.createDoubleSequence(start.getDataAt(0), stride.getDataAt(0), (int) (Math.abs((to.getDataAt(0) - start.getDataAt(0)) / stride.getDataAt(0)) + 1));
        }
    }

    @Specialization
    protected RAbstractVector seqLengthZero(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        if (topLengthProfile.profile(lengthOut == 0)) {
            validateParams(start, to);
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParams(start, to);
            return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), RRuntime.int2double(lengthOut), ascending(start, to));
        }
    }

    @Specialization
    protected RAbstractVector seqLengthZero(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        startLengthOne(start);
        toLengthOne(to);
        if (topLengthProfile.profile(lengthOut == 0)) {
            validateParams(start, to);
            return RDataFactory.createEmptyIntVector();
        } else {
            validateParams(start, to);
            return getVectorWithComputedStride(start.getDataAt(0), to.getDataAt(0), lengthOut, ascending(start, to));
        }
    }

    // logical vectors

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

    @Specialization
    protected RAbstractVector seqLengthZero(RMissing start, RMissing to, RMissing stride, int lengthOut, RMissing alongWith) {
        if (topLengthProfile.profile(lengthOut == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createIntSequence(1, 1, lengthOut);
        }
    }

    @Specialization
    protected RAbstractVector seqLengthZero(RMissing start, RMissing to, RMissing stride, double lengthOut, RMissing alongWith) {
        if (topLengthProfile.profile(lengthOut == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createIntSequence(1, 1, (int) Math.ceil(lengthOut));
        }
    }

    @Specialization
    protected RAbstractVector seqLengthZeroAlong(RMissing start, RMissing to, RMissing stride, Object lengthOut, RAbstractVector alongWith) {
        if (topLengthProfile.profile(alongWith.getLength() == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            return RDataFactory.createIntSequence(1, 1, alongWith.getLength());
        }
    }

    @Specialization
    protected RDoubleSequence seq(RMissing start, RAbstractIntVector to, RMissing stride, int lengthOut, RMissing alongWith) {
        toLengthOne(to);
        positiveLengthOut(lengthOut);
        validateParam(to.getDataAt(0), "to");
        return RDataFactory.createDoubleSequence(to.getDataAt(0) - lengthOut + 1, 1, lengthOut);
    }

    @Specialization
    protected RDoubleSequence seq(RMissing start, RAbstractIntVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        return seq(start, to, stride, (int) Math.ceil(lengthOut), alongWith);
    }

    @Specialization
    protected RDoubleSequence seq(RMissing start, RAbstractDoubleVector to, RMissing stride, double lengthOut, RMissing alongWith) {
        toLengthOne(to);
        positiveLengthOut(lengthOut);
        validateParam(to.getDataAt(0), "to");
        return RDataFactory.createDoubleSequence(to.getDataAt(0) - lengthOut + 1, 1, (int) Math.ceil(lengthOut));
    }

    @Specialization
    protected Object seq(RMissing start, RAbstractVector to, Object stride, RMissing lengthOut, RMissing alongWith) {
        toLengthOne(to);
        return seqRecursive(1.0, to, stride, lengthOut, alongWith);
    }

    private static boolean ascending(RAbstractIntVector start, RAbstractIntVector to) {
        return to.getDataAt(0) > start.getDataAt(0);
    }

    private static boolean ascending(RAbstractIntVector start, RAbstractDoubleVector to) {
        return to.getDataAt(0) > start.getDataAt(0);
    }

    private static boolean ascending(RAbstractDoubleVector start, RAbstractIntVector to) {
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

    protected boolean startLengthOne(RAbstractVector start) {
        if (start.getLength() != 1) {
            error.enter();
            throw RError.error(this, RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean toLengthOne(RAbstractVector to) {
        if (to.getLength() != 1) {
            error.enter();
            throw RError.error(this, RError.Message.MUST_BE_SCALAR, "to");
        }
        return true;
    }

    protected boolean positiveLengthOut(int lengthOut) {
        if (lengthOut < 0) {
            error.enter();
            throw RError.error(this, RError.Message.MUST_BE_POSITIVE, "length.out");
        }
        return true;
    }

    protected boolean positiveLengthOut(double lengthOut) {
        if (lengthOut < 0) {
            error.enter();
            throw RError.error(this, RError.Message.MUST_BE_POSITIVE, "length.out");
        }
        return true;
    }
}
