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

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class RClosures {

    // Logical to ...

    public static RAbstractIntVector createToIntVector(RLogicalVector vector) {
        return new RLogicalToIntVectorClosure(vector);
    }

    public static RAbstractDoubleVector createToDoubleVector(RLogicalVector vector) {
        return new RLogicalToDoubleVectorClosure(vector);
    }

    public static RAbstractComplexVector createToComplexVector(RLogicalVector vector) {
        return new RLogicalToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createToStringVector(RLogicalVector vector) {
        return new RLogicalToStringVectorClosure(vector);
    }

    public static RAbstractListVector createToListVector(RLogicalVector vector) {
        return new RLogicalToListVectorClosure(vector);
    }

    // Int to ...

    public static RAbstractComplexVector createToComplexVector(RIntSequence vector) {
        return new RIntSequenceToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createToStringVector(RIntSequence vector) {
        return new RIntSequenceToStringVectorClosure(vector);
    }

    public static RAbstractListVector createToListVector(RIntSequence vector) {
        return new RIntSequenceToListVectorClosure(vector);
    }

    public static RAbstractDoubleVector createToDoubleVector(RIntVector vector) {
        return new RIntToDoubleVectorClosure(vector);
    }

    public static RAbstractComplexVector createToComplexVector(RIntVector vector) {
        return new RIntToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createToStringVector(RIntVector vector) {
        return new RIntToStringVectorClosure(vector);
    }

    public static RAbstractListVector createToListVector(RIntVector vector) {
        return new RIntToListVectorClosure(vector);
    }

    // Double to ...

    public static RAbstractComplexVector createToComplexVector(RDoubleSequence vector) {
        return new RDoubleSequenceToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createToStringVector(RDoubleSequence vector) {
        return new RDoubleSequenceToStringVectorClosure(vector);
    }

    public static RAbstractIntVector createToIntVector(RDoubleSequence vector) {
        return new RDoubleSequenceToIntVectorClosure(vector);
    }

    public static RAbstractListVector createToListVector(RDoubleSequence vector) {
        return new RDoubleSequenceToListVectorClosure(vector);
    }

    public static RAbstractComplexVector createToComplexVector(RDoubleVector vector) {
        return new RDoubleToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createToStringVector(RDoubleVector vector) {
        return new RDoubleToStringVectorClosure(vector);
    }

    public static RAbstractIntVector createToIntVector(RDoubleVector vector) {
        return new RDoubleToIntVectorClosure(vector);
    }

    public static RAbstractListVector createToListVector(RDoubleVector vector) {
        return new RDoubleToListVectorClosure(vector);
    }

    // Raw to ...

    public static RAbstractIntVector createToIntVector(RRawVector vector) {
        return new RRawToIntVectorClosure(vector);
    }

    public static RAbstractDoubleVector createToDoubleVector(RRawVector vector) {
        return new RRawToDoubleVectorClosure(vector);
    }

    public static RAbstractComplexVector createToComplexVector(RRawVector vector) {
        return new RRawToComplexVectorClosure(vector);
    }

    public static RAbstractStringVector createToStringVector(RRawVector vector) {
        return new RRawToStringVectorClosure(vector);
    }

    public static RAbstractListVector createToListVector(RRawVector vector) {
        return new RRawToListVectorClosure(vector);
    }

    // Complex to ...

    public static RAbstractStringVector createToStringVector(RComplexVector vector) {
        return new RComplexToStringVectorClosure(vector);
    }

    public static RAbstractListVector createToListVector(RComplexVector vector) {
        return new RComplexToListVectorClosure(vector);
    }

    // Character to ...

    public static RAbstractListVector createToListVector(RStringVector vector) {
        return new RStringToListVectorClosure(vector);
    }

    // Factor to vector

    public static RAbstractVector createFactorToVector(RAbstractIntVector factor, boolean withNames, RAbstractVector levels) {
        if (levels instanceof RAbstractStringVector) {
            return new RFactorToStringVectorClosure(factor, (RAbstractStringVector) levels, withNames);
        } else {
            throw RError.error(RError.SHOW_CALLER, Message.MALFORMED_FACTOR);
        }
    }
}
