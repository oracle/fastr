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

public class TestBuiltin_extract_parentasis_factor extends TestBase {

    @Test
    public void testextract_parentasis_factor1() {
        assertEval("argv <- structure(list(x = structure(c(111L, 88L, 93L, 74L, 138L,     103L, 46L, 114L, 112L, 24L, 99L, 97L, 57L, 40L, 86L, 37L,     124L, 9L, 20L, 54L, 145L, 3L, 7L, 134L, 98L, 143L, 131L,     47L, 128L, 116L, 137L, 5L, 132L, 21L, 81L, 58L, 108L, 17L,     107L, 126L, 2L, 18L, 75L, 4L, 63L, 121L, 84L, 101L, 123L,     102L, 36L, 48L, 12L, 105L, 100L, 90L, 34L, 55L, 68L, 10L,     52L, 91L, 146L, 127L, 1L, 29L, 106L, 26L, 115L, 118L, 25L,     82L, 16L, 45L, 95L, 69L, 72L, 15L, 120L, 104L, 125L, 6L,     140L, 65L, 62L, 39L, 35L, 38L, 83L, 117L, 42L, 13L, 87L,     22L, 53L, 41L, 113L, 73L, 133L, 23L, 80L, 8L, 19L, 78L, 60L,     31L, 33L, 147L, 139L, 56L, 130L, 64L, 71L, 43L, 136L, 89L,     94L, 96L, 70L, 59L, 129L, 27L, 92L, 51L, 77L, 50L, 66L, 119L,     135L, 110L, 144L, 109L, 67L, 44L, 32L, 141L, 76L, 79L, 49L,     142L, 30L, 14L, 85L, 28L, 11L, 61L, 122L), .Label = c('1415787_at',     '1415904_at', '1415993_at', '1416164_at', '1416181_at', '1416221_at',     '1416481_s_at', '1416812_at', '1416855_at', '1416949_s_at',     '1417129_a_at', '1417425_at', '1417447_at', '1417466_at',     '1417572_at', '1417624_at', '1417667_a_at', '1417964_at',     '1418084_at', '1418382_at', '1418424_at', '1418471_at', '1418479_at',     '1418486_at', '1418516_at', '1418560_at', '1418649_at', '1418835_at',     '1419361_at', '1419430_at', '1419686_at', '1419833_s_at',     '1420011_s_at', '1420643_at', '1420886_a_at', '1421045_at',     '1421180_at', '1421773_at', '1422018_at', '1422557_s_at',     '1422671_s_at', '1422809_at', '1422850_at', '1422979_at',     '1423095_s_at', '1423110_at', '1423123_at', '1423124_x_at',     '1423176_at', '1423319_at', '1423852_at', '1423924_s_at',     '1424107_at', '1424186_at', '1424212_at', '1424243_at', '1424474_a_at',     '1424749_at', '1425494_s_at', '1425534_at', '1425779_a_at',     '1426083_a_at', '1426295_at', '1426371_at', '1426485_at',     '1426510_at', '1426628_at', '1426845_at', '1427120_at', '1427208_at',     '1427256_at', '1427314_at', '1427672_a_at', '1428922_at',     '1428942_at', '1429177_x_at', '1429514_at', '1429859_a_at',     '1431830_at', '1433512_at', '1434326_x_at', '1434485_a_at',     '1434831_a_at', '1434920_a_at', '1435129_at', '1435327_at',     '1435357_at', '1436392_s_at', '1436528_at', '1436886_x_at',     '1437163_x_at', '1437223_s_at', '1437434_a_at', '1437455_a_at',     '1438312_s_at', '1438651_a_at', '1439148_a_at', '1439373_x_at',     '1439381_x_at', '1439962_at', '1448131_at', '1448143_at',     '1448147_at', '1448259_at', '1448269_a_at', '1448466_at',     '1448601_s_at', '1448630_a_at', '1448823_at', '1448943_at',     '1448995_at', '1449059_a_at', '1449376_at', '1449623_at',     '1449630_s_at', '1449697_s_at', '1449699_s_at', '1449755_at',     '1449773_s_at', '1449885_at', '1450070_s_at', '1450723_at',     '1450846_at', '1450857_a_at', '1450941_at', '1451103_at',     '1451266_at', '1451317_at', '1451332_at', '1451415_at', '1451418_a_at',     '1451532_s_at', '1451536_at', '1452003_at', '1452110_at',     '1452183_a_at', '1452665_at', '1452671_s_at', '1452869_at',     '1453030_at', '1455056_at', '1455517_at', '1456174_x_at',     '1456393_at', '1456434_x_at', '1460260_s_at', '1460359_at'),     class = 'factor'), 1:25), .Names = c('x', ''));" +
                        "do.call('[.factor', argv)");
    }
}
