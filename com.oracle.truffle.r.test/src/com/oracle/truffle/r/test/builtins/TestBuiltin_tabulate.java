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
public class TestBuiltin_tabulate extends TestBase {

    @Test
    public void testtabulate1() {
        assertEval("argv <- list(1L, 1L); .Internal(tabulate(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testtabulate2() {
        assertEval("argv <- list(1:6, 6L); .Internal(tabulate(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testtabulate3() {
        assertEval("argv <- list(integer(0), 1L); .Internal(tabulate(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testtabulate4() {
        assertEval("argv <- list(c(1L, 9L, 13L, 25L, 11L, 24L, 3L, 20L, 20L, 15L, 20L, 14L, 24L, 19L, 12L, 8L, 1L, 11L, 4L, 3L, 21L, 25L, 10L, 3L, 12L), 25L); .Internal(tabulate(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testtabulate5() {
        assertEval("argv <- list(structure(1:49, .Label = c('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48'), class = 'factor'), 49L); .Internal(tabulate(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testtabulate6() {
        assertEval("argv <- list(integer(0), 0L); .Internal(tabulate(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testtabulate8() {
        assertEval("argv <- structure(list(bin = numeric(0)), .Names = 'bin');do.call('tabulate', argv)");
    }

    @Test
    public void testTabulate() {
        assertEval("{tabulate(c(2,3,5))}");
        assertEval("{tabulate(c(2,3,3,5), nbins = 10)}");
        assertEval("{tabulate(c(-2,0,2,3,3,5))}");
        assertEval("{tabulate(c(-2,0,2,3,3,5), nbins = 3)}");
        assertEval("{tabulate(factor(letters[1:10]))}");
    }
}
