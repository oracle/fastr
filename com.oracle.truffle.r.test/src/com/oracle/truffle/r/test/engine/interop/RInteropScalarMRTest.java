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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.data.RInteropScalar;
import org.junit.Assert;

public class RInteropScalarMRTest extends AbstractMRTest {

    @Test
    public void testRInteroptScalar() throws Exception {
        for (TruffleObject obj : createTruffleObjects()) {
            RInteropScalar is = (RInteropScalar) obj;
            testRIS(obj, is.getJavaType());
        }
    }

    private static void testRIS(TruffleObject obj, Class<?> unboxedType) throws Exception {
        assertFalse(ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), obj));
        assertFalse(ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), obj));

        assertTrue(ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), obj));
        Object ub = ForeignAccess.sendUnbox(Message.UNBOX.createNode(), obj);
        assertEquals(unboxedType, ub.getClass().getField("TYPE").get(null));
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{RInteropScalar.RInteropByte.valueOf(Byte.MAX_VALUE),
                        RInteropScalar.RInteropChar.valueOf('a'),
                        RInteropScalar.RInteropFloat.valueOf(Float.MAX_VALUE),
                        RInteropScalar.RInteropLong.valueOf(Long.MAX_VALUE),
                        RInteropScalar.RInteropShort.valueOf(Short.MAX_VALUE)};
    }

    @Override
    protected boolean isBoxed(TruffleObject arg0) {
        return true;
    }

    @Override
    protected Object getUnboxed(TruffleObject obj) {
        RInteropScalar is = (RInteropScalar) obj;
        try {
            return is.getClass().getDeclaredMethod("getValue").invoke(is);
        } catch (Exception ex) {
            Assert.fail("can't read interop scalar value " + ex);
        }
        return null;
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }
}
