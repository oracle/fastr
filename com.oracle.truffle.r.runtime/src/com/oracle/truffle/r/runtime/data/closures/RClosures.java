/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class RClosures {

    // Logical to ...

    public static RAbstractIntVector createLogicalToIntVector(RAbstractLogicalVector vector) {
        return new RLogicalToIntVectorClosure(vector);
    }

    public static RAbstractDoubleVector createLogicalToDoubleVector(RAbstractLogicalVector vector) {
        return new RLogicalToDoubleVectorClosure(vector);
    }

    public static RAbstractComplexVector createLogicalToComplexVector(RAbstractLogicalVector vector) {
        return new RLogicalToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createLogicalToStringVector(RAbstractLogicalVector vector) {
        return new RLogicalToStringVectorClosure(vector);
    }

    // Int to ...

    public static RAbstractDoubleVector createIntToDoubleVector(RAbstractIntVector vector) {
        return new RIntToDoubleVectorClosure(vector);
    }

    public static RAbstractComplexVector createIntToComplexVector(RAbstractIntVector vector) {
        return new RIntToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createIntToStringVector(RAbstractIntVector vector) {
        return new RIntToStringVectorClosure(vector);
    }

    // Double to ...

    public static RAbstractComplexVector createDoubleToComplexVector(RAbstractDoubleVector vector) {
        return new RDoubleToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createDoubleToStringVector(RAbstractDoubleVector vector) {
        return new RDoubleToStringVectorClosure(vector);
    }

    public static RAbstractIntVector createDoubleToIntVector(RAbstractDoubleVector vector) {
        return new RDoubleToIntVectorClosure(vector);
    }

    // Raw to ...

    public static RAbstractIntVector createRawToIntVector(RAbstractRawVector vector) {
        return new RRawToIntVectorClosure(vector);
    }

    public static RAbstractDoubleVector createRawToDoubleVector(RAbstractRawVector vector) {
        return new RRawToDoubleVectorClosure(vector);
    }

    public static RAbstractComplexVector createRawToComplexVector(RAbstractRawVector vector) {
        return new RRawToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createRawToStringVector(RAbstractRawVector vector) {
        return new RRawToStringVectorClosure(vector);
    }

    // Complex to ...

    public static RAbstractStringVector createComplexToStringVector(RAbstractComplexVector vector) {
        return new RComplexToStringVectorClosure(vector);
    }

    // Vector to list

    public static RAbstractListVector createAbstractVectorToListVector(RAbstractVector vector) {
        return new RAbstactVectorToListClosure(vector);
    }

    // Factor to vector

    public static RAbstractVector createFactorToVector(RAbstractIntVector factor, boolean withNames, RVector<?> levels) {
        if (levels == null) {
            return new RFactorToStringVectorClosure(factor, null, withNames);
        } else {
            switch (levels.getRType()) {
                case Integer:
                    return new RFactorToIntVectorClosure(factor, (RAbstractIntVector) levels, withNames);
                case Double:
                    return new RFactorToDoubleVectorClosure(factor, (RAbstractDoubleVector) levels, withNames);
                case Logical:
                    return new RFactorToIntVectorClosure(factor, createLogicalToIntVector((RAbstractLogicalVector) levels), withNames);
                case Complex:
                    return new RFactorToComplexVectorClosure(factor, (RAbstractComplexVector) levels, withNames);
                case Character:
                    return new RFactorToStringVectorClosure(factor, (RAbstractStringVector) levels, withNames);
                case Raw:
                    return new RFactorToIntVectorClosure(factor, createRawToIntVector((RAbstractRawVector) levels), withNames);
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }
    }
}
