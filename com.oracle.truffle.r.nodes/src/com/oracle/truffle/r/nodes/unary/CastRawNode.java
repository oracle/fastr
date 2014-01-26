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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public abstract class CastRawNode extends CastNode {

    @Specialization
    public RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    public RRaw doInt(int operand) {
        return RDataFactory.createRaw((byte) operand);
    }

    @Specialization
    public RRawVector doIntVector(RIntVector value) {
        return performAbstractIntVector(value);
    }

    @Specialization
    public RRawVector doIntSequence(RIntSequence value) {
        return performAbstractIntVector(value);
    }

    private static RRawVector performAbstractIntVector(RAbstractIntVector value) {
        int length = value.getLength();
        byte[] array = new byte[length];
        for (int i = 0; i < length; ++i) {
            array[i] = (byte) value.getDataAt(i);
        }
        return RDataFactory.createRawVector(array, value.getNames());
    }

    @Specialization
    public RRaw doBoolean(byte operand) {
        return RDataFactory.createRaw(operand);
    }

    @Specialization
    public RRaw doRaw(RRaw operand) {
        return operand;
    }

    @Specialization
    public RRawVector doRawVector(RRawVector operand) {
        return operand;
    }

    @Generic
    public int doOther(Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw new ConversionFailedException(operand.getClass().getName());
    }

}
