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
public class TestBuiltin_basename extends TestBase {

    @Test
    public void testbasename1() {
        assertEval("argv <- list('/home/roman/r-instrumented/library/base/help/DateTimeClasses'); .Internal(basename(argv[[1]]))");
    }

    @Test
    public void testbasename2() {
        assertEval("argv <- list(structure('myTst', .Names = '')); .Internal(basename(argv[[1]]))");
    }

    @Test
    public void testbasename3() {
        assertEval("argv <- list(c('file55711ba85492.R', '/file55711ba85492.R')); .Internal(basename(argv[[1]]))");
    }

    @Test
    public void testbasename4() {
        assertEval("argv <- list(character(0)); .Internal(basename(argv[[1]]))");
    }

    @Test
    public void testbasename5() {
        assertEval("argv <- list(structure('/home/lzhao/hg/r-instrumented/library/utils', .Names = 'Dir')); .Internal(basename(argv[[1]]))");
    }

    @Test
    public void testbasename6() {
        assertEval("argv <- list('tk_messageBox.Rd'); .Internal(basename(argv[[1]]))");
    }

    @Test
    public void testbasename7() {
        assertEval("argv <- list(c('.', '.', '.', '.', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'R', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'data', 'inst', 'inst', 'inst/doc', 'inst/doc', 'inst/doc', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'man', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb/figures', 'noweb/figures', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb/rates', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'noweb', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'src', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'tests', 'vignettes', 'vignettes', 'vignettes')); .Internal(basename(argv[[1]]))");
    }

    @Test
    public void testbasename9() {
        assertEval("argv <- structure(list(path = 'myTst'), .Names = 'path');do.call('basename', argv)");
    }
}
