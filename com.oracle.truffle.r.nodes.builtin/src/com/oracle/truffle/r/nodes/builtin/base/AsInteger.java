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
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "as.integer", kind = PRIMITIVE, parameterNames = {"x", "..."})
public abstract class AsInteger extends RBuiltinNode {

    @Child private CastIntegerNode castIntNode;

    private void initCast() {
        if (castIntNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castIntNode = insert(CastIntegerNodeGen.create(false, false, false));
        }
    }

    @Specialization
    protected int asInteger(int value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected int asInteger(double value) {
        controlVisibility();
        initCast();
        return (int) castIntNode.executeInt(value);
    }

    @Specialization
    protected int asInteger(byte value) {
        controlVisibility();
        initCast();
        return (int) castIntNode.executeInt(value);
    }

    @Specialization
    protected int asInteger(RComplex value) {
        controlVisibility();
        initCast();
        return (int) castIntNode.executeInt(value);
    }

    @Specialization
    protected int asInteger(RRaw value) {
        controlVisibility();
        initCast();
        return (int) castIntNode.executeInt(value);
    }

    @Specialization
    protected int asInteger(String value) {
        controlVisibility();
        initCast();
        return (int) castIntNode.executeInt(value);
    }

    @Specialization
    protected RIntVector asInteger(@SuppressWarnings("unused") RNull value) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
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
    protected RAbstractIntVector asInteger(RAbstractVector vector) {
        controlVisibility();
        initCast();
        return (RAbstractIntVector) castIntNode.executeInt(vector);
    }

    @Specialization
    protected RIntVector asInteger(RFactor factor) {
        return asInteger(factor.getVector());
    }

    @Specialization
    protected int asInteger(RConnection conn) {
        return conn.getDescriptor();
    }
}
