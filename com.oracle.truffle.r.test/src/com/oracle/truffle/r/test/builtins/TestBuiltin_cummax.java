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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_cummax extends TestBase {

    @Test
    public void testcummax1() {
        assertEval("argv <- list(c(3L, 2L, 1L, 2L, 1L, 0L, 4L, 3L, 2L));cummax(argv[[1]]);");
    }

    @Test
    public void testcummax2() {
        assertEval("argv <- list(c(1.4124321047876e-05, 0.00123993824202733, 0.00149456828694326, 0.00559442649445619, 0.00589461369451042, 0.00682814400910408, 0.00716033530387356, 0.00831306755655091, 0.0117236981036592, 0.0193564395772821, 0.0305747157670471, 0.0790837327244679, 0.158516621910594, 0.166302063477173, 0.240901842706431, 0.30743590191449, 0.310605928993035, 0.378620529843491, 0.394843673266257, 0.463217214123843, 0.846006725553553, 1.91986719718639, 2.30025314520167, 2.31702860292334, 2.66225504155806, 2.89838614884136, 2.93533263484596, 3.92915929103845, 6.05054801269533, 6.38133071205875, 6.62764115953293, 8.28240123423701, 8.53690564463391, 12.5838414070157, 12.5601043160765, 12.3043865122123, 12.7666868655065, 13.228566067383, 12.7230281716064, 12.9903781159995, 12.727240095027, 12.2523157614464, 11.8051459071199, 11.7060028009859, 11.5037817968679, 12.2693077958414, 11.5842811936712, 11.6626896867753, 10.9424154292091, 10.3816792396216));cummax(argv[[1]]);");
    }

    @Test
    public void testcummax3() {
        assertEval(Ignored.Unknown, "argv <- list(list());cummax(argv[[1]]);");
    }

    @Test
    public void testcummax4() {
        assertEval("argv <- list(FALSE);cummax(argv[[1]]);");
    }

    @Test
    public void testcummax5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')));cummax(argv[[1]]);");
    }

    @Test
    public void testcummax6() {
        assertEval(Ignored.Unknown, "argv <- list(NULL);cummax(argv[[1]]);");
    }

    @Test
    public void testcummax7() {
        assertEval(Ignored.Unknown, "argv <- list(character(0));cummax(argv[[1]]);");
    }

    @Test
    public void testcummax8() {
        assertEval(Ignored.Unknown, "argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));cummax(argv[[1]]);");
    }

    @Test
    public void testCumulativeMax() {
        assertEval("{ cummax(c(1,2,3)) }");
        assertEval("{ cummax(NA) }");
        assertEval("{ cummax(c(2000000000L, NA, 2000000000L)) }");
        assertEval("{ cummax(1:10) }");
        assertEval("{ cummax(c(TRUE,FALSE,TRUE)) }");
        assertEval("{ cummax(c(TRUE,FALSE,NA,TRUE)) }");
        assertEval("{ cummax(as.logical(-2:2)) }");

        assertEval(Ignored.Unknown, "{ cummax(c(1+1i,2-3i,4+5i)) }");
        assertEval(Ignored.Unknown, "{ cummax(c(1+1i, NA, 2+3i)) }");
    }
}
