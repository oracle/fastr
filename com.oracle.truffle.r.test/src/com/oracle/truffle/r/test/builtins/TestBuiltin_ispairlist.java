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
public class TestBuiltin_ispairlist extends TestBase {

    @Test
    public void testispairlist1() {
        assertEval("argv <- list(list(NULL, c('time', 'status')));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist2() {
        assertEval("argv <- list(structure(list(y = structure(c(8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536, 9.79424), .Tsp = c(1962.25, 1971.75, 4), class = 'ts'), lag.quarterly.revenue = c(8.79636, 8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536), price.index = c(4.70997, 4.70217, 4.68944, 4.68558, 4.64019, 4.62553, 4.61991, 4.61654, 4.61407, 4.60766, 4.60227, 4.5896, 4.57592, 4.58661, 4.57997, 4.57176, 4.56104, 4.54906, 4.53957, 4.51018, 4.50352, 4.4936, 4.46505, 4.44924, 4.43966, 4.42025, 4.4106, 4.41151, 4.3981, 4.38513, 4.3732, 4.3277, 4.32023, 4.30909, 4.30909, 4.30552, 4.29627, 4.27839, 4.27789), income.level = c(5.8211, 5.82558, 5.83112, 5.84046, 5.85036, 5.86464, 5.87769, 5.89763, 5.92574, 5.94232, 5.95365, 5.9612, 5.97805, 6.00377, 6.02829, 6.03475, 6.03906, 6.05046, 6.05563, 6.06093, 6.07103, 6.08018, 6.08858, 6.10199, 6.11207, 6.11596, 6.12129, 6.122, 6.13119, 6.14705, 6.15336, 6.15627, 6.16274, 6.17369, 6.16135, 6.18231, 6.18768, 6.19377, 6.2003), market.potential = c(12.9699, 12.9733, 12.9774, 12.9806, 12.9831, 12.9854, 12.99, 12.9943, 12.9992, 13.0033, 13.0099, 13.0159, 13.0212, 13.0265, 13.0351, 13.0429, 13.0497, 13.0551, 13.0634, 13.0693, 13.0737, 13.077, 13.0849, 13.0918, 13.095, 13.0984, 13.1089, 13.1169, 13.1222, 13.1266, 13.1356, 13.1415, 13.1444, 13.1459, 13.152, 13.1593, 13.1579, 13.1625, 13.1664)), .Names = c('y', 'lag.quarterly.revenue', 'price.index', 'income.level', 'market.potential'), row.names = c('1962.25', '1962.5', '1962.75', '1963', '1963.25', '1963.5', '1963.75', '1964', '1964.25', '1964.5', '1964.75', '1965', '1965.25', '1965.5', '1965.75', '1966', '1966.25', '1966.5', '1966.75', '1967', '1967.25', '1967.5', '1967.75', '1968', '1968.25', '1968.5', '1968.75', '1969', '1969.25', '1969.5', '1969.75', '1970', '1970.25', '1970.5', '1970.75', '1971', '1971.25', '1971.5', '1971.75'), class = 'data.frame'));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist3() {
        assertEval("argv <- list(structure(list(), .Names = character(0), row.names = integer(0), class = 'data.frame'));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist4() {
        assertEval("argv <- list(structure(list(double.eps = 2.22044604925031e-16, double.neg.eps = 1.11022302462516e-16, double.xmin = 2.2250738585072e-308, double.xmax = 1.79769313486232e+308, double.base = 2L, double.digits = 53L, double.rounding = 5L, double.guard = 0L, double.ulp.digits = -52L, double.neg.ulp.digits = -53L, double.exponent = 11L, double.min.exp = -1022L, double.max.exp = 1024L, integer.max = 2147483647L, sizeof.long = 8L, sizeof.longlong = 8L, sizeof.longdouble = 16L, sizeof.pointer = 8L), .Names = c('double.eps', 'double.neg.eps', 'double.xmin', 'double.xmax', 'double.base', 'double.digits', 'double.rounding', 'double.guard', 'double.ulp.digits', 'double.neg.ulp.digits', 'double.exponent', 'double.min.exp', 'double.max.exp', 'integer.max', 'sizeof.long', 'sizeof.longlong', 'sizeof.longdouble', 'sizeof.pointer')));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist5() {
        assertEval("argv <- list(structure(list(widths = structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.880387330793302, 0.872694837975491, 0.862482627238653, 0.85694743348285, 0.844691564126209, 0.837992157219203, 0.826065549537932, 0.821813943091766, 0.801815430713592, 0.777814415451275, 0.862246453969427, 0.852609391860845, 0.852456189097792, 0.852184980555031, 0.847433895388854, 0.847321709738264, 0.843043642624801, 0.839180526055581, 0.838791375904974, 0.832684615585117, 0.829332878628487, 0.818738807141856, 0.817393740696655, 0.795187378905238, 0.771817782697421, 0.644452148607831, 0.532190150080465, 0.330404926016424), .Dim = c(28L, 3L), .Dimnames = list(c('10', '9', '2', '4', '8', '7', '6', '3', '1', '5', '22', '19', '11', '13', '18', '15', '16', '23', '24', '12', '14', '20', '21', '17', '25', '27', '28', '26'), c('cluster', 'neighbor', 'sil_width'))), clus.avg.widths = c(0.838270528963027, 0.778192810753059), avg.width = 0.799649138685191), .Names = c('widths', 'clus.avg.widths', 'avg.width')));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist6() {
        assertEval("argv <- list(structure(list(loc = c(0.0804034870161223, 10.3548347412639), cov = structure(c(3.01119301965569, 6.14320559215603, 6.14320559215603, 14.7924762275451), .Dim = c(2L, 2L)), d2 = 2, wt = c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0), sqdist = c(0.439364946869246, 0.0143172566761092, 0.783644692619938, 0.766252947443554, 0.346865912102713, 1.41583192825661, 0.168485512965902, 0.354299830956879, 0.0943280426627965, 1.05001058449122, 1.02875556201707, 0.229332323173361, 0.873263925064789, 2.00000009960498, 0.449304354954282, 0.155023307933165, 0.118273979375253, 0.361693898800799, 0.21462398586105, 0.155558909016629, 0.471723661454506, 0.719528696331092, 0.0738164380664225, 1.46001193111051, 0.140785322548143, 0.127761195166703, 0.048012401156175, 0.811750426884519, 0.425827709817574, 0.163016638545231, 0.557810866640707, 0.277350147637843, 0.0781399119055092, 1.29559183995835, 0.718376405567138, 1.37650242941478, 0.175087780508154, 0.233808973148729, 0.693473805463067, 0.189096604125073, 1.96893781800017, 0.4759756980592, 1.69665760380474, 0.277965749373647, 0.920525436884815, 0.57525234053591, 1.59389578665009, 0.175715364671313, 0.972045794851437, 1.75514684962809, 0.0597413185507202, 0.174340343040626, 0.143421553552865, 0.997322770596838, 1.94096736957465, 2.00000001159796, 0.367000821772989, 0.682474530588235, 1.20976163307984, 1.27031685239035, 1.79775635513363, 0.0857761902860323, 0.435578932929501, 0.214370604878221, 0.494714247412686, 1.78784623754399, 1.24216674083069, 1.87749485326709, 0.0533296334123023, 1.45588362584438, 2.00000000631459, 0.208857144738039, 0.119251291573058, 0.365303924649962, 0.690656674239668, 0.0396958405786268, 0.258262120876164, 1.57360254057537, 0.307548421049514, 0.628417063100241, 1.00647098749202, 0.297624360530352, 0.400289147351669, 1.98298426250944, 0.129127182829694, 0.0794695319493149, 0.991481735944321, 0.444068154119836, 0.206790162395106, 0.574310829851377, 0.181887577583334, 0.433872021297517, 0.802994892604009, 0.293053770941001, 1.7002969001965, 0.77984639982848, 1.36127407487932, 0.761935213110323, 0.597915313430067, 0.237134831067472), prob = NULL, tol = 1e-07, eps = 9.96049758228423e-08, it = 898L, maxit = 5000,     ierr = 0L, conv = TRUE), .Names = c('loc', 'cov', 'd2', 'wt', 'sqdist', 'prob', 'tol', 'eps', 'it', 'maxit', 'ierr', 'conv'), class = 'ellipsoid'));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist7() {
        assertEval("argv <- list(structure(list(sec = c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), hour = c(20L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 20L, 20L, 20L, 20L, 19L, 19L, 19L, 20L, 20L, 20L, 19L, 20L, 19L, 19L, 19L, 20L), mday = c(30L, 31L, 31L, 31L, 31L, 31L, 31L, 31L, 31L, 30L, 30L, 30L, 30L, 31L, 31L, 31L, 30L, 30L, 30L, 31L, 30L, 31L, 31L, 31L, 30L), mon = c(5L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 5L, 5L, 5L, 5L, 11L, 11L, 11L, 5L, 5L, 5L, 11L, 5L, 11L, 11L, 11L, 5L), year = c(72L, 72L, 73L, 74L, 75L, 76L, 77L, 78L, 79L, 81L, 82L, 83L, 85L, 87L, 89L, 90L, 92L, 93L, 94L, 95L, 97L, 98L, 105L, 108L, 112L), wday = c(5L, 0L, 1L, 2L, 3L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 0L, 4L, 0L, 1L, 2L, 3L, 4L, 0L, 1L, 4L, 6L, 3L, 6L), yday = c(181L, 365L, 364L, 364L, 364L, 365L, 364L, 364L, 364L, 180L, 180L, 180L, 180L, 364L, 364L, 364L, 181L, 180L, 180L, 364L, 180L, 364L, 364L, 365L, 181L), isdst = c(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 1L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), tzone = c('', 'EST', 'EDT')));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist8() {
        assertEval("argv <- list(structure(list(usr = c(0.568, 1.432, -1.08, 1.08), xaxp = c(0.6, 1.4, 4), yaxp = c(-1, 1, 4)), .Names = c('usr', 'xaxp', 'yaxp')));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist9() {
        assertEval("argv <- list(structure(list(x = c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1), y = c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), fac = structure(c(1L, 3L, 2L, 3L, 3L, 1L, 2L, 3L, 2L, 2L), .Label = c('A', 'B', 'C'), class = 'factor')), .Names = c('x', 'y', 'fac'), row.names = c(NA, -10L), class = 'data.frame'));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist10() {
        assertEval("argv <- list(structure(list(height = c(58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72), weight = c(115, 117, 120, 123, 126, 129, 132, 135, 139, 142, 146, 150, 154, 159, 164)), .Names = c('height', 'weight'), row.names = c(NA, -15L), class = 'data.frame'));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist11() {
        assertEval("argv <- list(structure(list(a_string = c('foo', 'bar'), a_bool = FALSE, a_struct = structure(list(a = 1, b = structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), c = 'foo'), .Names = c('a', 'b', 'c')), a_cell = structure(list(1, 'foo', structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), 'bar'), .Dim = c(2L, 2L)), a_complex_scalar = 0+1i, a_list = list(1, structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), 'foo'), a_complex_matrix = structure(c(1+2i, 5+0i, 3-4i, -6+0i), .Dim = c(2L, 2L)), a_range = c(1, 2, 3, 4, 5), a_scalar = 1,     a_complex_3_d_array = structure(c(1+1i, 3+1i, 2+1i, 4+1i, 5-1i, 7-1i, 6-1i, 8-1i), .Dim = c(2L, 2L, 2L)), a_3_d_array = structure(c(1, 3, 2, 4, 5, 7, 6, 8), .Dim = c(2L, 2L, 2L)), a_matrix = structure(c(1, 3, 2, 4), .Dim = c(2L, 2L)), a_bool_matrix = structure(c(TRUE, FALSE, FALSE, TRUE), .Dim = c(2L, 2L))), .Names = c('a_string', 'a_bool', 'a_struct', 'a_cell', 'a_complex_scalar', 'a_list', 'a_complex_matrix', 'a_range', 'a_scalar', 'a_complex_3_d_array', 'a_3_d_array', 'a_matrix', 'a_bool_matrix')));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist12() {
        assertEval("argv <- list(structure(list(f = structure(c(1L, 1L, 1L), .Label = c('1', '2'), class = 'factor'), u = structure(12:14, unit = 'kg', class = 'avector')), .Names = c('f', 'u'), row.names = 2:4, class = 'data.frame'));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist13() {
        assertEval("argv <- list(structure(list(a = c(1L, 2L, 3L, NA), b = c(NA, 3.14159265358979, 3.14159265358979, 3.14159265358979), c = c(TRUE, NA, FALSE, TRUE), d = structure(c(1L, 2L, NA, 3L), .Label = c('aa', 'bb', 'dd'), class = 'factor'), e = structure(c('a1', NA, NA, 'a4'), class = 'AsIs'), f = structure(c(11323, NA, NA, 12717), class = 'Date')), .Names = c('a', 'b', 'c', 'd', 'e', 'f'), row.names = c(NA, -4L), class = 'data.frame'));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist14() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.pairlist(argv[[1]]);");
    }

    @Test
    public void testispairlist16() {
        assertEval("argv <- list(NULL);do.call('is.pairlist', argv)");
    }
}
