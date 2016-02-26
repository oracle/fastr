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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestBuiltin_typeof extends TestBase {

    @Test
    public void testtypeof1() {
        assertEval("argv <- list(structure(c(1.23315637025077, 1.2394120713065, 1.46775472234056, 1.3131362441571, 0.834956748134462, 1.32096970624938, 1.38337762052736, 1.70911044284799, 1.23539395906175, 0.858346823253787, 1.52283660048288, 1.31313061976496, 1.12786658428867, 1.44366133143941, 1.27218165569433, 1.26731245914676, 1.42168796660342, 1.48945666726, 2.09136324227313, 1.36556393622446, 1.19497185571598, 1.3558872236906, 1.28486176009175, 0.896505382640118, 1.2920383545426, 1.43713738151143, 1.28325876023887, 1.8647069237969, 1.28069027865337, 1.3282363039678, 1.4132805261745, 0.646368078716031, 1.17057458108707, 1.35016461104197, 1.35509309393051, 0.62815321214884, 0.933778507736315, 1.38267166577057, 1.7643327299387, 0.662074713268515, 1.31638314484599, 0.127879987991043, 1.19108675802219, 1.27268759462974, 0.4383313914982, 1.4144264042562, 0.693758539302211, 1.47501143044129, 1.18104902231565, 1.31313716894023, 1.16251137109995, 1.33271580458282, 1.2645836556729, 1.27403739912758, 0.707073961081345, 1.02664693047896, NaN, 0.753985804351041, 1.38430649521587, 1.07546693634877, 1.19187230661588, 1.28069027865337, 1.31026717493666, 1.21822955912256, 1.13243112343561, 1.63256872758035, 1.02552404019857, 1.20828070506052, 1.33930727426782, 1.26731245914676, 1.38337762052736, 1.52793749920214, 1.07081398391753, 1.24912672913647, 1.44366133143941, 1.2823536700583, 1.38311795520175, 0.534347523417853, 1.25766711144813, 1.92388210662423, 1.52790220067279, 1.10639731743869, 1.88278431408355, 1.17178985993101, 1.13471940645093, 1.33429991787085, 1.59592895672966, 0.952232923176189, 2.67777307729144, 0.98546699757923, 0.534607888905458, 1.18840135978238, 2.67777307729144), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof2() {
        assertEval("argv <- list(structure(c(1960, 1960, 1960, 1960, 1960, 1960, 1960, 1960, 1960, 1960, 1960, 1961, 1961, 1961, 1961, 1961, 1961, 1961, 1961, 1961), .Tsp = c(1960.08333333333, 1961.66666666667, 12), class = 'ts')); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof3() {
        assertEval("argv <- list(structure(c(NA, 9, 3, 3), .Names = c('<none>', 'Hair:Eye', 'Hair:Sex', 'Eye:Sex'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof4() {
        assertEval("argv <- list(structure(function (x, y = NULL) standardGeneric('tcrossprod'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof5() {
        assertEval("argv <- list(structure(c('Min.   :    0.060  ', '1st Qu.:    0.320  ', 'Median :    0.630  ', 'Mean   :  909.592  ', '3rd Qu.:    0.905  ', 'Max.   :10000.000  '), .Dim = c(6L, 1L), .Dimnames = list(c('', '', '', '', '', ''), '      x'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof6() {
        assertEval("argv <- list(structure(c(-0.0529307911108286, -0.200175675120066), .Names = c('(Intercept)', 'xTRUE'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof7() {
        assertEval("argv <- list(complex(0)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof8() {
        assertEval("argv <- list(structure(list(is.array = FALSE, is.atomic = FALSE, is.call = FALSE, is.character = FALSE, is.complex = FALSE, is.data.frame = FALSE, is.double = FALSE, is.environment = FALSE, is.expression = FALSE, is.factor = FALSE, is.finite = NA, is.function = FALSE, is.infinite = NA, is.integer = FALSE, is.language = FALSE, is.list = TRUE, is.logical = FALSE, is.matrix = FALSE, is.na = structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('coefficients', 'residuals', 'effects', 'rank', 'fitted.values', 'assign', 'qr', 'df.residual', 'xlevels', 'call', 'terms', 'model')), is.name = FALSE, is.nan = NA, is.null = FALSE, is.numeric = FALSE, is.numeric_version = FALSE, is.object = TRUE, is.ordered = FALSE, is.package_version = FALSE, is.pairlist = FALSE, is.primitive = FALSE, is.qr = FALSE, is.raw = FALSE, is.recursive = TRUE, is.symbol = FALSE, is.table = FALSE, is.vector = FALSE), .Names = c('is.array', 'is.atomic', 'is.call', 'is.character', 'is.complex', 'is.data.frame', 'is.double', 'is.environment', 'is.expression', 'is.factor', 'is.finite', 'is.function', 'is.infinite', 'is.integer', 'is.language', 'is.list', 'is.logical', 'is.matrix', 'is.na', 'is.name', 'is.nan', 'is.null', 'is.numeric', 'is.numeric_version', 'is.object', 'is.ordered', 'is.package_version', 'is.pairlist', 'is.primitive', 'is.qr', 'is.raw', 'is.recursive', 'is.symbol', 'is.table', 'is.vector'), class = 'isList')); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof9() {
        assertEval("argv <- list(c(-21.222245139688+176.377752294836i, -21.222245139688-176.377752294836i, 61.0965873274467+76.779430575699i, 61.0965873274467-76.779430575699i, -11.7486843755171+0i)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof10() {
        assertEval("argv <- list(c(NA, '1', NA, '2', '1', NA, NA, '1', '4', '1', NA, '4', '1', '3', NA, '4', '2', '2', NA, '4', '4', '2', '4', '4', '2', '1', '4', '4', '3', '1', '1', '4', '1', '4', NA, '1', '4', '4', '2', '2', '4', '4', '3', '4', '2', '2', '3', '3', '4', '1', '1', '1', '4', '1', '4', '4', '4', '4', NA, '4', '4', '4', NA, '1', '2', '3', '4', '3', '4', '2', '4', '4', '1', '4', '1', '4', NA, '4', '2', '1', '4', '1', '1', '1', '4', '4', '2', '4', '1', '1', '1', '4', '1', '1', '1', '4', '3', '1', '4', '3', '2', '4', '3', '1', '4', '2', '4', NA, '4', '4', '4', '2', '1', '4', '4', NA, '2', '4', '4', '1', '1', '1', '1', '4', '1', '2', '3', '2', '1', '4', '4', '4', '1', NA, '4', '2', '2', '2', '4', '4', '3', '3', '4', '2', '4', '3', '1', '1', '4', '2', '4', '3', '1', '4', '3', '4', '4', '1', '1', '4', '4', '3', '1', '1', '2', '1', '3', '4', '2', '2', '2', '4', '4', '3', '2', '1', '1', '4', '1', '1', '2', NA, '2', '3', '3', '2', '1', '1', '1', '1', '4', '4', '4', '4', '4', '4', '2', '2', '1', '4', '1', '4', '3', '4', '2', '3', '1', '3', '1', '4', '1', '4', '1', '4', '3', '3', '4', '4', '1', NA, '3', '4', '4', '4', '4', '4', '4', '3', '4', '3', '4', '2', '4', '4', '1', '2', NA, '4', '4', '4', '4', '1', '2', '1', '1', '2', '1', '4', '2', '3', '1', '4', '4', '4', '1', '2', '1', '4', '2', '1', '3', '1', '2', '2', '1', '2', '1', NA, '3', '2', '2', '4', '1', '4', '4', '2', '4', '4', '4', '2', '1', '4', '2', '4', '4', '4', '4', '4', '1', '3', '4', '3', '4', '1', NA, '4', NA, '1', '1', '1', '4', '4', '4', '4', '2', '4', '3', '2', NA, '1', '4', '4', '3', '4', '4', '4', '2', '4', '2', '1', '4', '4', NA, '4', '4', '3', '3', '4', '2', '2', '4', '1', '4', '4', '4', '3', '4', '4', '4', '3', '2', '1', '3', '1', '4', '1', '4', '2', NA, '1', '4', '4', '3', '1', '4', '1', '4', '1', '4', '4', '1', '2', '2', '1', '4', '1', '1', '4', NA, '4', NA, '4', '4', '4', '1', '4', '2', '1', '2', '2', '2', '2', '1', '1', '2', '1', '4', '2', '3', '3', '1', '3', '1', '4', '1', '3', '2', '2', '4', '1', NA, '3', '4', '2', '4', '4', '4', '4', '4', '4', '3', '4', '4', '3', '2', '1', '4', '4', '2', '4', '2', '1', '2', '1', '1', '1', '1', '4', '4', '1', '1', '4', '1', '4', '4', '4', '1', '1', NA, '3', '2', '4', '4', '4', '4', '2', '3', '3', '2', NA, '4', '2', '4', '4', '1', '1', '4', '4', '1', '1', '4', '1', '2', '2', '2', '2', '1', '4', '4', '1', '2', '2', '2', '3', '4', '4', '3', '4', '1', '1', '4', '4', NA, '4', '1', '4', '4', '4', '1', '4', '4', '1', '2', '4', '4', '4', '4', '1', '2', '4', '4', '2', '1', '4', '2', '4', '2', '2', '4', '1', '3', '3', '2', '4', '1', '4', '4', '4', '1', NA, '4', '4', '2', '4', '4', '4', '4', '4', '2', NA, '4', '2', '4', '3', '1', '4', '4', '3', '4', '2', '4', '4', '1', '2', '1', '4', '1', '3', '3', '1', '4', '4', '2', '4', '4', '4', '4', '3', '2', '3', '3', '2', NA, '3', '4', '4', '3', '3', '4', '4', '4', '1', '4', '4', '4', '4', '4', '4', '4', '2', '4', '2', '3', '4', '1', '3', '1', NA, '4', '1', '2', '2', '1', '4', '3', '3', '4', '1', '1', '3')); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof11() {
        assertEval("argv <- list(structure(list(base = c(11L, 11L, 6L, 8L, 66L, 27L, 12L, 52L, 23L, 10L, 52L, 33L, 18L, 42L, 87L, 50L, 18L, 111L, 18L, 20L, 12L, 9L, 17L, 28L, 55L, 9L, 10L, 47L, 76L, 38L, 19L, 10L, 19L, 24L, 31L, 14L, 11L, 67L, 41L, 7L, 22L, 13L, 46L, 36L, 38L, 7L, 36L, 11L, 151L, 22L, 41L, 32L, 56L, 24L, 16L, 22L, 25L, 13L, 12L)), .Names = 'base', row.names = c(1L, 5L, 9L, 13L, 17L, 21L, 25L, 29L, 33L, 37L, 41L, 45L, 49L, 53L, 57L, 61L, 65L, 69L, 73L, 77L, 81L, 85L, 89L, 93L, 97L, 101L, 105L, 109L, 113L, 117L, 121L, 125L, 129L, 133L, 137L, 141L, 145L, 149L, 153L, 157L, 161L, 165L, 169L, 173L, 177L, 181L, 185L, 189L, 193L, 197L, 201L, 205L, 209L, 213L, 217L, 221L, 225L, 229L, 233L))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof12() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('Min.   : 1.000  ', '1st Qu.: 9.000  ', 'Median :18.000  ', 'Mean   :14.742  ', '3rd Qu.:20.000  ', 'Max.   :23.000  ', NA, 'Min.   :5.0000  ', '1st Qu.:5.3000  ', 'Median :6.1000  ', 'Mean   :6.0841  ', '3rd Qu.:6.6000  ', 'Max.   :7.7000  ', NA, 'Min.   :  1.000  ', '1st Qu.: 24.250  ', 'Median : 56.500  ', 'Mean   : 56.928  ', '3rd Qu.: 86.750  ', 'Max.   :117.000  ', 'NA's   :16  ', 'Min.   :  0.500  ', '1st Qu.: 11.325  ', 'Median : 23.400  ', 'Mean   : 45.603  ', '3rd Qu.: 47.550  ', 'Max.   :370.000  ', NA, 'Min.   :0.00300  ', '1st Qu.:0.04425  ', 'Median :0.11300  ', 'Mean   :0.15422  ', '3rd Qu.:0.21925  ', 'Max.   :0.81000  ', NA), .Dim = c(7L, 5L), .Dimnames = list(c('', '', '', '', '', '', ''), c('    event', '     mag', '   station', '     dist', '    accel')))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof13() {
        assertEval("argv <- list(c(2L, 1L, NA)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof14() {
        assertEval("argv <- list(raw(0)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof15() {
        assertEval("argv <- list(c(1.1+0i, NA, 3+0i)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof16() {
        assertEval("argv <- list(1e+05); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof17() {
        assertEval("argv <- list(structure(c(' ', '***'), legend = '0 ‘***’ 0.001 ‘**’ 0.01 ‘*’ 0.05 ‘.’ 0.1 ‘ ’ 1', class = 'noquote')); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof18() {
        assertEval("argv <- list(structure(c(1+1i, 2+1.4142135623731i, 3+1.73205080756888i, 4+2i, 5+2.23606797749979i, 6+2.44948974278318i, 7+2.64575131106459i, 8+2.82842712474619i, 9+3i, 10+3.1622776601684i), id = character(0), class = structure('withId', package = '.GlobalEnv'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof19() {
        assertEval("argv <- list(structure(list(x = structure(1L, .Label = '1.3', class = 'factor')), .Names = 'x', row.names = c(NA, -1L), class = 'data.frame')); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof20() {
        assertEval("argv <- list(structure(list(a = 1), .Dim = 1L, .Dimnames = list('a'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof21() {
        assertEval("argv <- list(structure(list(a = 1), .Names = 'a', .Tsp = c(1, 1, 1), class = 'ts')); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof22() {
        assertEval("argv <- list(c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof23() {
        assertEval("argv <- list(2.22044604925031e-16); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof24() {
        assertEval("argv <- list(structure(list(c0 = logical(0)), .Names = 'c0', row.names = integer(0))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof25() {
        assertEval("argv <- list(structure(3.14159265358979, comment = 'Start with pi', class = structure('num1', package = '.GlobalEnv'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof26() {
        assertEval("argv <- list(structure(c(NA, 0.1945), .Names = c('1', '2'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof27() {
        assertEval("argv <- list(structure(c(1L, 1L), .Label = 'Ctl', class = 'factor')); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof28() {
        assertEval("argv <- list(c(1L, NA, 1L)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof29() {
        assertEval("argv <- list(c(NA, NA, NA)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof31() {
        assertEval("argv <- list(structure(list(Y = c(130L, 157L, 174L, 117L, 114L, 161L, 141L, 105L, 140L, 118L, 156L, 61L, 91L, 97L, 100L, 70L, 108L, 126L, 149L, 96L, 124L, 121L, 144L, 68L, 64L, 112L, 86L, 60L, 102L, 89L, 96L, 89L, 129L, 132L, 124L, 74L, 89L, 81L, 122L, 64L, 103L, 132L, 133L, 70L, 89L, 104L, 117L, 62L, 90L, 100L, 116L, 80L, 82L, 94L, 126L, 63L, 70L, 109L, 99L, 53L, 74L, 118L, 113L, 89L, 82L, 86L, 104L, 97L, 99L, 119L, 121L), B = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L), .Label = c('I', 'II', 'III', 'IV', 'V', 'VI'), class = 'factor'), V = structure(c(3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L), .Label = c('Golden.rain', 'Marvellous', 'Victory'), class = 'factor'), N = structure(c(2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt'), class = 'factor')), .Names = c('Y', 'B', 'V', 'N'), terms = quote(Y ~ B + V + N + V:N), row.names = 2:72)); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof32() {
        assertEval("argv <- list(structure(c(-3.001e+155, -1.067e+107, -1.976e+62, -9.961e+152, -2.059e+23, 1), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.'))); .Internal(typeof(argv[[1]]))");
    }

    @Test
    public void testtypeof33() {
        assertEval("argv <- structure(list(x = c(1.1 + (0+0i), NA, 3 + (0+0i))),     .Names = 'x');do.call('typeof', argv)");
    }

    @Test
    public void testtypeof34() {
        assertEval("argv <- structure(list(x = c(NA_integer_, NA_integer_, NA_integer_)),     .Names = 'x');do.call('typeof', argv)");
    }

    @Test
    public void testtypeof35() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(x = function(file = ifelse(onefile, 'Rplots.pdf',     'Rplot%03d.pdf'), width, height, onefile, family, title,     fonts, version, paper, encoding, bg, fg, pointsize, pagecentre,     colormodel, useDingbats, useKerning, fillOddEven, compress) {    initPSandPDFfonts()    new <- list()    if (!missing(width)) new$width <- width    if (!missing(height)) new$height <- height    if (!missing(onefile)) new$onefile <- onefile    if (!missing(title)) new$title <- title    if (!missing(fonts)) new$fonts <- fonts    if (!missing(version)) new$version <- version    if (!missing(paper)) new$paper <- paper    if (!missing(encoding)) new$encoding <- encoding    if (!missing(bg)) new$bg <- bg    if (!missing(fg)) new$fg <- fg    if (!missing(pointsize)) new$pointsize <- pointsize    if (!missing(pagecentre)) new$pagecentre <- pagecentre    if (!missing(colormodel)) new$colormodel <- colormodel    if (!missing(useDingbats)) new$useDingbats <- useDingbats    if (!missing(useKerning)) new$useKerning <- useKerning    if (!missing(fillOddEven)) new$fillOddEven <- fillOddEven    if (!missing(compress)) new$compress <- compress    old <- check.options(new, name.opt = '.PDF.Options', envir = .PSenv)    if (!missing(family) && (inherits(family, 'Type1Font') ||         inherits(family, 'CIDFont'))) {        enc <- family$encoding        if (inherits(family, 'Type1Font') && !is.null(enc) &&             enc != 'default' && (is.null(old$encoding) || old$encoding ==             'default')) old$encoding <- enc        family <- family$metrics    }    if (is.null(old$encoding) || old$encoding == 'default') old$encoding <- guessEncoding()    if (!missing(family)) {        if (length(family) == 4L) {            family <- c(family, 'Symbol.afm')        } else if (length(family) == 5L) {        } else if (length(family) == 1L) {            pf <- pdfFonts(family)[[1L]]            if (is.null(pf)) stop(gettextf('unknown family '%s'',                 family), domain = NA)            matchFont(pf, old$encoding)        } else stop('invalid 'family' argument')        old$family <- family    }    version <- old$version    versions <- c('1.1', '1.2', '1.3', '1.4', '1.5', '1.6', '1.7',         '2.0')    if (version %in% versions) version <- as.integer(strsplit(version,         '[.]')[[1L]]) else stop('invalid PDF version')    onefile <- old$onefile    if (!checkIntFormat(file)) stop(gettextf('invalid 'file' argument '%s'',         file), domain = NA)    .External(C_PDF, file, old$paper, old$family, old$encoding,         old$bg, old$fg, old$width, old$height, old$pointsize,         onefile, old$pagecentre, old$title, old$fonts, version[1L],         version[2L], old$colormodel, old$useDingbats, old$useKerning,         old$fillOddEven, old$compress)    invisible()}), .Names = 'x');"
                                        + "do.call('typeof', argv)");
    }

    @Test
    public void testTypeOf() {
        assertEval("{ typeof(1) }");
        assertEval("{ typeof(1L) }");
        assertEval("{ typeof(function(){}) }");
        assertEval("{ typeof(\"hi\") }");
        assertEval("{ typeof(sum) }");
        assertEval("{ typeof(NULL) }");
        assertEval("{ typeof(TRUE) }");
        assertEval("{ typeof(\"test\") }");
        assertEval("{ typeof(c(1, 2, 3)) }");
        assertEval("{ typeof(c(1L, 2L, 3L)) }");
        assertEval("{ typeof(1:3) }");
        assertEval("{ typeof(c(TRUE, TRUE, FALSE)) }");
        assertEval("{ typeof(typeof(NULL)) }");
        assertEval("{ length(typeof(NULL)) }");
        assertEval("{ typeof(length(typeof(NULL))) }");

        assertEval("{ f <- function(...) typeof(...); f(1)}");
        assertEval("{ f <- function(...) typeof(...); f(1, 2)}");
        assertEval("{ f <- function(...) typeof(...); f(1, 2, 3)}");
        assertEval("{ f <- function(...) typeof(...); f(1, 2, 3, 4)}");

        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); typeof(x) }");
        assertEval("{ x<-data.frame(c(\"a\", \"b\", \"a\")); typeof(x) }");

        assertEval("{ f <- function(...) typeof(...); f()}");

        assertEval("{  typeof(seq(1,2)) }");
    }
}
