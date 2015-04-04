/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
        return createLogicalToIntVector(vector, check.neverSeenNA());
    }

    public static RAbstractDoubleVector createLogicalToDoubleVector(RAbstractLogicalVector vector, NACheck check) {
        return createLogicalToDoubleVector(vector, check.neverSeenNA());
    }

    public static RAbstractComplexVector createLogicalToComplexVector(RAbstractLogicalVector vector, NACheck check) {
        return createLogicalToComplexVector(vector, check.neverSeenNA());
    }

    public static RAbstractStringVector createLogicalToStringVector(RAbstractLogicalVector vector, NACheck check) {
        return createLogicalToStringVector(vector, check.neverSeenNA());
    }

    public static RAbstractIntVector createLogicalToIntVector(RAbstractLogicalVector vector, boolean neverSeenNA) {
        return new RLogicalToIntVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractDoubleVector createLogicalToDoubleVector(RAbstractLogicalVector vector, boolean neverSeenNA) {
        return new RLogicalToDoubleVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractComplexVector createLogicalToComplexVector(RAbstractLogicalVector vector, boolean neverSeenNA) {
        return new RLogicalToComplexVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractStringVector createLogicalToStringVector(RAbstractLogicalVector vector, boolean neverSeenNA) {
        return new RLogicalToStringVectorClosure(vector, neverSeenNA);
    }

    // Int to ...

    public static RAbstractDoubleVector createIntToDoubleVector(RAbstractIntVector vector, NACheck check) {
        return createIntToDoubleVector(vector, check.neverSeenNA());
    }

    public static RAbstractComplexVector createIntToComplexVector(RAbstractIntVector vector, NACheck check) {
        return createIntToComplexVector(vector, check.neverSeenNA());
    }

    public static RAbstractStringVector createIntToStringVector(RAbstractIntVector vector, NACheck check) {
        return createIntToStringVector(vector, check.neverSeenNA());
    }

    public static RAbstractDoubleVector createIntToDoubleVector(RAbstractIntVector vector, boolean neverSeenNA) {
        return new RIntToDoubleVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractComplexVector createIntToComplexVector(RAbstractIntVector vector, boolean neverSeenNA) {
        return new RIntToComplexVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractStringVector createIntToStringVector(RAbstractIntVector vector, boolean neverSeenNA) {
        return new RIntToStringVectorClosure(vector, neverSeenNA);
    }

    // Double to ...

    public static RAbstractComplexVector createDoubleToComplexVector(RAbstractDoubleVector vector, NACheck check) {
        return createDoubleToComplexVector(vector, check.neverSeenNA());
    }

    public static RAbstractStringVector createDoubleToStringVector(RAbstractDoubleVector vector, NACheck check) {
        return createDoubleToStringVector(vector, check.neverSeenNA());
    }

    public static RAbstractIntVector createDoubleToIntVector(RAbstractDoubleVector vector, NACheck check) {
        return createDoubleToIntVector(vector, check.neverSeenNA());
    }

    public static RAbstractIntVector createComplexToIntVectorDiscardImaginary(RAbstractComplexVector vector, NACheck check) {
        return createComplexToIntVectorDiscardImaginary(vector, check.neverSeenNA());
    }

    public static RAbstractComplexVector createDoubleToComplexVector(RAbstractDoubleVector vector, boolean neverSeenNA) {
        return new RDoubleToComplexVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractStringVector createDoubleToStringVector(RAbstractDoubleVector vector, boolean neverSeenNA) {
        return new RDoubleToStringVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractIntVector createDoubleToIntVector(RAbstractDoubleVector vector, boolean neverSeenNA) {
        return new RDoubleToIntVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractIntVector createComplexToIntVectorDiscardImaginary(RAbstractComplexVector vector, boolean neverSeenNA) {
        return new RComplexToIntVectorClosure(vector, neverSeenNA);
    }

    // Raw to ...

    public static RAbstractIntVector createRawToIntVector(RAbstractRawVector vector, NACheck check) {
        return createRawToIntVector(vector, check.neverSeenNA());
    }

    public static RAbstractDoubleVector createRawToDoubleVector(RAbstractRawVector vector, NACheck check) {
        return createRawToDoubleVector(vector, check.neverSeenNA());
    }

    public static RAbstractComplexVector createRawToComplexVector(RAbstractRawVector vector, NACheck check) {
        return createRawToComplexVector(vector, check.neverSeenNA());
    }

    public static RAbstractStringVector createRawToStringVector(RAbstractRawVector vector, NACheck check) {
        return createRawToStringVector(vector, check.neverSeenNA());
    }

    public static RAbstractIntVector createRawToIntVector(RAbstractRawVector vector, boolean neverSeenNA) {
        return new RRawToIntVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractDoubleVector createRawToDoubleVector(RAbstractRawVector vector, boolean neverSeenNA) {
        return new RRawToDoubleVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractComplexVector createRawToComplexVector(RAbstractRawVector vector, boolean neverSeenNA) {
        return new RRawToComplexVectorClosure(vector, neverSeenNA);
    }

    public static RAbstractStringVector createRawToStringVector(RAbstractRawVector vector, boolean neverSeenNA) {
        return new RRawToStringVectorClosure(vector, neverSeenNA);
    }

    // Complex to ...

    public static RAbstractStringVector createComplexToStringVector(RAbstractComplexVector vector, NACheck check) {
        return createComplexToStringVector(vector, check.neverSeenNA());
    }

    public static RAbstractStringVector createComplexToStringVector(RAbstractComplexVector vector, boolean neverSeenNA) {
        return new RComplexToStringVectorClosure(vector, neverSeenNA);
    }

    // Factor to vector

    public static RAbstractVector createFactorToVector(RFactor factor, NACheck check, boolean withNames, RAttributeProfiles attrProfiles) {
        RAbstractVector levels = factor.getLevels(attrProfiles);
        boolean complete = check.hasNeverBeenTrue();
        if (levels == null) {
            return new RFactorToStringVectorClosure(factor, null, complete, withNames);
        } else {
            if (levels.getElementClass() == RInt.class) {
                return new RFactorToIntVectorClosure(factor, (RAbstractIntVector) levels, complete, withNames);
            } else if (levels.getElementClass() == RDouble.class) {
                return new RFactorToDoubleVectorClosure(factor, (RAbstractDoubleVector) levels, complete, withNames);
            } else if (levels.getElementClass() == RLogical.class) {
                return new RFactorToIntVectorClosure(factor, createLogicalToIntVector((RAbstractLogicalVector) levels, check), complete, withNames);
            } else if (levels.getElementClass() == RComplex.class) {
                return new RFactorToComplexVectorClosure(factor, (RAbstractComplexVector) levels, complete, withNames);
            } else if (levels.getElementClass() == RString.class) {
                return new RFactorToStringVectorClosure(factor, (RAbstractStringVector) levels, complete, withNames);
            } else {
                assert levels.getElementClass() == RRaw.class;
                return new RFactorToIntVectorClosure(factor, createRawToIntVector((RAbstractRawVector) levels, check), complete, withNames);
            }

        }
    }

}
