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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "as.integer", kind = PRIMITIVE, parameterNames = {"x", "..."})
@SuppressWarnings("unused")
public abstract class AsInteger extends RBuiltinNode {

    @Child private CastIntegerNode castIntNode;

    private void initCast() {
        if (castIntNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castIntNode = insert(CastIntegerNodeFactory.create(null, false, false, false));
        }
    }

    private int castInt(VirtualFrame frame, int o) {
        initCast();
        return (int) castIntNode.executeInt(frame, o);
    }

    private int castInt(VirtualFrame frame, double o) {
        initCast();
        return (int) castIntNode.executeInt(frame, o);
    }

    private int castInt(VirtualFrame frame, byte o) {
        initCast();
        return (int) castIntNode.executeInt(frame, o);
    }

    private int castInt(VirtualFrame frame, Object o) {
        initCast();
        return (int) castIntNode.executeInt(frame, o);
    }

    private RAbstractIntVector castIntVector(VirtualFrame frame, Object o) {
        initCast();
        return (RAbstractIntVector) castIntNode.executeInt(frame, o);
    }

    @Specialization
    protected int asInteger(int value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected int asInteger(VirtualFrame frame, double value) {
        controlVisibility();
        return castInt(frame, value);
    }

    @Specialization
    protected int asInteger(VirtualFrame frame, byte value) {
        controlVisibility();
        return castInt(frame, value);
    }

    @Specialization
    protected int asInteger(VirtualFrame frame, RComplex value) {
        controlVisibility();
        return castInt(frame, value);
    }

    @Specialization
    protected int asInteger(VirtualFrame frame, RRaw value) {
        controlVisibility();
        return castInt(frame, value);
    }

    @Specialization
    protected int asInteger(VirtualFrame frame, String value) {
        controlVisibility();
        return castInt(frame, value);
    }

    @Specialization
    protected int asInteger(RNull vector) {
        controlVisibility();
        return RRuntime.INT_NA;
    }

    @Specialization
    protected RIntVector asInteger(RIntVector vector) {
        controlVisibility();
        return RDataFactory.createIntVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization
    protected RIntVector asInteger(RIntSequence sequence) {
        controlVisibility();
        return (RIntVector) sequence.createVector();
    }

    @Specialization
    protected RAbstractIntVector asInteger(VirtualFrame frame, RAbstractVector vector) {
        controlVisibility();
        return castIntVector(frame, vector);
    }
}
