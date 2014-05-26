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
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@SuppressWarnings("unused")
public abstract class IsTypeNode extends RBuiltinNode {

    @Specialization
    public byte isType(int value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RIntVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(double value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RDoubleVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(String value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RStringVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(byte value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RLogicalVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RNull value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RComplex value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RComplexVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RRaw value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RRawVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RList value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RIntSequence value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RDoubleSequence value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(RFunction value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte isType(Object value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }
}
