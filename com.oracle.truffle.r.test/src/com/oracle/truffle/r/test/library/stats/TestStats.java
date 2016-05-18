/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.stats;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestStats extends TestBase {
    @Test
    public void testCor() {
        assertEval("{ cor(c(1,2,3),c(1,2,3)) }");
        assertEval("{ as.integer(cor(c(1,2,3),c(1,2,5))*10000000) }");
        assertEval("{ cor(cbind(c(3,2,1), c(1,2,3))) }");
        assertEval(Output.ContainsWarning, "{ cor(cbind(c(1, 1, 1), c(1, 1, 1))) }");
        assertEval("{ cor(cbind(c(1:9,0/0), 101:110)) }");
        assertEval("{ round( cor(cbind(c(10,5,4,1), c(2,5,10,5))), digits=5 ) }");
    }

    @Test
    public void testCov() {
        assertEval("{ cov(c(1,2,3),c(1,2,3)) }");
        assertEval("{ cov(c(1,2,3),c(1,2,4)) }");
        assertEval("{ cov(c(1,2,3),c(1,2,5)) }");
    }

    @Test
    public void testFFT() {
        assertEval("{ fft(1:4) }");
        assertEval("{ fft(1:4, inverse=TRUE) }");
        assertEval("{ fft(10) }");
        assertEval("{ fft(cbind(1:2,3:4)) }");
    }

    @Test
    public void testSd() {
        assertEval("{ round(100*sd(c(1,2))^2) }");
    }

    @Test
    public void testQgamma() {
        assertEval("{ qgamma(0.5, shape=1) }");
        assertEval("{ p <- (1:9)/10 ; qgamma(p, shape=1) }");

        assertEval("{ qgamma(0.5, shape=double()) }");
        assertEval("{ qgamma(0.5, shape=1, rate=double()) }");

        assertEval("{ qgamma(0.5, shape=c(2,1), scale=c(3,4)) }");
        assertEval("{ qgamma(0.5, shape=c(2,1), scale=c(3,4,5)) }");
        assertEval("{ qgamma(0.5, shape=c(2,1,2), scale=c(3,4,5)) }");
        assertEval("{ qgamma(c(0.5, 0.7), shape=c(2,1,2), scale=c(3,4,5)) }");
        assertEval("{ qgamma(c(0.5, 0.7, 0.5), shape=c(2,1,2), scale=c(3,4,5)) }");

        assertEval("{ x<-c(a=0.5, b=0.7); attr(x, \"foo\")<-\"foo\"; qgamma(x, shape=1) }");
        assertEval("{ x<-c(0.5); y<-c(s1=1); z<-c(s1=7); qgamma(x, shape=y, rate=z) }");
        assertEval("{ x<-c(0.5); y<-c(s1=1); z<-c(s1=7); attr(z, \"foo\")<-\"foo\"; qgamma(x, shape=y, rate=z) }");
        assertEval("{ x<-c(0.5); y<-c(s1=1, s2=2); z<-c(s1=7, s2=8); qgamma(0.5, shape=y, rate=z) }");
        assertEval("{ x<-c(a=0.5); y<-c(s1=1); attr(y, \"bar\")<-\"bar\"; z<-c(7, s3=8); attr(z, \"foo\")<-\"foo\"; qgamma(x, shape=y, rate=z) }");
    }

    @Test
    public void testQbinom() {
        assertEval("qbinom(1:50/100,20,0.7)");
        assertEval("qbinom(1:100/100,20,0.1)");
        assertEval("qbinom(1:100/100,5,0.1)");
        assertEval("qbinom(1:100/100,0,0.1)");
        assertEval("qbinom(1,20,0.1)");
        assertEval("qbinom(0.66,20,0.1)");
        assertEval("qbinom(0,20,0.1)");
        assertEval("qbinom(0,20,c(0.1,0.9))");
        assertEval("qbinom(0,20,c(0.1,1.9))");
        assertEval("qbinom(0,integer(),c(0.1,0.9))");
    }

    @Test
    public void testRbinom() {
        assertEval("set.seed(123); rbinom(10,20,c(0.3,0.2))");
        assertEval("set.seed(123); rbinom(1,20,c(0.3,0.2))");
        assertEval("set.seed(123); rbinom(c(1,2),20,c(0.3,0.2))");
        assertEval("set.seed(123); rbinom(c(1,2),c(2,10),c(0.3,0.2))");
        assertEval("set.seed(123); rbinom(c(12),c(2,10),c(0.3))");
    }

    @Test
    public void testDbinom() {
        assertEval("round(dbinom(81,c(10,12,14),c(0.3,0.4,0.3,0.1,0.33)),digits=9)");
        assertEval("round(dbinom(c(81,2,4,9),c(10,12,14),c(0.3,0.4,0.3,0.1,0.33)),digits=9)");
        assertEval("round(dbinom(0.9,c(10,12,14),c(0.3,0.4,0.3,0.1,0.33)),digits=9)");
        assertEval("round(dbinom(2,14,0.33),digits=9)");
    }

    @Test
    public void testPnorm() {
        assertEval("pnorm(1:10/10,c(2,10.5),c(3,7))");
        assertEval("pnorm(1:10/10,10L,c(3))");
        assertEval("pnorm(1:10/10,c(2,10.5),c(3))");
        assertEval("round(pnorm(1:10/10,c(2,NaN),c(3)),digits=7)");
        assertEval("round(pnorm(1:10/10,c(2,NA),c(3)),digits=7)");
    }

    @Test
    public void testQnorm() {
        assertEval("qnorm(c(0.1,0.9,0.5,0.00001,0.99999), 100, c(20,1))");
        assertEval("round(qnorm(c(0.1,0.9,0.5,1.00001,0.99999), 100, c(20,1)), digits=5)");
    }

    @Test
    public void testRandom() {
        assertEval("{ set.seed(4357, \"default\"); sum(runif(10)) }");
        assertEval("{ set.seed(4336, \"default\"); sum(runif(10000)) }");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); sum(runif(100)) }");
        assertEval("{ set.seed(4357, \"default\"); round( rnorm(3), digits = 5 ) }");
        assertEval("{ set.seed(7); runif(10) }");
        assertEval("{ set.seed(7); runif(100) }");
        assertEval("{ set.seed(7); runif(25*25) }");
        assertEval("{ set.seed(7); rnorm(10) }");
        assertEval("{ set.seed(7); rnorm(100) }");
        assertEval("{ set.seed(7); rnorm(25*25) }");
        assertEval("{ set.seed(7); matrix(rnorm(10), ncol=5) }");
        assertEval("{ set.seed(7); matrix(rnorm(100), ncol=10) }");
        assertEval("{ set.seed(7); matrix(rnorm(25*25), ncol=25) }");
        assertEval("{ set.seed(4357, \"default\"); round( rnorm(3,1000,10), digits = 5 ) }");

        assertEval("{ set.seed(7); round( runif(3), digits = 5 ) }");
        assertEval("{ set.seed(7); round( runif(3,1,10), digits = 5 ) }");

        assertEval("{ set.seed(7); round( rbinom(3,3,0.9), digits = 5 ) }");
        assertEval("{ set.seed(7); round( rbinom(3,10,(1:5)/5), digits = 5 ) }");
    }

    @Test
    public void testRandomIgnore() {
        assertEval(Ignored.Unknown, "{ set.seed(7); round( rnorm(3,c(1000,2,3),c(10,11)), digits = 5 ) }");
        assertEval(Ignored.Unknown, "{ set.seed(7); round( runif(3,1:3,3:2), digits = 5 ) }");

        assertEval(Ignored.Unknown, "{ set.seed(7); round( rgamma(3,1), digits = 5 ) }");
        assertEval(Ignored.Unknown, "{ set.seed(7); round( rgamma(3,0.5,scale=1:3), digits = 5 ) }");
        assertEval(Ignored.Unknown, "{ set.seed(7); round( rgamma(3,0.5,rate=1:3), digits = 5 ) }");

        assertEval(Ignored.Unknown, "{ set.seed(7); round( rlnorm(3), digits = 5 ) }");
        assertEval(Ignored.Unknown, "{ set.seed(7); round( rlnorm(3,sdlog=c(10,3,0.5)), digits = 5 ) }");

        assertEval(Ignored.Unknown, "{ set.seed(7); round( rcauchy(3), digits = 5 ) }");
        assertEval(Ignored.Unknown, "{ set.seed(7); round( rcauchy(3, scale=4, location=1:3), digits = 5 ) }");
    }

    @Test
    public void testNaFail() {
        assertEval("na.fail(c(1,2,3))");
        assertEval("na.fail(c(1L, 2L))");
        assertEval("na.fail(c(1,NA,3))");
        assertEval("na.fail(c(NA, 2L))");
        assertEval(Output.ContainsError, "na.fail(c())");
        assertEval(Output.ContainsError, "na.fail(NULL)");
    }
}
