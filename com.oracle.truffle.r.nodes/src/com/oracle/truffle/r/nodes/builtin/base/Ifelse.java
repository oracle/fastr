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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@SuppressWarnings("unused")
@RBuiltin(name = "ifelse", kind = SUBSTITUTE)
// TODO revert to R
public abstract class Ifelse extends RBuiltinNode {

    private final NACheck na = NACheck.create();

    protected boolean isNA(byte test) {
        na.enable(test);
        return na.check(test);
    }

    @Specialization(order = 0, guards = "isNA")
    public byte ifelseNA(byte test, double yes, double no) {
        controlVisibility();
        return RRuntime.LOGICAL_NA;
    }

    @Specialization(order = 1, guards = "!isNA")
    public double ifelse(byte test, double yes, double no) {
        controlVisibility();
        return test == RRuntime.LOGICAL_TRUE ? yes : no;
    }

    @Specialization(order = 2, guards = "isNA")
    public byte ifelseNA(byte test, int yes, int no) {
        controlVisibility();
        return RRuntime.LOGICAL_NA;
    }

    @Specialization(order = 3, guards = "!isNA")
    public int ifelse(byte test, int yes, int no) {
        controlVisibility();
        return test == RRuntime.LOGICAL_TRUE ? yes : no;
    }

    @Specialization(order = 4, guards = "isNA")
    public byte ifelseNA(byte test, String yes, String no) {
        controlVisibility();
        return RRuntime.LOGICAL_NA;
    }

    @Specialization(order = 5, guards = "!isNA")
    public String ifelse(byte test, String yes, String no) {
        controlVisibility();
        return test == RRuntime.LOGICAL_TRUE ? yes : no;
    }

    @Specialization(order = 100)
    public RDoubleVector ifelse(RLogicalVector lvec, RDoubleVector dvec, double no) {
        // just one special case for version.R
        assert lvec.getLength() == 1;
        double[] data = new double[]{lvec.getDataAt(0) == RRuntime.LOGICAL_TRUE ? dvec.getDataAt(0) : no};
        return RDataFactory.createDoubleVector(data, data[0] != RRuntime.DOUBLE_NA);
    }

}
