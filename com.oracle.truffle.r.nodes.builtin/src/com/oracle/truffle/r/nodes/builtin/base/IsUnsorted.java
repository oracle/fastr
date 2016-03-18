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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.binary.BinaryMapBooleanFunctionNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.BinaryCompare;

@RBuiltin(name = "is.unsorted", kind = INTERNAL, parameterNames = {"x", "strictly"})
// TODO support strictly
// TODO support lists
public abstract class IsUnsorted extends RBuiltinNode {

    @Child private BinaryMapBooleanFunctionNode ge = new BinaryMapBooleanFunctionNode(BinaryCompare.GREATER_EQUAL.create());

    @Specialization
    protected byte isUnsorted(RAbstractDoubleVector x, @SuppressWarnings("unused") byte strictly) {
        controlVisibility();
        double last = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            double current = x.getDataAt(k);
            if (ge.applyLogical(current, last) == RRuntime.LOGICAL_FALSE) {
                return RRuntime.LOGICAL_TRUE;
            }
            last = current;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isUnsorted(RAbstractIntVector x, @SuppressWarnings("unused") byte strictly) {
        controlVisibility();
        int last = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            int current = x.getDataAt(k);
            if (ge.applyLogical(current, last) == RRuntime.LOGICAL_FALSE) {
                return RRuntime.LOGICAL_TRUE;
            }
            last = current;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isUnsorted(RAbstractStringVector x, @SuppressWarnings("unused") byte strictly) {
        controlVisibility();
        String last = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); k++) {
            String current = x.getDataAt(k);
            if (ge.applyLogical(current, last) == RRuntime.LOGICAL_FALSE) {
                return RRuntime.LOGICAL_TRUE;
            }
            last = current;
        }
        return RRuntime.LOGICAL_FALSE;
    }
}
