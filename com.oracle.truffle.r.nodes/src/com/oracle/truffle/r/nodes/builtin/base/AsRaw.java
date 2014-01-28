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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("as.raw")
@SuppressWarnings("unused")
public abstract class AsRaw extends RBuiltinNode {

    @Child private AsRaw asRawRecursive;
    @Child private CastIntegerNode castInteger;

    public abstract Object executeRaw(VirtualFrame frame, Object o);

    @Specialization
    public RRawVector asRaw(RNull vector) {
        return RDataFactory.createRawVector(0);
    }

    @Specialization
    public RRaw asRaw(byte logical) {
        if (RRuntime.isNA(logical)) {
            // TODO: Output out-of-range warning.
            return RDataFactory.createRaw((byte) 0);
        }
        return RDataFactory.createRaw(logical);
    }

    @Specialization
    public RRaw asRaw(int value) {
        int result = (value & 0xFF);
        if (result == value) {
            return RDataFactory.createRaw((byte) result);
        }
        // TODO: Output out-of-range warning.
        return RDataFactory.createRaw((byte) 0);
    }

    @Specialization
    public RRaw asRaw(double value) {
        return asRaw((int) value);
    }

    @Specialization
    public RRaw asRaw(RComplex value) {
        // TODO: Output dropping of imaginary part warning if imaginary part not 0.0.
        return asRaw(value.getRealPart());
    }

    @Specialization
    public RRaw asRaw(VirtualFrame frame, String value) {
        // TODO: Output NA and out-of-range warning.
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreter();
            castInteger = adoptChild(CastIntegerNodeFactory.create(null, false, false));
        }
        return asRaw((int) castInteger.executeInt(frame, value));
    }

    @Specialization
    public RRaw asRaw(RRaw value) {
        return value;
    }

    @Specialization
    public RRawVector asRaw(RIntVector vector) {
        return performAbstractIntVector(vector);
    }

    @Specialization
    public RRawVector asRaw(RIntSequence vector) {
        return performAbstractIntVector(vector);
    }

    @Specialization
    public RRawVector asRaw(RDoubleSequence vector) {
        return performAbstractDoubleVector(vector);
    }

    @Specialization
    public RRawVector asRaw(RDoubleVector vector) {
        return performAbstractDoubleVector(vector);
    }

    @Specialization
    public RRawVector asRaw(RStringVector vector) {
        int length = vector.getLength();
        RRawVector result = RDataFactory.createRawVector(length);
        for (int i = 0; i < length; i++) {
            result.updateDataAt(i, RDataFactory.createRaw((byte) 0));
        }
        return result;
    }

    @Specialization
    public RRawVector asRaw(RLogicalVector vector) {
        int length = vector.getLength();
        RRawVector result = RDataFactory.createRawVector(length);
        for (int i = 0; i < length; i++) {
            result.updateDataAt(i, asRaw(vector.getDataAt(i)));
        }
        return result;
    }

    @Specialization
    public RRawVector asRaw(RComplexVector vector) {
        int length = vector.getLength();
        RRawVector result = RDataFactory.createRawVector(length);
        for (int i = 0; i < length; i++) {
            result.updateDataAt(i, asRaw(vector.getDataAt(i)));
        }
        return result;
    }

    @Specialization
    public RRawVector asRaw(RRawVector value) {
        return value;
    }

    @Specialization
    public RRawVector asRaw(VirtualFrame frame, RList value) {
        if (asRawRecursive == null) {
            CompilerDirectives.transferToInterpreter();
            asRawRecursive = adoptChild(AsRawFactory.create(new RNode[1], getBuiltin()));
        }
        int length = value.getLength();
        RRawVector result = RDataFactory.createRawVector(length);
        for (int i = 0; i < length; ++i) {
            result.updateDataAt(i, (RRaw) asRawRecursive.executeRaw(frame, value.getDataAt(i)));
        }
        return result;
    }

    private RRawVector performAbstractIntVector(RAbstractIntVector vector) {
        int length = vector.getLength();
        RRawVector result = RDataFactory.createRawVector(length);
        for (int i = 0; i < length; i++) {
            result.updateDataAt(i, asRaw(vector.getDataAt(i)));
        }
        return result;
    }

    private RRawVector performAbstractDoubleVector(RAbstractDoubleVector vector) {
        int length = vector.getLength();
        RRawVector result = RDataFactory.createRawVector(length);
        for (int i = 0; i < length; i++) {
            result.updateDataAt(i, asRaw(vector.getDataAt(i)));
        }
        return result;
    }
}
