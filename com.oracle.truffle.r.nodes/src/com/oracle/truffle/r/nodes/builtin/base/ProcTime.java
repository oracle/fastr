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

@RBuiltin("proc.time")
public abstract class ProcTime extends RBuiltinNode {

    /**
     * Very basic implementation of proc.time(). Unlike GNU R, we only provide the third (of five)
     * values, and this value is not the "total elapsed time for the current R process", but rather
     * the current time (in seconds). Used for benchmarking.
     */
    @Specialization
    public RDoubleVector procTime() {
        double[] data = new double[5];
        data[2] = System.currentTimeMillis() / 1000.0;
        return RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
    }

}
