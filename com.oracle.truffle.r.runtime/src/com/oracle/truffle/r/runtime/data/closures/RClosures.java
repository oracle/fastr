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

    public static RAbstractIntVector createToIntVector(RLogicalVector vector, boolean keepAttributes) {
        return new RLogicalToIntVectorClosure(vector, keepAttributes);
    }

    public static RAbstractDoubleVector createToDoubleVector(RLogicalVector vector, boolean keepAttributes) {
        return new RLogicalToDoubleVectorClosure(vector, keepAttributes);
    }

    public static RAbstractComplexVector createToComplexVector(RLogicalVector vector, boolean keepAttributes) {
        return new RLogicalToComplexVectorClosure(vector, keepAttributes);
    }

    public static RAbstractStringVector createToStringVector(RLogicalVector vector, boolean keepAttributes) {
        return new RLogicalToStringVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RLogicalVector vector, boolean keepAttributes) {
        return new RLogicalToListVectorClosure(vector, keepAttributes);
    }

    // Int to ...

    public static RAbstractComplexVector createToComplexVector(RIntSequence vector, boolean keepAttributes) {
        return new RIntSequenceToComplexVectorClosure(vector, keepAttributes);
    }

    public static RAbstractStringVector createToStringVector(RIntSequence vector, boolean keepAttributes) {
        return new RIntSequenceToStringVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RIntSequence vector, boolean keepAttributes) {
        return new RIntSequenceToListVectorClosure(vector, keepAttributes);
    }

    public static RAbstractDoubleVector createToDoubleVector(RIntVector vector, boolean keepAttributes) {
        return new RIntToDoubleVectorClosure(vector, keepAttributes);
    }

    public static RAbstractComplexVector createToComplexVector(RIntVector vector, boolean keepAttributes) {
        return new RIntToComplexVectorClosure(vector, keepAttributes);
    }

    public static RAbstractStringVector createToStringVector(RIntVector vector, boolean keepAttributes) {
        return new RIntToStringVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RIntVector vector, boolean keepAttributes) {
        return new RIntToListVectorClosure(vector, keepAttributes);
    }

    // Double to ...

    public static RAbstractComplexVector createToComplexVector(RDoubleSequence vector) {
        return new RDoubleSequenceToComplexVectorClosure(vector, false);
    }

    public static RAbstractComplexVector createToComplexVector(RDoubleSequence vector, boolean keepAttributes) {
        return new RDoubleSequenceToComplexVectorClosure(vector, keepAttributes);
    }

    public static RAbstractStringVector createToStringVector(RDoubleSequence vector) {
        return new RDoubleSequenceToStringVectorClosure(vector, false);
    }

    public static RAbstractStringVector createToStringVector(RDoubleSequence vector, boolean keepAttributes) {
        return new RDoubleSequenceToStringVectorClosure(vector, keepAttributes);
    }

    public static RAbstractIntVector createToIntVector(RDoubleSequence vector) {
        return new RDoubleSequenceToIntVectorClosure(vector, false);
    }

    public static RAbstractIntVector createToIntVector(RDoubleSequence vector, boolean keepAttributes) {
        return new RDoubleSequenceToIntVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RDoubleSequence vector) {
        return new RDoubleSequenceToListVectorClosure(vector, false);
    }

    public static RAbstractListVector createToListVector(RDoubleSequence vector, boolean keepAttributes) {
        return new RDoubleSequenceToListVectorClosure(vector, keepAttributes);
    }

    public static RAbstractComplexVector createToComplexVector(RDoubleVector vector) {
        return new RDoubleToComplexVectorClosure(vector, false);
    }

    public static RAbstractComplexVector createToComplexVector(RDoubleVector vector, boolean keepAttributes) {
        return new RDoubleToComplexVectorClosure(vector, keepAttributes);
    }

    public static RAbstractStringVector createToStringVector(RDoubleVector vector) {
        return new RDoubleToStringVectorClosure(vector, false);
    }

    public static RAbstractStringVector createToStringVector(RDoubleVector vector, boolean keepAttributes) {
        return new RDoubleToStringVectorClosure(vector, keepAttributes);
    }

    public static RAbstractIntVector createToIntVector(RDoubleVector vector) {
        return new RDoubleToIntVectorClosure(vector, false);
    }

    public static RAbstractIntVector createToIntVector(RDoubleVector vector, boolean keepAttributes) {
        return new RDoubleToIntVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RDoubleVector vector) {
        return new RDoubleToListVectorClosure(vector, false);
    }

    public static RAbstractListVector createToListVector(RDoubleVector vector, boolean keepAttributes) {
        return new RDoubleToListVectorClosure(vector, keepAttributes);
    }

    // Raw to ...

    public static RAbstractIntVector createToIntVector(RRawVector vector) {
        return new RRawToIntVectorClosure(vector, false);
    }

    public static RAbstractIntVector createToIntVector(RRawVector vector, boolean keepAttributes) {
        return new RRawToIntVectorClosure(vector, keepAttributes);
    }

    public static RAbstractDoubleVector createToDoubleVector(RRawVector vector) {
        return new RRawToDoubleVectorClosure(vector, false);
    }

    public static RAbstractDoubleVector createToDoubleVector(RRawVector vector, boolean keepAttributes) {
        return new RRawToDoubleVectorClosure(vector, keepAttributes);
    }

    public static RAbstractComplexVector createToComplexVector(RRawVector vector) {
        return new RRawToComplexVectorClosure(vector, false);
    }

    public static RAbstractComplexVector createToComplexVector(RRawVector vector, boolean keepAttributes) {
        return new RRawToComplexVectorClosure(vector, keepAttributes);
    }

    public static RAbstractStringVector createToStringVector(RRawVector vector) {
        return new RRawToStringVectorClosure(vector, false);
    }

    public static RAbstractStringVector createToStringVector(RRawVector vector, boolean keepAttributes) {
        return new RRawToStringVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RRawVector vector) {
        return new RRawToListVectorClosure(vector, false);
    }

    public static RAbstractListVector createToListVector(RRawVector vector, boolean keepAttributes) {
        return new RRawToListVectorClosure(vector, keepAttributes);
    }

    // Complex to ...

    public static RAbstractStringVector createToStringVector(RComplexVector vector) {
        return new RComplexToStringVectorClosure(vector, false);
    }

    public static RAbstractStringVector createToStringVector(RComplexVector vector, boolean keepAttributes) {
        return new RComplexToStringVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RComplexVector vector) {
        return new RComplexToListVectorClosure(vector, false);
    }

    public static RAbstractListVector createToListVector(RComplexVector vector, boolean keepAttributes) {
        return new RComplexToListVectorClosure(vector, keepAttributes);
    }

    // Character to ...

    public static RAbstractListVector createToListVector(RStringVector vector, boolean keepAttributes) {
        return new RStringToListVectorClosure(vector, keepAttributes);
    }

    // Factor to vector

    public static RAbstractVector createFactorToVector(RAbstractIntVector factor, boolean withNames, RAbstractVector levels) {
        return createFactorToVector(factor, withNames, levels, false);
    }

    public static RAbstractVector createFactorToVector(RAbstractIntVector factor, boolean withNames, RAbstractVector levels, boolean keepAttributes) {
        if (levels instanceof RAbstractStringVector) {
            return new RFactorToStringVectorClosure(factor, (RAbstractStringVector) levels, withNames, keepAttributes);
        } else {
            throw RError.error(RError.SHOW_CALLER, Message.MALFORMED_FACTOR);
        }
    }
}
