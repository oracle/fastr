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

//Checkstyle: stop line length check
public class TestBuiltin_seq_along extends TestBase {
    @Test
    public void testseq1() {
        assertEval("argv <- list(c('y', 'A', 'U', 'V'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq2() {
        assertEval("argv <- list(structure(c(1, 2, 3, 0, 10, NA), .Dim = c(3L, 2L)));do.call('seq_along', argv);");
    }

    @Test
    public void testseq3() {
        assertEval("argv <- list(0L);seq_len(argv[[1]]);");
    }

    @Test
    public void testseq4() {
        assertEval("argv <- list(structure(list(x = 1:3, y = structure(1:3, .Label = c('A', 'D', 'E'), class = 'factor'), z = c(6, 9, 10)), .Names = c('x', 'y', 'z'), row.names = c(NA, -3L), class = 'data.frame'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq7() {
        assertEval("argv <- list(c(TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE));do.call('seq_along', argv);");
    }

    @Test
    public void testseq8() {
        assertEval("argv <- list(structure(list(levels = c('1', '2', NA), class = 'factor'), .Names = c('levels', 'class')));do.call('seq_along', argv);");
    }

    @Test
    public void testseq9() {
        assertEval("argv <- list(list(c(1+1i, 2+1.4142135623731i, 3+1.73205080756888i, 4+2i, 5+2.23606797749979i, 6+2.44948974278318i, 7+2.64575131106459i, 8+2.82842712474619i, 9+3i, 10+3.1622776601684i)));do.call('seq_along', argv);");
    }

    @Test
    public void testseq10() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, NA, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, NA, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52), .Dim = c(98L, 2L), .Dimnames = list(NULL, c('intercept', 'trend'))));do.call('seq_along', argv);");
    }

    @Test
    public void testseq11() {
        assertEval("argv <- list(list(structure(c(112, 118, 132, 129, 121, 135, 148, 148, 136, 119, 104, 118, 115, 126, 141, 135, 125, 149, 170, 170, 158, 133, 114, 140, 145, 150, 178, 163, 172, 178, 199, 199, 184, 162, 146, 166, 171, 180, 193, 181, 183, 218, 230, 242, 209, 191, 172, 194, 196, 196, 236, 235, 229, 243, 264, 272, 237, 211, 180, 201, 204, 188, 235, 227, 234, 264, 302, 293, 259, 229, 203, 229, 242, 233, 267, 269, 270, 315, 364, 347, 312, 274, 237, 278, 284, 277, 317, 313, 318, 374, 413, 405, 355, 306, 271, 306, 315, 301, 356, 348, 355, 422, 465, 467, 404, 347, 305, 336, 340, 318, 362, 348, 363, 435, 491, 505, 404, 359, 310, 337, 360, 342, 406, 396, 420, 472, 548, 559, 463, 407, 362, 405, 417, 391, 419, 461, 472, 535, 622, 606, 508, 461, 390, 432), .Tsp = c(1949, 1960.91666666667, 12), class = 'ts'), structure(c(419.147602949539, 391.474665943444, 435.919286153217, 443.935203034261, 455.023399013445, 517.28707821144, 589.71337277669, 582.999919227301, 484.573388713116, 428.878182738437, 368.526582998452, 406.728709993152, 415.660571294428, 388.716535970235, 433.006017658935, 440.885684396326, 451.651900136866, 513.051252429496, 584.327164324967, 577.055407135124, 479.076505013118, 423.494870357491, 363.43932958967, 400.592058645117), .Tsp = c(1961, 1962.91666666667, 12), class = 'ts'), structure(c(484.030717075782, 462.954959541421, 526.353307750503, 546.165638262644, 569.502470928676, 657.838443307596, 761.241730163307, 763.280655335144, 642.989004951864, 576.423799567567, 501.429012064338, 559.981301364233, 591.700754553767, 565.210772316967, 642.377841008703, 666.682421047093, 695.547100430962, 804.065022775202, 931.340589597203, 934.837830059897, 788.422986194072, 707.666678543854, 616.37838266375, 689.250456425465), .Tsp = c(1961, 1962.91666666667, 12), class = 'ts')));do.call('seq_along', argv);");
    }

    @Test
    public void testseq12() {
        assertEval("argv <- list(c(0.923879532511287+0.38268343236509i, 0.707106781186548+0.707106781186547i, 0.38268343236509+0.923879532511287i, 0+1i, -0.38268343236509+0.923879532511287i, -0.707106781186547+0.707106781186548i, -0.923879532511287+0.38268343236509i, -1+0i, -0.923879532511287-0.38268343236509i, -0.707106781186548-0.707106781186547i, -0.38268343236509-0.923879532511287i, 0-1i, 0.38268343236509-0.923879532511287i, 0.707106781186547-0.707106781186548i, 0.923879532511287-0.38268343236509i, 1-0i));do.call('seq_along', argv);");
    }

    @Test
    public void testseq14() {
        assertEval("argv <- list(structure(list(g = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L), .Label = c('1', '2', '3', '4'), class = 'factor')), .Names = 'g'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq15() {
        assertEval("argv <- list(structure(list(20), row.names = c(NA, -1L)));do.call('seq_along', argv);");
    }

    @Test
    public void testseq16() {
        assertEval("argv <- list(list(list(c('', '', '\\036', '', 'New', 'print()', '(S3)', 'method', 'for', 'class', '\\\'function\\\',', '', '', '', '', '', '', '', 'also', 'used', 'for', 'auto-printing.', '', 'Further,', '.Primitive', '', '', '', '', '', '', '', 'functions', 'now', 'print', 'and', 'auto-print', 'identically.', '', 'The', 'new', 'method', '', '', '', '', '', '', '', 'is', 'based', 'on', 'code', 'suggestions', 'by', 'Romain', 'François.'))));do.call('seq_along', argv);");
    }

    @Test
    public void testseq17() {
        assertEval("argv <- list(structure(list(Topic = c('myTst-package', 'foo-class', 'myTst', 'show,foo-method', 'show,foo-method', 'show-methods'), File = c('myTst-package', 'foo-class', 'myTst-package', 'foo-class', 'show-methods', 'show-methods')), .Names = c('Topic', 'File'), row.names = c(3L, 1L, 4L, 2L, 6L, 5L)));do.call('seq_along', argv);");
    }

    @Test
    public void testseq18() {
        assertEval("argv <- list(structure(list(structure(' A Simple Plot and Legend Demo ', Rd_tag = 'TEXT')), Rd_tag = 'Rd', class = 'Rd'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq19() {
        assertEval("argv <- list(structure(list(Topic = character(0), File = character(0), Title = character(0), Internal = character(0)), .Names = c('Topic', 'File', 'Title', 'Internal'), row.names = integer(0), class = 'data.frame'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq20() {
        assertEval("argv <- list(structure(c(2.21843970753346, 1.80732678656753, -1.09399175987006, 0.585986462327552, -5.68458926982395, 1.23352238598674, 0.457950438444482, 2.51599006679516, -2.28780372840319, 5.53596062467695, 2.17890565095959, -1.59611751350773, -2.9672978602151, 0.745175851232308, 1.93388282434376, -3.03559459078455, 2.19500990255906, 0.0725275773318347, -0.75336064096447, -1.15505962102859, -2.84782886882524, -1.41070341448251, -0.540252474026749, 4.87719739781058, 0.890715639552621, -0.968642103099399, 1.73177156113283, -0.993218102309356, -0.656454198323984, -1.5299506933835, -0.298424468882268, 6.51011264717937, 2.68326774833378, 1.99295445531679, -0.214079422583434, 6.73505308264589, -4.54579214489424, -2.3991834444486, -1.71479569181251, -6.47293095421849, -1.67116930820449, -11.5853328029437, -2.48588878138021, -0.888857646918452, 8.06807102468956, -0.216046323028316, 6.24682938323398, -1.74761908105831, 2.53082303181417, 2.31410662801887, 2.97453294161523, -2.88723068649699, -1.04144266580674, -0.835536300630093, -6.10229135345437, -4.37605802846523, -1.94289029309402e-16, 5.96619037131792, -1.1474434665393, 3.78819830631063, -3.01580771910632, -0.656454198323984, 1.50824785799851, -2.06401783962239, -3.02346226775125, 0.407243897855763, -3.96478352340807, -2.12718621336067, -0.78924288871239, -3.03559459078455, 0.457950438444496, -0.797900839851943, -3.38233849466459, 1.97815029009903, 0.745175851232309, -1.09645503136389, 0.341748714147263, 7.32472922782987, -1.33672649241008, 1.51931399477032, 0.00590129163826772, -4.09533092706814, 0.195481697042187, -2.7736762657602, -3.48737543915568, 0.536312040203338, 0.775871729180551, 4.37979177946206, 1.30271070089245, 4.2132287611068, 7.33457656622414, 3.28311350719274, -1.30271070089245), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93')));do.call('seq_along', argv);");
    }

    @Test
    public void testseq21() {
        assertEval("argv <- list(structure(list(surname = structure(integer(0), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(integer(0), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(integer(0), .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = integer(0), class = 'data.frame'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq22() {
        assertEval("argv <- list(structure(list(A = 0:10, B = 10:20, `NA` = 20:30), .Names = c('A', 'B', NA), row.names = c(NA, -11L), class = 'data.frame'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq23() {
        assertEval("argv <- list(c(TRUE, TRUE, TRUE));do.call('seq_along', argv);");
    }

    @Test
    public void testseq24() {
        assertEval("argv <- list(structure(c(3, 8), .Dim = 2L, .Dimnames = structure(list(g = c('1', '2')), .Names = 'g'), call = quote(by.data.frame(data = X, INDICES = g, FUN = colMeans)), class = 'by'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq25() {
        assertEval("argv <- list(structure(list(.Data = 'numeric'), .Names = '.Data'));do.call('seq_along', argv);");
    }

    @Test
    public void testseq28() {
        assertEval("argv <- list(structure(list(num = 1:4, fac = structure(11:14,     .Label = c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',         'k', 'l', 'm', 'n', 'o'), class = 'factor'), date = structure(c(15065,     15066, 15067, 15068), class = 'Date'), pv = structure(list(1:3,     4:5, 6:7, 8:10), class = c('package_version', 'numeric_version'))),     .Names = c('num', 'fac', 'date', 'pv'), row.names = c(NA,         -4L), class = 'data.frame'));" +
                        "do.call('seq_along', argv)");
    }

    @Test
    public void testWithNonStandardLength() {
        assertEval("{ x <- c(1,2,3); class(x) <- 'myclass'; length.myclass <- function(w) 42; seq_along(x) }");
        assertEval("{ x <- c(1,2,3); class(x) <- 'myclass'; length.myclass <- function(w) c(100, 200); seq_along(x) }");
        assertEval("{ x <- c(1,2,3); class(x) <- 'myclass'; length.myclass <- function(w) '48'; seq_along(x) }");
        assertEval("{ x <- c(1,2,3); class(x) <- 'myclass'; length.myclass <- function(w) numeric(0); seq_along(x) }");
        assertEval(Output.IgnoreWarningContext, "{ x <- c(1,2,3); class(x) <- 'myclass'; length.myclass <- function(w) 'hello world'; seq_along(x) }");
        assertEval("{ length <- function(x) 42; seq_along(c(1,2,3)) }");
        assertEval("{ assign('length.myclass', function(...) 42, envir=.__S3MethodsTable__.); x <- 1; class(x) <- 'myclass'; res <- seq_along(x); rm('length.myclass', envir=.__S3MethodsTable__.); res }");
    }
}
