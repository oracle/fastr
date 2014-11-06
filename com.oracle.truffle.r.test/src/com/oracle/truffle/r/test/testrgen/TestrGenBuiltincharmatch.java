/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestrGenBuiltincharmatch extends TestBase {

    @Test
    @Ignore
    public void testcharmatch1() {
        assertEval("argv <- list(c(\'x\', \'y\', \'z\'), c(\'row.names\', \'x\', \'y\', \'z\'), 0L); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testcharmatch2() {
        assertEval("argv <- list(character(0), c(\'row.names\', \'height\', \'weight\'), 0L); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testcharmatch3() {
        assertEval("argv <- list(\'package:methods\', c(\'.GlobalEnv\', \'CheckExEnv\', \'package:stats\', \'package:graphics\', \'package:grDevices\', \'package:utils\', \'package:datasets\', \'package:methods\', \'Autoloads\', \'package:base\'), NA_integer_); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testcharmatch4() {
        assertEval("argv <- list(\'package:methods\', c(\'.GlobalEnv\', \'package:graphics\', \'package:stats\', \'Autoloads\', \'package:base\'), NA_integer_); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testcharmatch5() {
        assertEval("argv <- list(c(\'0\', \'1\'), c(\'0\', \'1\'), 0); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testcharmatch6() {
        assertEval("argv <- list(c(\'m\', \'f\'), c(\'male\', \'female\'), NA_integer_); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testcharmatch7() {
        assertEval("argv <- list(\'me\', c(\'mean\', \'median\', \'mode\'), NA_integer_); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    @Ignore
    public void testcharmatch8() {
        assertEval("argv <- list(character(0), c(\'semiTransparency\', \'transparentBackground\', \'rasterImage\', \'capture\', \'locator\', \'events\'), 0L); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
