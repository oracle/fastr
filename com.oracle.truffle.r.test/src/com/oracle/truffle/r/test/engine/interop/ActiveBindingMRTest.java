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
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import org.junit.Test;

public class ActiveBindingMRTest extends AbstractMRTest {

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
    protected Object getUnboxed(TruffleObject obj) {
        return ((ActiveBinding) obj).readValue();
    }

    @Override
    protected boolean isPointer(TruffleObject obj) {
        return false;
    }

    @Override
    protected TruffleObject[] createTruffleObjects() throws Exception {
        Source src = Source.newBuilder("f=function() {}").mimeType("text/x-r").name("test.R").build();
        PolyglotEngine.Value result = engine.eval(src);
        RFunction fn = result.as(RFunction.class);
        return new TruffleObject[]{new ActiveBinding(RType.Any, fn)};
    }

    @Override
    protected TruffleObject createEmptyTruffleObject() throws Exception {
        return null;
    }
}
