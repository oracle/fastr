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
public class TestBuiltin_Date2POSIXlt extends TestBase {

    @Test
    public void testDate2POSIXlt1() {
        assertEval("argv <- list(structure(c(14712, 14712), class = 'Date')); .Internal(Date2POSIXlt(argv[[1]]))");
    }

    @Test
    public void testDate2POSIXlt2() {
        assertEval("argv <- list(structure(c(11323, NA, NA, 12717), class = 'Date')); .Internal(Date2POSIXlt(argv[[1]]))");
    }

    @Test
    public void testDate2POSIXlt3() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L), class = 'Date')); .Internal(Date2POSIXlt(argv[[1]]))");
    }

    @Test
    public void testDate2POSIXlt4() {
        assertEval("argv <- list(structure(c(FALSE, FALSE), class = 'Date')); .Internal(Date2POSIXlt(argv[[1]]))");
    }

    @Test
    public void testDate2POSIXlt5() {
        assertEval("argv <- list(NULL); .Internal(Date2POSIXlt(argv[[1]]))");
    }

    @Test
    public void testDate2POSIXlt6() {
        assertEval("argv <- list(character(0)); .Internal(Date2POSIXlt(argv[[1]]))");
    }

    @Test
    public void testDate2POSIXlt7() {
        assertEval("argv <- list(structure(11323.9154302836, class = 'Date')); .Internal(Date2POSIXlt(argv[[1]]))");
    }

    @Test
    public void testDate2POSIXlt8() {
        assertEval("argv <- list(structure(c(-21915, -21550, -21185, -20819, -20454, -20089, -19724, -19358, -18993, -18628, -18263, -17897, -17532, -17167, -16802, -16436, -16071, -15706, -15341, -14975, -14610, -14245, -13880, -13514, -13149, -12784, -12419, -12053, -11688, -11323, -10958, -10592, -10227, -9862, -9497, -9131, -8766, -8401, -8036, -7670, -7305, -6940, -6575, -6209, -5844, -5479, -5114, -4748, -4383, -4018, -3653, -3287, -2922, -2557, -2192, -1826, -1461, -1096, -731, -365, 0, 365, 730, 1096, 1461, 1826, 2191, 2557, 2922, 3287, 3652, 4018, 4383, 4748, 5113, 5479, 5844, 6209, 6574, 6940, 7305, 7670, 8035, 8401, 8766, 9131, 9496, 9862, 10227, 10592), class = 'Date')); .Internal(Date2POSIXlt(argv[[1]]))");
    }

    @Test
    public void testDate2POSIXlt9() {
        assertEval("argv <- list(structure(c(11354, 11382, 11413), class = 'Date')); .Internal(Date2POSIXlt(argv[[1]]))");
    }
}
