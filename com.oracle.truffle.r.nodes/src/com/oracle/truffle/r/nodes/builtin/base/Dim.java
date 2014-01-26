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
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("dim")
@SuppressWarnings("unused")
public abstract class Dim extends RBuiltinNode {

    @Specialization
    public RNull dim(RNull vector) {
        return RNull.instance;
    }

    @Specialization
    public RNull dim(int vector) {
        return RNull.instance;
    }

    @Specialization
    public RNull dim(double vector) {
        return RNull.instance;
    }

    @Specialization
    public RNull dim(byte vector) {
        return RNull.instance;
    }

    @Specialization(guards = "!hasDimensions")
    public RNull dim(RAbstractVector vector) {
        return RNull.instance;
    }

    @Specialization(guards = "hasDimensions")
    public RIntVector dimWithDimensions(RAbstractVector vector) {
        return RDataFactory.createIntVector(vector.getDimensions(), RDataFactory.COMPLETE_VECTOR);
    }

    public static boolean hasDimensions(RAbstractVector vector) {
        return vector.hasDimensions();
    }
}
