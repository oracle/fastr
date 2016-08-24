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
public class TestBuiltin_cummin extends TestBase {

    @Test
    public void testcummin1() {
        assertEval("argv <- list(c(3L, 2L, 1L, 2L, 1L, 0L, 4L, 3L, 2L));cummin(argv[[1]]);");
    }

    @Test
    public void testcummin2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')));cummin(argv[[1]]);");
    }

    @Test
    public void testcummin3() {
        assertEval("argv <- list(c(0.943789021783783, 0.931269398230562, 0.936135627032134, 0.76691878645786, 0.751187345517812, 0.732102071759373, 0.736981399184748, 0.745009176294265, 0.742307320914255, 0.711777799810146, 0.726511637567943, 0.690091181919273, 0.656233947317988, 0.662510996891949, 0.657978635660952, 0.44347561790306, 0.428400063839846, 0.342071801782345, 0.329359004493355, 0.312959379967, 0.204112170963036, 0.153481444959266, 0.152881906752072, 0.141986935549763, 0.125244789347208, 0.126329692184989, 0.107405157884553, 0.0483432414602031, 0.0271151539974933, 0.0237499953844365, 0.0234803429360305, 0.0199319312722803, 0.0204957267942993, 0.0167583890578386, 0.0121314575180917, 0.0121935863008149, 0.00645581491628309, 0.00266833883057866, 0.00182178254845008, 0.00120243057473427, 0.000941101987534066, 0.000909248927476008, 0.000993184583142412, 0.00101050520477321, 0.00117777399883288, 0.000412294699846418, 0.000504381657773829, 1.12994568383008e-05));cummin(argv[[1]]);");
    }

    @Test
    public void testcummin4() {
        assertEval(Ignored.Unknown, "argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));cummin(argv[[1]]);");
    }

    @Test
    public void testcummin5() {
        assertEval(Ignored.Unknown, "argv <- list(logical(0));cummin(argv[[1]]);");
    }

    @Test
    public void testcummin6() {
        assertEval(Ignored.Unknown, "argv <- list(character(0));cummin(argv[[1]]);");
    }

    @Test
    public void testcummin7() {
        assertEval("argv <- list(NULL);cummin(argv[[1]]);");
    }

    @Test
    public void testcummin8() {
        assertEval("argv <- list(FALSE);cummin(argv[[1]]);");
    }

    @Test
    public void testCumulativeMin() {
        assertEval("{ cummin(c(1,2,3)) }");
        assertEval("{ cummin(NA) }");
        assertEval("{ cummin(1:10) }");
        assertEval("{ cummin(c(2000000000L, NA, 2000000000L)) }");
        assertEval("{ cummin(c(TRUE,FALSE,TRUE)) }");
        assertEval("{ cummin(c(TRUE,FALSE,NA,TRUE)) }");
        assertEval("{ cummin(as.logical(-2:2)) }");

        // Error message mismatch.
        assertEval(Ignored.Unknown, "{ cummin(c(1+1i,2-3i,4+5i)) }");
        assertEval(Ignored.Unknown, "{ cummin(c(1+1i, NA, 2+3i)) }");
    }
}
