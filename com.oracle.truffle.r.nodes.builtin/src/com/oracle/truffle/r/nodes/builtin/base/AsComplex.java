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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "as.complex", kind = PRIMITIVE, parameterNames = {"x", "..."})
public abstract class AsComplex extends RBuiltinNode {

    @Child private CastComplexNode castComplexNode;

    private void initCast() {
        if (castComplexNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplexNode = insert(CastComplexNodeGen.create(false, false, false));
        }
    }

    @Specialization
    protected RComplex doComplex(RComplex value) {
        return value;
    }

    @Specialization
    protected RComplex doInt(int value) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(value);
    }

    @Specialization
    protected RComplex doDouble(double value) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(value);
    }

    @Specialization
    protected RComplex doLogical(byte value) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(value);
    }

    @Specialization
    protected RComplex doString(String value) {
        initCast();
        return (RComplex) castComplexNode.executeComplex(value);
    }

    @Specialization
    protected RComplexVector doNull(@SuppressWarnings("unused") RNull value) {
        return RDataFactory.createComplexVector(0);
    }

    @Specialization
    protected RComplexVector doComplexVector(RComplexVector vector) {
        return RDataFactory.createComplexVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization
    protected RComplexVector doIntVector(RAbstractVector vector) {
        initCast();
        return (RComplexVector) castComplexNode.executeComplex(vector);
    }
}
