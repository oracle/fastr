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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

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

    @Specialization(order = 0, guards = "zero")
    public int seq(int start, int to, Object stride, RMissing lengthOut) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 1, guards = "ascending")
    public RIntSequence seq(int start, int to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(start, 1, to - start + 1);
    }

    @Specialization(order = 2, guards = "!ascending")
    public RIntSequence seqIntDesc(int start, int to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(start, -1, start - to + 1);
    }

    @Specialization(order = 10, guards = "ascending")
    public RIntSequence seq(int start, int to, int stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(start, stride, (to - start + 1) / stride);
    }

    @Specialization(order = 11, guards = "!ascending")
    public RIntSequence seqIntDesc(int start, int to, int stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createIntSequence(start, stride, (start - to + 1) / -stride);
    }

    @Specialization(order = 100, guards = "zero")
    public double seq(double start, double to, Object stride, RMissing lengthOut) {
        controlVisibility();
        return 0;
    }

    @Specialization(order = 101, guards = "ascending")
    public RDoubleSequence seq(double start, double to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, 1, (int) (to - start + 1));
    }

    @Specialization(order = 102, guards = "!ascending")
    public RDoubleSequence seqIntDesc(double start, double to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, -1, (int) (start - to + 1));
    }

    @Specialization(order = 110, guards = "ascending")
    public RDoubleSequence seq(double start, double to, double stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, stride, (int) ((to - start + 1) / stride));
    }

    @Specialization(order = 111, guards = "!ascending")
    public RDoubleSequence seqIntDesc(double start, double to, double stride, RMissing lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, stride, (int) ((start - to + 1) / -stride));
    }

    @Specialization(order = 120)
    public int seqFrom(double start, RMissing to, RMissing stride, RMissing lengthOut) {
        controlVisibility();
        return (int) start;
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

    @Specialization(order = 160)
    public RDoubleSequence seq(double start, RMissing to, RMissing stride, int lengthOut) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, 1, lengthOut);
    }

    protected static boolean ascending(int start, int to) {
        return to > start;
    }

    protected static boolean ascending(double start, double to) {
        return to > start;
    }

    protected static boolean zero(int start, int to) {
        return start == 0 && to == 0;
    }

    protected static boolean zero(double start, double to) {
        return start == 0 && to == 0;
    }

}
