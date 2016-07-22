/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.fastr;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestStateTrans extends TestBase {

    @Test
    public void testTransitions() {
        assertEvalFastR("{ f<-function(x) .fastr.refcountinfo(x); f(c(1,2)) }", "1");
        assertEvalFastR("{ f<-function(x) { y<-x; .fastr.refcountinfo(y) }; f(c(1,2)) }", "2");
        assertEvalFastR("{ x<-c(1,2); f<-function(x) .fastr.refcountinfo(x); f(x) }", "2");
        assertEvalFastR("{ f<-function(x) { xi1<-.fastr.identity(x); x[1]<-7; xi2<-.fastr.identity(x); xi1 == xi2 }; f(c(1,2)) }", "TRUE");
        assertEvalFastR("{ f<-function(y) { x<-y; xi1<-.fastr.identity(x); x[1]<-7; xi2<-.fastr.identity(x); xi1 == xi2 }; f(c(1,2)) }", "FALSE");
        // after returning from read-only functions, vector should be modifiable without
        // creating a copy
        assertEvalFastR("{ x<-rep(1, 100); xi1<-.fastr.identity(x); f<-function(x) { x }; f(x); x[1]<-7; xi2<-.fastr.identity(x); xi1 == xi2 }", "TRUE");
        assertEvalFastR("{ x<-rep(1, 100); xi1<-.fastr.identity(x); f<-function(x) { y<-x; y }; f(x); x[1]<-7; xi2<-.fastr.identity(x); xi1 == xi2 }", "TRUE");
    }
}
