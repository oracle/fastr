/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_rbind extends TestBase {

    @Test
    public void testrbind1() {
        assertEval("argv <- list(structure(c(3, 3, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,     8, 8, 8, 8, 8, 9, 9, 11, 11, 13, 13, 13, 13, 13, 13, 13,     14, 14, 14, 14, 16, 16, 31, 31, 31, 33, 33, 43, 43, 43, 61,     61, 61, 62, 62, 106, 106, 110, 110, 110, 110, 163, 163, 165,     165, 165, 168, 168, 172, 172, 172, 204, 204, 206, 206, 206,     206, 206, 211, 211, 241, 241, 241, 241, 244, 244, 249, 249,     250, 250, 250, 250, 252, 252, 252, 252, 252, 252, 252, 252,     252, 252, 252, 252, 256, 256, 265, 265, 265, 265, 265, 265,     265, 265, 265, 267, 267, 267, 269, 269, 269, 291, 291, 291,     291, 291, 291, 291, 312, 312, 312, 312, 312, 314, 314, 314,     314, 314, 2.484906649788, 6.27476202124194, 3.97029191355212,     3.98898404656427, 4.52178857704904, 0, 2.30258509299405,     4.59511985013459, 1.6094379124341, 2.94443897916644, 1.94591014905531,     2.99573227355399, 4.36944785246702, 1.38629436111989, 2.39789527279837,     3.98898404656427, 2.07944154167984, 5.64897423816121, 5.75574221358691,     2.89037175789616, 3.09104245335832, 4.70953020131233, 4.98360662170834,     1.6094379124341, 1.6094379124341, 4.70048036579242, 1.6094379124341,     4.54329478227, 1.6094379124341, 4.49980967033027, 5.62762111369064,     5.11799381241676, 2.39789527279837, 6.28785856016178, 5.4380793089232,     3.63758615972639, 5.76205138278018, 2.83321334405622, 5.7037824746562,     5.90263333340137, 3.40119738166216, 3.63758615972639, 4.31748811353631,     5.58724865840025, 5.32787616878958, 4.06044301054642, 6.22059017009974,     6.20455776256869, 5.2040066870768, 6.20253551718792, 3.78418963391826,     2.94443897916644, 2.63905732961526, 6.24804287450843, 2.63905732961526,     5.74620319054015, 1.79175946922805, 5.44241771052179, 4.99721227376411,     5.93753620508243, 4.02535169073515, 4.74493212836325, 5.90536184805457,     6.00388706710654, 4.91998092582813, 5.73979291217923, 3.13549421592915,     3.17805383034795, 3.58351893845611, 4.89783979995091, 4.49980967033027,     6.0913098820777, 5.75257263882563, 2.30258509299405, 2.77258872223978,     5.28826703069454, 6.10924758276437, 4.74493212836325, 6.16331480403464,     4.57471097850338, 3.55534806148941, 1.38629436111989, 4.46590811865458,     5.93224518744801, 0.693147180559945, 3.95124371858143, 4.0943445622221,     3.17805383034795, 2.484906649788, 5.15905529921453, 3.80666248977032,     2.484906649788, 3.3322045101752, 1.94591014905531, 2.77258872223978,     4.71849887129509, 6.23244801655052, 2.99573227355399, 3.71357206670431,     3.36729582998647, 5.64897423816121, 3.55534806148941, 0.693147180559945,     3.04452243772342, 4.30406509320417, 2.56494935746154, 3.61091791264422,     4.69134788222914, 5.93753620508243, 4.95582705760126, -0.693147180559945,     3.87120101090789, 6.31896811374643, 6.06145691892802, 1.79175946922805,     2.19722457733622, 2.07944154167984, 2.07944154167984, 1.94591014905531,     4.51085950651685, 5.85507192220243, 4.57471097850338, 0.693147180559945,     1.6094379124341, 4.36944785246702, 5.36129216570943, 4.40671924726425,     4.85981240436167, 3.61091791264422, 3.73766961828337, 1,     0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1,     0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0,     1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0,     1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1,     0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,     1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1,     1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0,     0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1,     0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1,     1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1,     1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1,     1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1,     0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0,     1, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1,     1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1,     1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0,     1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0,     0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 0, 0, 0,     0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1,     0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0,     0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,     0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0,     1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1,     0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1,     1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0,     0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0,     0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0,     1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1,     1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,     0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1,     0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0,     0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,     1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1,     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0,     0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,     0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1,     1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0,     0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0,     1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1,     0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1,     1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1), .Dim = c(130L, 10L)),     structure(c(316, 316, 316, 5.3890717298165, 2.39789527279837,         5.67332326717149, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0,         0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0), .Dim = c(3L, 10L)));" +
                        "do.call('rbind', argv)");
    }

    @Test
    public void testrbind2() {
        assertEval("argv <- list(c(0L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L,     1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L,     1L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 6L, 1L, 0L, 1L, 20L, 1L,     0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 4L, 0L, 0L, 3L,     6L, 2L, 0L, 14L, 1L, 0L, 16L, 0L, 1L, 0L, 5L, 1L, 0L, 2L,     4L, 0L, 0L, 5L, 0L, 2L, 0L, 1L, 7L, 2L, 0L, 0L, 2L, 0L, 0L,     0L, 0L, 0L, 0L, 1L, 2L, 0L, 0L, 0L, 4L, 0L, 0L, 4L, 0L, 0L,     0L, 0L, 5L, 0L, 18L, 0L, 4L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     5L, 0L, 10L, 0L, 2L, 2L, 6L, 0L, 5L, 7L, 0L, 3L, 0L, 1L,     0L, 3L, 2L, 0L, 5L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 2L,     3L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 2L,     0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 3L, 0L, 1L, 3L, 0L, 0L, 0L, 1L, 1L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 4L, 5L, 0L, 0L,     0L, 4L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 2L, 0L,     0L, 2L), c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 4L, 1L, 5L, 1L, 0L, 1L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 7L, 0L, 0L, 0L, 0L, 7L, 0L, 0L, 1L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 8L, 3L, 3L, 0L, 0L, 1L, 0L,     0L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 2L, 0L, 0L, 1L, 0L, 0L,     0L, 0L, 2L, 0L, 0L, 7L, 0L, 1L, 0L, 13L, 1L, 2L, 0L, 0L,     0L, 0L, 0L, 5L, 0L, 2L, 0L, 8L, 0L, 3L, 0L, 0L, 5L, 0L, 0L,     0L, 0L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L, 0L, 1L, 0L, 0L, 0L,     10L, 0L, 0L, 27L, 3L, 1L, 0L, 0L, 0L, 0L, 2L, 3L, 0L, 1L,     1L, 0L, 4L, 7L, 6L, 3L, 0L, 0L, 0L, 0L, 0L, 0L, 14L, 8L,     0L, 0L, 19L, 0L, 0L, 0L, 1L, 0L, 2L, 0L, 6L, 2L, 2L, 0L,     0L, 3L, 0L, 0L, 0L, 0L, 0L, 0L, 4L, 1L, 1L, 8L, 0L, 0L, 2L,     0L, 4L, 0L, 2L, 0L, 0L, 0L, 3L, 0L, 8L, 0L, 0L, 1L, 0L, 1L,     1L, 0L));" +
                        "do.call('rbind', argv)");
    }

    @Test
    public void testrbind3() {
        assertEval("argv <- list(c(32L, 34L, 37L, 33L, 20L, 40L, 39L, 22L, 33L, 37L,     37L, 38L, 39L, 37L, 36L, 39L, 39L, 40L, 37L, 38L, 35L, 40L,     17L, 39L, 40L, 34L, 40L, 37L, 26L, 40L, 33L, 36L, 38L, 27L,     36L, 36L, 37L, 39L, 40L, 37L, 39L, 40L, 38L, 32L, 37L, 36L,     17L, 36L, 39L, 34L, 40L, 40L, 40L, 37L, 40L, 38L, 39L, 36L,     38L, 40L, 39L, 38L, 39L, 38L, 38L, 40L, 33L, 39L, 40L, 33L,     36L, 34L, 40L, 37L, 26L, 37L, 40L, 40L, 40L, 36L, 39L, 33L,     38L, 40L, 13L, 37L, 22L, 40L, 37L, 40L, 27L, 39L, 35L, 36L,     31L, 24L, 39L, 32L, 38L, 38L), c(8, 6, 3, 7, 20, 0, 1, 18,     7, 3, 3, 2, 1, 3, 4, 1, 1, 0, 3, 2, 5, 0, 23, 1, 0, 6, 0,     3, 14, 0, 7, 4, 2, 13, 4, 4, 3, 1, 0, 3, 1, 0, 2, 8, 3, 4,     23, 4, 1, 6, 0, 0, 0, 3, 0, 2, 1, 4, 2, 0, 1, 2, 1, 2, 2,     0, 7, 1, 0, 7, 4, 6, 0, 3, 14, 3, 0, 0, 0, 4, 1, 7, 2, 0,     27, 3, 18, 0, 3, 0, 13, 1, 5, 4, 9, 16, 1, 8, 2, 2));" +
                        "do.call('rbind', argv)");
    }

    @Test
    public void testRbind() {
        assertEval("{ rbind(1.1:3.3,1.1:3.3) }");
        assertEval("{ rbind() }");
        assertEval("{ rbind(1:3,2) }");
        assertEval("{ rbind(1:3,1:3) }");
        assertEval("{ m <- matrix(1:6, ncol=2) ; rbind(m, 11:12) }");
        assertEval("{ m <- matrix(1:6, ncol=2) ; rbind(11:12, m) }");
        assertEval(Output.IgnoreWarningContext, "{ m <- matrix(1:6, nrow=2) ; rbind(11:12, m) }");

        assertEval("{ rbind(c(1,2)) }");
        assertEval("{ rbind(a=c(b=1,c=2)) }");
        assertEval("{ rbind(c(b=1,c=2)) }");
        assertEval("{ rbind(c(1,c=2)) }");
        assertEval("{ v<-c(b=1, c=2); rbind(v) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y')))) }");

        assertEval("{ rbind(a=c(1,2), b=c(3,4)) }");
        assertEval("{ rbind(a=c(x=1,y=2), b=c(3,4)) }");
        assertEval("{ rbind(a=c(1,2), b=c(x=3,y=4)) }");
        assertEval("{ rbind(a=c(x=1,2), b=c(3,y=4)) }");
        assertEval("{ rbind(a=c(1,2), b=c(3,y=4)) }");
        assertEval("{ rbind(a=c(1,x=2), b=c(y=3,4,5,6)) }");
        assertEval("{ rbind(a=c(1,x=2), b=c(3,4,5,6)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y'))), z=c(8,9)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), c('x', 'y'))), c(8,9)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(c('a', 'b'), NULL)), z=c(8,9)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2, dimnames=list(NULL, c('x', 'y'))), c(m=8,n=9)) }");
        assertEval("{ rbind(matrix(1:4, nrow=2), z=c(m=8,n=9)) }");

        assertEval("{ info <- c(\"print\", \"AES\", \"print.AES\") ; ns <- integer(0) ; rbind(info, ns) }");

        assertEval("{ x<-list(a=7, b=NULL, c=42); y<-as.data.frame(do.call(rbind,x)); y }");

        assertEval("{ x<-data.frame(c(1,2),c(3,4)); dimnames(x) <- list(c(\"A\", \"B\"), c(\"C\", \"D\")); rbind(x) }");

        assertEval("rbind(character(0))");
        assertEval("rbind(character(0), 'f')");
        assertEval("rbind(55, character(0))");
        assertEval("rbind(a=55, character(0))");

        assertEval("v <- 1; attr(v, 'a') <- 'a'; rbind(v); rbind(v, v)");
        assertEval("v <- 1; attr(v, 'a') <- 'a'; attr(v, 'a1') <- 'a1'; rbind(v); rbind(v, v)");
        assertEval("v <- 1:3; attr(v, 'a') <- 'a'; attr(v, 'a1') <- 'a1'; rbind(v); rbind(v, v)");
        assertEval("v <- 1:3; v1<-1:3; attr(v, 'a') <- 'a'; attr(v1, 'a1') <- 'a1'; rbind(v, v1)");
    }

    @Test
    public void testGenericDispatch() {
        assertEval("{ v <- 1; class(v) <- 'foo'; rbind.foo <- function(...) 'foo'; rbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; rbind.foo <- function(...) 'foo'; v2 <- 1; class(v2) <- 'foo'; rbind(v2) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; assign('rbind.foo', function(x) {'foo'}, envir=.__S3MethodsTable__.); result <- rbind(v) ; rm('rbind.foo', envir=.__S3MethodsTable__.); result;}");

        // segfault in gnur
        assertEval(Ignored.ReferenceError, "{ v <- 1; class(v) <- 'foo'; rbind.foo <- length; rbind(v) }");
        assertEval(Ignored.WrongCaller, "{ v <- 1; class(v) <- 'foo'; rbind.foo <- rawToBits; rbind(v) }");

        assertEval("{ v <- 1; class(v) <- 'foo'; rbind(v) }");
        assertEval("{ v <- 1; rbind.foo <- function(...) 'foo'; rbind(v) }");

        assertEval("{ v <- 1; class(v) <- 'foo'; rbind.foo <- function(deparse.level, ...) 'foo'; rbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; rbind.foo <- function(deparse.level, x) 'foo'; rbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; rbind.foo <- function(deparse.level, x1, x2) 'foo'; rbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; rbind.foo <- function(x0, deparse.level, x1, x2) 'foo'; rbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; rbind.foo <- function(x0, x1, x2) 'foo'; rbind(v) }");
        assertEval("{ v <- 1; class(v) <- 'foo'; rbind.foo <- function(x) 'foo'; rbind(v) }");
        assertEval(Ignored.WrongCaller, "{ v <- 1; class(v) <- 'foo'; rbind.foo <- function() 'foo'; rbind(v) }");

        assertEval("{ v <- 1; class(v) <- c('foo1', 'foo2'); rbind.foo1 <- function(...) 'foo1'; rbind(v) }");
        assertEval("{ v <- 1; class(v) <- c('foo1', 'foo2'); rbind.foo2 <- function(...) 'foo2'; rbind(v) }");
        assertEval("{ v <- 1; class(v) <- c('foo1', 'foo2'); rbind.foo1 <- function(...) 'foo1'; rbind.foo2 <- function(...) 'foo2'; rbind(v) }");

        assertEval("{ v1 <- 1; class(v1) <- 'foo1'; rbind.foo1 <- function(...) 'foo1'; v2 <- 2; class(v2) <- 'foo2'; rbind.foo2 <- function(...) 'foo2'; rbind(v1, v2) }");
        assertEval("{ v1 <- 1; class(v1) <- 'foo1'; rbind.foo1 <- function(...) 'foo1'; v2 <- 2; class(v2) <- 'foo2'; rbind(v1, v2) }");
        assertEval("{ v1 <- 1; class(v1) <- 'foo1'; v2 <- 2; class(v2) <- 'foo2'; rbind.foo2 <- function(...) 'foo2'; rbind(v1, v2) }");
    }

    @Test
    public void testDimnames() {
        assertEval("{ attributes(rbind(integer(0))) }");
        assertEval("{ attributes(rbind(1L)) }");
        assertEval("{ attributes(rbind(c(1L, 2L))) }");
        assertEval("{ attributes(rbind(1L, 2L)) }");

        assertEval("{ rbind(structure(1:4, dim=c(2,2), dimnames=list(c('y1', 'y2'), c('x1', 'x2'))), 1L) }");
        assertEval("{ rbind(structure(1:4, dim=c(2,2), dimnames=list(y=c('y1', 'y2'), x=c('x1', 'x2'))), 1L) }");
        assertEval("{ rbind(structure(1:4, dim=c(2,2)), 1L) }");

        assertEval("{ attributes(rbind(structure(1:4, dim=c(2,2), dimnames=list(c('y1', 'y2'), c('x1', 'x2'))), 1L)) }");
        assertEval("{ attributes(rbind(structure(1:4, dim=c(2,2), dimnames=list(y=c('y1', 'y2'), x=c('x1', 'x2'))), 1L)) }");
        assertEval("{ attributes(rbind(structure(1:4, dim=c(2,2)), 1L)) }");

        assertEval("{ rbind(NULL, integer(0)) }");
        assertEval("{ rbind(integer(0), integer(0)) }");
        assertEval("{ rbind(c(1), integer(0)) }");
        assertEval("{ rbind(structure(1:4, dim=c(2,2), dimnames=list(y=c('y1', 'y2'), x=c('x1', 'x2'))), integer(0)) }");

        assertEval("{ attributes(rbind(NULL, integer(0))) }");
        assertEval("{ attributes(rbind(integer(0), integer(0))) }");
        assertEval("{ attributes(rbind(c(1), integer(0))) }");
        assertEval("{ attributes(rbind(structure(1:4, dim=c(2,2), dimnames=list(y=c('y1', 'y2'), x=c('x1', 'x2'))), integer(0))) }");
    }

    @Test
    public void testRetType() {
        assertEval("dput(rbind(NULL))");
        assertEval("dput(rbind(NULL, integer(0)))");
        assertEval("dput(rbind(NULL, NULL, integer(0)))");
        assertEval("dput(rbind(NULL, NULL, double(0)))");
        assertEval("dput(rbind(NULL, NULL, integer(0), double(0)))");
        assertEval("dput(rbind(NULL, NULL, double(0), integer(0)))");
        assertEval("dput(rbind(NULL, NULL, double(0), character(0)))");
        assertEval("dput(rbind(NULL, NULL, double(0), integer(0), character(0)))");
        assertEval("dput(rbind(c(NULL, NULL), integer(0)))");
        assertEval("dput(rbind(integer(0)))");
        assertEval("dput(rbind(integer(0), NULL, NULL))");
    }
}
