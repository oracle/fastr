/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tck;

import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.tck.TruffleTCK;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.TruffleVM;

import static org.junit.Assert.*;

import org.junit.Test;

public class FastRTckTest extends TruffleTCK {
    @Test
    public void testVerifyPresence() {
        TruffleVM vm = TruffleVM.newVM().build();
        assertTrue("Our language is present", vm.getLanguages().containsKey("text/x-r"));
    }

    // @formatter:off
    private static final Source INITIALIZATION = Source.fromText(
        "fourtyTwo <- function() {\n" +
        "  42L\n" +
        "}\n" +
        "Interop.export('fourtyTwo', fourtyTwo)\n" +
        "plus <- function(a, b) {\n" +
        "  a + b\n" +
        "}\n" +
        "Interop.export('plus', plus)\n" +
        "apply <- function(f) {\n" +
        "  f(18L, 32L) + 10L\n" +
        "}\n" +
        "Interop.export('apply', apply)\n" +
        "null <- function() {\n" +
        "  NULL\n" +
        "}\n" +
        "Interop.export('null', null)\n" +
        "counter <- 0L\n" +
        "count <- function() {\n" +
        "  counter <<- counter + 1L\n" +
        "}\n" +
        "Interop.export('count', count)\n",
        "<initialization>"
    ).withMimeType(TruffleRLanguage.MIME);
    // @formatter:on

    @Override
    protected TruffleVM prepareVM() throws Exception {
        TruffleVM vm = TruffleVM.newVM().build();
        vm.eval(INITIALIZATION);
        return vm;
    }

    @Override
    protected String mimeType() {
        return "text/x-r";
    }

    @Override
    protected String fourtyTwo() {
        return "fourtyTwo";
    }

    @Override
    protected String plusInt() {
        return "plus";
    }

    @Override
    protected String returnsNull() {
        return "null";
    }

    @Override
    protected String applyNumbers() {
        return "apply";
    }

    @Override
    protected String countInvocations() {
        return "count";
    }

    @Override
    protected String invalidCode() {
        // @formatter:off
        return
            "main <- f unction() {\n" +
            "  re turn(42)\n" +
            "}\n";
        // @formatter:on
    }

    @Override
    @Test
    public void testNull() {
        // disabled because we don't provide a Java "null" value in R
    }
}
