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
public class TestBuiltin_sum extends TestBase {

    @Test
    public void testsum1() {
        assertEval("argv <- list(structure(313, .Names = ''));sum(argv[[1]]);");
    }

    @Test
    public void testsum2() {
        assertEval("argv <- list(structure(c(-3.02896519757699, -2.37177231827064, -2.49295252901048, -2.99672420295655, -2.59773735414265, -2.26026537208028, -2.74080517809177, -3.785668787425, -2.80120311135215, -2.57773983108655, -5.06092522358575, -2.25629807618983), .Names = c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23')));sum(argv[[1]]);");
    }

    @Test
    public void testsum3() {
        assertEval("argv <- list(structure(c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), class = 'table', .Dim = 60L, .Dimnames = structure(list(r = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60')), .Names = 'r')));sum(argv[[1]]);");
    }

    @Test
    public void testsum4() {
        assertEval("argv <- list(structure(c(-1.94895827232912e-306, 0, 9.36477567902783e-210, 3.61651164350633e-272, 0, -6.24957292845831e-288, 8.01866432306958e-186, 8.68951728615672e-228, -4.51587577314873e-307, 3.44824066264446e-249, -3.07734214990199e-295, -1.63737039109249e-287, -6.4228533959362e-323, 1.99299936577426e-196, -3.77967355768316e-310, 2.99503783387623e-261, 3.69173164797792e-278, -1.16866860094119e-314, 3.18619635538936e-115, -9.28843414181544e-322, 2.14105998923225e-270, 0, 5.16415548974996e-245, 6.63795852562496, 0, 0, 0, -3.94804275684608e-291, 5.96425287032638e-268, 7.18607375106076e-283, 6.05285733998884e-274, 3.00167530091305e-245, -1.10890415661145e-316, -2.83304044404219e-287, 2.03740072057053e-254, 7.14727745939762e-228, 1.98119254926182e-280, 0, -4.86004310565019e-285, -5.29597124993551e-297, 4.62156398366003e-269, 0, 0, 4.73760851736069e-283, -5.12888870803705e-287, -1.74386324923243e-285, -1.06055701952213e-300, 1.32316178368225e-266, 0, 1.3776952356639e-276, 1.33745692946041e-273, 3.1799923028917e-275, 0, 6.14747062861386e-255, -8.73416235737445e-295, 5.68676829309248e-139, 1.04052519425852e-222, -4.06077295726178e-297, -4.44772889827087e-294, 0), .Dim = 60L, .Dimnames = list(c('1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1'))));sum(argv[[1]]);");
    }

    @Test
    public void testsum5() {
        assertEval("argv <- list(c(2.24999999999999, 0.809999999999997, 3.24, 2.89, 0.0899999999999994, 1.96, 6.25, 0.490000000000001, 9.00000000000001, 0.00999999999999993, 0.249999999999998, 4.41000000000001, 3.24, 3.60999999999999));sum(argv[[1]]);");
    }

    @Test
    public void testsum6() {
        assertEval("argv <- list(structure(c(42L, 1L, 0L, 16L, 84L, 0L, 3L, 0L, 0L), .Dim = c(3L, 3L), .Dimnames = structure(list(c('(0,2.5]', '(2.5,4]', NA), c('(2,5.5]', '(5.5,10]', NA)), .Names = c('', '')), class = 'table'));sum(argv[[1]]);");
    }

    @Test
    public void testsum7() {
        assertEval("argv <- list(structure(c(TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(101L, 3L), .Dimnames = list(NULL, c('t1', '10 * t1', 't1 - 4')), .Tsp = c(1, 101, 1), class = c('mts', 'ts', 'matrix')));sum(argv[[1]]);");
    }

    @Test
    public void testsum8() {
        assertEval("argv <- list(structure(c(1, 0, 0, 0, 1, 0, 0, 0, 1), .Dim = c(3L, 3L)), extrarg = FALSE);sum(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsum9() {
        assertEval("sum( );");
    }

    @Test
    public void testsum10() {
        assertEval("argv <- list(structure(c(1.40573809911564e-05, 2.82275077233781e-31, 298891091593544, 127.398214346512, 1.96341150816547e-14, 0.100886417276321, 18137216711891664896, 75471627676.8224, 1.0423615801344e-05, 4184414.04436152, 0.00284364073413795, 0.124845661512668, 6.26689770279226e-09, 137507212543972912, 2.4515242751977e-06, 17279.5247449809, 9.31653241445582, 2.95260115615331e-07, 2.47540394744834e+33, 1.06282257391682e-08, 280.577969261283, 2.2306055461863e-12, 28194894.3770164, 3.27212932994522e+56, 2.35217223982607e-15, 1.93298541124412e-16, 3.82110080220967e-22, 0.020664532453814, 838.952367401989, 1.48989538272057, 58.0422958839475, 25315229.2305008, 1.14418993503202e-07, 0.141089233086962, 385551.97528297, 72589269069.5057, 3.63818589408037, 3.93055539198144e-15, 0.484224006687325, 0.00122384090262982, 509.681530848699, 1.09212481089264e-13, 4.20671904141446e-12, 1.41116557356417, 0.161225941178553, 0.369883152940699, 0.000211066453902523, 1536.88854142326, 1.21220206873588e-13, 18.2818077643159, 67.5636713643055, 33.0891402079429, 1.17150909115673e-23, 304202.728006006, 0.00353331875245541, 4.32607156718415e+28, 776642523346.066, 0.00115835488031795, 0.00496146869860724, 5.31574527522895e-12), .Dim = 60L, .Dimnames = list(c('1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1'))));sum(argv[[1]]);");
    }

    @Test
    public void testsum11() {
        assertEval("argv <- list(1:10, 1+1i);sum(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsum12() {
        assertEval("argv <- list(structure(c(2.19172926547288, 0.330877282992322, 2.43947034322552, 1.27858232527863, 1.34995699383497, 2.31589710162493, -0.100056357529734, 2.12383938264747, 1.1838950457512, 2.61258053432978, -0.0427077741902965, 3.32267292229042, 2.72078100200188, 1.50121623996352, 0.286636947273328, 0.276805761557996, 1.58770673719377, 1.2835349906538, 2.21171702752298, 0.371840556806206, 0.185048028214896, 0.083459109156465, 0.189963621072562, 0.342346999660209, -0.144296693248728, 0.352178185375541, 0.217818647266003, 2.22181318666033, 0.121145321065238, 0.289914009178438, 0.257143390127332, -0.0394307122851858, 2.17200902055199, 0.229288363933891, 1.6805330236497, -0.20328380754072, 0.25058926631711, 1.33428464681067, -0.00502156228152353, NaN, 3.26336745706726, 1.9882766360458, 0.117868259160127, 2.69303413653791, 2.56113239793806, 0.265336044890108, 3.07945609430521, 0.221095709171114, 0.0408573043900261, 0.278444292510551, 1.33428464681067, 1.06801925109484, 0.209625992503226, 3.06212550875829, 0.116229728207572, 1.39280910631599, 2.53602717112413, 0.0457728972476921, 4.7651657207987, 0.194879213930228, 0.114591197255017, 3.26336745706726, 0.0883747020141311, 1.88846207251023, 0.119506790112683, 1.87250223559585, 2.35968057412311, 0.29974519489377, 0.29974519489377, 0.031026118674694, 1.47173382020508, 2.41624560055216, 0.167024187736788, 3.40508627615607, 3.34290799326704, 0.0113637472440299, 0.0588811448681349, 1.54990439966979, 1.35522847629799, 3.07945609430521, 0.224372771076225, 0.129337975828015, 2.99455268975735, 2.83681720000879, 0.0506884901053581, 0.334154344897433, 3.91642660780075, 1.17486192682541, 2.77775688906243, 0.194879213930228, 1.77308062628823, 0.298106663941215, 1.45438038819124, 0.193240682977673, 3.30267449277716, 1.38194860291798, 1.66007135758589, 5.1096442273052, 0.337431406802543, 0.363647902043429), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93', '94', '95', '96', '97', '98', '99', '100')));sum(argv[[1]]);");
    }

    @Test
    public void testsum13() {
        assertEval("argv <- list(numeric(0));sum(argv[[1]]);");
    }

    @Test
    public void testsum14() {
        assertEval("argv <- list(c(49, 61, NA, NA));sum(argv[[1]]);");
    }

    @Test
    public void testsum15() {
        assertEval("argv <- list(1073741824L, 1073741824L, 0);sum(argv[[1]],argv[[2]],argv[[3]]);");
    }

    @Test
    public void testsum16() {
        assertEval(Ignored.Unknown, "argv <- list(1073741824L, 1073741824L);sum(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testsum17() {
        assertEval("argv <- list(structure(c(TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Tsp = c(1, 101, 1), class = 'ts'));sum(argv[[1]]);");
    }

    @Test
    public void testsum18() {
        assertEval(Ignored.Unknown, "argv <- list(c(1073741824L, 1073741824L));sum(argv[[1]]);");
    }

    @Test
    public void testsum19() {
        assertEval("argv <- list(structure(c(0.0946626763551857, 1.56832184548816e-06, 0.226697441774756, 0.00453708504256956, 0.258320521579932, 2.57680524978307, 0.467122524211721, 5.16534267196331, 0.694563006492192, 0.197723848046524, 0.000799319848162311, 0.000570944537286636, 0.0654689540726797, 0.000146788076901938, 0.00669686464041458, 0.00765355286634145, 0.0786604017778045, 1.25812820036403e-06, 0.167435582495234, 0.00356206279639504, 0.25547689715822, 2.72165076185825, 0.488128793070721, 3.66078502081211, 0.898984802200849, 0.190804322887854, 0.00080933803412378, 0.000578096808847448, 0.0782510283683936, 0.000156595864868186, 0.00698171105046541, 0.00797910506602018), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32')));sum(argv[[1]]);");
    }

    @Test
    public void testsum20() {
        assertEval("argv <- list(c(TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE));sum(argv[[1]]);");
    }

    @Test
    public void testsum21() {
        assertEval("argv <- list(structure(c(6L, 12L, 18L, 24L, 30L, 36L, 42L, 48L, 54L, 60L, 66L, 72L, 78L, 84L, 90L, 96L, 102L, 108L, 114L, 120L), .Dim = 4:5, .Dimnames = list(NULL, c('V5', 'V6', 'V7', 'V8', 'V9'))));sum(argv[[1]]);");
    }

    @Test
    public void testsum23() {
        assertEval("argv <- list(2, 3, NA);do.call('sum', argv)");
    }

    @Test
    public void testSum() {
        assertEval("{ sum() }");
        assertEval("{ sum(0, 1, 2, 3) }");
        assertEval("{ sum(c(0, 1, 2, 3)) }");
        assertEval("{ sum(c(0, 1, 2, 3), 4) }");
        assertEval("{ sum(1:6, 3, 4) }");
        assertEval("{ sum(1:6, 3L, TRUE) }");
        assertEval("{ `sum`(1:10) }");
        assertEval("{ x<-c(FALSE, FALSE); is.double(sum(x)) }");
        assertEval("{ x<-c(FALSE, FALSE); is.integer(sum(x)) }");

        assertEval("{ is.logical(sum(TRUE, FALSE)) }");
        assertEval("{ is.logical(sum(TRUE)) }");
        assertEval("{ sum(as.raw(42), as.raw(7)) }");
        assertEval("{ sum(42+42i, 7+7i) }");
        assertEval("{ sum(\"42\", \"7\") }");

        assertEval("{ sum(as.double(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.double(NA), na.rm=FALSE) }");
        assertEval("{ sum(as.double(NA), as.double(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.double(NA), as.double(NA), na.rm=FALSE) }");
        assertEval("{ sum(as.integer(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.integer(NA), na.rm=FALSE) }");
        assertEval("{ sum(as.integer(NA), as.integer(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.integer(NA), as.integer(NA), na.rm=FALSE) }");
        assertEval("{ sum(as.character(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.character(NA), na.rm=FALSE) }");
        assertEval("{ sum(as.character(NA), as.character(NA), na.rm=TRUE) }");
        assertEval("{ sum(as.character(NA), as.character(NA), na.rm=FALSE) }");
        assertEval("{ sum(42L, as.integer(NA), na.rm=TRUE) }");
        assertEval("{ sum(42L, as.integer(NA), na.rm=FALSE) }");
        assertEval("{ sum(42, as.double(NA), na.rm=TRUE) }");
        assertEval("{ sum(42, as.double(NA), na.rm=FALSE) }");
        assertEval("{ sum(\"42\", as.character(NA), na.rm=TRUE) }");
        assertEval("{ sum(\"42\", as.character(NA), na.rm=FALSE) }");
        assertEval("{ sum(42L, as.integer(NA), 7L, na.rm=TRUE) }");
        assertEval("{ sum(42L, as.integer(NA), 7L, na.rm=FALSE) }");
        assertEval("{ sum(42, as.double(NA), 7, na.rm=TRUE) }");
        assertEval("{ sum(42, as.double(NA), 7, na.rm=FALSE) }");
        assertEval("{ sum(\"42\", as.character(NA), \"7\", na.rm=TRUE) }");
        assertEval("{ sum(\"42\", as.character(NA), \"7\", na.rm=FALSE) }");

        assertEval("{ sum(0, 1[3]) }");
        assertEval("{ sum(na.rm=FALSE, 0, 1[3]) }");
        assertEval("{ sum(0, na.rm=FALSE, 1[3]) }");
        assertEval("{ sum(0, 1[3], na.rm=FALSE) }");
        assertEval("{ sum(0, 1[3], na.rm=TRUE) }");
        assertEval("{ sum(1+1i,2,NA, na.rm=TRUE) }");

        assertEval("sum(v <- 42)");
    }
}
