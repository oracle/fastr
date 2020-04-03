/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class RClosures {

    public static RIntVector createToIntVector(RAbstractVector vector, boolean keepAttributes) {
        return RIntVector.createClosure(vector, keepAttributes);
    }

    public static RDoubleVector createToDoubleVector(RAbstractVector vector, boolean keepAttributes) {
        return RDoubleVector.createClosure(vector, keepAttributes);
    }

    public static RAbstractComplexVector createToComplexVector(RAbstractVector vector, boolean keepAttributes) {
        return new RToComplexVectorClosure(vector, keepAttributes);
    }

    public static RAbstractStringVector createToStringVector(RAbstractVector vector, boolean keepAttributes) {
        return new RToStringVectorClosure(vector, keepAttributes);
    }

    // Factor to vector

    public static RAbstractVector createFactorToVector(RIntVector factor, boolean withNames, RAbstractVector levels) {
        return createFactorToVector(factor, withNames, levels, false);
    }

    public static RAbstractVector createFactorToVector(RIntVector factor, boolean withNames, RAbstractVector levels, boolean keepAttributes) {
        if (levels instanceof RAbstractStringVector) {
            return new RFactorToStringVectorClosure(factor, (RAbstractStringVector) levels, withNames, keepAttributes);
        } else {
            throw RError.error(RError.SHOW_CALLER, Message.MALFORMED_FACTOR);
        }
    }

    // ... To List

    public static RAbstractListVector createToListVector(RAbstractLogicalVector vector, boolean keepAttributes) {
        return new RLogicalToListVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RIntVector vector, boolean keepAttributes) {
        return new RIntToListVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RDoubleVector vector, boolean keepAttributes) {
        return new RDoubleToListVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RRawVector vector, boolean keepAttributes) {
        return new RRawToListVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RAbstractComplexVector vector, boolean keepAttributes) {
        return new RComplexToListVectorClosure(vector, keepAttributes);
    }

    public static RAbstractListVector createToListVector(RAbstractStringVector vector, boolean keepAttributes) {
        return new RStringToListVectorClosure(vector, keepAttributes);
    }

    /**
     * (Shallow) copies the names, dimnames, rownames and dimensions attributes. The original
     * contract of closures is that there attributes are preserved even when
     * {@code keepAttributes == true}.
     */
    public static void initRegAttributes(RAbstractVector closure, RAbstractVector delegate) {
        closure.setNames(delegate.getNames());
        closure.setDimensions(delegate.getDimensions());
        closure.setDimNames(delegate.getDimNames());
        Object rowNames = delegate.getRowNames();
        if (rowNames != RNull.instance) {
            closure.setRowNames((RAbstractVector) rowNames);
        }
    }

}
