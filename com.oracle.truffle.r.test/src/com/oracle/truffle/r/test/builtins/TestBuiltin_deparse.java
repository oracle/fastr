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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_deparse extends TestBase {

    @Test
    public void testdeparse1() {
        assertEval("argv <- list(quote(rsp), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse2() {
        assertEval("argv <- list(quote(rnorm(1, sd = Inf)), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse3() {
        assertEval("argv <- list(quote(rnorm(2, c(1, NA))), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse4() {
        assertEval("argv <- list(quote(unclass(x)), 500, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse5() {
        assertEval("argv <- list(quote(cor(rnorm(10), NULL)), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse6() {
        assertEval("argv <- list(quote(5 * exp(-x)), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(quote(y ~ ((g1) * exp((log(g2/g1)) * (1 - exp(-k * (x - Ta)))/(1 - exp(-k * (Tb - Ta)))))), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse8() {
        assertEval("argv <- list(quote(tt <- table(c(rep(0, 7), rep(1, 4), rep(5, 3)))), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse9() {
        assertEval("argv <- list(quote(utils::str), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse10() {
        assertEval("argv <- list(quote(1:10), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse11() {
        assertEval("argv <- list(quote(x[[i]]), 500L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse12() {
        assertEval("argv <- list(quote(t1 - 4), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse13() {
        assertEval("argv <- list(quote(read.table('foo1')), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse14() {
        assertEval("argv <- list(quote(`[.data.frame`(dd, , 'x')), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse15() {
        assertEval(Ignored.Unknown, "argv <- list(1e-07, 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse16() {
        assertEval("argv <- list('coef.corStruct', 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse17() {
        assertEval(Ignored.Unknown,
                        "argv <- list('Version of 'graph' is too old --- no tests done here!\\n', 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse18() {
        assertEval("argv <- list(Inf, 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse19() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(Sex = structure(c(2L, 2L, 1L, 1L, 2L, 2L), .Label = c('Female', 'Male'), class = 'factor'), age = c(15, 20, 10, 12, 2, 4), Subject = structure(c(2L, 2L, 1L, 1L, 3L, 3L), .Label = c('F30', 'M01', 'M04'), class = 'factor')), .Names = c('Sex', 'age', 'Subject'), row.names = c(NA, -6L), class = 'data.frame'), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse20() {
        assertEval("argv <- list(TRUE, 500L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse21() {
        assertEval("argv <- list(.Primitive('interactive'), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse22() {
        assertEval("argv <- list(0+1i, 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse23() {
        assertEval("argv <- list(quote(cor(Z[, FALSE], use = 'pairwise.complete.obs', method = 'kendall')), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse24() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(7.4, 8, 12.6, 11.5, 14.3, 14.9, 8.6, 13.8, 20.1, 8.6, 6.9, 9.7, 9.2, 10.9, 13.2, 11.5, 12, 18.4, 11.5, 9.7, 9.7, 16.6, 9.7, 12, 16.6, 14.9, 8, 12, 14.9, 5.7, 7.4, 8.6, 9.7, 16.1, 9.2, 8.6, 14.3, 9.7, 6.9, 13.8, 11.5, 10.9, 9.2, 8, 13.8, 11.5, 14.9, 20.7, 9.2, 11.5, 10.3, 6.3, 1.7, 4.6, 6.3, 8, 8, 10.3, 11.5, 14.9, 8, 6.9, 13.8, 7.4, 6.9, 7.4, 4.6, 4, 10.3, 8, 8.6, 11.5, 11.5, 11.5, 9.7, 11.5, 10.3, 6.3, 7.4, 10.9, 10.3, 15.5, 14.3, 12.6, 9.7, 3.4, 8, 5.7, 9.7, 2.3, 6.3, 6.3, 6.9, 5.1, 2.8, 4.6, 7.4, 15.5, 10.9, 10.3, 10.9, 9.7, 14.9, 15.5, 6.3, 10.9, 11.5, 6.9, 13.8, 10.3, 10.3, 8, 12.6, 9.2, 10.3, 10.3, 16.6, 6.9, 13.2, 14.3, 8, 11.5), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse25() {
        assertEval(Ignored.Unknown, "argv <- list(1e+05, 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse26() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(distance = c(26, 25, 29, 31, 21.5, 22.5), age = c(8, 10, 12, 14, 8, 10), Subject = structure(c(2L, 2L, 2L, 2L, 1L, 1L), .Label = c('M02', 'M01'), class = c('ordered', 'factor')), Sex = structure(c(1L, 1L, 1L, 1L, 1L, 1L), .Label = c('Male', 'Female'), class = 'factor')), .Names = c('distance', 'age', 'Subject', 'Sex'), row.names = c('1', '2', '3', '4', '5', '6'), class = 'data.frame'), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse27() {
        assertEval("argv <- list('\\t *ERROR* !!\\n', 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse28() {
        assertEval("argv <- list('\\n\\f\\n', 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse29() {
        assertEval("argv <- list(' +\\\\.', 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse30() {
        assertEval(Ignored.Unknown, "argv <- list(structure(FALSE, .Dim = 1L), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse31() {
        assertEval(Ignored.Unknown,
                        "argv <- list(c(0, 0.587785252292473, 0.951056516295154, 0.951056516295154, 0.587785252292473, 1.22464679914735e-16, -0.587785252292473, -0.951056516295154, -0.951056516295154, -0.587785252292473, -2.44929359829471e-16, 0.587785252292473, 0.951056516295154), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse32() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(39.384847580955, 40.3469409309138, 42.6018205723052, 46.6665176252597, 51.3438205965467, 60.0069972599329, 64.6480892875058, 62.5709232928432, 57.679739382496, 49.5060394945433, 43.474726406114, 39.8236314289602, 38.361391396627, 37.9275637097922, 43.6868952734483, 45.1919846859641, 51.722520194987, 59.3399821539983, 61.9345241730145, 62.1515308754468, 57.6561604617486, 49.2849925780811, 42.606775772378, 39.6394677676018, 38.6328048791077, 38.4418602988203, 43.1520834957543, 45.6551746936999, 51.7999631155049, 59.5021948495759, 62.9217123388139, 62.0751910659837, 57.8048619656866, 49.5091658164884, 42.8045075272742, 40.2515159054665), .Tsp = c(1937, 1939.91666666667, 12), class = 'ts'), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse33() {
        assertEval("argv <- list(NA_real_, 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse34() {
        assertEval("argv <- list(quote(lm(formula = y ~ x1 + x2 + x3)), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse35() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(112, 118, 132, 129, 121, 135, 148, 148, 136, 119, 104, 118, 115, 126, 141, 135, 125, 149, 170, 170, 158, 133, 114, 140, 145, 150, 178, 163, 172, 178, 199, 199, 184, 162, 146, 166, 171, 180, 193, 181, 183, 218, 230, 242, 209, 191, 172, 194, 196, 196, 236, 235, 229, 243, 264, 272, 237, 211, 180, 201, 204, 188, 235, 227, 234, 264, 302, 293, 259, 229, 203, 229, 242, 233, 267, 269, 270, 315, 364, 347, 312, 274, 237, 278, 284, 277, 317, 313, 318, 374, 413, 405, 355, 306, 271, 306, 315, 301, 356, 348, 355, 422, 465, 467, 404, 347, 305, 336, 340, 318, 362, 348, 363, 435, 491, 505, 404, 359, 310, 337, 360, 342, 406, 396, 420, 472, 548, 559, 463, 407, 362, 405, 417, 391, 419, 461, 472, 535, 622, 606, 508, 461, 390, 432, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 419.147602949539, 391.474665943444, 435.919286153217, 443.935203034261, 455.023399013445, 517.28707821144, 589.71337277669, 582.999919227301, 484.573388713116, 428.878182738437, 368.526582998452, 406.728709993152, 415.660571294428, 388.716535970235, 433.006017658935, 440.885684396326, 451.651900136866, 513.051252429496, 584.327164324967, 577.055407135124, 479.076505013118, 423.494870357491, 363.43932958967, 400.592058645117, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 484.030717075782, 462.954959541421, 526.353307750503, 546.165638262644, 569.502470928676, 657.838443307596, 761.241730163307, 763.280655335144, 642.989004951864, 576.423799567567, 501.429012064338, 559.981301364233, 591.700754553767, 565.210772316967, 642.377841008703, 666.682421047093, 695.547100430962, 804.065022775202, 931.340589597203, 934.837830059897, 788.422986194072, 707.666678543854, 616.37838266375, 689.250456425465), .Dim = c(168L, 3L), .Dimnames = list(NULL, c('structure(c(112, 118, 132, 129, 121, 135, 148, 148, 136, 119, ', 'structure(c(419.147602949539, 391.474665943444, 435.919286153217, ', 'structure(c(484.030717075782, 462.954959541421, 526.353307750503, ')), .Tsp = c(1949, 1962.91666666667, 12), class = c('mts', 'ts', 'matrix')), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse36() {
        assertEval("argv <- list(numeric(0), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse37() {
        assertEval("argv <- list(0:12, 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse38() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse39() {
        assertEval("argv <- list(NA, 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse40() {
        assertEval(Ignored.Unknown, "argv <- list(logical(0), logical(0), FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse41() {
        assertEval("argv <- list(FALSE, 50L, FALSE, 69, 2L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse42() {
        assertEval("argv <- list(c(FALSE, FALSE), 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse43() {
        assertEval("argv <- list(quote(glm(formula = y ~ x, family = poisson(identity), start = c(1, 0))), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse44() {
        assertEval(Ignored.Unknown,
                        "argv <- list(quote(lm(formula = 1000/MPG.city ~ Weight + Cylinders + Type + EngineSize + DriveTrain, data = Cars93)), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse45() {
        assertEval("argv <- list(0.333333333333333, 60L, FALSE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse46() {
        assertEval("argv <- list(quote(Fr ~ (Hair + Eye + Sex)^2), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse47() {
        assertEval("argv <- list(quote(glm(formula = cbind(X, M) ~ M.user + Temp + M.user:Temp, family = binomial, data = detg1)), 60L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testdeparse48() {
        assertEval("argv <- list(quote(x[[i]] <- 0.9999997), 500L, TRUE, 69, -1L); .Internal(deparse(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testDeparse() {
        assertEval("{ deparse(TRUE) }");
        assertEval("{ deparse(c(T, F)) }");
        assertEval("{ deparse(17) }");
        assertEval("{ deparse(-17) }");
        assertEval("{ deparse(0) }");
        assertEval("{ deparse(0L) }");
        assertEval("{ deparse(-0) }");
        assertEval("{ deparse(-0L) }");
        assertEval("{ deparse(16L) }");
        assertEval("{ deparse(-16L) }");
        assertEval("{ deparse(5i) }");
        assertEval("{ deparse(-5i) }");
        assertEval("{ deparse(199.1234-5i) }");
        assertEval("{ deparse(-199.1234+5.77i) }");
        assertEval("{ deparse(new.env()) }");
        assertEval("{ deparse(NA_integer_) }");
        assertEval("{ deparse(NA_complex_) }");
        assertEval("{ deparse(NA_real_) }");
        assertEval("{ deparse(NA_character_) }");
        assertEval("{ deparse(1:2) }");
        assertEval("{ deparse(1:6) }");
        assertEval("{ deparse(c(1L,2L,3L)) }");
        assertEval("{ deparse(c(1,2,3)) }");
        assertEval("{ deparse(c(NA_integer_, 1L,2L,3L)) }");
        assertEval("{ deparse(c(1L,2L,3L, NA_integer_)) }");
        assertEval("{ deparse(c(3L,2L,1L)) }");
        assertEval("{ deparse(c(-2L,-1L,0L,1L)) }");
        assertEval("{ k <- 2 ; deparse(k) }");
        assertEval("{ deparse(round) }");
        assertEval("{ x<-expression(1); deparse(x) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; deparse(f(c(1,2))) }");
        assertEval("{ f<-function(...) { substitute(list(...)) }; deparse(f(c(x=1,2))) }");
        assertEval("{ f <- function(x) { deparse(substitute(x)) } ; f(a + b * (c - d)) }");
        assertEval("{ f<-function(x) { deparse(x) }; l<-list(7); f(l) }");
        assertEval("{ f<-function(x) { deparse(x) }; l<-list(7, 42); f(l) }");
        assertEval("{ f<-function(x) { deparse(x) }; l<-list(7, list(42)); f(l) }");
        assertEval("{ deparse(expression(a+b, c+d)) }");

        assertEval("{ f <- function() 23 ; deparse(f) }");
        assertEval("{ deparse(nrow) }");
        // should deparse as structure(...
        assertEval("{ e <- new.env(); assign(\"a\", 1, e); assign(\"b\", 2, e); le <- as.list(e); deparse(le)}");
        assertEval("{ deparse(function (a1, a2, a3) if (!(a1 || a2) && a3) 42 else 7) }");

        assertEval("unserialize(serialize(quote(!(a <- TRUE)), NULL))");
        assertEval("unserialize(serialize(quote(a[a <- TRUE]), NULL))");

        assertEval("{ x<-c(a=42, b=7); deparse(x) }");
    }

    @Test
    public void testIsValidName() {
        assertFalse(RDeparse.isValidName(""));
        assertFalse(RDeparse.isValidName("7"));
        assertTrue(RDeparse.isValidName(".f7"));
        assertFalse(RDeparse.isValidName(".7"));
        assertTrue(RDeparse.isValidName("x7_y.z"));
        assertFalse(RDeparse.isValidName("x%"));
        assertTrue(RDeparse.isValidName("..."));
        assertFalse(RDeparse.isValidName("while"));
    }
}
