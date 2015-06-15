/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "as.complex", kind = PRIMITIVE, parameterNames = {"x", "..."})
public abstract class AsComplex extends RBuiltinNode {

    @Child private CastComplexNode castComplexNode;

    private void initCast() {
        if (castComplexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplexNode = insert(CastComplexNodeGen.create(false, false, false));
        }
    }

    private RComplex castComplex(int o) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(o);
    }

    private RComplex castComplex(double o) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(o);
    }

    private RComplex castComplex(byte o) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(o);
    }

    private RComplex castComplex(Object o) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(o);
    }

    private RComplexVector castComplexVector(Object o) {
        initCast();
        return (RComplexVector) castComplexNode.executeComplex(o);
    }

    @Specialization
    protected RComplex doComplex(RComplex value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected RComplex doInt(int value) {
        controlVisibility();
        return castComplex(value);
    }

    @Specialization
    protected RComplex doDouble(double value) {
        controlVisibility();
        return castComplex(value);
    }

    @Specialization
    protected RComplex doLogical(byte value) {
        controlVisibility();
        return castComplex(value);
    }

    @Specialization
    protected RComplex doString(String value) {
        controlVisibility();
        return castComplex(value);
    }

    @Specialization
    protected RComplexVector doNull(@SuppressWarnings("unused") RNull value) {
        controlVisibility();
        return RDataFactory.createComplexVector(0);
    }

    @Specialization
    protected RComplexVector doComplexVector(RComplexVector vector) {
        controlVisibility();
        return RDataFactory.createComplexVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization
    protected RComplexVector doIntVector(RAbstractVector vector) {
        controlVisibility();
        return castComplexVector(vector);
    }
}
