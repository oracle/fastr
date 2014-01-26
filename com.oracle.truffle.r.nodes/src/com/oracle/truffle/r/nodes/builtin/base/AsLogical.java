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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("as.logical")
@SuppressWarnings("unused")
public abstract class AsLogical extends RBuiltinNode {

    private final NACheck check = NACheck.create();

    @Child CastLogicalNode castLogicalNode;

    public abstract RLogicalVector executeRLogicalVector(VirtualFrame frame, Object o) throws UnexpectedResultException;

    private byte castLogical(VirtualFrame frame, Object o) {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreter();
            castLogicalNode = adoptChild(CastLogicalNodeFactory.create(null, false, false, getContext()));
        }
        return (byte) castLogicalNode.executeByte(frame, o);
    }

    private RLogicalVector castLogicalVector(VirtualFrame frame, Object o) {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreter();
            castLogicalNode = adoptChild(CastLogicalNodeFactory.create(null, false, false, getContext()));
        }
        return (RLogicalVector) castLogicalNode.executeLogicalVector(frame, o);
    }

    @Specialization
    public byte asLogical(byte value) {
        return value;
    }

    @Specialization(order = 10)
    public byte asLogical(int value) {
        check.enable(value);
        return check.convertIntToLogical(value);
    }

    @Specialization(order = 12)
    public byte asLogical(double value) {
        check.enable(value);
        return check.convertDoubleToLogical(value);
    }

    @Specialization(order = 14)
    public byte asLogical(RComplex value) {
        check.enable(value);
        return check.convertComplexToLogical(value);
    }

    @Specialization
    public byte asLogical(VirtualFrame frame, String value) {
        check.enable(value);
        return check.convertStringToLogical(getContext(), value);
    }

    @Specialization
    public RLogicalVector asLogical(RNull vector) {
        return RDataFactory.createLogicalVector(0);
    }

    @Specialization
    public RLogicalVector asLogical(RLogicalVector vector) {
        return vector;
    }

    @Specialization
    public RLogicalVector asLogical(VirtualFrame frame, RIntVector vector) {
        return castLogicalVector(frame, vector);
    }

    @Specialization
    public RLogicalVector asLogical(VirtualFrame frame, RStringVector vector) {
        return castLogicalVector(frame, vector);
    }

    @Specialization
    public RLogicalVector asLogical(VirtualFrame frame, RDoubleVector vector) {
        return castLogicalVector(frame, vector);
    }

    @Specialization
    public RLogicalVector asLogical(VirtualFrame frame, RComplexVector vector) {
        return castLogicalVector(frame, vector);
    }

    @Specialization
    public RLogicalVector asLogical(VirtualFrame frame, RDoubleSequence sequence) {
        return castLogicalVector(frame, sequence);
    }

    @Specialization
    public RLogicalVector asLogical(VirtualFrame frame, RIntSequence sequence) {
        return castLogicalVector(frame, sequence);
    }

    @Specialization
    public RLogicalVector asLogical(VirtualFrame frame, RRawVector vector) {
        return castLogicalVector(frame, vector);
    }
}
