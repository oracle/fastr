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
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "as.double", aliases = {"as.numeric"}, kind = PRIMITIVE, parameterNames = {"x", "..."})
// TODO define alias in R
public abstract class AsDouble extends RBuiltinNode {

    @Child private CastDoubleNode castDoubleNode;

    private void initCast() {
        if (castDoubleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDoubleNode = insert(CastDoubleNodeGen.create(false, false, false));
        }
    }

    @Specialization
    protected double asDouble(double value) {
        return value;
    }

    @Specialization
    protected double asDoubleInt(int value) {
        initCast();
        return (double) castDoubleNode.executeDouble(value);
    }

    @Specialization
    protected double asDouble(byte value) {
        initCast();
        return (double) castDoubleNode.executeDouble(value);
    }

    @Specialization
    protected double asDouble(RComplex value) {
        initCast();
        return (double) castDoubleNode.executeDouble(value);
    }

    @Specialization
    protected double asDouble(String value) {
        initCast();
        return (double) castDoubleNode.executeDouble(value);
    }

    @Specialization
    protected RDoubleVector asDouble(@SuppressWarnings("unused") RNull vector) {
        return RDataFactory.createDoubleVector(0);
    }

    @Specialization
    protected RDoubleVector asDouble(RDoubleVector vector) {
        return RDataFactory.createDoubleVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization
    protected RDoubleSequence asDouble(RDoubleSequence sequence) {
        return sequence;
    }

    @Specialization
    protected RDoubleSequence asDouble(RIntSequence sequence) {
        return RDataFactory.createDoubleSequence(sequence.getStart(), sequence.getStride(), sequence.getLength());
    }

    @Specialization
    protected RDoubleVector asDouble(RAbstractVector vector) {
        initCast();
        return (RDoubleVector) castDoubleNode.executeDouble(vector);
    }
}
