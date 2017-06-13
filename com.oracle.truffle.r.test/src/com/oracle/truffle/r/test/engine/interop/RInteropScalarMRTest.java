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
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;

public class RInteropScalarMRTest {

    @Test
    public void testRInteroptScalar() throws UnsupportedMessageException {
        testRIS("toByte", "" + Byte.MAX_VALUE, Byte.class);
        testRIS("toChar", "'a'", Character.class);
        testRIS("toFloat", "" + Float.MAX_VALUE, Float.class);
        testRIS("toLong", "" + Long.MAX_VALUE, Long.class);
        testRIS("toShort", "" + Short.MAX_VALUE, Short.class);
    }

    private static void testRIS(String toInteropScalarBuiltin, String value, Class<?> unboxedType) throws UnsupportedMessageException {
        TruffleObject l = createRInteroptScalarTO(toInteropScalarBuiltin, value);

        assertFalse(ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), l));
        assertFalse(ForeignAccess.sendHasSize(Message.HAS_SIZE.createNode(), l));

        assertTrue(ForeignAccess.sendIsBoxed(Message.IS_BOXED.createNode(), l));
        Object ub = ForeignAccess.sendUnbox(Message.UNBOX.createNode(), l);
        assertEquals(unboxedType, ub.getClass());
    }

    private static TruffleObject createRInteroptScalarTO(String toInteropScalarBuiltin, String value) {
        PolyglotEngine engine = PolyglotEngine.newBuilder().build();
        Source src = Source.newBuilder(".fastr.interop." + toInteropScalarBuiltin + "(" + value + ")").mimeType("text/x-r").name("test.R").build();
        PolyglotEngine.Value result = engine.eval(src);
        return result.as(TruffleObject.class);
    }
}
