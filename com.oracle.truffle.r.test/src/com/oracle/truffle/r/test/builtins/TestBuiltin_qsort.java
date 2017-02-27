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
public class TestBuiltin_qsort extends TestBase {

    @Test
    public void testqsort1() {
        assertEval("argv <- list(3L, FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort2() {
        assertEval("argv <- list(c(306, 455, 1010, 210, 883, 1022, 218, 166, 170, 567, 144, 613, 707, 88, 301, 624, 371, 394, 574, 118, 390, 12, 26, 533, 53, 814, 93, 460, 583, 303, 519, 643, 189, 246, 689, 65, 132, 223, 175, 163, 428, 230, 840, 11, 176, 791, 95, 196, 806, 284, 147, 655, 239, 30, 179, 310, 477, 364, 107, 177, 156, 429, 15, 181, 283, 13, 212, 524, 288, 363, 442, 54, 558, 207, 92, 60, 202, 353, 267, 387, 457, 337, 404, 222, 458, 31, 229, 444, 329, 291, 292, 142, 413, 320, 285, 197, 180, 300, 259, 110, 286, 270, 81, 131, 225, 269, 279, 135, 59, 105, 237, 221, 185, 183, 116, 188, 191, 174), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort3() {
        assertEval("argv <- list(numeric(0), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort4() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(1.64819754690779, 0.502718643389684, 0.845467412356198, 0.467247395729231, -0.402055063696625, 0.923526703253396, -0.0080556407117813, 1.03378423761425, -0.799126981726699, 1.00423302095334, -0.311973356192691, -0.88614958536232, -1.9222548962705, 1.61970074406333, 0.519269904664384, -0.055849931834021, 0.696417610118512), TRUE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort5() {
        assertEval(Ignored.Unknown, "argv <- list(c(1L, 7L, 11L, 12L, 13L, 19L, 25L, 3L, 8L), TRUE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort6() {
        assertEval("argv <- list(c(1, 2, 4, 6, 8, 3, 5, 7), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort7() {
        assertEval("argv <- list(c(1, 173, 346, 518, 691, 174, 519), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort8() {
        assertEval("argv <- list(c(1, 42, 83, 124, 166, 43, 84, 125), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort9() {
        assertEval("argv <- list(c(-Inf, -Inf, -Inf, -Inf, -Inf, -Inf, 0, 1, 2, 3, 4, 5, Inf, Inf, Inf, Inf, Inf), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort10() {
        assertEval("argv <- list(c(1, 13, 26, 38, 51, 14, 39), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort11() {
        assertEval("argv <- list(c(-Inf, -Inf, -Inf, -Inf, -Inf, 0, 1, 2, 3, 4, Inf, Inf, Inf, Inf), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort12() {
        assertEval(Ignored.Unknown, "argv <- list(FALSE, FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testqsort13() {
        assertEval("argv <- list(c(63, 187, 64, 188), FALSE); .Internal(qsort(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testArgsCasts() {
        // GnuR "qsort" outputs
        // "Error: argument is not a numeric vector"
        // for the raw vector while both "sort" and "qsort" in GnuR output
        // "Error: raw vectors cannot be sorted"
        // We share the same Casts adapter for all "sort","psort" and "qsort"
        // so prefixing with ignore the error message here for now.
        assertEval(Output.IgnoreErrorMessage, "{ .Internal(qsort(as.raw(c(0x44,0x40)), FALSE)) }");
        assertEval("{ .Internal(sort(NULL, FALSE)) }");
        assertEval("{ lv<-list(a=5,b=c(1,2)); .Internal(sort(lv,FALSE)) }");
    }
}
