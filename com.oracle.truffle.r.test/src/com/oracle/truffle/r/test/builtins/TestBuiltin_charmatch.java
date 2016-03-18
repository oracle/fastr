/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_charmatch extends TestBase {

    @Test
    public void testcharmatch1() {
        assertEval("argv <- list(c('x', 'y', 'z'), c('row.names', 'x', 'y', 'z'), 0L); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testcharmatch2() {
        assertEval("argv <- list(character(0), c('row.names', 'height', 'weight'), 0L); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testcharmatch3() {
        assertEval("argv <- list('package:methods', c('.GlobalEnv', 'CheckExEnv', 'package:stats', 'package:graphics', 'package:grDevices', 'package:utils', 'package:datasets', 'package:methods', 'Autoloads', 'package:base'), NA_integer_); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testcharmatch4() {
        assertEval("argv <- list('package:methods', c('.GlobalEnv', 'package:graphics', 'package:stats', 'Autoloads', 'package:base'), NA_integer_); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testcharmatch5() {
        assertEval("argv <- list(c('0', '1'), c('0', '1'), 0); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testcharmatch6() {
        assertEval("argv <- list(c('m', 'f'), c('male', 'female'), NA_integer_); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testcharmatch7() {
        assertEval("argv <- list('me', c('mean', 'median', 'mode'), NA_integer_); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testcharmatch8() {
        assertEval("argv <- list(character(0), c('semiTransparency', 'transparentBackground', 'rasterImage', 'capture', 'locator', 'events'), 0L); .Internal(charmatch(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testCharMatch() {
        assertEval("{charmatch(\"abc\", \"deeee\",c(\"3\",\"4\"))}");
        assertEval("{charmatch(\"abc\", \"deeee\")}");
        assertEval("{charmatch(\"abc\", \"deeeec\",c(\"3\",\"4\"))}");
        assertEval("{charmatch(\"\", \"\")}");
        assertEval("{charmatch(\"m\",   c(\"mean\", \"median\", \"mode\"))}");
        assertEval("{charmatch(\"med\", c(\"mean\", \"median\", \"mode\"))}");
        assertEval("{charmatch(matrix(c(9,3,1,6),2,2,byrow=T), \"hello\")}");
        assertEval("{charmatch(matrix(c('h',3,'e',6),2,2,byrow=T), \"hello\")}");
        assertEval("{charmatch(c(\"ole\",\"ab\"),c(\"ole\",\"ab\"))}");
        assertEval("{charmatch(c(\"ole\",\"ab\"),c(\"ole\",\"ole\"))}");
        assertEval("{charmatch(matrix(c('h','l','e',6),2,2,byrow=T), \"hello\")}");
    }
}
