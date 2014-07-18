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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "seq", aliases = {"seq.int"}, kind = SUBSTITUTE)
// Implement in R, but seq.int is PRIMITIVE
@SuppressWarnings("unused")
public abstract class Seq extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"from", "to", "by", "length.out"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization(order = 0, guards = {"zero", "startLengthOne", "toLengthOne"})
    public int seq(RAbstractIntVector start, RAbstractIntVector to, Object stride, RMissing lengthOut) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 1, guards = {"startLengthOne", "toLengthOne", "ascending"})
    public RIntSequence seq(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), 1, to.getDataAt(0) - start.getDataAt(0) + 1);
    }

    @Specialization(order = 2, guards = {"startLengthOne", "toLengthOne", "!ascending"})
    public RIntSequence seqIntDesc(RAbstractIntVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), -1, start.getDataAt(0) - to.getDataAt(0) + 1);
    }

    @Specialization(order = 10, guards = {"startLengthOne", "toLengthOne", "ascending"})
    public RIntSequence seq(RAbstractIntVector start, RAbstractIntVector to, int stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), stride, (to.getDataAt(0) - start.getDataAt(0) + 1) / stride);
    }

    @Specialization(order = 11, guards = {"startLengthOne", "toLengthOne", "!ascending"})
    public RIntSequence seqIntDesc(RAbstractIntVector start, RAbstractIntVector to, int stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(start.getDataAt(0), stride, (start.getDataAt(0) - to.getDataAt(0) + 1) / -stride);
    }

    @Specialization(order = 30, guards = {"startLengthOne", "toLengthOne", "zero"})
    public double seq(RAbstractIntVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 31, guards = {"startLengthOne", "toLengthOne", "ascending"})
    public RDoubleSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, (int) (to.getDataAt(0) - start.getDataAt(0) + 1));
    }

    @Specialization(order = 32, guards = {"startLengthOne", "toLengthOne", "!ascending"})
    public RDoubleSequence seqIntDesc(RAbstractIntVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), -1, (int) (start.getDataAt(0) - to.getDataAt(0) + 1));
    }

    @Specialization(order = 40, guards = {"startLengthOne", "toLengthOne", "ascending"})
    public RDoubleSequence seq(RAbstractIntVector start, RAbstractDoubleVector to, int stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (to.getDataAt(0) - start.getDataAt(0) + 1) / stride);
    }

    @Specialization(order = 41, guards = {"startLengthOne", "toLengthOne", "!ascending"})
    public RDoubleSequence seqIntDesc(RAbstractIntVector start, RAbstractDoubleVector to, int stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (start.getDataAt(0) - to.getDataAt(0) + 1) / -stride);
    }

    @Specialization(order = 50, guards = {"startLengthOne", "toLengthOne", "zero"})
    public double seq(RAbstractDoubleVector start, RAbstractIntVector to, Object stride, RMissing lengthOut) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 51, guards = {"startLengthOne", "toLengthOne", "ascending"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, (int) (to.getDataAt(0) - start.getDataAt(0) + 1));
    }

    @Specialization(order = 52, guards = {"startLengthOne", "toLengthOne", "!ascending"})
    public RDoubleSequence seqIntDesc(RAbstractDoubleVector start, RAbstractIntVector to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), -1, (int) (start.getDataAt(0) - to.getDataAt(0) + 1));
    }

    @Specialization(order = 60, guards = {"startLengthOne", "toLengthOne", "ascending"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractIntVector to, int stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (to.getDataAt(0) - start.getDataAt(0) + 1) / stride);
    }

    @Specialization(order = 61, guards = {"startLengthOne", "toLengthOne", "!ascending"})
    public RDoubleSequence seqIntDesc(RAbstractDoubleVector start, RAbstractIntVector to, int stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) (start.getDataAt(0) - to.getDataAt(0) + 1) / -stride);
    }

    @Specialization(order = 100, guards = {"startLengthOne", "toLengthOne", "zero"})
    public double seq(RAbstractDoubleVector start, RAbstractDoubleVector to, Object stride, RMissing lengthOut) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 101, guards = {"startLengthOne", "toLengthOne", "ascending"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, (int) (to.getDataAt(0) - start.getDataAt(0) + 1));
    }

    @Specialization(order = 102, guards = {"startLengthOne", "toLengthOne", "!ascending"})
    public RDoubleSequence seqIntDesc(RAbstractDoubleVector start, RAbstractDoubleVector to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), -1, (int) (start.getDataAt(0) - to.getDataAt(0) + 1));
    }

    @Specialization(order = 110, guards = {"startLengthOne", "toLengthOne", "ascending"})
    public RDoubleSequence seq(RAbstractDoubleVector start, RAbstractDoubleVector to, double stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) ((to.getDataAt(0) - start.getDataAt(0) + 1) / stride));
    }

    @Specialization(order = 111, guards = {"startLengthOne", "toLengthOne", "!ascending"})
    public RDoubleSequence seqIntDesc(RAbstractDoubleVector start, RAbstractDoubleVector to, double stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), stride, (int) ((start.getDataAt(0) - to.getDataAt(0) + 1) / -stride));
    }

    @Specialization(order = 120, guards = "startLengthOne")
    public int seqFrom(RAbstractDoubleVector start, RMissing to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return (int) start.getDataAt(0);
    }

    @Specialization(order = 150)
    public RIntSequence seq(RMissing start, RMissing to, RMissing stride, int lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, lengthOut);
    }

    @Specialization(order = 151)
    public RIntSequence seq(RMissing start, RMissing to, RMissing stride, double lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, (int) lengthOut);
    }

    @Specialization(order = 160, guards = "startLengthOne")
    public RDoubleSequence seq(RAbstractDoubleVector start, RMissing to, RMissing stride, int lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start.getDataAt(0), 1, lengthOut);
    }

    protected static boolean ascending(RAbstractIntVector start, RAbstractIntVector to) {
        return to.getDataAt(0) > start.getDataAt(0);
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
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "from");
        }
        return true;
    }

    protected boolean toLengthOne(RAbstractVector start, RAbstractVector to) {
        if (to.getLength() != 1) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_SCALAR, "to");
        }
        return true;
    }
}
