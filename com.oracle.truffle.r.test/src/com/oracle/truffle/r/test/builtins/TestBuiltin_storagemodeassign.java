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
public class TestBuiltin_storagemodeassign extends TestBase {

    @Test
    public void teststoragemodeassign1() {
        assertEval("argv <- list(structure(c(4L, 5L, 10L, 9L, 13L, 13L, 12L, 15L, 18L, 19L, 22L, 27L, 28L, 24L, 27L, 28L, 30L, 31L, 32L, 36L, 28L, 32L, 35L, 33L, 38L, 41L, 38L, 38L, 32L, 34L, 44L, 44L, 44L, 46L, 47L, 49L, 50L, 53L, 52L, 55L, 54L, 60L, 63L, 86L, 85L, 85L, 78L, 74L, 97L, 98L, 98L, 99L, 99L, 101L, 108L, 110L, 108L, 111L, 115L, 117L, 70L, 77L, 83L, 61L, 69L, 78L, 66L, 58L, 64L, 69L, 66L, 61L, 76L, 72L, 64L, 53L, 63L, 59L, 77L, 49L, 69L, 88L, 75L, 61L, 65L, 74L, 72L, 76L, 58L, 55L, 60L, 52L, 60L, 61L, 72L, 147L, 149L, 153L, 154L, 151L, 150L, 145L, 143L, 143L, 141L, 156L, 149L, 143L, 142L, 149L, 152L, 142L, 144L, 152L, 155L, 124L, 136L, 139L, 132L, 115L, 96L, 94L, 96L, 122L, 116L, 124L, 119L, 128L, 115L, 111L, 111L, 116L, 126L, 117L, 115L, 4L, 12L, 21L, 15L, 15L, 16L, 18L, 13L, 20L, 21L, 23L, 25L, 27L, 31L, 30L), .Dim = c(75L, 2L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75'), c('x', 'y'))), value = 'double');`storage.mode<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void teststoragemodeassign2() {
        assertEval("argv <- list(3.14159265358979, value = 'complex');`storage.mode<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void teststoragemodeassign3() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE), .Dim = c(4L, 4L)), value = 'integer');`storage.mode<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void teststoragemodeassign4() {
        assertEval("argv <- list(structure(c(2, 0, 1, 2), .Dim = c(2L, 2L), .Dimnames = list(c('A', 'B'), c('A', 'B'))), value = 'logical');`storage.mode<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void teststoragemodeassign5() {
        assertEval("argv <- list(structure(c(2.5, 0, 0.75, 0, 2.5, -2.5, 0.75, -2.5, 2.8), .Dim = c(3L, 3L)), value = 'double');`storage.mode<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testUpdateStorageMode() {
        assertEval("{ x <- c(1L, 2L); storage.mode(x) <- \"double\"}");
        assertEval("{ x <- c(1L, 2L); storage.mode(x) <- \"not.double\"}");
        assertEval("{ x <- c(1L, 2L); dim(x)<-c(1,2); storage.mode(x) <- \"double\"; x}");
    }
}
