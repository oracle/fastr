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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.r.runtime.ops.na.NACheck;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class RClosures {

    // Logical to ...

    public static RAbstractIntVector createLogicalToIntVector(RAbstractLogicalVector vector, NACheck check) {
        return new RLogicalToIntVectorClosure(vector, check);
    }

    public static RAbstractDoubleVector createLogicalToDoubleVector(RAbstractLogicalVector vector, NACheck check) {
        return new RLogicalToDoubleVectorClosure(vector, check);
    }

    public static RAbstractComplexVector createLogicalToComplexVector(RAbstractLogicalVector vector, NACheck check) {
        return new RLogicalToComplexVectorClosure(vector, check);
    }

    public static RAbstractStringVector createLogicalToStringVector(RAbstractLogicalVector vector, NACheck check) {
        return new RLogicalToStringVectorClosure(vector, check);
    }

    // Int to ...

    public static RAbstractDoubleVector createIntToDoubleVector(RAbstractIntVector vector, NACheck check) {
        return new RIntToDoubleVectorClosure(vector, check);
    }

    public static RAbstractComplexVector createIntToComplexVector(RAbstractIntVector vector, NACheck check) {
        return new RIntToComplexVectorClosure(vector, check);
    }

    public static RAbstractStringVector createIntToStringVector(RAbstractIntVector vector, NACheck check) {
        return new RIntToStringVectorClosure(vector, check);
    }

    // Double to ...

    public static RAbstractComplexVector createDoubleToComplexVector(RAbstractDoubleVector vector, NACheck check) {
        return new RDoubleToComplexVectorClosure(vector, check);
    }

    public static RAbstractStringVector createDoubleToStringVector(RAbstractDoubleVector vector, NACheck check) {
        return new RDoubleToStringVectorClosure(vector, check);
    }

    public static RAbstractIntVector createDoubleToIntVector(RAbstractDoubleVector vector, NACheck check) {
        return new RDoubleToIntVectorClosure(vector, check);
    }

    public static RAbstractIntVector createComplexToIntVectorDiscardImaginary(RAbstractComplexVector vector, NACheck check) {
        return new RComplexToIntVectorClosure(vector, check);
    }

    // Raw to ...

    public static RAbstractIntVector createRawToIntVector(RAbstractRawVector vector, NACheck check) {
        return new RRawToIntVectorClosure(vector, check);
    }

    public static RAbstractDoubleVector createRawToDoubleVector(RAbstractRawVector vector, NACheck check) {
        return new RRawToDoubleVectorClosure(vector, check);
    }

    public static RAbstractComplexVector createRawToComplexVector(RAbstractRawVector vector, NACheck check) {
        return new RRawToComplexVectorClosure(vector, check);
    }

    public static RAbstractStringVector createRawToStringVector(RAbstractRawVector vector, NACheck check) {
        return new RRawToStringVectorClosure(vector, check);
    }

    // Complex to ...

    public static RAbstractStringVector createComplexToStringVector(RAbstractComplexVector vector, NACheck check) {
        return new RComplexToStringVectorClosure(vector, check);
    }

    // Factor to vector

    public static RAbstractVector createFactorToVector(RFactor factor, NACheck check) {
        RAbstractVector levels = factor.getVector().getLevels();
        if (levels == null) {
            return new RFactorToStringVectorClosure(factor, null, check);
        } else {
            if (levels.getElementClass() == RInt.class) {
                return new RFactorToIntVectorClosure(factor, (RAbstractIntVector) levels, check);
            } else if (levels.getElementClass() == RDouble.class) {
                return new RFactorToDoubleVectorClosure(factor, (RAbstractDoubleVector) levels, check);
            } else if (levels.getElementClass() == RLogical.class) {
                return new RFactorToIntVectorClosure(factor, createLogicalToIntVector((RAbstractLogicalVector) levels, check), check);
            } else if (levels.getElementClass() == RComplex.class) {
                return new RFactorToComplexVectorClosure(factor, (RAbstractComplexVector) levels, check);
            } else if (levels.getElementClass() == RString.class) {
                return new RFactorToStringVectorClosure(factor, (RAbstractStringVector) levels, check);
            } else {
                assert levels.getElementClass() == RRaw.class;
                return new RFactorToIntVectorClosure(factor, createRawToIntVector((RAbstractRawVector) levels, check), check);
            }

        }
    }

}
