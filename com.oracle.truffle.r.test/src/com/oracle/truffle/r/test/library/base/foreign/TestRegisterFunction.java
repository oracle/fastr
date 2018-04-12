/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.base.foreign;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestRegisterFunction extends TestBase {

    @Test
    public void testDotCall() {
        assertEvalFastR(".fastr.register.functions('testLib', environment(), 1, list(callfun1=function() {1})); .Call(callfun1)==1", "T");

        assertEvalFastR(".fastr.register.functions('testLib', environment(), 1, list(callfun2=function() {2}, callfun3=function() {3}));" +
                        ".fastr.register.functions('testLib', environment(), 1, list(callfun4=function() {4}));" + " .Call(callfun2)==2 && .Call(callfun3)==3 && .Call(callfun4)==4", "T");

        assertEvalFastR(".fastr.register.functions('testLib', environment(), 1, list(callfunptr=function() {5})); " +
                        "assign('callptr', getNativeSymbolInfo('callfunptr', 'testLib')$address); " +
                        ".Call(callptr)==5", "T");
    }

    @Test
    public void testDotExternal() {
        assertEvalFastR(".fastr.register.functions('testLib', environment(), 3, list(externalfun1=function() {1})); .External(externalfun1)==1", "T");

        assertEvalFastR(".fastr.register.functions('testLib', environment(), 3, list(externalfun2=function() {2}, externalfun3=function() {3}));" +
                        ".fastr.register.functions('testLib', environment(), 3, list(externalfun4=function() {4}));" +
                        " .External(externalfun2)==2 && .External(externalfun3)==3 && .External(externalfun4)==4",
                        "T");
    }

    @Test
    public void testDotC() {
        assertEvalFastR(".fastr.register.functions('testLib', environment(), 0, list(cfun1=function() {1})); .C(cfun1)==1", "T");

        assertEvalFastR(".fastr.register.functions('testLib', environment(), 0, list(cfun2=function() {2}, cfun3=function() {3}));" +
                        ".fastr.register.functions('testLib', environment(), 0, list(cfun4=function() {4}));" + " .C(cfun2)==2 && .C(cfun3)==3 && .C(cfun4)==4",
                        "T");
    }

    @Test
    public void testDotFortran() {
        assertEvalFastR(".fastr.register.functions('testLib', environment(), 2, list(ffun5=function() {5})); .Fortran(ffun5)==5", "T");

        assertEvalFastR(".fastr.register.functions('testLib', environment(), 2, list(ffun6=function() {6}, ffun7=function() {7}));" +
                        ".fastr.register.functions('testLib', environment(), 2, list(ffun8=function() {8}));" +
                        " .Fortran(ffun6)==6 && .Fortran(ffun7)==7 && .Fortran(ffun8)==8",
                        "T");
    }
}
