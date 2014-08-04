/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltinpathexpand extends TestBase {

    @Test
    public void testpathexpand1() {
        assertEval("argv <- list(\'/tmp/RtmptPgrXI/Pkgs/pkgA\'); .Internal(path.expand(argv[[1]]))");
    }

    @Test
    public void testpathexpand2() {
        assertEval("argv <- list(c(\'/home/lzhao/hg/r-instrumented/tests/compiler.Rcheck\', \'/home/lzhao/R/x86_64-unknown-linux-gnu-library/3.0\')); .Internal(path.expand(argv[[1]]))");
    }

    @Test
    public void testpathexpand3() {
        assertEval("argv <- list(character(0)); .Internal(path.expand(argv[[1]]))");
    }
}