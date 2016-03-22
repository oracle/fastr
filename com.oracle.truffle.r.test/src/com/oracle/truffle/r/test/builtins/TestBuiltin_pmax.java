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
public class TestBuiltin_pmax extends TestBase {

    @Test
    public void testpmax1() {
        assertEval("argv <- list(FALSE, 5L, 12); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax2() {
        assertEval("argv <- list(FALSE, -100, structure(c(-Inf, 82.9775012103133, 8.55983483385341e+101, -Inf, 79.3831968838961, 8.55983483385341e+101), .Names = c('', '', '', '', '', ''))); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax3() {
        assertEval("argv <- list(FALSE, c(0L, 1L, 1L, 1L, 2L), 5L, c(6L, 5L, 5L, 5L, 4L)); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmax4() {
        assertEval("argv <- list(FALSE, 0, numeric(0)); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax5() {
        assertEval("argv <- list(FALSE, structure(c(63.5607991023966, 46.8465846258113, 40.7088275958184, 31.3395189414991, 42.5666751143734, 47.0610532806931, 23.9315410227325, 43.0690616089581, 66.7869292908986, 49.2243580808943, 31.6784834018036, 24.3875466143556, 48.4619434336134, 53.5787701502931, 25.0466211495357, 45.0758464889871, 66.9256619232735, 49.3266089980428, 31.7843035976521, 24.4690118450696, 50.7406402769298, 56.0980619029545, 17.201254072711, 30.956714016252), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24')), 2.22044604925031e-16); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax6() {
        assertEval("argv <- list(FALSE, FALSE, FALSE); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax7() {
        assertEval("argv <- list(FALSE, 1L, c(15L, 15L, 15L, 15L, 15L, 15L, 15L, 15L, 15L)); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax8() {
        assertEval("argv <- list(FALSE, structure(c(0.0193057433072215, 0.00434780301273374, 0.0549750394687487, 0.510714717273168, 0.0482077179041234, 0.349752997299534, 0.15114556457294, 0.614610341225044, 0.270367074042314, 0.376738504472563, 0.00100006670765362, 0.616978737736246, 0.000115089535300671, 0.114479803728228, 0.0345012755277619, 0.520238904129887, 0.0177036726480846, 0.00345369763623826, 0.0372744005491215, 0.245210198359521, 0.0651842100459408, 0.4506670448926, 0.178923774229777, 0.332256206500317, 0.402299202627705, 0.380395198873703, 0.000984316947253816, 0.403063829062269, 0.000174431720286923, 0.138958543973059, 0.0379750520636422, 0.379247258699123), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32')), 0); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax9() {
        assertEval("argv <- list(FALSE, structure(c(35.2592591597479, 59.4999999843455, 12.4507044164935, 2.53543312099158, 10.3703703404756, 42.0000005728299, 8.14084538858294, 34.04724471918, 7.77778142338517, 26.9999999889474, 6.70422536805755, 3.62204828940961, 2.59259259558406, 14.4999999939529, 6.70422536805755, 5.79527724426002, 32.7407408614199, 59.5000000376209, 13.54929592464, 4.46456690511876, 9.62962966454155, 42.0000006104361, 8.85915523787816, 59.9527554977598, 7.22222565443263, 27.0000000131229, 7.29577463400041, 6.37795443616981, 2.40740742585304, 14.500000006936, 7.29577463400041, 10.2047270647755), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32')), 2.22044604925031e-16); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax10() {
        assertEval("argv <- list(FALSE, c(1.05, 1.92, 0.36, 4.98, 4.56, 0.69, -5.97, 1.26, 5.58, -0.06, -4.92, -1.38, -0.3, 3.75, 1.11, 0.93, 3.33, 4.95, 0.99, 2.67, -0.75, -2.61, -0.66, 2.13, -6.78, 2.31, -0.15, 0.96, -1.92, 1.17, 0.57, -4.86, 1.11, 0.06, 2.91, -7.86, 0.45, 4.65, -4.23, -7.05, -1.29, 1.71, -1.98, -0.24, 0.06, 0.72, -0.99, -0.09, -3.39, 0.96, 4.65, 6.39, -0.3, -0.96, -2.01, 4.32, 0.12, -3.3, -2.85, -0.57, -2.04, -1.29, -2.52, 2.07, -1.95, 2.13, 0.57, 1.35, 1.35, -3.57, 3.9, 0.42, -1.08, -1.5, -1.41, -3.93, -3.06, 3.51, 4.53, -0.99, -0.03, -1.77, -0.84, -0.54, -3.21, 1.98, -2.13, 5.64, -0.42, -0.57, 2.52, 1.32, 3.99, -0.6, -1.35, 4.38, 3, -3.06, 2.04, 2.52), 0); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax11() {
        assertEval("argv <- list(FALSE, c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L), 7L, c(7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 6L)); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testpmax12() {
        assertEval("argv <- list(FALSE, 1:7, structure(c(2, 3, 4, 2, 2, 2), .Dim = c(3L, 2L), .Dimnames = list(NULL, c('a', '')))); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testpmax13() {
        assertEval("argv <- list(FALSE, c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE)); .Internal(pmax(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testpmax14() {
        assertEval("argv <- list(FALSE, structure(c(0, 0, -0.0906283137921162, -0.0801994352402973, -0.0235093686536505, -0.131187875867331, -0.131187875867331, -0.131187875867331, -0.131187875867331, 0, 0, 0, -0.106539777104723, -0.106539777104723, -0.106539777104723, 0, 0, 0.126786975893341, 0.126786975893341, 0.126786975893341, 0, -0.131187875867331, -0.131187875867331, -0.131187875867331, 0, -0.106539777104723, -0.106539777104723, -0.106539777104723, 0, 0, 0, -0.106539777104723, 0.172297822926899, 0.172297822926899, 0, 0, 0, 0, 0, -0.106539777104723, -0.106539777104723, -0.106539777104723, -0.106539777104723, 0, 0, 0, 0.172297822926899, 0.172297822926899), .Dim = c(12L, 4L)), 0); .Internal(pmax(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testPMax() {
        assertEval("{ pmax(c(1L, 7L), c(42L, 1L)) }");
        assertEval("{ pmax(c(1L, 7L), integer()) }");
        assertEval(Output.ContainsWarning, "{ pmax(c(1L, 7L, 8L), c(1L), c(42L, 1L)) }");
        assertEval("{ pmax(c(1L, 7L), c(42L, as.integer(NA))) }");
        assertEval("{ pmax(c(1L, 7L), c(42L, as.integer(NA)), na.rm=TRUE) }");

        assertEval("{ pmax(c(1, 7), c(42, 1)) }");
        assertEval("{ pmax(c(1, 7), double()) }");
        assertEval(Output.ContainsWarning, "{ pmax(c(1, 7, 8), c(1), c(42, 1)) }");
        assertEval("{ pmax(c(1, 7), c(42, as.double(NA))) }");
        assertEval("{ pmax(c(1, 7), c(42, as.double(NA)), na.rm=TRUE) }");

        assertEval("{ pmax(c(\"1\", \"7\"), c(\"42\", \"1\")) }");
        assertEval("{ pmax(c(\"1\", \"7\"), character()) }");
        assertEval(Output.ContainsWarning, "{ pmax(c(\"1\", \"7\", \"8\"), c(\"1\"), c(\"42\", \"1\")) }");
        assertEval("{ pmax(c(\"1\", \"7\"), c(\"42\", as.character(NA))) }");
        assertEval("{ pmax(c(\"1\", \"7\"), c(\"42\", as.character(NA)), na.rm=TRUE) }");
        assertEval("{ pmax(c(\"1\", as.character(NA)), c(\"42\", \"1\"), na.rm=TRUE) }");
        assertEval("{ pmax(c(\"1\", as.character(NA)), c(as.character(NA), as.character(NA)), c(\"42\", \"1\"), na.rm=TRUE) }");

        assertEval("{ pmax(c(FALSE, TRUE), c(TRUE, FALSE)) }");
        assertEval("{ pmax(c(FALSE, TRUE), logical()) }");
        assertEval("{ pmax(c(FALSE, TRUE), c(FALSE, NA)) }");

        assertEval(Output.ContainsError, "{ pmax(as.raw(42)) }");
        assertEval(Output.ContainsError, "{ pmax(7+42i) }");
    }
}
