/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Arguably unnecessary when {@code identical} is implemented, as {@code isTRUE(x)} is defined to be
 * {@code identical(TRUE, x}.
 */
@RBuiltin({"isTRUE"})
public abstract class IsTRUE extends RBuiltinNode {

    @Specialization(order = 0)
    public RLogicalVector isTRUE(byte x) {
        byte xx = x;
        if (x == RRuntime.LOGICAL_NA) {
            xx = RRuntime.LOGICAL_FALSE;
        }
        return RDataFactory.createLogicalVectorFromScalar(xx);
    }

    @Specialization(order = 1, guards = "exactlyTrue")
    public RLogicalVector isTRUE(@SuppressWarnings("unused") RLogicalVector x) {
        return RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_TRUE);
    }

    @Generic
    public RLogicalVector isTRUEGeneric(@SuppressWarnings("unused") Object x) {
        return RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_FALSE);
    }

    public static boolean exactlyTrue(RLogicalVector v) {
        return v.getLength() == 1 && v.getDataAt(0) == RRuntime.LOGICAL_TRUE && (v.getAttributes() == null || v.getAttributes().isEmpty());
    }
}
