/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_isenvironment extends TestBase {

    @Test
    public void testisenvironment1() {
        assertEval("argv <- list(structure(list(B = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L), .Label = c('I', 'II', 'III', 'IV', 'V', 'VI'), class = 'factor'), V = structure(c(3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L), .Label = c('Golden.rain', 'Marvellous', 'Victory'), class = 'factor'), N = structure(c(2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt'), class = 'factor'), Y = c(130L, 157L, 174L, 117L, 114L, 161L, 141L, 105L, 140L, 118L, 156L, 61L, 91L, 97L, 100L, 70L, 108L, 126L, 149L, 96L, 124L, 121L, 144L, 68L, 64L, 112L, 86L, 60L, 102L, 89L, 96L, 89L, 129L, 132L, 124L, 74L, 89L, 81L, 122L, 64L, 103L, 132L, 133L, 70L, 89L, 104L, 117L, 62L, 90L, 100L, 116L, 80L, 82L, 94L, 126L, 63L, 70L, 109L, 99L, 53L, 74L, 118L, 113L, 89L, 82L, 86L, 104L, 97L, 99L, 119L, 121L)), .Names = c('B', 'V', 'N', 'Y'), row.names = 2:72, class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment2() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment3() {
        assertEval("argv <- list(structure(list(ID = c(65L, 65L), Age = c(18L, 18L), OME = structure(c(1L, 1L), .Label = c('N/A', 'high', 'low'), class = 'factor'), Loud = c(35L, 50L), Noise = structure(c(2L, 2L), .Label = c('coherent', 'incoherent'), class = 'factor'), Correct = 0:1, Trials = c(1L, 1L), UID = c(71L, 71L), UIDn = c(71.1, 71.1)), .Names = c('ID', 'Age', 'OME', 'Loud', 'Noise', 'Correct', 'Trials', 'UID', 'UIDn'), row.names = c(691L, 701L), class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment4() {
        assertEval("argv <- list(c(1.21066831870929-1.66634219937781i, -1.3109785475009-4.03477541783013i, -8.31047673770943-2.70023589529419i, -14.5607479203187+10.5790080335186i, 0.0000113099668+37.0708585836817i, 61.7727497177743+44.8804951883406i, 149.572574104172-48.599145973822i, 100.099969025816-308.076538446181i, -392.173075447774-539.779307378543i, -1374.2485561312+0.00083854036i, -1663.75779164483+2289.96937670968i, 1801.61375934412+5544.78319295828i, 11420.6629218159+3710.7883116515i, 20010.0779347631-14538.198725138i, -0.0466281166-50944.5738831589i, -84891.1497420117-61676.9039120865i, -205549.85324714+66787.431606375i, -137561.979567894+423373.822075425i, 538943.735314369+741790.604941508i, 1888559.09402798-2.30472576i));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment5() {
        assertEval("argv <- list(structure(c(FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE), .Dim = c(5L, 5L)));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment6() {
        assertEval("argv <- list(structure(list(y = structure(c(8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536, 9.79424), .Tsp = c(1962.25, 1971.75, 4), class = 'ts'), lag.quarterly.revenue = c(8.79636, 8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536), price.index = c(4.70997, 4.70217, 4.68944, 4.68558, 4.64019, 4.62553, 4.61991, 4.61654, 4.61407, 4.60766, 4.60227, 4.5896, 4.57592, 4.58661, 4.57997, 4.57176, 4.56104, 4.54906, 4.53957, 4.51018, 4.50352, 4.4936, 4.46505, 4.44924, 4.43966, 4.42025, 4.4106, 4.41151, 4.3981, 4.38513, 4.3732, 4.3277, 4.32023, 4.30909, 4.30909, 4.30552, 4.29627, 4.27839, 4.27789), income.level = c(5.8211, 5.82558, 5.83112, 5.84046, 5.85036, 5.86464, 5.87769, 5.89763, 5.92574, 5.94232, 5.95365, 5.9612, 5.97805, 6.00377, 6.02829, 6.03475, 6.03906, 6.05046, 6.05563, 6.06093, 6.07103, 6.08018, 6.08858, 6.10199, 6.11207, 6.11596, 6.12129, 6.122, 6.13119, 6.14705, 6.15336, 6.15627, 6.16274, 6.17369, 6.16135, 6.18231, 6.18768, 6.19377, 6.2003), market.potential = c(12.9699, 12.9733, 12.9774, 12.9806, 12.9831, 12.9854, 12.99, 12.9943, 12.9992, 13.0033, 13.0099, 13.0159, 13.0212, 13.0265, 13.0351, 13.0429, 13.0497, 13.0551, 13.0634, 13.0693, 13.0737, 13.077, 13.0849, 13.0918, 13.095, 13.0984, 13.1089, 13.1169, 13.1222, 13.1266, 13.1356, 13.1415, 13.1444, 13.1459, 13.152, 13.1593, 13.1579, 13.1625, 13.1664)), .Names = c('y', 'lag.quarterly.revenue', 'price.index', 'income.level', 'market.potential'), row.names = c('1962.25', '1962.5', '1962.75', '1963', '1963.25', '1963.5', '1963.75', '1964', '1964.25', '1964.5', '1964.75', '1965', '1965.25', '1965.5', '1965.75', '1966', '1966.25', '1966.5', '1966.75', '1967', '1967.25', '1967.5', '1967.75', '1968', '1968.25', '1968.5', '1968.75', '1969', '1969.25', '1969.5', '1969.75', '1970', '1970.25', '1970.5', '1970.75', '1971', '1971.25', '1971.5', '1971.75'), class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment7() {
        assertEval("argv <- list(structure(c(1+0i, 5+0i, 9+0i, 13+0i, 17+0i, 21+0i, 2+0i, 6+0i, 10+0i, 14+0i, 18+0i, 22+0i, 3+0i, 7+0i, 11+0i, 15+0i, 19+0i, 23+0i, 4+0i, 8+0i, 12+0i, 16+0i, 20+0i, 24+0i), .Dim = c(6L, 4L)));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment8() {
        assertEval("argv <- list(structure(list(z = c(-3.32814298919898, -2.50040106767383, -1.77388580318944, -1.1253131847654, -0.538436159302777, 0, 0.494272294062615, 0.955576378633541, 1.38763151510367, 1.79457990658358, 2.17975641634868, 2.54588157879649, 2.89520069061619), par.vals = structure(c(14.1116522107389, 16.2879401909036, 18.4642281710683, 20.640516151233, 22.8168041313977, 24.9930921115624, 27.1693800917271, 29.3456680718919, 31.5219560520566, 33.6982440322213, 35.874532012386, 38.0508199925507, 40.2271079727154, 9.77292620586829, 6.94555751970939, 5.3344468962477, 4.29948550876711, 3.58192055542193, 3.05706219239734, 2.6578922832996, 2.34490705796319, 2.09354140228955, 1.88766190416604, 1.71625883349025, 1.57157057930468, 1.44797941211333), .Dim = c(13L, 2L), .Dimnames = list(NULL, c('ymax', 'xhalf')))), .Names = c('z', 'par.vals'), row.names = c(NA, -13L), class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment9() {
        assertEval("argv <- list(structure(list(GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962, Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551)), .Names = c('GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year', 'Employed'), row.names = 1947:1962, class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment10() {
        assertEval("argv <- list(structure(list(breaks = c(26, 30, 54, 25, 70, 52, 51, 26, 67, 18, 21, 29, 17, 12, 18, 35, 30, 36, 36, 21, 24, 18, 10, 43, 28, 15, 26, 27, 14, 29, 19, 29, 31, 41, 20, 44, 42, 26, 19, 16, 39, 28, 21, 39, 29, 20, 21, 24, 17, 13, 15, 15, 16, 28), wool = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('A', 'B'), class = 'factor'), tension = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('L', 'M', 'H'), class = 'factor')), .Names = c('breaks', 'wool', 'tension'), row.names = c(NA, -54L), class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment11() {
        assertEval("argv <- list(structure(list(y = c(0.219628047744843, 0.360454661130887, NA, 0.114681204747219, -1.14267533343616, 0.772374419482067, 0.681741904304867, 0.171869265068012, 2.08409180391906, 0.367547276775469), x1 = c(1L, 2L, 3L, NA, 5L, 6L, 7L, 8L, 9L, 10L), x2 = 1:10, x3 = c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), wt = c(0, 1, 1, 1, 1, 1, 1, 1, 1, 1)), .Names = c('y', 'x1', 'x2', 'x3', 'wt'), row.names = c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'), class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment12() {
        assertEval("argv <- list(structure(list(x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861)), .Names = c('x', 'y'), row.names = c(NA, -10L), class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment13() {
        assertEval("argv <- list(structure(list(weight = c(1.9, 3.1, 3.3, 4.8, 5.3, 6.1, 6.4, 7.6, 9.8, 12.4), depression = c(2, 1, 5, 5, 20, 20, 23, 10, 30, 25)), .Names = c('weight', 'depression'), row.names = c(NA, -10L), class = 'data.frame'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment14() {
        assertEval("argv <- list(structure(list(loglik = c(-577.080015666702, -568.702653976085, -567.639101463216, -565.252511135152), Chisq = c(NA, 16.7547233812336, 2.12710502573896, 4.77318065612872), Df = c(NA, 1, 1, 3), `Pr(>|Chi|)` = c(NA, 4.25362427346476e-05, 0.144713844418628, 0.189179603743297)), .Names = c('loglik', 'Chisq', 'Df', 'Pr(>|Chi|)'), row.names = c('NULL', 'ph.ecog', 'wt.loss', 'poly(age, 3)'), class = c('anova', 'data.frame'), heading = 'Analysis of Deviance Table\\n Cox model: response is Surv(time, status)\\nTerms added sequentially (first to last)\\n'));is.environment(argv[[1]]);");
    }

    @Test
    public void testisenvironment16() {
        assertEval("argv <- list(numeric(0));is.environment(argv[[1]]);");
    }
}
