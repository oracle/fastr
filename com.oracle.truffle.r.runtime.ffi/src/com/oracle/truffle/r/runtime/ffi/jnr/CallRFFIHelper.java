/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.jnr;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

/**
 * This class provides methods that match the functionality of the macro/function definitions in
 * thye R header files, e.g. {@code Rinternals.h} that are used by C/C++ code. For ease of
 * identification, we use method names that, as far as possible, match the names in the hreader
 * files. These methods should never be called from normal FastR code.
 */
public class CallRFFIHelper {
    @SuppressWarnings("unused") private static final NACheck elementNACheck = NACheck.create();

    // Checkstyle: stop method name check

    static RIntVector Rf_ScalarInteger(int value) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    static RDoubleVector Rf_ScalarDouble(double value) {
        return RDataFactory.createDoubleVectorFromScalar(value);
    }

    static int Rf_asInteger(Object x) {
        if (x instanceof Integer) {
            return ((Integer) x).intValue();
        } else if (x instanceof RIntVector) {
            return ((RIntVector) x).getDataAt(0);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static double Rf_asReal(Object x) {
        if (x instanceof Double) {
            return ((Double) x).doubleValue();
        } else if (x instanceof RDoubleVector) {
            return ((RDoubleVector) x).getDataAt(0);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static String Rf_asChar(Object x) {
        if (x instanceof String) {
            return (String) x;
        } else if (x instanceof RStringVector) {
            return ((RStringVector) x).getDataAt(0);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static int LENGTH(Object x) {
        if (x instanceof RAbstractContainer) {
            return ((RAbstractContainer) x).getLength();
        } else if (x instanceof Integer || x instanceof Double || x instanceof Byte || x instanceof String) {
            return 1;
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static void SET_STRING_ELT(Object x, int i, Object v) {
        // TODO error checks
        RStringVector xv = (RStringVector) x;
        xv.setElement(i, v);
    }

    static byte[] RAW(Object x) {
        if (x instanceof RRawVector) {
            return ((RRawVector) x).getDataCopy();
        } else {
            throw RInternalError.unimplemented();
        }

    }

    static int NAMED(Object x) {
        if (x instanceof RShareable) {
            return ((RShareable) x).isShared() ? 1 : 0;
        } else {
            throw RInternalError.unimplemented();
        }
    }

    static Object Rf_duplicate(Object x) {
        if (x instanceof RAbstractVector) {
            return ((RAbstractVector) x).copy();
        } else {
            throw RInternalError.unimplemented();
        }
    }
    // Checkstyle: resume method name check

}
