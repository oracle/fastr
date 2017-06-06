/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_radixsort extends TestBase {

    @Test
    public void testradixsort1() {
        // FIXME ArrayIndexOutOfBoundsException: 3
        // at InternalNode$InternalCallWrapNode.prepareArgs(InternalNode.java:309)
        assertEval(Ignored.ImplementationError, "argv <- list(structure(1L, .Label = c('Ctl', 'Trt'), class = 'factor'), TRUE, FALSE); .Internal(radixsort(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testradixsort2() {
        // FIXME ArrayIndexOutOfBoundsException: 3
        // at InternalNode$InternalCallWrapNode.prepareArgs(InternalNode.java:309)
        assertEval(Ignored.ImplementationError,
                        "argv <- list(structure(c(2L, 1L, 3L), .Label = c('1', '2', NA), class = 'factor'), TRUE, FALSE); .Internal(radixsort(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testradixsort3() {
        // FIXME ArrayIndexOutOfBoundsException: 3
        // at InternalNode$InternalCallWrapNode.prepareArgs(InternalNode.java:309)
        assertEval(Ignored.ImplementationError,
                        "argv <- list(structure(c(3L, 7L, 1L, 5L, 10L, 8L, 2L, 6L, 4L, 9L), .Label = c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), class = 'factor'), TRUE, FALSE); .Internal(radixsort(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testradixsort5() {
        // FIXME ArrayIndexOutOfBoundsException: 3
        // at InternalNode$InternalCallWrapNode.prepareArgs(InternalNode.java:309)
        assertEval(Ignored.ImplementationError,
                        "argv <- list(structure(c(64L, 67L, 92L, 71L, 74L, 69L, 78L, 94L, 95L, 80L, 119L, 114L, 97L, 72L, 75L, 96L, 99L, 54L, 101L, 93L, 58L, 138L, NA, 139L, 126L, 84L, 79L, 62L, 52L, 68L, 81L, 87L, 109L, 35L, 51L, 61L, 59L, 131L, 137L, 73L, 77L, 70L, 90L, 107L, 29L, 89L, 57L, 66L, 76L, 37L, 155L, 16L, 102L, 56L, 123L, 36L, 60L, 40L, 43L, 85L, 65L, 11L, 47L, 103L, 86L, 13L, 63L, 132L, 91L, 98L, 105L, 134L, 14L, 124L, 117L, 55L, 41L, 24L, 50L, 110L, 129L, 88L, 141L, 145L, 133L, 25L, 46L, 120L, 83L, 121L, 104L, 53L, 44L, 113L, 100L, 19L, 108L, 31L, 82L, 127L, 111L, 116L, 38L, 150L, 48L, 22L, 112L, 140L, 27L, 149L, 115L, 130L, 8L, 28L, 106L, 23L, 125L, 33L, 39L, 32L, 15L, 49L, 144L, 7L, 17L, 122L, 118L, 5L, 128L, 12L, 45L, 21L, 42L, 147L, 26L, 1L, 4L, 153L, 151L, 3L, 136L), .Label = c('(360,365]', '(365,370]', '(370,375]', '(375,380]', '(380,385]', '(385,390]', '(390,395]', '(395,400]', '(400,405]', '(405,410]', '(410,415]', '(415,420]', '(420,425]', '(425,430]', '(430,435]', '(435,440]', '(440,445]', '(445,450]', '(450,455]', '(455,460]', '(460,465]', '(465,470]', '(470,475]', '(475,480]', '(480,485]', '(485,490]', '(490,495]', '(495,500]', '(500,505]', '(505,510]', '(510,515]', '(515,520]', '(520,525]', '(525,530]', '(530,535]', '(535,540]', '(540,545]', '(545,550]', '(550,555]', '(555,560]', '(560,565]', '(565,570]', '(570,575]', '(575,580]', '(580,585]', '(585,590]', '(590,595]', '(595,600]', '(600,605]', '(605,610]', '(610,615]', '(615,620]', '(620,625]', '(625,630]', '(630,635]', '(635,640]', '(640,645]', '(645,650]', '(650,655]', '(655,660]', '(660,665]', '(665,670]', '(670,675]', '(675,680]', '(680,685]', '(685,690]', '(690,695]', '(695,700]', '(700,705]', '(705,710]', '(710,715]', '(715,720]', '(720,725]', '(725,730]', '(730,735]', '(735,740]', '(740,745]', '(745,750]', '(750,755]', '(755,760]', '(760,765]', '(765,770]', '(770,775]', '(775,780]', '(780,785]', '(785,790]', '(790,795]', '(795,800]', '(800,805]', '(805,810]', '(810,815]', '(815,820]', '(820,825]', '(825,830]', '(830,835]', '(835,840]', '(840,845]', '(845,850]', '(850,855]', '(855,860]', '(860,865]', '(865,870]', '(870,875]', '(875,880]', '(880,885]', '(885,890]', '(890,895]', '(895,900]', '(900,905]', '(905,910]', '(910,915]', '(915,920]', '(920,925]', '(925,930]', '(930,935]', '(935,940]', '(940,945]', '(945,950]', '(950,955]', '(955,960]', '(960,965]', '(965,970]', '(970,975]', '(975,980]', '(980,985]', '(985,990]', '(990,995]', '(995,1000]', '(1000,1005]', '(1005,1010]', '(1010,1015]', '(1015,1020]', '(1020,1025]', '(1025,1030]', '(1030,1035]', '(1035,1040]', '(1040,1045]', '(1045,1050]', '(1050,1055]', '(1055,1060]', '(1060,1065]', '(1065,1070]', '(1070,1075]', '(1075,1080]', '(1080,1085]', '(1085,1090]', '(1090,1095]', '(1095,1100]', '(1100,1105]', '(1105,1110]', '(1110,1115]', '(1115,1120]', '(1120,1125]', '(1125,1130]', '(1130,1135]'), class = 'factor'), TRUE, FALSE); .Internal(radixsort(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testradixsort6() {
        // FIXME ArrayIndexOutOfBoundsException: 3
        // at InternalNode$InternalCallWrapNode.prepareArgs(InternalNode.java:309)
        assertEval(Ignored.ImplementationError, "argv <- list(structure(integer(0), .Label = character(0), class = 'factor'), TRUE, FALSE); .Internal(radixsort(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
