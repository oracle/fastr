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
public class TestBuiltin_levelsassign extends TestBase {

    @Test
    public void testlevelsassign1() {
        assertEval("argv <- list(structure(1:2, .Label = c('a', 'b'), class = 'factor'), value = structure(list(C = 'C', A = 'a', B = 'b'), .Names = c('C', 'A', 'B')));`levels<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlevelsassign2() {
        assertEval("argv <- list(structure(c(24L, 13L, 15L, 68L, 39L, 74L, 22L, 1L, 8L, 55L, 24L, 20L, 51L, 13L, 3L, 4L, 5L, 6L, 15L, 2L, 8L, 60L, 67L, 23L, 58L, 24L, 22L, 21L, 37L, 74L, 59L, 39L, 14L, 14L, 19L, 23L, 70L, 21L, 22L, 31L, 29L, 30L, 45L, 58L, 17L, 7L, 19L, 26L, 39L, 74L, 57L, 59L, 12L, 72L, 70L, 37L, 64L, 16L, 18L, 21L, 22L, 8L, 62L, 61L, 63L, 71L, 105L, 64L, 10L, 41L, 8L, 27L, 11L, 34L, 32L, 33L, 68L, 107L, NA, 66L, NA, 65L, 48L, 52L, 43L, 47L, 46L, 44L, 41L, 54L, 28L, 50L, 40L, NA, 69L, NA, 75L, 109L, NA, 86L, 112L, 110L, 104L, 24L, 111L, 87L, NA, NA, 92L, 73L, 85L, 90L, 89L, NA, 83L, NA, 102L, NA, 108L, 88L, 91L, 93L, NA, 94L, 84L, NA, 106L, NA, 95L, 82L, 56L, 87L, 109L, 75L, 104L, 110L, 112L, 111L, 24L, 73L, 85L, 86L, 90L, 89L, 102L, 88L, 92L, 9L, 49L, 42L, 38L, 35L, 36L, 25L, NA, NA, 9L, 49L, 42L, NA, 36L, 38L, 25L, 53L, 79L, 78L, 103L, 77L, 80L, 114L, 97L, 113L, 76L, 96L, 81L, 116L, 99L, 117L, 115L, 98L, 101L, 100L), .Label = c('1008', '1011', '1013', '1014', '1015', '1016', '1027', '1028', '1030', '1032', '1051', '1052', '1083', '1093', '1095', '1096', '110', '1102', '111', '1117', '112', '113', '116', '117', '1219', '125', '1250', '1251', '126', '127', '128', '1291', '1292', '1293', '1298', '1299', '130', '1308', '135', '1376', '1377', '1383', '1408', '1409', '141', '1410', '1411', '1413', '1418', '1422', '1438', '1445', '1456', '1492', '2001', '2316', '262', '266', '269', '270', '2708', '2714', '2715', '272', '2728', '2734', '280', '283', '286', '290', '3501', '411', '412', '475', '5028', '5042', '5043', '5044', '5045', '5047', '5049', '5050', '5051', '5052', '5053', '5054', '5055', '5056', '5057', '5058', '5059', '5060', '5061', '5062', '5066', '5067', '5068', '5069', '5070', '5072', '5073', '5115', '5160', '5165', '655', '724', '885', '931', '942', '952', '955', '958', 'c118', 'c168', 'c203', 'c204', 'c266'), class = 'factor'), value = c('1008', '1011', '1013', '1014', '1015', '1016', '1027', '1028', '1030', '1032', '1051', '1052', '1083', '1093', '1095', '1096', '110', '1102', '111', '1117', '112', '113', '116', 'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz', '1219', '125', '1250', '1251', '126', '127', '128', '1291', '1292', '1293', '1298', '1299', '130', '1308', '135', '1376', '1377', '1383', '1408', '1409', '141', '1410', '1411', '1413', '1418', '1422', '1438', '1445', '1456', '1492', '2001', '2316', '262', '266', '269', '270', '2708', '2714', '2715', '272', '2728', '2734', '280', '283', '286', '290', '3501', '411', '412', '475', '5028', '5042', '5043', '5044', '5045', '5047', '5049', '5050', '5051', '5052', '5053', '5054', '5055', '5056', '5057', '5058', '5059', '5060', '5061', '5062', '5066', '5067', '5068', '5069', '5070', '5072', '5073', '5115', '5160', '5165', '655', '724', '885', '931', '942', '952', '955', '958', 'c118', 'c168', 'c203', 'c204', 'c266'));`levels<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlevelsassign4() {
        assertEval("argv <- list(structure(c(1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L), .Label = c('1', '2', '3'), class = 'factor'), value = structure(list(A = c(1, 3), B = 2), .Names = c('A', 'B')));`levels<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlevelsassign5() {
        assertEval(Ignored.Unknown, "argv <- list(structure(FALSE, .Label = FALSE), FALSE);`levels<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlevelsassign6() {
        assertEval(Ignored.Unknown, "argv <- list(NULL, NULL);`levels<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testlevelsassign7() {
        assertEval(Ignored.Unknown, "argv <- list(structure(list(), .Label = list()), list());`levels<-`(argv[[1]],argv[[2]]);");
    }
}
