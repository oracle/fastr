/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_cbind extends TestBase {

    @Test
    public void testcbind1() {
        assertEval("argv <- list(748L, c(5.08759633523238, 4.0943445622221, 5.66642668811243,     3.43398720448515), c(1L, 1L, 1L, 1L), 1L, c(FALSE, TRUE,     TRUE, TRUE), c(0, 1, 0, 1), c(0, 1, 1, 1), c(0, 1, 0, 1),     c(FALSE, FALSE, TRUE, FALSE), c(FALSE, FALSE, FALSE, TRUE));" +
                        "do.call('cbind', argv)");
    }

    @Test
    public void testcbind2() {
        assertEval("argv <- list(structure(c(-0.0296690260968828, 0.200337918547016,     -0.38901358729166, 0.076054310915896, -0.5953576286578, 1.55058467328697,     -0.189955959788191, -1.31965097077132, 0.596281133731208,     1.22982396127581), .Dim = c(10L, 1L), .Dimnames = list(NULL,     'runif.10...pi.2..pi.2.'), circularp = structure(list(type = 'angles',     units = 'radians', template = 'none', modulo = 'asis', zero = 0,     rotation = 'counter'), .Names = c('type', 'units', 'template',     'modulo', 'zero', 'rotation')), class = c('circular', 'matrix')),     structure(c(-0.0296690260968828, 0.200337918547016, -0.38901358729166,         0.076054310915896, -0.5953576286578, 1.55058467328697,         -0.189955959788191, -1.31965097077132, 0.596281133731208,         1.22982396127581), .Dim = c(10L, 1L), .Dimnames = list(NULL,         'runif.10...pi.2..pi.2.'), circularp = structure(list(type = 'angles',         units = 'radians', template = 'none', modulo = 'asis',         zero = 0, rotation = 'counter'), .Names = c('type', 'units',         'template', 'modulo', 'zero', 'rotation')), class = c('circular',         'matrix')));" +
                        "do.call('cbind', argv)");
    }

    @Test
    public void testcbind3() {
        assertEval("argv <- list(structure(c(3L, 1L, 2L, 2L, 2L, 2L, 1L, 2L, 2L,     2L, 1L, 2L, 2L, 1L, 2L, 3L, 3L, 2L, 2L, 2L, 2L, 4L, 4L, 3L,     1L, 2L, 2L, 1L, 2L, 3L, 1L, 1L, 1L, 4L, 2L, 2L, 2L, 2L, 1L,     1L, 2L, 1L, 3L, 3L, 2L, 2L, 3L, 2L, 2L, 1L, 2L, 2L, 2L, 2L,     1L, 1L, 4L, 2L, 3L, 2L, 1L, 3L, 2L, 3L, 1L, 2L, 3L, 4L, 2L,     4L, 2L, 3L, 1L, 1L, 3L, 4L, 3L, 1L, 2L, 2L, 1L, 2L, 3L, 1L,     2L, 2L, 2L, 2L, 4L, 2L, 2L, 2L, 3L, 1L, 1L, 1L, 2L, 2L, 4L,     1L, 1L, 1L, 1L, 2L, 4L, 3L, 2L, 3L, 3L, 2L, 2L, 2L, 2L, 2L,     1L, 2L, 2L, 2L, 2L, 2L, 4L, 2L, 2L, 1L, 3L, 2L, 2L, 1L, 4L,     1L, 3L, 2L, 2L, 3L, 2L, 1L, 2L, 1L, 1L, 2L, 2L, 1L, 2L, 3L,     2L, 1L, 3L, 1L, 3L, 1L, 1L, 1L, 2L, 1L, 2L, 1L, 3L, 1L, 2L,     1L, 2L, 2L, 4L, 2L, 2L, 2L, 2L, 1L, 3L, 1L, 1L, 1L, 2L, 2L,     3L, 2L, 4L, 3L, 3L, 4L, 1L, 3L, 2L, 2L, 4L, 2L, 1L, 2L, 2L,     2L, 3L, 2L, 2L, 1L, 2L, 3L, 2L, 1L, 2L, 2L), .Label = c('1 Extremely well',     '2 Quite well', '3 Not too well', '4 Not well at all'), class = 'factor'),     structure(c(1L, 2L, 2L, 4L, 2L, 2L, 1L, 2L, 2L, 3L, 2L, 3L,         3L, 1L, 2L, 3L, 3L, 2L, 1L, 4L, 2L, 3L, 1L, 3L, 1L, 4L,         1L, 1L, 3L, 4L, 2L, 1L, 2L, 3L, 2L, 3L, 4L, 4L, 1L, 4L,         3L, 1L, 3L, 3L, 2L, 3L, 2L, 2L, 3L, 1L, 2L, 2L, 2L, 3L,         2L, 1L, 4L, 2L, 3L, 4L, 1L, 3L, 2L, 3L, 1L, 2L, 2L, 4L,         2L, 4L, 2L, 3L, 1L, 1L, 4L, 4L, 3L, 2L, 3L, 2L, 3L, 3L,         3L, 2L, 3L, 2L, 2L, 3L, 4L, 2L, 4L, 2L, 3L, 1L, 1L, 1L,         2L, 3L, 4L, 3L, 2L, 1L, 2L, 1L, 4L, 3L, 1L, 2L, 2L, 2L,         2L, 2L, 3L, 4L, 2L, 3L, 2L, 1L, 3L, 2L, 3L, 3L, 3L, 1L,         2L, 2L, 3L, 2L, 4L, 1L, 3L, 3L, 4L, 3L, 2L, 2L, 3L, 2L,         4L, 4L, 2L, 1L, 4L, 3L, 2L, 4L, 3L, 4L, 2L, 2L, 1L, 2L,         3L, 1L, 2L, 3L, 2L, 1L, 4L, 3L, 2L, 3L, 3L, 2L, 2L, 1L,         4L, 2L, 3L, 2L, 2L, 2L, 4L, 2L, 4L, 2L, 3L, 3L, 4L, 4L,         1L, 3L, 4L, 3L, 4L, 2L, 1L, 2L, 2L, 2L, 2L, 3L, 2L, 2L,         2L, 3L, 3L, 2L, 3L, 2L), .Label = c('1 Extremely well',         '2 Quite well', '3 Not too well', '4 Not well at all'),         class = 'factor'), structure(c(2L, 2L, 2L, 2L, 2L, 2L,         1L, 2L, 2L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 2L, 1L, 2L, 1L,         1L, 3L, 1L, 2L, 1L, 3L, 2L, 2L, 3L, 2L, 2L, 3L, 2L, 1L,         2L, 2L, 3L, 4L, 2L, 2L, 2L, 2L, 3L, 2L, 2L, 2L, 3L, 1L,         2L, 3L, 2L, 1L, 1L, 3L, 2L, 2L, 4L, 2L, 2L, 2L, 4L, 2L,         2L, 2L, 3L, 3L, 3L, 4L, 2L, 1L, 2L, 2L, 1L, 1L, 1L, 2L,         2L, 1L, 1L, 2L, 1L, 2L, 3L, 2L, 3L, 2L, 2L, 4L, 3L, 2L,         2L, 2L, 2L, 1L, 1L, 1L, 2L, 2L, 2L, 1L, 1L, 1L, 2L, 2L,         4L, 3L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 1L,         2L, 2L, 3L, 2L, 3L, 2L, 2L, 2L, 2L, 1L, 3L, 2L, 2L, 2L,         2L, 3L, 2L, 2L, 1L, 1L, 1L, 2L, 2L, 1L, 2L, 2L, 2L, 3L,         2L, 2L, 3L, 1L, 1L, 4L, 2L, 1L, 2L, 1L, 2L, 1L, 1L, 2L,         2L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 3L, 1L, 1L, 2L, 3L, 1L,         3L, 2L, 2L, 2L, 2L, 3L, 2L, 2L, 2L, 2L, 3L, 2L, 1L, 2L,         2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('1 Extremely well',         '2 Quite well', '3 Not too well', '4 Not well at all'),         class = 'factor'), structure(c(1L, 2L, 2L, 1L, 3L, 2L,         2L, 3L, 2L, 2L, 3L, 2L, 2L, 4L, 1L, 4L, 2L, 4L, 3L, 2L,         3L, 2L, 3L, 1L, 1L, 1L, 3L, 1L, 2L, 2L, 2L, 2L, 2L, 2L,         3L, 2L, 1L, 1L, 4L, 3L, 1L, 4L, 3L, 1L, 2L, 2L, 2L, 2L,         1L, 1L, 2L, 2L, 3L, 2L, 1L, 3L, 4L, 2L, 1L, 2L, 2L, 2L,         2L, 1L, 2L, 2L, 1L, 2L, 2L, 1L, 3L, 1L, 4L, 2L, 2L, 4L,         2L, 2L, 2L, 3L, 4L, 2L, 2L, 1L, 1L, 3L, 2L, 1L, 2L, 4L,         2L, 1L, 2L, 2L, 2L, 2L, 1L, 2L, 1L, 1L, 1L, 3L, 3L, 2L,         1L, 1L, 2L, 3L, 1L, 2L, 2L, 3L, 2L, 2L, 3L, 2L, 3L, 3L,         2L, 3L, 2L, 2L, 2L, 3L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 2L,         2L, 2L, 2L, 2L, 2L, 1L, 2L, 4L, 2L, 2L, 2L, 2L, 2L, 2L,         2L, 1L, 3L, 3L, 2L, 3L, 2L, 2L, 2L, 2L, 4L, 2L, 2L, 1L,         2L, 2L, 2L, 2L, 2L, 1L, 3L, 2L, 2L, 1L, 3L, 2L, 4L, 3L,         2L, 2L, 1L, 1L, 3L, 2L, 2L, 1L, 2L, 2L, 2L, 3L, 1L, 2L,         2L, 2L, 3L, 2L, 2L, 3L, 2L, 2L, 1L, 2L, 2L, 1L), .Label = c('1 Extremely well',         '2 Quite well', '3 Not too well', '4 Not well at all'),         class = 'factor'), structure(c(1L, 2L, 3L, 1L, 4L, 3L,         4L, 2L, 2L, 4L, 2L, 2L, 2L, 4L, 2L, 3L, 2L, 4L, 4L, 4L,         3L, 2L, 3L, 3L, 1L, 1L, 3L, 2L, 3L, 2L, 2L, 2L, 2L, 1L,         3L, 2L, 1L, 1L, 4L, 4L, 1L, 1L, 3L, 2L, 4L, 3L, 2L, 3L,         1L, 1L, 3L, 3L, 4L, 2L, 1L, 4L, 4L, 4L, 2L, 3L, 2L, 2L,         3L, 2L, 4L, 3L, 1L, 2L, 3L, 1L, 3L, 2L, 3L, 4L, 2L, 4L,         2L, 3L, 3L, 3L, 4L, 1L, 2L, 2L, 2L, 4L, 2L, 3L, 2L, 4L,         2L, 2L, 2L, 3L, 3L, 3L, 1L, 3L, 2L, 2L, 3L, 4L, 4L, 3L,         2L, 1L, 3L, 2L, 2L, 2L, 3L, 1L, 2L, 2L, 2L, 2L, 4L, 3L,         3L, 3L, 2L, 4L, 2L, 3L, 2L, 2L, 3L, 3L, 3L, 3L, 2L, 3L,         2L, 2L, 3L, 3L, 2L, 2L, 4L, 3L, 3L, 3L, 3L, 2L, 3L, 2L,         2L, 2L, 3L, 4L, 3L, 2L, 2L, 3L, 2L, 1L, 4L, 3L, 2L, 1L,         3L, 3L, 3L, 3L, 2L, 1L, 3L, 2L, 2L, 4L, 3L, 4L, 4L, 3L,         2L, 2L, 1L, 2L, 3L, 2L, 3L, 2L, 2L, 3L, 3L, 4L, 3L, 3L,         2L, 4L, 4L, 2L, 2L, 4L, 3L, 1L, 2L, 2L, 2L, 2L), .Label = c('1 Extremely well',         '2 Quite well', '3 Not too well', '4 Not well at all'),         class = 'factor'), structure(c(2L, 2L, 2L, 2L, 4L, 2L,         2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 2L, 2L, 2L, 4L, 2L, 4L,         3L, 2L, 3L, 2L, 1L, 1L, 2L, 2L, 2L, 1L, 2L, 2L, 2L, 2L,         2L, 2L, 1L, 1L, 3L, 3L, 2L, 4L, 2L, 2L, 2L, 2L, 2L, 2L,         1L, 1L, 3L, 4L, 1L, 2L, 1L, 2L, 4L, 4L, 2L, 1L, 2L, 2L,         2L, 1L, 2L, 2L, 1L, 2L, 2L, 1L, 1L, 2L, 1L, 2L, 2L, 2L,         2L, 2L, 2L, 2L, 4L, 2L, 3L, 2L, 2L, 3L, 2L, 2L, 2L, 4L,         2L, 2L, 2L, 2L, 3L, 2L, 1L, 2L, 1L, 1L, 2L, 2L, 3L, 2L,         2L, 1L, 3L, 3L, 2L, 3L, 2L, 1L, 2L, 3L, 3L, 2L, 2L, 4L,         2L, 4L, 3L, 3L, 3L, 3L, 2L, 2L, 3L, 2L, 3L, 2L, 3L, 2L,         1L, 2L, 3L, 3L, 1L, 2L, 2L, 2L, 2L, 3L, 1L, 2L, 3L, 1L,         2L, 2L, 2L, 4L, 1L, 2L, 2L, 3L, 2L, 2L, 3L, 4L, 1L, 1L,         2L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 2L, 2L, 3L, 2L, 2L, 4L,         2L, 2L, 2L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 3L, 3L, 2L, 2L,         2L, 3L, 3L, 2L, 2L, 2L, 3L, 2L, 2L, 2L, 2L, 2L), .Label = c('1 Extremely well',         '2 Quite well', '3 Not too well', '4 Not well at all'),         class = 'factor'));" +
                        "do.call('cbind', argv)");
    }

    @Test
    public void testcbind4() {
        // FastR gives longer output of "structure(c(3, 2, 2,..." but gives
        // wider output of the structure (258 columns indent) which is less readable
        assertEval(Ignored.OutputFormatting,
                        "argv <- list(structure(list(Sepal.Length = c(5.1, 4.9, 4.7, 4.6,     5, 5.4, 4.6, 5, 4.4, 4.9, 5.4, 4.8, 4.8, 4.3, 5.8, 5.7, 5.4,     5.1, 5.7, 5.1, 5.4, 5.1, 4.6, 5.1, 4.8, 5, 5, 5.2, 5.2, 4.7,     4.8, 5.4, 5.2, 5.5, 4.9, 5, 5.5, 4.9, 4.4, 5.1, 5, 4.5, 4.4,     5, 5.1, 4.8, 5.1, 4.6, 5.3, 5, 7, 6.4, 6.9, 5.5, 6.5, 5.7,     6.3, 4.9, 6.6, 5.2, 5, 5.9, 6, 6.1, 5.6, 6.7, 5.6, 5.8, 6.2,     5.6, 5.9, 6.1, 6.3, 6.1, 6.4, 6.6, 6.8, 6.7, 6, 5.7, 5.5,     5.5, 5.8, 6, 5.4, 6, 6.7, 6.3, 5.6, 5.5, 5.5, 6.1, 5.8, 5,     5.6, 5.7, 5.7, 6.2, 5.1, 5.7, 6.3, 5.8, 7.1, 6.3, 6.5, 7.6,     4.9, 7.3, 6.7, 7.2, 6.5, 6.4, 6.8, 5.7, 5.8, 6.4, 6.5, 7.7,     7.7, 6, 6.9, 5.6, 7.7, 6.3, 6.7, 7.2, 6.2, 6.1, 6.4, 7.2,     7.4, 7.9, 6.4, 6.3, 6.1, 7.7, 6.3, 6.4, 6, 6.9, 6.7, 6.9,     5.8, 6.8, 6.7, 6.7, 6.3, 6.5, 6.2, 5.9), Sepal.Width = c(4,     3, 3, 3, 4, 4, 3, 3, 3, 3, 4, 3, 3, 3, 4, 4, 4, 4, 4, 4,     3, 4, 4, 3, 3, 3, 3, 4, 3, 3, 3, 3, 4, 4, 3, 3, 4, 4, 3,     3, 4, 2, 3, 4, 4, 3, 4, 3, 4, 3, 3, 3, 3, 2, 3, 3, 3, 2,     3, 3, 2, 3, 2, 3, 3, 3, 3, 3, 2, 2, 3, 3, 2, 3, 3, 3, 3,     3, 3, 3, 2, 2, 3, 3, 3, 3, 3, 2, 3, 2, 3, 3, 3, 2, 3, 3,     3, 3, 2, 3, 3, 3, 3, 3, 3, 3, 2, 3, 2, 4, 3, 3, 3, 2, 3,     3, 3, 4, 3, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 3,     3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 3, 3, 3), Petal.Length = c(1.4,     1.4, 1.3, 1.5, 1.4, 1.7, 1.4, 1.5, 1.4, 1.5, 1.5, 1.6, 1.4,     1.1, 1.2, 1.5, 1.3, 1.4, 1.7, 1.5, 1.7, 1.5, 1, 1.7, 1.9,     1.6, 1.6, 1.5, 1.4, 1.6, 1.6, 1.5, 1.5, 1.4, 1.5, 1.2, 1.3,     1.4, 1.3, 1.5, 1.3, 1.3, 1.3, 1.6, 1.9, 1.4, 1.6, 1.4, 1.5,     1.4, 4.7, 4.5, 4.9, 4, 4.6, 4.5, 4.7, 3.3, 4.6, 3.9, 3.5,     4.2, 4, 4.7, 3.6, 4.4, 4.5, 4.1, 4.5, 3.9, 4.8, 4, 4.9, 4.7,     4.3, 4.4, 4.8, 5, 4.5, 3.5, 3.8, 3.7, 3.9, 5.1, 4.5, 4.5,     4.7, 4.4, 4.1, 4, 4.4, 4.6, 4, 3.3, 4.2, 4.2, 4.2, 4.3, 3,     4.1, 6, 5.1, 5.9, 5.6, 5.8, 6.6, 4.5, 6.3, 5.8, 6.1, 5.1,     5.3, 5.5, 5, 5.1, 5.3, 5.5, 6.7, 6.9, 5, 5.7, 4.9, 6.7, 4.9,     5.7, 6, 4.8, 4.9, 5.6, 5.8, 6.1, 6.4, 5.6, 5.1, 5.6, 6.1,     5.6, 5.5, 4.8, 5.4, 5.6, 5.1, 5.1, 5.9, 5.7, 5.2, 5, 5.2,     5.4, 5.1), Petal.Width = c(0.2, 0.2, 0.2, 0.2, 0.2, 0.4,     0.3, 0.2, 0.2, 0.1, 0.2, 0.2, 0.1, 0.1, 0.2, 0.4, 0.4, 0.3,     0.3, 0.3, 0.2, 0.4, 0.2, 0.5, 0.2, 0.2, 0.4, 0.2, 0.2, 0.2,     0.2, 0.4, 0.1, 0.2, 0.2, 0.2, 0.2, 0.1, 0.2, 0.2, 0.3, 0.3,     0.2, 0.6, 0.4, 0.3, 0.2, 0.2, 0.2, 0.2, 1.4, 1.5, 1.5, 1.3,     1.5, 1.3, 1.6, 1, 1.3, 1.4, 1, 1.5, 1, 1.4, 1.3, 1.4, 1.5,     1, 1.5, 1.1, 1.8, 1.3, 1.5, 1.2, 1.3, 1.4, 1.4, 1.7, 1.5,     1, 1.1, 1, 1.2, 1.6, 1.5, 1.6, 1.5, 1.3, 1.3, 1.3, 1.2, 1.4,     1.2, 1, 1.3, 1.2, 1.3, 1.3, 1.1, 1.3, 2.5, 1.9, 2.1, 1.8,     2.2, 2.1, 1.7, 1.8, 1.8, 2.5, 2, 1.9, 2.1, 2, 2.4, 2.3, 1.8,     2.2, 2.3, 1.5, 2.3, 2, 2, 1.8, 2.1, 1.8, 1.8, 1.8, 2.1, 1.6,     1.9, 2, 2.2, 1.5, 1.4, 2.3, 2.4, 1.8, 1.8, 2.1, 2.4, 2.3,     1.9, 2.3, 2.5, 2.3, 1.9, 2, 2.3, 1.8), Species = structure(c(1L,     1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L,     1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L,     1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L,     1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L,     2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L,     2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L,     2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L,     3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L,     3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L,     3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L),     .Label = c('setosa', 'versicolor', 'virginica'), class = 'factor')),     .Names = c('Sepal.Length', 'Sepal.Width', 'Petal.Length',         'Petal.Width', 'Species'), row.names = c(NA, -150L),     class = 'data.frame'), structure(c(3, 2, 2, 2, 3, 3, 2, 2,     2, 2, 3, 2, 2, 2, 3, 3, 3, 3, 3, 3, 2, 3, 3, 2, 2, 2, 2,     3, 2, 2, 2, 2, 3, 3, 2, 2, 3, 3, 2, 2, 3, 1, 2, 3, 3, 2,     3, 2, 3, 2, 2, 2, 2, 1, 2, 2, 2, 1, 2, 2, 1, 2, 1, 2, 2,     2, 2, 2, 1, 1, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2,     2, 2, 2, 1, 2, 1, 2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 2, 2, 2,     2, 2, 2, 1, 2, 1, 3, 2, 2, 2, 1, 2, 2, 2, 3, 2, 1, 2, 2,     2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2,     2, 2, 2, 2, 2, 1, 2, 2, 2), .Names = c('4', '3', '3', '3',     '4', '4', '3', '3', '3', '3', '4', '3', '3', '3', '4', '4',     '4', '4', '4', '4', '3', '4', '4', '3', '3', '3', '3', '4',     '3', '3', '3', '3', '4', '4', '3', '3', '4', '4', '3', '3',     '4', '2', '3', '4', '4', '3', '4', '3', '4', '3', '3', '3',     '3', '2', '3', '3', '3', '2', '3', '3', '2', '3', '2', '3',     '3', '3', '3', '3', '2', '2', '3', '3', '2', '3', '3', '3',     '3', '3', '3', '3', '2', '2', '3', '3', '3', '3', '3', '2',     '3', '2', '3', '3', '3', '2', '3', '3', '3', '3', '2', '3',     '3', '3', '3', '3', '3', '3', '2', '3', '2', '4', '3', '3',     '3', '2', '3', '3', '3', '4', '3', '2', '3', '3', '3', '3',     '3', '3', '3', '3', '3', '3', '3', '4', '3', '3', '3', '3',     '3', '3', '3', '3', '3', '3', '3', '3', '3', '3', '2', '3',     '3', '3')));" +
                                        "do.call('cbind', argv)");
    }

    @Test
    public void testcbind5() {
        assertEval("argv <- list(structure(c(1, 223, 312, 712, 889, 1201, 1467),     .Names = c('', '1th break', '2th break', '3th break', '4th break',         '5th break', '6th break')), structure(c(222, 311, 711,     888, 1200, 1466, 1600), .Names = c('1th break', '2th break',     '3th break', '4th break', '5th break', '6th break', '7th break')));" +
                        "do.call('cbind', argv)");
    }

    @Test
    public void testcbind6() {
        assertEval("cbind2(3, 4)");
        assertEval("cbind2(matrix(1:10, 2, 2), matrix(11:20, 2, 2))");
    }

    @Test
    public void testCbind() {
        assertEval("{ cbind() }");
        assertEval("{ cbind(1:3,2) }");
        assertEval("{ cbind(1:3,1:3) }");
        assertEval("{ m <- matrix(1:6, nrow=2) ; cbind(11:12, m) }");

        assertEval("{ cbind(c(1,2)) }");
        assertEval("{ cbind(a=c(b=1,c=2)) }");
        assertEval("{ cbind(c(b=1,c=2)) }");
        assertEval("{ cbind(c(1,c=2)) }");
        assertEval("{ v<-c(b=1, c=2); cbind(v) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y')))) }");
        assertEval("{x <- numeric(); y <- matrix(2,2); dimnames(y) <- list(c(NA_character_,NA_character_), c(NA_character_)); rbind(x, y) }");

        assertEval("{ cbind(a=c(1,2), b=c(3,4)) }");
        assertEval("{ cbind(a=c(x=1,y=2), b=c(3,4)) }");
        assertEval("{ cbind(a=c(1,2), b=c(x=3,y=4)) }");
        assertEval("{ cbind(a=c(x=1,2), b=c(3,y=4)) }");
        assertEval("{ cbind(a=c(1,2), b=c(3,y=4)) }");
        assertEval("{ cbind(a=c(1,x=2), b=c(y=3,4,5,6)) }");
        assertEval("{ cbind(a=c(1,x=2), b=c(3,4,5,6)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y'))), z=c(8,9)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y'))), c(8,9)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), NULL)), z=c(8,9)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2, dimnames=list(NULL, c('x', 'y'))), c(m=8,n=9)) }");
        assertEval("{ cbind(matrix(1:4, nrow=2), z=c(m=8,n=9)) }");

        assertEval("{ cbind(list(1,2), TRUE, \"a\") }");
        assertEval(Output.IgnoreWarningContext, "{ cbind(1:3,1:2) }");
        assertEval("{ cbind(2,3, complex(3,3,2));}");
        assertEval("{ cbind(2,3, c(1,1,1)) }");
        assertEval("{ cbind(2.1:10,32.2) }");

        assertEval("{ x<-list(a=7, b=NULL, c=42); y<-as.data.frame(do.call(cbind,x)); y }");

        // Note: CachedExtractVectorNode replaces vector 'a', 'b', with a scalar 'b', which caused
        // cbind to fail
        assertEval("x <- matrix(1:20, 10, 2); dimnames(x) <- list(1:10, c('a','b')); cbind(1, x[,-1,drop=FALSE]);");

        assertEval("cbind(character(0))");
        assertEval("cbind(character(0), 'f')");
        assertEval("cbind(55, character(0))");
        assertEval("cbind(a=55, character(0))");

        assertEval("v <- 1; attr(v, 'a') <- 'a'; cbind(v); cbind(v, v)");
        assertEval("v <- 1; attr(v, 'a') <- 'a'; attr(v, 'a1') <- 'a1'; cbind(v); cbind(v, v)");
        assertEval("v <- 1:3; attr(v, 'a') <- 'a'; attr(v, 'a1') <- 'a1'; cbind(v); cbind(v, v)");
        assertEval("v <- 1:3; v1<-1:3; attr(v, 'a') <- 'a'; attr(v1, 'a1') <- 'a1'; cbind(v, v1)");
    }

    @Test
    public void testGenericDispatch() {
        assertEval("{ v <- 1; class(v) <- 'foo'; cbind.foo <- function(...) 'foo'; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; cbind.foo <- function(...) 'foo'; v2 <- 1; class(v2) <- 'foo'; cbind(v2) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; assign('cbind.foo', function(x) {'foo'}, envir=.__S3MethodsTable__.); result <- cbind(v) ; rm('cbind.foo', envir=.__S3MethodsTable__.); result;}");

        // segfault in gnur
        assertEval(Ignored.ReferenceError, "{ v <- 1; class(v) <- 'foo'; cbind.foo <- length; cbind(v) }");

        assertEval(Output.IgnoreErrorContext, "{ v <- 1; class(v) <- 'foo'; cbind.foo <- rawToBits; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; cbind(v) }");
        assertEval("{ v <- 1; cbind.foo <- function(...) 'foo'; cbind(v) }");

        assertEval("{ v <- 1; class(v) <- 'foo'; cbind.foo <- function(deparse.level, ...) 'foo'; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; cbind.foo <- function(deparse.level, x) 'foo'; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; cbind.foo <- function(deparse.level, x1, x2) 'foo'; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; cbind.foo <- function(x0, deparse.level, x1, x2) 'foo'; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; cbind.foo <- function(x0, x1, x2) 'foo'; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; cbind.foo <- function(x) 'foo'; cbind(v) }");
        assertEval(Ignored.WrongCaller, "{ v <- 1; class(v) <- 'foo'; cbind.foo <- function() 'foo'; cbind(v) }");

        assertEval("{ v <- 1; class(v) <- c('foo1', 'foo2'); cbind.foo1 <- function(...) 'foo1'; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- c('foo1', 'foo2'); cbind.foo2 <- function(...) 'foo2'; cbind(v) }");
        assertEval("{ v <- 1; class(v) <- c('foo1', 'foo2'); cbind.foo1 <- function(...) 'foo1'; cbind.foo2 <- function(...) 'foo2'; cbind(v) }");

        assertEval("{ v1 <- 1; class(v1) <- 'foo1'; cbind.foo1 <- function(...) 'foo1'; v2 <- 2; class(v2) <- 'foo2'; cbind.foo2 <- function(...) 'foo2'; cbind(v1, v2) }");
        assertEval("{ v1 <- 1; class(v1) <- 'foo1'; cbind.foo1 <- function(...) 'foo1'; v2 <- 2; class(v2) <- 'foo2'; cbind(v1, v2) }");
        assertEval("{ v1 <- 1; class(v1) <- 'foo1'; v2 <- 2; class(v2) <- 'foo2'; cbind.foo2 <- function(...) 'foo2'; cbind(v1, v2) }");

        // S4
        assertEval("{ setClass('fooo', slots = c(a='numeric')); obj <- new('fooo'); cbind(obj) }");
    }

    @Test
    public void testDimnames() {
        assertEval("{ attributes(cbind(integer(0))) }");
        assertEval("{ attributes(cbind(list())) }");
        assertEval("{ attributes(cbind(matrix())) }");
        assertEval("{ attributes(cbind(1L)) }");
        assertEval("{ attributes(cbind(c(1L, 2L))) }");
        assertEval("{ attributes(cbind(list(1L, 2L))) }");
        assertEval("{ attributes(cbind(matrix(1L, 2L))) }");
        assertEval("{ attributes(cbind(1L, 2L)) }");

        assertEval("{ cbind(structure(1:4, dim=c(2,2), dimnames=list(c('y1', 'y2'), c('x1', 'x2'))), 1L) }");
        assertEval("{ cbind(structure(1:4, dim=c(2,2), dimnames=list(y=c('y1', 'y2'), x=c('x1', 'x2'))), 1L) }");
        assertEval("{ cbind(structure(1:4, dim=c(2,2)), 1L) }");

        assertEval("{ attributes(cbind(structure(1:4, dim=c(2,2), dimnames=list(c('y1', 'y2'), c('x1', 'x2'))), 1L)) }");
        assertEval("{ attributes(cbind(structure(1:4, dim=c(2,2), dimnames=list(y=c('y1', 'y2'), x=c('x1', 'x2'))), 1L)) }");
        assertEval("{ attributes(cbind(structure(1:4, dim=c(2,2)), 1L)) }");

        assertEval("{ cbind(NULL, integer(0)) }");
        assertEval("{ cbind(integer(0), integer(0)) }");
        assertEval("{ cbind(c(1), integer(0)) }");
        assertEval("{ cbind(structure(1:4, dim=c(2,2), dimnames=list(y=c('y1', 'y2'), x=c('x1', 'x2'))), integer(0)) }");

        assertEval("{ attributes(cbind(NULL, integer(0))) }");
        assertEval("{ attributes(cbind(integer(0), integer(0))) }");
        assertEval("{ attributes(cbind(c(1), integer(0))) }");
        assertEval("{ attributes(cbind(structure(1:4, dim=c(2,2), dimnames=list(y=c('y1', 'y2'), x=c('x1', 'x2'))), integer(0))) }");
    }

    @Test
    public void testRetType() {
        assertEval("dput(cbind(NULL))");
        assertEval("dput(cbind(NULL, integer(0)))");
        assertEval("dput(cbind(NULL, NULL, integer(0)))");
        assertEval("dput(cbind(NULL, NULL, double(0)))");
        assertEval("dput(cbind(NULL, NULL, integer(0), double(0)))");
        assertEval("dput(cbind(NULL, NULL, double(0), integer(0)))");
        assertEval("dput(cbind(NULL, NULL, double(0), character(0)))");
        assertEval("dput(cbind(NULL, NULL, double(0), integer(0), character(0)))");
        assertEval("dput(cbind(c(NULL, NULL), integer(0)))");
        assertEval("dput(cbind(integer(0)))");
        assertEval("dput(cbind(integer(0), NULL, NULL))");
        // FIXME FastR wrongly adds dimnames to the result
        assertEval(Ignored.ImplementationError, "dput(cbind(substitute(graphics::par), list(as.symbol('a'))))");
        assertEval("typeof(cbind(substitute(graphics::par), list(as.symbol('a'))))");
    }
}
