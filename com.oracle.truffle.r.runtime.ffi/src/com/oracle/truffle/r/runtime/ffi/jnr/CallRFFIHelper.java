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

import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

/**
 * This class provides methods that match the functionality of the macro/function definitions in
 * thye R header files, e.g. {@code Rinternals.h} that are used by C/C++ code. For ease of
 * identification, we use method names that, as far as possible, match the names in the hreader
 * files. These methods should never be called from normal FastR code.
 */
public class CallRFFIHelper {
    @SuppressWarnings("unused") private static final NACheck elementNACheck = NACheck.create();

    static RIntVector ScalarInteger(int value) {
        return RDataFactory.createIntVectorFromScalar(value);
    }

    /**
     * Helper function that handles {@link Integer} and {@link RIntVector} "vectors".
     *
     * @return value at logical index 0
     */
    static int getIntDataAtZero(Object x) {
        if (x instanceof Integer) {
            return ((Integer) x).intValue();
        } else if (x instanceof RIntVector) {
            return ((RIntVector) x).getDataAt(0);
        } else {
            assert false;
            return 0;
        }
    }

}
