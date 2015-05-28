/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_print extends TestBase {

    @Test
    public void testprint1() {
        assertEval("argv <- structure(list(x = 'The leverage of the points is'),     .Names = 'x');do.call('print', argv)");
    }

    @Test
    public void testprint2() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(x = structure(list(modelID = 0L, terms = Species ~     Sepal.Length + Sepal.Width + Petal.Length + Petal.Width,     class.lev = c('setosa', 'versicolor', 'virginica'), model = 'rf',     formula = Species ~ Sepal.Length + Sepal.Width + Petal.Length +         Petal.Width, noClasses = 3L, priorClassProb = c(0.333333333333333,         0.333333333333333, 0.333333333333333), avgTrainPrediction = 0,     noNumeric = 4L, noDiscrete = 1L, discAttrNames = 'Species',     discValNames = list(c('setosa', 'versicolor', 'virginica')),     numAttrNames = c('Sepal.Length', 'Sepal.Width', 'Petal.Length',         'Petal.Width'), discmap = 1L, nummap = 2:5, skipmap = integer(0)),     .Names = c('modelID', 'terms', 'class.lev', 'model', 'formula',         'noClasses', 'priorClassProb', 'avgTrainPrediction',         'noNumeric', 'noDiscrete', 'discAttrNames', 'discValNames',         'numAttrNames', 'discmap', 'nummap', 'skipmap'), class = 'CoreModel')),     .Names = 'x');"
                                        + "do.call('print', argv)");
    }

    @Test
    public void testprint3() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(x = structure(list(CV = c(4.44258707232128,     1.3448257559694, 0.885694975057761, 0.838088461472644), mit = structure(list(p = structure(c(0.452549279246557,     0.13386271764225, 0.267245510599797, 0.146342492511396),     .Names = c('cmp1', 'cmp2', 'cmp3', 'cmp4')), mu = structure(c(0.381966097098555,     3.82765024730876, 1.80304880351015, 2.5878804906034, 2.61803339869107,     0.203368399460934, 1.05601823938856, 0.0596409214659023),     .Dim = c(4L, 2L), .Dimnames = list(c('cmp1', 'cmp2', 'cmp3',         'cmp4'), c('k1', 'k2'))), Sigma = structure(c(0.22917975838358,     0.847714717429939, 0.288537968483766, 0.738832302812549,     -0.400000241640847, -0.0861897092187198, -0.100073467783835,     -0.170562219060232, -0.400000241640847, -0.0861897092187198,     -0.100073467783835, -0.170562219060232, 1.57082072508295,     0.0727738502834565, 0.219785702621389, 0.217416957416503),     .Dim = c(4L, 4L), .Dimnames = list(c('cmp1', 'cmp2', 'cmp3',         'cmp4'), c('k1k1', 'k1k2', 'k2k1', 'k2k2'))), df = 1),     .Names = c('p', 'mu', 'Sigma', 'df')), summary = structure(list(H = c(1,     2, 3, 4), METHOD.mu = structure(c(1L, 1L, 1L, 1L), .Label = 'BFGS',     class = 'factor'), TIME.mu = c(1.301, 0.634, 1.148, 0.716000000000001),     METHOD.p = structure(c(1L, 2L, 2L, 2L), .Label = c('NONE',         'NLMINB'), class = 'factor'), TIME.p = c(0, 0.00600000000000023,         0.0129999999999981, 0.0309999999999988), CV = c(4.44258707232128,         1.3448257559694, 0.885694975057761, 0.838088461472644)),     .Names = c('H', 'METHOD.mu', 'TIME.mu', 'METHOD.p', 'TIME.p',         'CV'), row.names = c(NA, 4L), class = 'data.frame')),     .Names = c('CV', 'mit', 'summary'))), .Names = 'x');"
                                        + "do.call('print', argv)");
    }

    @Test
    public void testprint4() {
        assertEval("argv <- structure(list(x = c(1.12029789965078, -0.718988837588323,     -0.799820795962862, 1.36325504609423, -0.877647212109208,     -1.46608694151033, -0.277315770575131, 0.49759016736751,     -1.49309981133256, 0.147586557048694, 1.32490895489118, -0.993328430480091,     -0.809428793397133, 1.39969712961021, 0.43065679489178, 0.19581824909626,     -0.0622842939729247, 0.57841339234696, 2.31951400192491,     2.93765523729633)), .Names = 'x');"
                        + "do.call('print', argv)");
    }

    @Test
    public void testprint5() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(x = structure(list(statistic = structure(0.87901108669074,     .Names = 't'), parameter = structure(19, .Names = 'df'),     p.value = 0.390376937081292, conf.int = structure(c(-0.332667989442433,         0.814407243771461), conf.level = 0.95), estimate = structure(0.240869627164514,         .Names = 'mean of x'), null.value = structure(0, .Names = 'mean'),     alternative = 'two.sided', method = 'One Sample t-test',     data.name = 'x'), .Names = c('statistic', 'parameter', 'p.value',     'conf.int', 'estimate', 'null.value', 'alternative', 'method',     'data.name'), class = 'htest')), .Names = 'x');"
                                        + "do.call('print', argv)");
    }

    @Test
    public void testPrint() {
        assertEval("{ print(23) }");
        assertEval("{ print(1:3,quote=TRUE) }");
        assertEval("{ print(list(1,2,3),quote=TRUE) }");
        assertEval("{ x<-c(1,2); names(x)=c(\"a\", \"b\"); print(x,quote=TRUE) }");
        assertEval("{ x<-c(1, 2:20, 21); n<-\"a\"; n[21]=\"b\"; names(x)<-n; print(x,quote=TRUE) }");
        assertEval("{ x<-c(10000000, 10000:10007, 21000000); n<-\"a\"; n[10]=\"b\"; names(x)<-n; print(x,quote=TRUE) }");
        assertEval("{ x<-c(\"11\", \"7\", \"2222\", \"7\", \"33\"); print(x,quote=TRUE) }");
        assertEval("{  x<-c(11, 7, 2222, 7, 33); print(x,quote=TRUE) }");
        assertEval("{ x<-c(\"11\", \"7\", \"2222\", \"7\", \"33\"); names(x)<-1:5; print(x,quote=TRUE) }");
        assertEval("{ x<-c(11, 7, 2222, 7, 33); names(x)<-1:5; print(x,quote=TRUE) }");
        assertEval("{ print(list(list(list(1,2),list(3)),list(list(4),list(5,6))),quote=TRUE) }");
        assertEval("{ print(c(1.1,2.34567),quote=TRUE) }");
        assertEval("{ print(c(1,2.34567),quote=TRUE) }");
        assertEval("{ print(c(11.1,2.34567),quote=TRUE) }");
        assertEval("{ nql <- noquote(letters); print(nql)}");
        assertEval("{ nql <- noquote(letters); nql[1:4] <- \"oh\"; print(nql)}");
        assertEval("{ print(c(\"foo\"),quote=FALSE)}");
        assertEval("{ x<-matrix(c(\"a\",\"b\",\"c\",\"d\"),nrow=2);print(x,quote=FALSE)}");
        assertEval("{ y<-c(\"a\",\"b\",\"c\",\"d\");dim(y)<-c(1,2,2);print(y,quote=FALSE)}");
        assertEval("{ n <- 17 ; fac <- factor(rep(1:3, length = n), levels = 1:5) ; y<-tapply(1:n, fac, sum); y }");
        assertEval("{ nql <- noquote(letters); nql}");
    }
}
