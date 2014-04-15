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
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("as.complex")
@SuppressWarnings("unused")
public abstract class AsComplex extends RBuiltinNode {

    @Child CastComplexNode castComplexNode;

    public abstract RComplexVector executeRComplexVector(VirtualFrame frame, Object o) throws UnexpectedResultException;

    private void initCast() {
        if (castComplexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplexNode = insert(CastComplexNodeFactory.create(null, false, false, false));
        }
    }

    private RComplex castComplex(VirtualFrame frame, int o) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(frame, o);
    }

    private RComplex castComplex(VirtualFrame frame, double o) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(frame, o);
    }

    private RComplex castComplex(VirtualFrame frame, byte o) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(frame, o);
    }

    private RComplex castComplex(VirtualFrame frame, Object o) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(frame, o);
    }

    private RComplexVector castComplexVector(VirtualFrame frame, Object o) {
        initCast();
        return (RComplexVector) castComplexNode.executeComplex(frame, o);
    }

    @Specialization
    public RComplex doComplex(RComplex value) {
        controlVisibility();
        return value;
    }

    @Specialization
    public RComplex doInt(VirtualFrame frame, int value) {
        controlVisibility();
        return castComplex(frame, value);
    }

    @Specialization
    public RComplex doDouble(VirtualFrame frame, double value) {
        controlVisibility();
        return castComplex(frame, value);
    }

    @Specialization
    public RComplex doLogical(VirtualFrame frame, byte value) {
        controlVisibility();
        return castComplex(frame, value);
    }

    @Specialization
    public RComplex doString(VirtualFrame frame, String value) {
        controlVisibility();
        return castComplex(frame, value);
    }

    @Specialization
    public RComplexVector doNull(RNull value) {
        controlVisibility();
        return RDataFactory.createComplexVector(0);
    }

    @Specialization
    public RComplexVector doComplexVector(VirtualFrame frame, RComplexVector vector) {
        controlVisibility();
        return RDataFactory.createComplexVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization
    public RComplexVector doIntVector(VirtualFrame frame, RAbstractVector vector) {
        controlVisibility();
        return castComplexVector(frame, vector);
    }
}
