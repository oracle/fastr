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

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteropComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import static com.oracle.truffle.r.test.engine.interop.AbstractMRTest.context;
import com.oracle.truffle.r.test.generate.FastRSession;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RComplexMRTest extends AbstractMRTest {

    @Test
    @Override
    public void testIsNull() throws Exception {
        super.testIsNull(); // force inherited tests from AbstractMRTest
    }

    @Test
    public void testInteropComplex() throws Exception {
        Object c = create("1+42i");
        assertEquals(RComplex.valueOf(1, 42), c);

        RFunction fun = (RFunction) create("function() 1+42i");
        TruffleObject obj = (TruffleObject) ForeignAccess.sendExecute(Message.EXECUTE.createNode(), fun);
        assertTrue(obj instanceof RAbstractComplexVector);
        assertFalse(obj instanceof RInteropComplex);

        obj = (TruffleObject) ForeignAccess.sendRead(Message.READ.createNode(), obj, 0);
        assertFalse(obj instanceof RAbstractComplexVector);
        assertTrue(obj instanceof RInteropComplex);
    }

    @Override
    protected boolean isNull(TruffleObject obj) {
        assert obj instanceof RComplex;
        return ((RComplex) obj).isNA();
    }

    @Override
    protected int getSize(TruffleObject arg0) {
        return 1;
    }

    @Override
    protected boolean canRead(TruffleObject arg0) {
        return true;
    }

    @Override
    protected boolean shouldTestToNative(TruffleObject obj) {
        return true;
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        return new TruffleObject[]{RComplex.valueOf(1, 1), RComplex.createNA()};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }

    @Override
    protected String[] getKeys(TruffleObject obj) {
        return new String[]{"re", "im"};
    }

    private static Object create(String fun) {
        Source src = Source.newBuilder("R", fun, "<testrfunction>").internal(true).buildLiteral();
        Value result = context.eval(src);
        return FastRSession.getReceiver(result);
    }
}
