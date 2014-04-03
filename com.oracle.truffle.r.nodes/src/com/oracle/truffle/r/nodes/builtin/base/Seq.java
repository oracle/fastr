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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin("seq")
@SuppressWarnings("unused")
public abstract class Seq extends RBuiltinNode {

    public static boolean ascending(int start, int to) {
        return to > start;
    }

    public static boolean ascending(double start, double to) {
        return to > start;
    }

    @Specialization(order = 1, guards = "ascending")
    public RIntSequence seq(int start, int to, RMissing stride) {
        controlVisibility();
        return RDataFactory.createIntSequence(start, 1, to - start + 1);
    }

    @Specialization(order = 2, guards = "!ascending")
    public RIntSequence seqIntDesc(int start, int to, RMissing stride) {
        controlVisibility();
        return RDataFactory.createIntSequence(start, -1, start - to + 1);
    }

    @Specialization(order = 10, guards = "ascending")
    public RIntSequence seq(int start, int to, int stride) {
        controlVisibility();
        return RDataFactory.createIntSequence(start, stride, (to - start + 1) / stride);
    }

    @Specialization(order = 11, guards = "!ascending")
    public RIntSequence seqIntDesc(int start, int to, int stride) {
        controlVisibility();
        return RDataFactory.createIntSequence(start, stride, (start - to + 1) / -stride);
    }

    @Specialization(order = 100, guards = "ascending")
    public RDoubleSequence seq(double start, double to, RMissing stride) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, 1, (int) (to - start + 1));
    }

    @Specialization(order = 102, guards = "!ascending")
    public RDoubleSequence seqIntDesc(double start, double to, RMissing stride) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, -1, (int) (start - to + 1));
    }

    @Specialization(order = 110, guards = "ascending")
    public RDoubleSequence seq(double start, double to, double stride) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, stride, (int) ((to - start + 1) / stride));
    }

    @Specialization(order = 111, guards = "!ascending")
    public RDoubleSequence seqIntDesc(double start, double to, double stride) {
        controlVisibility();
        return RDataFactory.createDoubleSequence(start, stride, (int) ((start - to + 1) / -stride));
    }

    @Specialization(order = 150)
    public RIntSequence seq(int start, RMissing to, RMissing stride) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, start);
    }

    @Specialization(order = 151)
    public RIntSequence seq(double start, RMissing to, RMissing stride) {
        controlVisibility();
        return RDataFactory.createIntSequence(1, 1, (int) (start));
    }

}
