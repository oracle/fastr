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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin("match")
public abstract class Match extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "table", "nomatch", "incomparables"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.INT_NA), ConstantNode.create(RNull.instance)};
    }

    @Child protected BooleanOperation eq = BinaryCompare.EQUAL.create();

    // FIXME deal with nomatch and incomparables parameters
    // FIXME deal with NA etc.

    @Specialization(order = 10)
    @SuppressWarnings("unused")
    public byte match(double x, RDoubleVector table, int nomatch, Object incomparables) {
        for (int k = 0; k < table.getLength(); ++k) {
            if (eq.op(x, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(order = 11)
    @SuppressWarnings("unused")
    public RLogicalVector match(RDoubleVector x, RDoubleVector table, int nomatch, Object incomparables) {
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            double xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 20)
    @SuppressWarnings("unused")
    public byte match(String x, RStringVector table, int nomatch, Object incomparables) {
        for (int k = 0; k < table.getLength(); ++k) {
            if (eq.op(x, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(order = 21)
    @SuppressWarnings("unused")
    public RLogicalVector match(RStringVector x, RStringVector table, int nomatch, Object incomparables) {
        byte[] result = new byte[x.getLength()]; // pre-initialised with LOGICAL_FALSE (0)
        for (int i = 0; i < result.length; ++i) {
            String xx = x.getDataAt(i);
            for (int k = 0; k < table.getLength(); ++k) {
                if (eq.op(xx, table.getDataAt(k)) == RRuntime.LOGICAL_TRUE) {
                    result[i] = RRuntime.LOGICAL_TRUE;
                    break;
                }
            }
        }
        return RDataFactory.createLogicalVector(result, RDataFactory.COMPLETE_VECTOR);
    }

}
