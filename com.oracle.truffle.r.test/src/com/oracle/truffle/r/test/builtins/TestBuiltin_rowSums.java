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
public class TestBuiltin_rowSums extends TestBase {

    @Test
    public void testrowSums1() {
        assertEval("argv <- list(structure(c(1L, 0L, 0L, 0L, 2L, 0L, 0L, 0L, 3L), .Dim = c(3L, 3L)), 3, 3, FALSE); .Internal(rowSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowSums2() {
        assertEval("argv <- list(structure(c(0.999999999999996, -5.93240323105314e-31, -1.22690013807617e-30, -2.66771163434776e-30, 6.26446113912225e-17, -5.95448228496283e-17, -3.41105197331973e-17, 2.9023639112071e-17, -1.98544126594555e-18, 5.0222594521389e-17, -2.11849144310153e-17, 1.78452635853509e-17, -6.4482597529713e-30, -5.93240323105314e-31, 1, -2.70177878542148e-31, 4.10223078154481e-30, -7.29760565072095e-17, 4.21528303119361e-16, 1.69346505683726e-16, -8.46179388945247e-17, -2.12579366024309e-17, -1.08608258515243e-16, -1.91006916081825e-17, -1.09544412010741e-16, 1.52841800386571e-30, -1.22690013807617e-30, -2.70177878542148e-31, 1, 5.63751592936848e-30, 4.44597451723816e-17, -1.70262938021701e-16, -4.55196812407612e-17, 1.77744051006272e-17, -5.96596846288922e-17, 9.58999363873063e-17, -5.74900426053008e-17, 1.14815157965335e-16, -3.6669706141133e-30, -2.66771163434776e-30, 4.10223078154481e-30, 5.63751592936848e-30, 1, -5.9187048613625e-18, -1.70068399080916e-16, -6.90189597604163e-18, -6.36439216662415e-19, -4.18657854049523e-19, 3.0354538418548e-17, -7.42749951284567e-18, -8.90495022022045e-18, 1.55306990715387e-30, 6.26446113912225e-17, -7.29760565072095e-17, 4.44597451723816e-17, -5.9187048613625e-18, 0.55411820856073, -0.00247216583270833, -0.0121967562426746, -0.0136834443420207, 0.00612437416936414, -0.00919613834986896, -0.017907465564047, 0.0210800496836485, 0, -5.95448228496283e-17, 4.21528303119361e-16, -1.70262938021701e-16, -1.70068399080916e-16, -0.00247216583270833, 0.0915588872840349, 0.00402469784261988, 0.0225004116141538, 0.00428277377776577, 0.0217395090957974, 0.00506691575079725, -0.155787004553276, -1.57863385792422e-15, -3.41105197331973e-17, 1.69346505683726e-16, -4.55196812407612e-17, -6.90189597604163e-18, -0.0121967562426746, 0.00402469784261988, 0.00831687169973403, 0.00905869237132552, 0.00121203034126938, 0.00939616211925082, 0.00270063068023987, -0.0350427136160765, 1.66845523703974e-15, 2.9023639112071e-17, -8.46179388945247e-17, 1.77744051006272e-17, -6.36439216662415e-19, -0.0136834443420207, 0.0225004116141538, 0.00905869237132552, 0.0237646346509256, 0.0107094040391417, 0.0632843775518589, 0.0165995956409034, -0.317638195769953, 3.75582594532732e-16, -1.98544126594555e-18, -2.12579366024309e-17, -5.96596846288922e-17, -4.18657854049523e-19, 0.00612437416936414, 0.00428277377776577, 0.00121203034126938, 0.0107094040391417, 0.000250414165674235, 0.0118733901248423, 0.0032448838873885, -0.0719898325072222, -4.32029144045995e-15, 5.0222594521389e-17, -1.08608258515243e-16, 9.58999363873063e-17, 3.0354538418548e-17, -0.00919613834986896, 0.0217395090957974, 0.00939616211925082, 0.0632843775518589, 0.0118733901248423, 0.0578950164197554, 0.0182925914744869, -0.367565522079614, -1.23944977824402e-15, -2.11849144310153e-17, -1.91006916081825e-17, -5.74900426053008e-17, -7.42749951284567e-18, -0.017907465564047, 0.00506691575079725, 0.00270063068023987, 0.0165995956409034, 0.0032448838873885, 0.0182925914744869, 0.00349919192597366, -0.0788502030216034, 0, 1.78452635853509e-17, -1.09544412010741e-16, 1.14815157965335e-16, -8.90495022022045e-18, 0.0210800496836485, -0.155787004553276, -0.0350427136160765, -0.317638195769953, -0.0719898325072222, -0.367565522079614, -0.0788502030216034, 2.49598569418347, -8.69223914290117e-16, -6.4482597529713e-30, 1.52841800386571e-30, -3.6669706141133e-30, 1.55306990715387e-30, 0, -1.57863385792422e-15, 1.66845523703974e-15, 3.75582594532732e-16, -4.32029144045995e-15, -1.23944977824402e-15, 0, -8.69223914290117e-16, 1), .Dim = c(13L, 13L), .Dimnames = list(c('(Intercept)', 'fac2', 'fac3', 'fac4', '', '', '', '', '', '', '', '', ''), NULL)), 13, 13, FALSE); .Internal(rowSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowSums3() {
        assertEval("argv <- list(structure(c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), .Dim = c(16L, 16L), .Dimnames = list(NULL, NULL)), 16, 16, TRUE); .Internal(rowSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowSums4() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(16L, 16L), .Dimnames = list(NULL, NULL)), 16, 16, FALSE); .Internal(rowSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowSums5() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 3L), .Dimnames = list(NULL, c('wt.loss', 'age', 'I(age)'))), 0, 3, FALSE); .Internal(rowSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowSums6() {
        assertEval("argv <- list(structure(c(1L, 0L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 1L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), .Dim = c(61L, 4L), .Dimnames = list(c('190', '191', '192', '193', '194', '195', '196', '197', '198', '199', '200', '201', '202', '203', '204', '205', '206', '207', '208', '209', '210', '211', '212', '213', '214', '215', '216', '217', '218', '219', '220', '221', '222', '223', '224', '225', '226', '227', '228', '229', '230', '231', '232', '233', '234', '235', '236', '237', '238', '239', '240', '241', '242', '243', '244', '245', '246', '247', '248', '249', '250'), NULL)), 61, 4, FALSE); .Internal(rowSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowSums7() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, NA, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(16L, 16L), .Dimnames = list(NULL, NULL)), 16, 16, TRUE); .Internal(rowSums(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testrowSums9() {
        assertEval("argv <- structure(list(x = structure(numeric(0), .Dim = c(0L,     2L))), .Names = 'x');do.call('rowSums', argv)");
    }

    @Test
    public void testRowSums() {
        assertEval("{ m <- matrix(1:6, nrow=2) ; rowSums(x = m) }");
        assertEval("{ m <- matrix(c(1,2,3,4,5,6), nrow=2) ; rowSums(m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowSums(m) }");
        assertEval("{ m <- matrix(c(NA,2,3,4,NA,6), nrow=2) ; rowSums(m, na.rm = TRUE) }");
        assertEval("{ rowSums(matrix(as.complex(1:6), nrow=2)) }");
        assertEval("{ rowSums(matrix((1:6)*(1+1i), nrow=2)) }");

        assertEval("{x<-cbind(1:3, 4:6, 7:9); rowSums(x)}");
        assertEval("{x<-cbind(1:3, NA, 7:9); rowSums(x)}");
        assertEval("{x<-cbind(1:3, NaN, 7:9); rowSums(x)}");
        assertEval("{x<-cbind(1:3, NaN, 7:9, 10:12); rowSums(x, na.rm=TRUE)}");
        assertEval("{x<-cbind(1:4, NA, NaN, 9:12); rowSums(x, na.rm=TRUE)}");
        assertEval("{x<-cbind(2L:10L,3L); rowSums(x)}");
        assertEval("{rowSums(matrix(c(3+2i,4+5i,2+0i,5+10i)))}");
        assertEval("{rowSums(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{rowSums(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = TRUE)}");
        assertEval("{rowSums(matrix(c(TRUE,FALSE,FALSE,NaN),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{rowSums(matrix(c(TRUE,FALSE,FALSE,NA),nrow=2,ncol=2), na.rm = FALSE)}");
        assertEval("{rowSums(matrix(c(NaN,4+5i,2+0i,5+10i),nrow=2,ncol=2), na.rm = TRUE)}");

        // Whichever value(NA or NaN) is first in the row will be returned for that row.
        assertEval("{rowSums(matrix(c(NA,NaN,NaN,NA),ncol=2,nrow=2))}");

        // rowSums on matrix drop dimension
        assertEval("{ a = rowSums(matrix(1:12,3,4)); is.null(dim(a)) }");

        // rowSums on matrix have correct length
        assertEval("{ a = rowSums(matrix(1:12,3,4)); length(a) }");

        // rowSums on matrix have correct values
        assertEval("{ a = rowSums(matrix(1:12,3,4)); c(a[1],a[2],a[3]) }");

        // rowSums on array have no dimension
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); is.null(dim(a)) }");

        // row on array have correct length
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); length(a) }");

        // rowSums on array have correct values
        assertEval("{ a = rowSums(array(1:24,c(2,3,4))); c(a[1],a[2]) }");

        assertEval("{x<-matrix(c(\"1\",\"2\",\"3\",\"4\"),ncol=2);rowSums(x)}");
    }
}
