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
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("as.complex")
@SuppressWarnings("unused")
public abstract class AsComplex extends RBuiltinNode {

    private final NACheck check;

    @Child CastComplexNode castComplexNode;

    public abstract RComplexVector executeRComplexVector(VirtualFrame frame, Object o) throws UnexpectedResultException;

    private RComplex castComplex(VirtualFrame frame, Object o) {
        if (castComplexNode == null) {
            CompilerDirectives.transferToInterpreter();
            castComplexNode = adoptChild(CastComplexNodeFactory.create(null));
        }
        return (RComplex) castComplexNode.executeComplex(frame, o);
    }

    private RComplexVector castComplexVector(VirtualFrame frame, Object o) {
        if (castComplexNode == null) {
            CompilerDirectives.transferToInterpreter();
            castComplexNode = adoptChild(CastComplexNodeFactory.create(null));
        }
        return (RComplexVector) castComplexNode.executeComplexVector(frame, o);
    }

    protected AsComplex() {
        this.check = NACheck.create();
    }

    protected AsComplex(AsComplex other) {
        this.check = other.check;
    }

    @Specialization
    public RComplex doComplex(RComplex value) {
        return value;
    }

    @Specialization
    public RComplex doInt(int value) {
        check.enable(value);
        return check.convertIntToComplex(value);
    }

    @Specialization
    public RComplex doDouble(double value) {
        check.enable(value);
        return check.convertDoubleToComplex(value);
    }

    @Specialization
    public RComplex doLogical(byte value) {
        check.enable(value);
        return check.convertLogicalToComplex(value);
    }

    @Specialization
    public RComplex doString(VirtualFrame frame, String value) {
        return castComplex(frame, value);
    }

    @Specialization
    public RComplexVector doNull(RNull value) {
        return RDataFactory.createComplexVector(0);
    }

    @Specialization
    public RComplexVector doComplexVector(VirtualFrame frame, RComplexVector vector) {
        return vector;
    }

    @Specialization
    public RComplexVector doIntVector(VirtualFrame frame, RIntVector vector) {
        return castComplexVector(frame, vector);
    }

    @Specialization
    public RComplexVector doDoubleVector(VirtualFrame frame, RDoubleVector vector) {
        return castComplexVector(frame, vector);
    }

    @Specialization
    public RComplexVector doLogicalVector(VirtualFrame frame, RLogicalVector vector) {
        return castComplexVector(frame, vector);
    }

    @Specialization
    public RComplexVector doRawVector(VirtualFrame frame, RRawVector vector) {
        return castComplexVector(frame, vector);
    }

    @Specialization
    public RComplexVector doStringVector(VirtualFrame frame, RStringVector vector) {
        return castComplexVector(frame, vector);
    }

    @Specialization
    public RComplexVector doIntSequence(VirtualFrame frame, RIntSequence sequence) {
        return castComplexVector(frame, sequence);
    }

    @Specialization
    public RComplexVector doDoubleSequence(VirtualFrame frame, RDoubleSequence sequence) {
        return castComplexVector(frame, sequence);
    }
}
