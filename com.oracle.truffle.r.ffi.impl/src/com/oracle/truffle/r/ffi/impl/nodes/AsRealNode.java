/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;

/**
 * Implements the {@code Rf_asReal} GNU R function (which is also used internally). The behavior is
 * subtly different (more permissive error-wise) that {@link CastDoubleNode}. Non-castable values
 * return {@code NA}.
 */
@TypeSystemReference(RTypes.class)
public abstract class AsRealNode extends FFIUpCallNode.Arg1 {

    public abstract double execute(Object obj);

    @Specialization
    protected double asReal(double obj) {
        return obj;
    }

    @Specialization
    protected double asReal(int obj) {
        return RRuntime.isNA(obj) ? RRuntime.DOUBLE_NA : obj;
    }

    @Specialization
    protected double asReal(RAbstractDoubleVector obj) {
        if (obj.getLength() == 0) {
            return RRuntime.DOUBLE_NA;
        }
        return obj.getDataAt(0);
    }

    @Specialization
    protected double asReal(RIntVector obj) {
        if (obj.getLength() == 0) {
            return RRuntime.DOUBLE_NA;
        }
        int result = obj.getDataAt(0);
        return RRuntime.isNA(result) ? RRuntime.DOUBLE_NA : result;
    }

    @Specialization(guards = "obj.getLength() > 0")
    protected double asReal(RAbstractAtomicVector obj,
                    @Cached("createNonPreserving()") CastDoubleNode castDoubleNode) {
        Object castObj = castDoubleNode.executeDouble(obj);
        if (castObj instanceof Double) {
            return (double) castObj;
        } else if (castObj instanceof RAbstractDoubleVector) {
            return ((RAbstractDoubleVector) castObj).getDataAt(0);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Fallback
    protected double asRealFallback(@SuppressWarnings("unused") Object obj) {
        return RRuntime.DOUBLE_NA;
    }

    public static AsRealNode create() {
        return AsRealNodeGen.create();
    }
}
