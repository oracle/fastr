/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RIntVector;

/**
 * Implements the {@code Rf_asInteger} GNU R function . The behavior is subtly different (more
 * permissive error-wise) that {@link CastIntegerNode}. Non-castable values return {@code NA}.
 */
@TypeSystemReference(RTypes.class)
public abstract class AsIntegerNode extends FFIUpCallNode.Arg1 {

    public abstract int execute(Object obj);

    @Specialization
    protected int asInteger(int obj) {
        return obj;
    }

    @Specialization
    protected int asInteger(double obj,
                    @Cached("createCastIntegerNode()") CastIntegerNode castIntegerNode) {
        return (int) castIntegerNode.executeInt(obj);
    }

    @Specialization
    protected int asInteger(RAbstractDoubleVector obj,
                    @Cached("createCastIntegerNode()") CastIntegerNode castIntegerNode) {
        if (obj.getLength() == 0) {
            return RRuntime.INT_NA;
        }
        Object castObj = castIntegerNode.executeInt(obj);
        if (castObj instanceof Integer) {
            return (Integer) castObj;
        } else if (castObj instanceof RIntVector) {
            return ((RIntVector) castObj).getDataAt(0);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Specialization
    protected int asInteger(RIntVector obj) {
        if (obj.getLength() == 0) {
            return RRuntime.INT_NA;
        }
        return obj.getDataAt(0);
    }

    @Specialization(guards = "obj.getLength() > 0")
    protected int asInteger(RAbstractAtomicVector obj,
                    @Cached("createCastIntegerNode()") CastIntegerNode castIntegerNode) {
        Object castObj = castIntegerNode.executeInt(obj);
        if (castObj instanceof Integer) {
            return (Integer) castObj;
        } else if (castObj instanceof RIntVector) {
            return ((RIntVector) castObj).getDataAt(0);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Fallback
    protected int asIntegerFallback(@SuppressWarnings("unused") Object obj) {
        return RRuntime.INT_NA;
    }

    public static AsIntegerNode create() {
        return AsIntegerNodeGen.create();
    }

    protected CastIntegerNode createCastIntegerNode() {
        return CastIntegerNode.createNonPreserving(RError.SHOW_CALLER);
    }
}
