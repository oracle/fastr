/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_rcond extends TestBase {

    @Test
    public void testrcond1() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(x = structure(c(FALSE, TRUE, FALSE, TRUE,     TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE,     TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE,     TRUE, TRUE, TRUE), .Dim = c(5L, 5L))), .Names = 'x');" +
                                        "do.call('rcond', argv)");
    }

    @Test
    public void testrcond2() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(x = structure(c(0.483017750550061 + (0+0i),     0.399143285583705 + (0+0i), 0.0162145779468119 + (0+0i),     0.125083255348727 + (0+0i), 0.0706489166477695 + (0+0i),     0.504917626501992 + (0+0i), 0.327679358422756 + (0+0i), 0.411779605317861 +         (0+0i), 0.202413034392521 + (0+0i), 0.307096319855191 +         (0+0i), 0.642031987197697 + (0+0i), 0.276873307069764 +         (0+0i), 0.103556007146835 + (0+0i), 0.256002754438668 +         (0+0i), 0.179779380792752 + (0+0i), 0.247455857461318 +         (0+0i), 0.215011228807271 + (0+0i), 0.493673762306571 +         (0+0i), 0.653446026844904 + (0+0i), 0.573559894575737 +         (0+0i), 0.863887825980783 + (0+0i), 0.637789903208613 +         (0+0i), 0.0137805955018848 + (0+0i), 0.529164811130613 +         (0+0i), 0.271472703316249 + (0+0i)), .Dim = c(5L, 5L))),     .Names = 'x');" +
                                        "do.call('rcond', argv)");
    }

}
