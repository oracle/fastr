/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.engine.interop;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.data.RRaw;
import org.junit.Test;

public class RRawMRTest extends AbstractMRTest {

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{RRaw.valueOf((byte) 1)};
    }

    @Test
    @Override
    public void testIsNull() throws Exception {
        super.testIsNull(); // force inherited tests from AbstractMRTest
    }

    @Override
    protected boolean isBoxed(TruffleObject obj) {
        return true;
    }

    @Override
    protected boolean isPointer(TruffleObject obj) {
        return false;
    }

    @Override
    protected boolean hasSize(TruffleObject obj) {
        return false;
    }

    @Override
    protected Object getUnboxed(TruffleObject obj) {
        return ((RRaw) obj).getValue();
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }
}
