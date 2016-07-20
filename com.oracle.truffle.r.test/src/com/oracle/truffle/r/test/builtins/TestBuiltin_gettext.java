/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
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
