/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_gettext extends TestBase {

    @Test
    public void testgettext1() {
        assertEval("argv <- list(NULL, 'Loading required package: %s'); .Internal(gettext(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testgettext2() {
        assertEval("argv <- list(NULL, ''); .Internal(gettext(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testgettext3() {
        assertEval("argv <- list(NULL, 'The following object is masked from ‘package:base’:\\n\\n    det\\n'); .Internal(gettext(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testgettext4() {
        assertEval("argv <- list(NULL, c('/', ' not meaningful for factors')); .Internal(gettext(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testgettext5() {
        assertEval("argv <- list(NULL, character(0)); .Internal(gettext(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testgettext6() {
        assertEval("argv <- list(NULL, NULL); .Internal(gettext(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testgettext() {
        assertEval("gettext('Loading required package: %s')");
        assertEval("gettext(domain='foo', 'bar')");
    }
}
