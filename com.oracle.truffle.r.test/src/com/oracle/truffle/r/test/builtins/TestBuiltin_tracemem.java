/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_tracemem extends TestBase {
    @Test
    public void basicTests() {
        // tracemem returns the hash in form of "<0x0fa0abcd>", we get rid of the "<" and ">" to
        // match it against the output that is produced by copying 'v', which should be in the form
        // of "tracemem[0x0fa0abcd->someotherhash]".
        assertEval("v <- c(1,10); addr<-tracemem(v); f<-function(x) x[[1]]<-42; out<-capture.output(f(v)); addr<-sub('>','',sub('<','',addr)); grep(paste0('tracemem[', addr), out, fixed=TRUE)");
    }
}
