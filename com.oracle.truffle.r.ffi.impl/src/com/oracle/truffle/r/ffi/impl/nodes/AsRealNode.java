/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.RIntVector;

/**
 * Implements the {@code Rf_asReal} GNU R function (which is also used internally). The behavior is
 * subtly different (more permissive error-wise) that {@link CastDoubleNode}. Non-castable values
 * return {@code NA}.
 */
@TypeSystemReference(RTypes.class)
@ImportStatic(DSLConfig.class)
public abstract class AsRealNode extends FFIUpCallNode.Arg1 {

    public abstract double execute(Object obj);

    @Specialization
    protected double asReal(double obj) {
        return obj;
    }

    @Specialization
    protected double asReal(int obj,
                    @Cached BranchProfile naBranchProfile) {
        if (RRuntime.isNA(obj)) {
            naBranchProfile.enter();
            return RRuntime.DOUBLE_NA;
        } else {
            return obj;
        }
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected double asReal(RDoubleVector obj,
                    @Cached BranchProfile naBranchProfile,
                    @CachedLibrary("obj.getData()") VectorDataLibrary dataLib) {
        Object data = obj.getData();
        if (dataLib.getLength(data) == 0) {
            naBranchProfile.enter();
            return RRuntime.DOUBLE_NA;
        }
        return dataLib.getDoubleAt(data, 0);
    }

    @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
    protected double asReal(RIntVector obj,
                    @Cached BranchProfile naBranchProfile,
                    @CachedLibrary("obj.getData()") VectorDataLibrary dataLib) {
        Object data = obj.getData();
        if (dataLib.getLength(data) == 0) {
            naBranchProfile.enter();
            return RRuntime.DOUBLE_NA;
        }
        int result = dataLib.getIntAt(data, 0);
        if (RRuntime.isNA(result)) {
            naBranchProfile.enter();
            return RRuntime.DOUBLE_NA;
        } else {
            return result;
        }
    }

    @Specialization(guards = "obj.getLength() > 0")
    protected double asReal(RAbstractAtomicVector obj,
                    @CachedLibrary(limit = "getCacheSize(2)") VectorDataLibrary dataLib,
                    @Cached("createNonPreserving()") CastDoubleNode castDoubleNode) {
        Object castObj = castDoubleNode.executeDouble(obj);
        if (castObj instanceof Double) {
            return (double) castObj;
        } else if (castObj instanceof RDoubleVector) {
            return dataLib.getDoubleAt(((RDoubleVector) castObj).getData(), 0);
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
