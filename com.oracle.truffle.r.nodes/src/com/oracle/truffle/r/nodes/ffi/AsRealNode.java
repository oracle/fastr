/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.ffi;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RTypesFlatLayout;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

/**
 * Implements the {@code Rf_asReal} GNU R function (which is also used internally). The behavior is
 * subtly different (more permissive error-wise) that {@link CastDoubleNode}. Non-castable values
 * return {@code NA}.
 */
@TypeSystemReference(RTypesFlatLayout.class)
@ImportStatic(IsVectorAtomicNodeLG0.class)
public abstract class AsRealNode extends FFIUpCallNode.Arg1 {

    public abstract double execute(Object obj);

    @Specialization
    protected double asReal(double obj) {
        return obj;
    }

    @Specialization
    protected double asReal(int obj) {
        return obj;
    }

    @Specialization
    protected double asReal(RAbstractDoubleVector obj) {
        if (obj.getLength() == 0) {
            return RRuntime.DOUBLE_NA;
        }
        return obj.getDataAt(0);
    }

    @Specialization
    protected double asReal(RAbstractIntVector obj) {
        if (obj.getLength() == 0) {
            return RRuntime.DOUBLE_NA;
        }
        return obj.getDataAt(0);
    }

    @Specialization(guards = "isVectorAtomicNodeLG0.execute(obj)")
    protected double asReal(Object obj,
                    @Cached("createNonPreserving()") CastDoubleNode castDoubleNode,
                    @SuppressWarnings("unused") @Cached("create()") IsVectorAtomicNodeLG0 isVectorAtomicNodeLG0) {
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

}
