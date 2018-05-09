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
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
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

        assertEval("{ .Internal(tabulate(c(2,3,5), 7)) }");
        assertEval("{ .Internal(tabulate(c(2L,3L,5L), c(7, 42))) }");
        assertEval("{ .Internal(tabulate(c(2L,3L,5L), integer())) }");
        assertEval("{ .Internal(tabulate(c(2L,3L,5L), -1)) }");
        assertEval("{ .Internal(tabulate(c(2L,3L,5L), NA)) }");
    }
}
