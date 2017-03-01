/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_shortRowNames extends TestBase {

    @Test
    public void testshortRowNames1() {
        assertEval("argv <- list(structure(list(c(8.44399377410362, 28.4640218366572, 12.2441566485997)), row.names = c(NA, -3L), class = 'data.frame'), 1L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames2() {
        assertEval("argv <- list(structure(list(y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861), x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), z = 1:10), .Names = c('y', 'x', 'z'), terms = quote(y ~ x * z - 1), row.names = c(NA, 10L), class = 'data.frame'), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames3() {
        assertEval("argv <- list(structure(list(weight = c(4.17, 5.58), group = structure(c(1L, 1L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = c('weight', 'group'), row.names = 1:2, class = 'data.frame'), 0L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames4() {
        assertEval("argv <- list(structure(list(x = 1:3, y = structure(1:3, .Label = c('A', 'D', 'E'), class = 'factor'), z = c(6, 9, 10)), .Names = c('x', 'y', 'z'), row.names = c(NA, -3L), class = 'data.frame'), 0L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames5() {
        assertEval("argv <- list(structure(list(c('4.1-0', '4.1-0', '4.1-0', '4.1-0', '4.1-0', '4.1-0', '4.0-3', '4.0-3', '4.0-3', '4.0-3', '4.0-3', '4.0-2', '4.0-2', '4.0-1', '4.0-1', '4.0-1', '4.0-1', '4.0-1', '4.0-1', '4.0-1', '4.0-1', '3.1-55', '3.1-55', '3.1-55', '3.1-54', '3.1-53', '3.1-53', '3.1-52', '3.1-51'), c(NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_), c(NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_), c('The C and R code has been reformatted for legibility.', 'The old compatibility function rpconvert() has been removed.', 'The cross-validation functions allow for user interrupt at the end\\nof evaluating each split.', 'Variable Reliability in data set car90 is corrected to be an\\nordered factor, as documented.', 'Surrogate splits are now considered only if they send two or more\\ncases _with non-zero weight_ each way.  For numeric/ordinal\\nvariables the restriction to non-zero weights is new: for\\ncategorical variables this is a new restriction.', 'Surrogate splits which improve only by rounding error over the\\ndefault split are no longer returned.  Where weights and missing\\nvalues are present, the splits component for some of these was not\\nreturned correctly.', 'A fit of class \\\'rpart\\\' now contains a component for variable\\n‘importance’, which is reported by the summary() method.', 'The text() method gains a minlength argument, like the labels()\\nmethod.  This adds finer control: the default remains pretty =\\nNULL, minlength = 1L.', 'The handling of fits with zero and fractional weights has been\\ncorrected: the results may be slightly different (or even\\nsubstantially different when the proportion of zero weights is\\nlarge).', 'Some memory leaks have been plugged.', 'There is a second vignette, longintro.Rnw, a version of the\\noriginal Mayo Tecnical Report on rpart.', 'Added dataset car90, a corrected version of the S-PLUS dataset\\ncar.all (used with permission).', 'This version does not use paste0{} and so works with R 2.14.x.', 'Merged in a set of Splus code changes that had accumulated at Mayo\\nover the course of a decade. The primary one is a change in how\\nindexing is done in the underlying C code, which leads to a major\\nspeed increase for large data sets.  Essentially, for the lower\\nleaves all our time used to be eaten up by bookkeeping, and this\\nwas replaced by a different approach.  The primary routine also\\nuses .Call{} so as to be more memory efficient.', 'The other major change was an error for asymmetric loss matrices,\\nprompted by a user query.  With L=loss asymmetric, the altered\\npriors were computed incorrectly - they were using L\\' instead of L.\\nUpshot - the tree would not not necessarily choose optimal splits\\nfor the given loss matrix.  Once chosen, splits were evaluated\\ncorrectly.  The printed “improvement” values are of course the\\nwrong ones as well.  It is interesting that for my little test\\ncase, with L quite asymmetric, the early splits in the tree are\\nunchanged - a good split still looks good.', 'Add the return.all argument to xpred.rpart().', 'Added a set of formal tests, i.e., cases with known answers to\\nwhich we can compare.', 'Add a usercode vignette, explaining how to add user defined\\nsplitting functions.', 'The class method now also returns the node probability.', 'Add the stagec data set, used in some tests.', 'The plot.rpart routine needs to store a value that will be visible\\nto the rpartco routine at a later time.  This is now done in an\\nenvironment in the namespace.', 'Force use of registered symbols in R >= 2.16.0', 'Update Polish translations.', 'Work on message formats.', 'Add Polish translations', 'rpart, rpart.matrix: allow backticks in formulae.', 'tests/backtick.R: regession test', 'src/xval.c: ensure unused code is not compiled in.', 'Change description of margin in ?plot.rpart as suggested by Bill\\nVenables.')), row.names = c(NA, -29L), class = 'data.frame'), 1L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames6() {
        assertEval("argv <- list(structure(list(c(101, 32741, 2147483621, 1.70141183460469e+38, 8.98846567431158e+307)), row.names = c(NA, -5L), class = 'data.frame'), 1L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames7() {
        assertEval("argv <- list(structure(list(srcfile = '/home/lzhao/hg/r-instrumented/library/stats/R/stats', frow = 5139L, lrow = 5139L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame'), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames8() {
        assertEval("argv <- list(structure(list(y = structure(c(8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536, 9.79424), .Tsp = c(1962.25, 1971.75, 4), class = 'ts'), lag.quarterly.revenue = c(8.79636, 8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536), price.index = c(4.70997, 4.70217, 4.68944, 4.68558, 4.64019, 4.62553, 4.61991, 4.61654, 4.61407, 4.60766, 4.60227, 4.5896, 4.57592, 4.58661, 4.57997, 4.57176, 4.56104, 4.54906, 4.53957, 4.51018, 4.50352, 4.4936, 4.46505, 4.44924, 4.43966, 4.42025, 4.4106, 4.41151, 4.3981, 4.38513, 4.3732, 4.3277, 4.32023, 4.30909, 4.30909, 4.30552, 4.29627, 4.27839, 4.27789), income.level = c(5.8211, 5.82558, 5.83112, 5.84046, 5.85036, 5.86464, 5.87769, 5.89763, 5.92574, 5.94232, 5.95365, 5.9612, 5.97805, 6.00377, 6.02829, 6.03475, 6.03906, 6.05046, 6.05563, 6.06093, 6.07103, 6.08018, 6.08858, 6.10199, 6.11207, 6.11596, 6.12129, 6.122, 6.13119, 6.14705, 6.15336, 6.15627, 6.16274, 6.17369, 6.16135, 6.18231, 6.18768, 6.19377, 6.2003), market.potential = c(12.9699, 12.9733, 12.9774, 12.9806, 12.9831, 12.9854, 12.99, 12.9943, 12.9992, 13.0033, 13.0099, 13.0159, 13.0212, 13.0265, 13.0351, 13.0429, 13.0497, 13.0551, 13.0634, 13.0693, 13.0737, 13.077, 13.0849, 13.0918, 13.095, 13.0984, 13.1089, 13.1169, 13.1222, 13.1266, 13.1356, 13.1415, 13.1444, 13.1459, 13.152, 13.1593, 13.1579, 13.1625, 13.1664)), .Names = c('y', 'lag.quarterly.revenue', 'price.index', 'income.level', 'market.potential'), row.names = c('1962.25', '1962.5', '1962.75', '1963', '1963.25', '1963.5', '1963.75', '1964', '1964.25', '1964.5', '1964.75', '1965', '1965.25', '1965.5', '1965.75', '1966', '1966.25', '1966.5', '1966.75', '1967', '1967.25', '1967.5', '1967.75', '1968', '1968.25', '1968.5', '1968.75', '1969', '1969.25', '1969.5', '1969.75', '1970', '1970.25', '1970.5', '1970.75', '1971', '1971.25', '1971.5', '1971.75'), class = 'data.frame'), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames9() {
        assertEval("argv <- list(structure(list(Topic = character(0), File = character(0), Title = character(0), Internal = character(0)), .Names = c('Topic', 'File', 'Title', 'Internal'), row.names = integer(0), class = 'data.frame'), 0L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames11() {
        assertEval("argv <- list(structure(list(age = c(40, 60, 80)), .Names = 'age', row.names = c(NA, -3L), class = 'data.frame'), 0L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames12() {
        assertEval("argv <- list(structure(list(surname = structure(integer(0), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(integer(0), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(integer(0), .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = integer(0), class = 'data.frame'), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames13() {
        assertEval("argv <- list(structure(list(age = 1:65), .Names = 'age'), 0L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames14() {
        assertEval("argv <- list(structure(list(variog = c(0.000474498531874882, 0.00702969158809408, 0.00702969158809408, 0.00398874346479977, 0.000383788683835002, 1.20172224431796e-06, 1.20172224431796e-06, 0.122905372955376, 0.378939119261529, 0.00604112083775904, 0.0365586576304611, 2.52242766079251e-05, 0.100345142776916, 0.00940165099100291, 0.149441544291522, 0.0295722090612792), dist = c(36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36, 36)), .Names = c('variog', 'dist'), row.names = c('2.16', '3.16', '4.16', '1.16', '8.16', '5.16', '6.16', '7.16', '11.16', '9.16', '10.16', '12.16', '13.16', '15.16', '14.16', '16.16'), class = c('Variogram', 'data.frame')), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames15() {
        assertEval("argv <- list(structure(list(age = c(-1, -0.959071428571429, -0.918142857142857, -0.877214285714286, -0.836285714285714, -0.795357142857143, -0.754428571428571, -0.7135, -0.672571428571429, -0.631642857142857, -0.590714285714286, -0.549785714285714, -0.508857142857143, -0.467928571428571, -0.427, -0.386071428571429, -0.345142857142857, -0.304214285714286, -0.263285714285714, -0.222357142857143, -0.181428571428571, -0.1405, -0.0995714285714285, -0.0586428571428571, -0.0177142857142856, 0.0232142857142859, 0.0641428571428573, 0.105071428571429, 0.146, 0.186928571428572, 0.227857142857143, 0.268785714285714, 0.309714285714286, 0.350642857142857, 0.391571428571429, 0.4325, 0.473428571428572, 0.514357142857143, 0.555285714285714, 0.596214285714286, 0.637142857142857, 0.678071428571429, 0.719, 0.759928571428572, 0.800857142857143, 0.841785714285714, 0.882714285714286, 0.923642857142857, 0.964571428571429, 1.0055), Subject = structure(c(25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L, 25L), .Label = c('10', '26', '25', '9', '2', '6', '7', '17', '16', '15', '8', '20', '1', '18', '5', '23', '11', '21', '3', '24', '22', '12', '13', '14', '19', '4'), class = 'factor')), .Names = c('age', 'Subject'), out.attrs = structure(list(dim = structure(c(50L, 26L), .Names = c('age', 'Subject')), dimnames = structure(list(    age = c('age=-1.00000000', 'age=-0.95907143', 'age=-0.91814286', 'age=-0.87721429', 'age=-0.83628571', 'age=-0.79535714', 'age=-0.75442857', 'age=-0.71350000', 'age=-0.67257143', 'age=-0.63164286', 'age=-0.59071429', 'age=-0.54978571', 'age=-0.50885714', 'age=-0.46792857', 'age=-0.42700000', 'age=-0.38607143', 'age=-0.34514286', 'age=-0.30421429', 'age=-0.26328571', 'age=-0.22235714', 'age=-0.18142857', 'age=-0.14050000', 'age=-0.09957143', 'age=-0.05864286', 'age=-0.01771429', 'age= 0.02321429',     'age= 0.06414286', 'age= 0.10507143', 'age= 0.14600000', 'age= 0.18692857', 'age= 0.22785714', 'age= 0.26878571', 'age= 0.30971429', 'age= 0.35064286', 'age= 0.39157143', 'age= 0.43250000', 'age= 0.47342857', 'age= 0.51435714', 'age= 0.55528571', 'age= 0.59621429', 'age= 0.63714286', 'age= 0.67807143', 'age= 0.71900000', 'age= 0.75992857', 'age= 0.80085714', 'age= 0.84178571', 'age= 0.88271429', 'age= 0.92364286', 'age= 0.96457143', 'age= 1.00550000'), Subject = c('Subject=10', 'Subject=26',     'Subject=25', 'Subject=9', 'Subject=2', 'Subject=6', 'Subject=7', 'Subject=17', 'Subject=16', 'Subject=15', 'Subject=8', 'Subject=20', 'Subject=1', 'Subject=18', 'Subject=5', 'Subject=23', 'Subject=11', 'Subject=21', 'Subject=3', 'Subject=24', 'Subject=22', 'Subject=12', 'Subject=13', 'Subject=14', 'Subject=19', 'Subject=4')), .Names = c('age', 'Subject'))), .Names = c('dim', 'dimnames')), row.names = 1201:1250, class = 'data.frame'), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames16() {
        assertEval("argv <- list(structure(list(variog = structure(c('0.007239522', '0.014584634', '0.014207936', '0.018442267', '0.011128505', '0.019910082', '0.027072311', '0.034140379', '0.028320657', '0.037525507'), class = 'AsIs'), dist = structure(c(' 1', ' 6', ' 7', ' 8', '13', '14', '15', '20', '21', '22'), class = 'AsIs'), n.pairs = structure(c(' 16', ' 16', '144', ' 16', ' 16', '128', ' 16', ' 16', '112', ' 16'), .Dim = 10L, .Dimnames = structure(list(c('1', '6', '7', '8', '13', '14', '15', '20', '21', '22')), .Names = ''))), .Names = c('variog', 'dist', 'n.pairs'), row.names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10'), class = 'data.frame'), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames17() {
        assertEval("argv <- list(structure(list(GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962, Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551)), .Names = c('GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year', 'Employed'), row.names = 1947:1962, class = 'data.frame'), 0L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames18() {
        assertEval("argv <- list(structure(list(surname = structure(c('McNeil', 'Ripley', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'AsIs'), nationality = structure(c('Australia', 'UK', 'UK', 'US', 'US', 'Australia'), class = 'AsIs'), deceased = structure(c('no', 'no', 'no', 'no', 'yes', 'no'), class = 'AsIs'), title = structure(c('Interactive Data Analysis', 'Spatial Statistics', 'Stochastic Simulation', 'LISP-STAT', 'Exploratory Data Analysis', 'Modern Applied Statistics ...'), class = 'AsIs'), other.author = structure(c(NA, NA, NA, NA, NA, 'Ripley'), class = 'AsIs')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = c('1', '2', '3', '4', '5', '6'), class = 'data.frame'), 1L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames19() {
        assertEval("argv <- list(structure(list(`cbind(X, M)` = structure(c(68, 42, 37, 24, 66, 33, 47, 23, 63, 29, 57, 19, 42, 30, 52, 43, 50, 23, 55, 47, 53, 27, 49, 29), .Dim = c(12L, 2L), .Dimnames = list(NULL, c('X', 'M'))), M.user = structure(c(1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L), .Label = c('N', 'Y'), class = 'factor', contrasts = 'contr.treatment'), Temp = structure(c(2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L), .Label = c('High', 'Low'), class = 'factor', contrasts = 'contr.treatment')), .Names = c('cbind(X, M)', 'M.user', 'Temp'), terms = quote(cbind(X, M) ~ M.user + Temp + M.user:Temp), row.names = c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23')), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames20() {
        assertEval("argv <- list(structure(list(height = numeric(0), weight = numeric(0)), .Names = c('height', 'weight'), row.names = integer(0), class = 'data.frame'), 0L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames21() {
        assertEval("argv <- list(structure(list(A = c(1, NA, 1), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA_integer_, NA_integer_, NA_integer_), E = c(FALSE, NA, TRUE), F = c('abc', NA, 'def')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames22() {
        assertEval("argv <- list(structure(list(Hair = structure(c(1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('Black', 'Brown', 'Red', 'Blond'), class = 'factor'), Eye = structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L), .Label = c('Brown', 'Blue', 'Hazel', 'Green'), class = 'factor'), Sex = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Male', 'Female'), class = 'factor')), .Names = c('Hair', 'Eye', 'Sex'), out.attrs = structure(list(dim = structure(c(4L, 4L, 2L), .Names = c('Hair', 'Eye', 'Sex')), dimnames = structure(list(Hair = c('Hair=Black', 'Hair=Brown', 'Hair=Red', 'Hair=Blond'), Eye = c('Eye=Brown', 'Eye=Blue', 'Eye=Hazel', 'Eye=Green'), Sex = c('Sex=Male', 'Sex=Female')), .Names = c('Hair', 'Eye', 'Sex'))), .Names = c('dim', 'dimnames')), class = 'data.frame', row.names = c(NA, -32L)), 1L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testshortRowNames23() {
        assertEval("argv <- list(structure(list(Df = c(NA, 1, 2), Deviance = c(12.2441566485997, 8.44399377410362, 11.9670615295804), AIC = c(73.9421143635373, 72.1419514890412, 77.665019244518)), .Names = c('Df', 'Deviance', 'AIC'), row.names = c('<none>', 'Temp', 'Soft'), class = c('anova', 'data.frame'), heading = c('Single term additions', '\\nModel:', 'cbind(X, M) ~ M.user')), 2L); .Internal(shortRowNames(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testArgCasts() {
        assertEval(".Internal(shortRowNames(42, -2))");
        assertEval(".Internal(shortRowNames(42, '1'))");
    }
}
