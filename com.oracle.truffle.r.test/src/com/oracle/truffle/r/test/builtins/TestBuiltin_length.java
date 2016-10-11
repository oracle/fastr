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
public class TestBuiltin_length extends TestBase {

    @Test
    public void testlength1() {
        assertEval("argv <- list('~ . + Soft+M.user:Temp');length(argv[[1]]);");
    }

    @Test
    public void testlength2() {
        assertEval("argv <- list(structure(list(A = c(1L, NA, 1L), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA, NA, NA), E = c(FALSE, NA, TRUE), F = structure(c(1L, NA, 2L), .Label = c('abc', 'def'), class = 'factor')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')));length(argv[[1]]);");
    }

    @Test
    public void testlength3() {
        assertEval(Ignored.Unknown, "argv <- list(structure('     \\\'Le français, c'est façile: Règles, Liberté, Egalité, Fraternité...\\\')\\n', Rd_tag = 'RCODE'));length(argv[[1]]);");
    }

    @Test
    public void testlength4() {
        assertEval("argv <- list(structure(list(a = 6:10), .Names = 'a', row.names = 6:10, class = 'data.frame'));length(argv[[1]]);");
    }

    @Test
    public void testlength5() {
        assertEval("argv <- list(structure(list(`log(x)` = c(0, 0.693147180559945, 1.09861228866811, 1.38629436111989, 1.6094379124341, 1.79175946922805, 1.94591014905531, 2.07944154167984, 2.19722457733622, 2.30258509299405, 2.39789527279837, 2.484906649788, 2.56494935746154, 2.63905732961526, 2.70805020110221, 2.77258872223978, 2.83321334405622, 2.89037175789616, 2.94443897916644, 2.99573227355399, 3.04452243772342, 3.09104245335832, 3.13549421592915, 3.17805383034795, 3.2188758248682, 3.25809653802148, 3.29583686600433, 3.3322045101752, 3.36729582998647, 3.40119738166216, 3.43398720448515, 3.46573590279973, 3.49650756146648, 3.52636052461616, 3.55534806148941, 3.58351893845611, 3.61091791264422, 3.63758615972639, 3.66356164612965, 3.68887945411394, 3.71357206670431, 3.73766961828337, 3.76120011569356, 3.78418963391826, 3.80666248977032, 3.8286413964891, 3.85014760171006, 3.87120101090789, 3.89182029811063, 3.91202300542815, 3.93182563272433, 3.95124371858143, 3.97029191355212, 3.98898404656427, 4.00733318523247, 4.02535169073515, 4.04305126783455, 4.06044301054642, 4.07753744390572, 4.0943445622221, 4.11087386417331, 4.12713438504509, 4.14313472639153, 4.15888308335967, 4.17438726989564, 4.18965474202643, 4.20469261939097, 4.21950770517611, 4.23410650459726, 4.24849524204936, 4.26267987704132, 4.27666611901606, 4.29045944114839, 4.30406509320417, 4.31748811353631, 4.33073334028633, 4.34380542185368, 4.35670882668959, 4.36944785246702, 4.38202663467388, 4.39444915467244, 4.40671924726425, 4.4188406077966, 4.43081679884331, 4.44265125649032, 4.45434729625351, 4.46590811865458, 4.47733681447821, 4.48863636973214, 4.49980967033027, 4.51085950651685, 4.52178857704904, 4.53259949315326, 4.54329478227, 4.55387689160054, 4.56434819146784, 4.57471097850338, 4.58496747867057, 4.59511985013459, 4.60517018598809), `log(z)` = c(2.39789527279837, 2.484906649788, 2.56494935746154, 2.63905732961526, 2.70805020110221, 2.77258872223978, 2.83321334405622, 2.89037175789616, 2.94443897916644, 2.99573227355399, 3.04452243772342, 3.09104245335832, 3.13549421592915, 3.17805383034795, 3.2188758248682, 3.25809653802148, 3.29583686600433, 3.3322045101752, 3.36729582998647, 3.40119738166216, 3.43398720448515, 3.46573590279973, 3.49650756146648, 3.52636052461616, 3.55534806148941, 3.58351893845611, 3.61091791264422, 3.63758615972639, 3.66356164612965, 3.68887945411394, 3.71357206670431, 3.73766961828337, 3.76120011569356, 3.78418963391826, 3.80666248977032, 3.8286413964891, 3.85014760171006, 3.87120101090789, 3.89182029811063, 3.91202300542815, 3.93182563272433, 3.95124371858143, 3.97029191355212, 3.98898404656427, 4.00733318523247, 4.02535169073515, 4.04305126783455, 4.06044301054642, 4.07753744390572, 4.0943445622221, 4.11087386417331, 4.12713438504509, 4.14313472639153, 4.15888308335967, 4.17438726989564, 4.18965474202643, 4.20469261939097, 4.21950770517611, 4.23410650459726, 4.24849524204936, 4.26267987704132, 4.27666611901606, 4.29045944114839, 4.30406509320417, 4.31748811353631, 4.33073334028633, 4.34380542185368, 4.35670882668959, 4.36944785246702, 4.38202663467388, 4.39444915467244, 4.40671924726425, 4.4188406077966, 4.43081679884331, 4.44265125649032, 4.45434729625351, 4.46590811865458, 4.47733681447821, 4.48863636973214, 4.49980967033027, 4.51085950651685, 4.52178857704904, 4.53259949315326, 4.54329478227, 4.55387689160054, 4.56434819146784, 4.57471097850338, 4.58496747867057, 4.59511985013459, 4.60517018598809, 4.61512051684126, 4.62497281328427, 4.63472898822964, 4.64439089914137, 4.65396035015752, 4.66343909411207, 4.67282883446191, 4.68213122712422, 4.69134788222914, 4.70048036579242)), .Names = c('log(x)', 'log(z)'), class = 'data.frame', row.names = c(NA, 100L), terms = quote(~log(x) + log(z))));length(argv[[1]]);");
    }

    @Test
    public void testlength6() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list('Residuals vs Fitted', 'Normal Q-Q', 'Scale-Location', 'Cook's distance', 'Residuals vs Leverage', expression('Cook's dist vs Leverage  ' * h[ii]/(1 - h[ii]))));length(argv[[1]]);");
    }

    @Test
    public void testlength7() {
        assertEval("argv <- list(structure(list(sec = c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), hour = c(20L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 20L, 20L, 20L, 20L, 19L, 19L, 19L, 20L, 20L, 20L, 19L, 20L, 19L, 19L, 19L, 20L), mday = c(30L, 31L, 31L, 31L, 31L, 31L, 31L, 31L, 31L, 30L, 30L, 30L, 30L, 31L, 31L, 31L, 30L, 30L, 30L, 31L, 30L, 31L, 31L, 31L, 30L), mon = c(5L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 5L, 5L, 5L, 5L, 11L, 11L, 11L, 5L, 5L, 5L, 11L, 5L, 11L, 11L, 11L, 5L), year = c(72L, 72L, 73L, 74L, 75L, 76L, 77L, 78L, 79L, 81L, 82L, 83L, 85L, 87L, 89L, 90L, 92L, 93L, 94L, 95L, 97L, 98L, 105L, 108L, 112L), wday = c(5L, 0L, 1L, 2L, 3L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 0L, 4L, 0L, 1L, 2L, 3L, 4L, 0L, 1L, 4L, 6L, 3L, 6L), yday = c(181L, 365L, 364L, 364L, 364L, 365L, 364L, 364L, 364L, 180L, 180L, 180L, 180L, 364L, 364L, 364L, 181L, 180L, 180L, 364L, 180L, 364L, 364L, 365L, 181L), isdst = c(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 1L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')));length(argv[[1]]);");
    }

    @Test
    public void testlength8() {
        assertEval("argv <- list(complex(0));length(argv[[1]]);");
    }

    @Test
    public void testlength9() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE));length(argv[[1]]);");
    }

    @Test
    public void testlength10() {
        assertEval("argv <- list(list(structure(c(-1L, -2L, -3L, -4L, -5L, -6L, -7L, -8L, -9L, -10L), .Dim = c(2L, 5L)), structure(list(V1 = 1:5, V2 = 6:10, V3 = 11:15, V4 = 16:20, V5 = 21:25), .Names = c('V1', 'V2', 'V3', 'V4', 'V5'), row.names = c(NA, -5L), class = 'data.frame')));length(argv[[1]]);");
    }

    @Test
    public void testlength11() {
        assertEval("argv <- list(structure(list(weight = c(4.17, 5.58), group = structure(c(1L, 1L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = c('weight', 'group'), row.names = 1:2, class = 'data.frame'));length(argv[[1]]);");
    }

    @Test
    public void testlength12() {
        assertEval("argv <- list(quote(cbind(X, M) ~ M.user + Temp + M.user:Temp));length(argv[[1]]);");
    }

    @Test
    public void testlength13() {
        assertEval("argv <- list(structure(c(-Inf, -Inf, 0, 0, 1, 2, Inf, Inf, Inf, -Inf, -Inf, 0, 0.5, 1, 2, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0, 1, 2, 2, Inf, Inf, -Inf, -Inf, -Inf, 0, 0.8, 1.6, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.3, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.4, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.2, 1.9, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.499999999999999, 1.33333333333333, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.325, Inf, Inf, Inf, Inf), .Dim = c(9L, 9L), .Dimnames = list(c('20%', '30%', '40%', '50%', '60%', '70%', '80%', '90%', '100%'), NULL)));length(argv[[1]]);");
    }

    @Test
    public void testlength14() {
        assertEval("argv <- list(structure(list(character = character(0), numeric = numeric(0), numeric = numeric(0), complex = complex(0), integer = integer(0), logical = logical(0), character = character(0)), .Names = c('character', 'numeric', 'numeric', 'complex', 'integer', 'logical', 'character')));length(argv[[1]]);");
    }

    @Test
    public void testlength15() {
        assertEval("argv <- list(structure(list(loc = c(0.0804034870161223, 10.3548347412639), cov = structure(c(3.01119301965569, 6.14320559215603, 6.14320559215603, 14.7924762275451), .Dim = c(2L, 2L)), d2 = 2, wt = c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0), sqdist = c(0.439364946869246, 0.0143172566761092, 0.783644692619938, 0.766252947443554, 0.346865912102713, 1.41583192825661, 0.168485512965902, 0.354299830956879, 0.0943280426627965, 1.05001058449122, 1.02875556201707, 0.229332323173361, 0.873263925064789, 2.00000009960498, 0.449304354954282, 0.155023307933165, 0.118273979375253, 0.361693898800799, 0.21462398586105, 0.155558909016629, 0.471723661454506, 0.719528696331092, 0.0738164380664225, 1.46001193111051, 0.140785322548143, 0.127761195166703, 0.048012401156175, 0.811750426884519, 0.425827709817574, 0.163016638545231, 0.557810866640707, 0.277350147637843, 0.0781399119055092, 1.29559183995835, 0.718376405567138, 1.37650242941478, 0.175087780508154, 0.233808973148729, 0.693473805463067, 0.189096604125073, 1.96893781800017, 0.4759756980592, 1.69665760380474, 0.277965749373647, 0.920525436884815, 0.57525234053591, 1.59389578665009, 0.175715364671313, 0.972045794851437, 1.75514684962809, 0.0597413185507202, 0.174340343040626, 0.143421553552865, 0.997322770596838, 1.94096736957465, 2.00000001159796, 0.367000821772989, 0.682474530588235, 1.20976163307984, 1.27031685239035, 1.79775635513363, 0.0857761902860323, 0.435578932929501, 0.214370604878221, 0.494714247412686, 1.78784623754399, 1.24216674083069, 1.87749485326709, 0.0533296334123023, 1.45588362584438, 2.00000000631459, 0.208857144738039, 0.119251291573058, 0.365303924649962, 0.690656674239668, 0.0396958405786268, 0.258262120876164, 1.57360254057537, 0.307548421049514, 0.628417063100241, 1.00647098749202, 0.297624360530352, 0.400289147351669, 1.98298426250944, 0.129127182829694, 0.0794695319493149, 0.991481735944321, 0.444068154119836, 0.206790162395106, 0.574310829851377, 0.181887577583334, 0.433872021297517, 0.802994892604009, 0.293053770941001, 1.7002969001965, 0.77984639982848, 1.36127407487932, 0.761935213110323, 0.597915313430067, 0.237134831067472), prob = NULL, tol = 1e-07, eps = 9.96049758228423e-08, it = 898L, maxit = 5000,     ierr = 0L, conv = TRUE), .Names = c('loc', 'cov', 'd2', 'wt', 'sqdist', 'prob', 'tol', 'eps', 'it', 'maxit', 'ierr', 'conv'), class = 'ellipsoid'));length(argv[[1]]);");
    }

    @Test
    public void testlength16() {
        assertEval("argv <- list(c(-167.089651989438, -122.420302709026));length(argv[[1]]);");
    }

    @Test
    public void testlength17() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils'), frow = c(1809L, 1810L, 1811L, 1812L, 1802L, 1827L, 1840L), lrow = c(1809L, 1814L, 1811L, 1813L, 1816L, 1834L, 1842L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 7L)));length(argv[[1]]);");
    }

    @Test
    public void testlength18() {
        assertEval("argv <- list(structure(list(object = structure(3.14159265358979, comment = 'Start with pi'), slots = 'comment', dataPart = TRUE), .Names = c('object', 'slots', 'dataPart')));length(argv[[1]]);");
    }

    @Test
    public void testlength19() {
        assertEval("argv <- list(structure(list(name = structure(c('McNeil', 'Ripley', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'AsIs'), title = structure(c(3L, 6L, 7L, 4L, 2L, 5L), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA, NA, NA, NA, NA, 1L), .Label = c('Ripley', 'Venables & Smith'), class = 'factor'), nationality = structure(c(1L, 2L, 2L, 3L, 3L, 1L), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(1L, 1L, 1L, 1L, 2L, 1L), .Label = c('no', 'yes'), class = 'factor')), .Names = c('name', 'title', 'other.author', 'nationality', 'deceased'), row.names = c(6L, 4L, 5L, 3L, 1L, 2L), class = 'data.frame'));length(argv[[1]]);");
    }

    @Test
    public void testlength20() {
        assertEval("argv <- list(c('  These operators return vectors containing the result of the element', '  by element operations.  The elements of shorter vectors are recycled', '  as necessary (with a ‘warning’ when they are recycled only', '  _fractionally_).  The operators are ‘+’ for addition,', '  ‘-’ for subtraction, ‘*’ for multiplication, ‘/’ for', '  division and ‘^’ for exponentiation.', '', '  ‘%%’ indicates ‘x mod y’ and ‘%/%’ indicates', '  integer division.  It is guaranteed that '));length(argv[[1]]);");
    }

    @Test
    public void testlength21() {
        assertEval("argv <- list(structure(c(3.14475800140539, 3.11465478132706, 3.10630529271564, 3.0844667956717, 3.10602734436792, 3.1179780987041, 3.10510218928681, 3.13870964347838, 3.1236627058491, 3.16426296817658, 3.18524449375727, 3.19607967740367, 3.12404668400251, 3.1073799072767, 3.10252776401906, 3.0888846453793, 3.10244112014795, 3.1099501880355, 3.10186319790916, 3.12297248377609, 3.11352136079872, 3.13902281247452, 3.1522015282299, 3.15900722027104), .Tsp = c(1983, 1984.91666666667, 12), class = 'ts'));length(argv[[1]]);");
    }

    @Test
    public void testlength22() {
        assertEval("argv <- list(c(3.14159265358979e-10, 0.0314159265358979, 3.14159265358979, 31.4159265358979, 314.159265358979, 314159265.358979, 3.14159265358979e+20));length(argv[[1]]);");
    }

    @Test
    public void testlength23() {
        assertEval("argv <- list(structure(list(names = character(0), row.names = integer(0), class = 'data.frame'), .Names = c('names', 'row.names', 'class')));length(argv[[1]]);");
    }

    @Test
    public void testlength24() {
        assertEval("argv <- list(structure(list(a = 1), .Dim = 1L, .Dimnames = list('a')));length(argv[[1]]);");
    }

    @Test
    public void testlength25() {
        assertEval("argv <- list(c(0+0i, -0.740437474899139-0.482946826369552i, -0.333166449062945-0.753763230370951i, 0+0i, 0.522033838837248+0.102958580568997i, 0+0i, 0+0i, 0+0i, -0.563222209454641-0.518013590538404i, -0.068796124369349+0.97981641556181i, 0.244428915757284-0.330850507052219i, 0+0i, 0+0i, 0+0i, -0.451685375030484+0.126357395265016i, 0.375304016677864+0.436900190874168i, -0.674059300339186+0.084416799015191i, 0+0i, 0.509114684244823-0.086484623694157i, -0.535642839219739+0.289927561259502i, 0.629727249341749+0.707648659913726i, 0+0i, -0.333800277698424-0.317646103980588i, -0.422186107911717+0.317002735170286i, -0.616692335171505+0.068946145379939i, -0.136100485502624-0.487679764177213i, -0.68086000613138+0.047032323152903i, 0.296209908189768+0.585533462557103i, 0.43280012844045+0.136998748692477i, -0.680205941942733-0.256569497284745i, 0+0i, 0+0i, 0+0i, -0.983769553611346-0.088288289740869i, -0.046488672133508-0.622109071207677i, 0+0i, 0.379095891586975-0.727769566649926i, 0+0i, 0+0i, -0.150428076228347+0.615598727377677i, 0.762964492726935+0.377685645913312i, -0.7825325866026+0.365371705974346i, -0.792443423040311-0.029652870362208i, 0.265771060547393-0.106618612674382i, -0.076741350022367-0.422144111460857i, 0.120061986786934-0.623033085890884i, 0+0i, -0.145741981978782+0.529165019069452i, 0+0i, 0+0i, 0+0i, 0+0i, 0+0i, 0.328611964770906+0.215416587846774i, -0.583053183540166-0.668235480667835i, -0.782507286391418+0.318827979750013i, 0+0i, 0+0i, 0+0i, 0+0i, -0.271871452223431+0.426340387811162i, 0.590808184713385-0.344468770084509i, 0+0i, 0+0i, 0+0i, 0+0i, 0.866602113481861-0.172567291859327i, 0.031389337713892-0.607820631329035i, 0+0i, 0+0i, 0.151969488085021-0.827990593142535i, -0.266853748421854-0.866413193943766i, 0.071623062591495-0.867246686843546i, -0.788765741891382+0.508717463380604i, -0.228835546857432-0.349587041980114i, 0.500139791176978-0.016703152458872i, 0.15619107374708-0.485402548890295i, -0.369039310626083+0.398423724273751i, 0+0i, -0.399467692630093-0.421179989556223i, 0.411274074028001+0.133781691724871i, 0.573364366690245+0.328833257005489i, 0+0i, 0.387209171815106+0.750271083217101i, 0+0i, 0+0i, -0.168543113030619+0.43048451175239i, 0+0i, 0.388005062566602-0.290649953587954i, -0.013004326537709-0.490434895455784i, 0.069845221019376-0.762134635168809i, 0+0i, 0.27384734040072+0.383667165938905i, 0+0i, -0.894951082455532+0.317442909372288i, 0.5073401683933-0.213001485168032i, 0+0i, -0.343169835663372+0.597359384628839i, -0.283179001991236-0.385834501657171i, -0.517794900198098-0.36732932802092i));length(argv[[1]]);");
    }

    @Test
    public void testlength26() {
        assertEval("argv <- list(structure(list(sec = numeric(0), min = integer(0), hour = integer(0), mday = integer(0), mon = integer(0), year = integer(0), wday = integer(0), yday = integer(0), isdst = integer(0)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')));length(argv[[1]]);");
    }

    @Test
    public void testlength27() {
        assertEval("argv <- list(c(FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE));length(argv[[1]]);");
    }

    @Test
    public void testlength28() {
        assertEval("argv <- list(structure(list(A = c(1, NA, 1), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA_integer_, NA_integer_, NA_integer_), E = c(FALSE, NA, TRUE), F = c('abc', NA, 'def')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')));length(argv[[1]]);");
    }

    @Test
    public void testlength29() {
        assertEval("argv <- list(list(structure(list(srcfile = c('/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R'), frow = 122:123, lrow = 122:123), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'), structure(list(srcfile = '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/rlm.R', frow = 124L, lrow = 124L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame')));length(argv[[1]]);");
    }

    @Test
    public void testlength30() {
        assertEval("argv <- list(structure(list(Df = c(NA, 1, 1, 1), `Sum of Sq` = c(NA, 820.907401534698, 26.7893827563485, 9.93175377572661), RSS = c(47.9727294003871, 868.880130935086, 74.7621121567356, 57.9044831761137), AIC = c(24.9738836085411, 60.6293256496563, 28.7417044039189, 25.4199908988691)), .Names = c('Df', 'Sum of Sq', 'RSS', 'AIC'), row.names = c('<none>', '- x1', '- x2', '- x4'), class = c('anova', 'data.frame')));length(argv[[1]]);");
    }

    @Test
    public void testlength31() {
        assertEval("argv <- list(list(structure(c(30.3398431424662, 53.0273088677942, 11.6721423374092, 3.1219098789343, 9.58888402166762, 41.0027598267751, 8.26816396385794, 46.7997906867199, 7.96616780447507, 27.2952699050281, 7.05399789883986, 5.03904688224502, 2.61409832611023, 14.9537930856989, 7.22312484916424, 8.25480759597494, 37.7467076615774, 65.972863357068, 14.5216619125438, 3.88406159493231, 10.6419076139158, 45.5055646732323, 9.17614988785547, 51.9392087455927, 7.84624470450625, 26.8843655076016, 6.94780665155944, 4.96318881123281, 2.45558360989303, 14.0470191347445, 6.78512618085463, 7.75424934595279), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32')), 2.22044604925031e-16));length(argv[[1]]);");
    }

    @Test
    public void testlength32() {
        assertEval("argv <- list(structure(c(78796800, 94694400, 126230400, 157766400, 189302400, 220924800, 252460800, 283996800, 315532800, 362793600, 394329600, 425865600, 489024000, 567993600, 631152000, 662688000, 709948800, 741484800, 773020800, 820454400, 867715200, 915148800, 1136073600, 1230768000, 1341100800), class = c('POSIXct', 'POSIXt')));length(argv[[1]]);");
    }

    @Test
    public void testlength33() {
        assertEval("argv <- list(c(1.03711990677284e+29, 4.58346574364236e+27, 2.02562481791768e+26, 8.95208153058638e+24, 3.95629847255715e+23, 1.7484534240517e+22, 7.7271454561408e+20, 34149480972585590784, 1509208102926327040, 66698206089453432, 2947672264272576, 130269967045726, 5757178273805.44, 254434569303.796, 11244672651.0134, 496975017.177538, 21967997.2327598, 971670.715389718, 43086.3667907314, 1929.95757399166, 90.0797181275922, 5.03402841668789, 0.625146618950265, -0.304867530220417, -0.123332207492738, 0.0884498083502638, 0.0243523396488189, -0.0527015109337102, -3.68088311960635e-05, -0.0351989174304481, 0.024656114194774));length(argv[[1]]);");
    }

    @Test
    public void testlength34() {
        assertEval("argv <- list(structure(c('1.0', NA, NA, 'methods, graphics, pkgA', NA, NA, NA, 'GPL (>= 2)', NA, NA, NA, NA, NA, NA, 'R 3.0.1; ; 2014-03-17 18:49:56 UTC; unix'), .Names = c('Version', NA, NA, 'Imports', NA, NA, NA, 'License', NA, NA, NA, NA, NA, NA, 'Built')));length(argv[[1]]);");
    }

    @Test
    public void testlength35() {
        assertEval("argv <- list(structure(c(-1, 0, 1, 2, 3), .Tsp = c(-1, 3, 1)));length(argv[[1]]);");
    }

    @Test
    public void testlength37() {
        assertEval(Ignored.Unknown,
                        "argv <- list(function(file = ifelse(onefile, 'Rplots.pdf', 'Rplot%03d.pdf'),     width, height, onefile, family, title, fonts, version, paper,     encoding, bg, fg, pointsize, pagecentre, colormodel, useDingbats,     useKerning, fillOddEven, compress) {    initPSandPDFfonts()    new <- list()    if (!missing(width)) new$width <- width    if (!missing(height)) new$height <- height    if (!missing(onefile)) new$onefile <- onefile    if (!missing(title)) new$title <- title    if (!missing(fonts)) new$fonts <- fonts    if (!missing(version)) new$version <- version    if (!missing(paper)) new$paper <- paper    if (!missing(encoding)) new$encoding <- encoding    if (!missing(bg)) new$bg <- bg    if (!missing(fg)) new$fg <- fg    if (!missing(pointsize)) new$pointsize <- pointsize    if (!missing(pagecentre)) new$pagecentre <- pagecentre    if (!missing(colormodel)) new$colormodel <- colormodel    if (!missing(useDingbats)) new$useDingbats <- useDingbats    if (!missing(useKerning)) new$useKerning <- useKerning    if (!missing(fillOddEven)) new$fillOddEven <- fillOddEven    if (!missing(compress)) new$compress <- compress    old <- check.options(new, name.opt = '.PDF.Options', envir = .PSenv)    if (!missing(family) && (inherits(family, 'Type1Font') ||         inherits(family, 'CIDFont'))) {        enc <- family$encoding        if (inherits(family, 'Type1Font') && !is.null(enc) &&             enc != 'default' && (is.null(old$encoding) || old$encoding ==             'default')) old$encoding <- enc        family <- family$metrics    }    if (is.null(old$encoding) || old$encoding == 'default') old$encoding <- guessEncoding()    if (!missing(family)) {        if (length(family) == 4L) {            family <- c(family, 'Symbol.afm')        } else if (length(family) == 5L) {        } else if (length(family) == 1L) {            pf <- pdfFonts(family)[[1L]]            if (is.null(pf)) stop(gettextf('unknown family '%s'',                 family), domain = NA)            matchFont(pf, old$encoding)        } else stop('invalid 'family' argument')        old$family <- family    }    version <- old$version    versions <- c('1.1', '1.2', '1.3', '1.4', '1.5', '1.6', '1.7',         '2.0')    if (version %in% versions) version <- as.integer(strsplit(version,         '[.]')[[1L]]) else stop('invalid PDF version')    onefile <- old$onefile    if (!checkIntFormat(file)) stop(gettextf('invalid 'file' argument '%s'',         file), domain = NA)    .External(C_PDF, file, old$paper, old$family, old$encoding,         old$bg, old$fg, old$width, old$height, old$pointsize,         onefile, old$pagecentre, old$title, old$fonts, version[1L],         version[2L], old$colormodel, old$useDingbats, old$useKerning,         old$fillOddEven, old$compress)    invisible()});" +
                                        "do.call('length', argv)");
    }

    @Test
    public void testLength() {
        assertEval("{ length(c(z=1:4)) }");
        assertEval("{ length(1) }");
        assertEval("{ length(NULL) }");
        assertEval("{ length(NA) }");
        assertEval("{ length(TRUE) }");
        assertEval("{ length(1L) }");
        assertEval("{ length(1+1i) }");
        assertEval("{ length(d<-dim(1:3)) }");
        assertEval("{ length(1:3) }");
        assertEval("length(quote(x))");
        assertEval("length(as.symbol('x'))");
    }
}
