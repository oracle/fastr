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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Implements the {@code Rf_asReal} GNU R function (which is also used internally). The behavior is
 * subtly different (more permissive error-wise) that {@link CastDoubleNode}. Non-castable values
 * return {@code NA}.
 */
public abstract class AsRealNode extends Node {
    @Child private CastDoubleNode castDoubleNode = CastDoubleNode.createNonPreserving();

    public abstract double execute(Object obj);

    @Specialization
    protected double asReal(Double obj) {
        return obj;
    }

    @Specialization
    protected double asReal(Integer obj) {
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

    @Specialization(guards = "isVectorAtomic(obj)")
    protected double asReal(Object obj) {
        Object castObj = castDoubleNode.executeDouble(obj);
        if (castObj instanceof Double) {
            return (double) castObj;
        } else if (castObj instanceof RDoubleVector) {
            return ((RDoubleVector) castObj).getDataAt(0);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Fallback
    protected double asRealFallback(@SuppressWarnings("unused") Object obj) {
        return RRuntime.DOUBLE_NA;
    }

    public static boolean isVectorAtomic(Object obj) {
        return obj instanceof Byte || obj instanceof String || isNonScalarVectorAtomic(obj);
    }

    private static boolean isNonScalarVectorAtomic(Object obj) {
        if (obj instanceof RAbstractLogicalVector || obj instanceof RAbstractStringVector || obj instanceof RAbstractComplexVector) {
            RAbstractVector avec = (RAbstractVector) obj;
            return avec.getLength() >= 1;
        } else {
            return false;
        }
    }
}
