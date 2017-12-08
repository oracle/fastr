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
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.test.generate.FastRSession;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Test;

public class ActiveBindingMRTest extends AbstractMRTest {

    @Test
    @Override
    public void testIsNull() throws Exception {
        super.testIsNull(); // force inherited tests from AbstractMRTest
    }

    @Override
    protected Object getUnboxed(TruffleObject obj) {
        return ((ActiveBinding) obj).readValue();
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        Source src = Source.newBuilder("R", "f=function() {}", "<testfunction>").internal(true).buildLiteral();
        Value result = context.eval(src);
        RFunction fn = (RFunction) FastRSession.getReceiver(result);
        return new TruffleObject[]{new ActiveBinding(RType.Any, fn)};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }
}
