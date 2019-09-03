/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.engine.interop;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RInteropNA;
import com.oracle.truffle.r.runtime.data.RInteropNA.RInteropComplexNA;
import org.junit.Test;

public class RInteropNAInteropTest extends AbstractInteropTest {

    @Override
    protected boolean shouldTestToNative(TruffleObject obj) {
        return false;
    }

    @Test
    @Override
    public void testIsNull() throws Exception {
        super.testIsNull(); // force inherited tests from AbstractInteropTest
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{RInteropNA.DOUBLE, RInteropNA.INT, RInteropNA.LOGICAL, RInteropNA.STRING,
                        new RInteropComplexNA(RComplex.createNA()),
                        new RInteropComplexNA(RComplex.valueOf(1, RRuntime.COMPLEX_NA_IMAGINARY_PART)),
                        new RInteropComplexNA(RComplex.valueOf(RRuntime.COMPLEX_NA_REAL_PART, 1))};
    }

    @Override
    protected boolean isNull(TruffleObject obj) {
        return true;
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }
}
