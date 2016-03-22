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
public class TestBuiltin_asPOSIXlt extends TestBase {

    @Test
    public void testasPOSIXlt1() {
        assertEval("argv <- list(structure(c(2147483648.4, 2147483648.8), class = c('POSIXct', 'POSIXt'), tzone = ''), ''); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt2() {
        assertEval("argv <- list(structure(c(FALSE, FALSE), class = c('POSIXct', 'POSIXt')), ''); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt3() {
        assertEval("argv <- list(structure(1041324768, class = c('POSIXct', 'POSIXt'), tzone = 'GMT'), 'GMT'); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt4() {
        assertEval("argv <- list(structure(c(1208865600, 1208952000, 1209038400, 1209124800, 1209211200), tzone = 'GMT', class = c('POSIXct', 'POSIXt')), 'GMT'); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt5() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)), ''); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt6() {
        assertEval("argv <- list(structure(32569542120, class = c('POSIXct', 'POSIXt')), ''); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt7() {
        assertEval("argv <- list(structure(c(1012798800, 1013403600, 1014008400, 1014613200), class = c('POSIXct', 'POSIXt'), tzone = ''), ''); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt8() {
        assertEval("argv <- list(structure(c(-1893412800, -1861876800, -1830340800, -1798718400, -1767182400, -1735646400, -1704110400, -1672488000, -1640952000, -1609416000, -1577880000, -1546257600, -1514721600, -1483185600, -1451649600, -1420027200, -1388491200, -1356955200, -1325419200, -1293796800, -1262260800, -1230724800, -1199188800, -1167566400, -1136030400, -1104494400, -1072958400, -1041336000, -1009800000, -978264000, -946728000, -915105600, -883569600, -852033600, -820497600, -788875200, -757339200, -725803200, -694267200, -662644800, -631108800, -599572800, -568036800, -536414400, -504878400, -473342400, -441806400, -410184000, -378648000, -347112000, -315576000, -283953600, -252417600, -220881600, -189345600, -157723200, -126187200, -94651200, -63115200, -31492800, 43200, 31579200, 63115200, 94737600, 126273600, 157809600, 189345600, 220968000, 252504000, 284040000, 315576000, 347198400, 378734400, 410270400, 441806400, 473428800, 504964800, 536500800, 568036800, 599659200, 631195200, 662731200, 694267200, 725889600, 757425600, 788961600, 820497600, 852120000, 883656000, 915192000), class = c('POSIXct', 'POSIXt'), tzone = 'GMT'), 'GMT'); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt9() {
        assertEval("argv <- list(list(), ''); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt10() {
        assertEval("argv <- list(character(0), ''); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testasPOSIXlt11() {
        assertEval("argv <- list(NULL, ''); .Internal(as.POSIXlt(argv[[1]], argv[[2]]))");
    }
}
