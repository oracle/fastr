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
public class TestBuiltin_drop extends TestBase {

    @Test
    public void testdrop1() {
        assertEval("argv <- list(structure(c(1.50117937791368, 1.64775918264914, 1.38492642933039, 2.16331573723593, 2.09255307288088, 1.44052566560935, 0.318431987979291, 1.53656071009121, 2.26440525774314, 1.31416376497534, 0.495338648866914, 1.09176681985947, 1.27372795677245, 1.95608222019614, 1.5112883299644, 1.48096147381224, 1.88531955584109, 2.15826126121057, 1.49107042586296, 1.77412108328316, 1.19791081639204, 0.884533302819684, 1.21307424446813, 1.68314051482667, 0.181961135294554, 1.71346737097883, 1.29900033689926, 1.4860159498376, 1.00078625140298, 1.52139728201513, 1.42030776150791, 0.505447600917635, 1.5112883299644, 1.33438166907678, 1.81455689148604, -1.61842961254877e-09, 1.40008985740647, 2.10771650095696, 0.611591597450209, 0.136470851066308, 1.10693024793555, 1.61237785047162, 0.990677299352257, 1.28383690882317, 1.33438166907678, 1.44558014163472, 1.15747500818916, 1.30910928894998, 0.753116926160307, 1.4860159498376, 2.10771650095696, 2.40087611042788, 1.27372795677245, 1.16252948421452, 0.985622823326897, 2.05211726467799, 1.3444906211275, 0.768280354236389, 0.844097494616799, 1.22823767254421, 0.980568347301536, 1.10693024793555, 0.899696730895766, 1.67303156277594, 0.995731775377618, 1.68314051482667, 1.42030776150791, 1.55172413816729, 1.55172413816729, 0.722790070008143, 1.98135460032294, 1.39503538138111, 1.14231158011308, 1.07154891575803, 1.08671234383411, 0.662136357703815, 0.808716162439274, 1.91564641199326, 2.08749859685552, 1.15747500818916, 1.3192182410007, 1.02605863152978, 1.18274738831596, 1.23329214856957, 0.783443782312471, 1.65786813469986, 0.965404919225454, 2.27451420979386, 1.25351005267101, 1.22823767254421, 1.74884870315635, 1.54666966214193, 1.99651802839903, 1.22318319651885, 1.09682129588483, 2.06222621672871, 1.82972031956212, 0.808716162439274, 1.66797708675058, 1.74884870315635), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93', '94', '95', '96', '97', '98', '99', '100'))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop2() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE), .Dim = c(40L, 1L), .Dimnames = list(c('r1', 'r2', 'r3', 'r4', 'r5', 'r6', 'r7', 'r8', 'r9', 'r10', 'r11', 'r12', 'r13', 'r14', 'r15', 'r16', 'r17', 'r18', 'r19', 'r20', 'r21', 'r22', 'r23', 'r24', 'r25', 'r26', 'r27', 'r28', 'r29', 'r30', 'r31', 'r32', 'r33', 'r34', 'r35', 'r36', 'r37', 'r38', 'r39', 'r40'), 'c1'))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop3() {
        assertEval("argv <- list(structure(c(-0.146170181357627, 24.3243243243243, NA, 84.2105263157895, 2.13784643479304), .Dim = c(5L, 1L))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop4() {
        assertEval(Ignored.Unknown, "argv <- list(structure(1, .Dim = c(1L, 1L))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop5() {
        assertEval(Ignored.Unknown, "argv <- list(structure(1:4, .Dim = c(4L, 1L), .Dimnames = list(c('a', 'b', 'c', 'd'), NULL))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop6() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0.0394556761478965, 0.0353930191803619, 0.0421772348795039, 0.0302920451103359, 0.0377457762283076, 0.0338591835815583, 0.0403493901288877, 0.0289792716248635, 0.0342125137383397, 0.0306897327119817, 0.0365724115929804, 0.0262666138482847, 0.0422657427158027, 0.0379137398889158, 0.0451811331581029, 0.032449470138841), .Dim = c(1L, 4L, 4L), .Dimnames = list('1', c('DAX', 'SMI', 'CAC', 'FTSE'), c('DAX', 'SMI', 'CAC', 'FTSE')))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop7() {
        assertEval("argv <- list(c(10.8924449093617, 19.1956646477802, 5.83862354833301, 8.94491073999977, 10.0151293814506)); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop8() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0'))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop9() {
        assertEval("argv <- list(structure(FALSE, .Tsp = c(1, 1, 1), class = 'ts')); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop10() {
        assertEval("argv <- list(structure(c(-2.12168716972669e-05, 7.51519194600216e-05, -6.21732236176711e-06), .Dim = c(3L, 1L))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop11() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(' 16', ' 16', '144', ' 16', ' 16', '128', ' 16', ' 16', '112', ' 16'), .Dim = 10L, .Dimnames = structure(list(c('1', '6', '7', '8', '13', '14', '15', '20', '21', '22')), .Names = ''))); .Internal(drop(argv[[1]]))");
    }

    @Test
    public void testdrop13() {
        assertEval("argv <- structure(list(x = structure(c(8, 4, 2), .Dim = c(3L,     1L))), .Names = 'x');do.call('drop', argv)");
    }

    @Test
    public void testDrop() {
        assertEval("{ x <- array(1:12, dim = c(1,3,1,1,2,1,2)); drop(x) }");
    }
}
