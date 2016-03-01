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
public class TestBuiltin_names extends TestBase {

    @Test
    public void testnames1() {
        assertEval("argv <- list(structure(list(size = 113, isdir = FALSE, mode = structure(436L, class = 'octmode'), mtime = structure(1395082088.72988, class = c('POSIXct', 'POSIXt')), ctime = structure(1395082088.72988, class = c('POSIXct', 'POSIXt')), atime = structure(1395082088.77388, class = c('POSIXct', 'POSIXt')), uid = 1001L, gid = 1001L, uname = 'roman', grname = 'roman'), .Names = c('size', 'isdir', 'mode', 'mtime', 'ctime', 'atime', 'uid', 'gid', 'uname', 'grname'), class = 'data.frame', row.names = '/tmp/RtmptPgrXI/file55711ba85492'));names(argv[[1]]);");
    }

    @Test
    public void testnames2() {
        assertEval("argv <- list(list(character(0), numeric(0), numeric(0), complex(0), integer(0), logical(0), character(0)));names(argv[[1]]);");
    }

    @Test
    public void testnames3() {
        assertEval("argv <- list(structure(list(), .Names = character(0), row.names = integer(0), class = 'data.frame'));names(argv[[1]]);");
    }

    @Test
    public void testnames4() {
        assertEval("argv <- list(structure(list(A = NULL, B = NULL, `NA` = NULL), .Names = c('A', 'B', NA)));names(argv[[1]]);");
    }

    @Test
    public void testnames6() {
        assertEval("argv <- list(structure(list(groups = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('1', '2', '3'), class = 'factor')), .Names = 'groups'));names(argv[[1]]);");
    }

    @Test
    public void testnames7() {
        assertEval("argv <- list(structure(1:20, .Tsp = c(1, 20, 1), class = 'ts'));names(argv[[1]]);");
    }

    @Test
    public void testnames8() {
        assertEval("argv <- list(structure(c(12L, 120L, 116L), .Dim = 3L, .Dimnames = structure(list(c('0-5yrs', '6-11yrs', '12+ yrs')), .Names = ''), class = 'table'));names(argv[[1]]);");
    }

    @Test
    public void testnames9() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(sec = c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), min = c(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), hour = c(20L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 19L, 20L, 20L, 20L, 20L, 19L, 19L, 19L, 20L, 20L, 20L, 19L, 20L, 19L, 19L, 19L, 20L), mday = c(30L, 31L, 31L, 31L, 31L, 31L, 31L, 31L, 31L, 30L, 30L, 30L, 30L, 31L, 31L, 31L, 30L, 30L, 30L, 31L, 30L, 31L, 31L, 31L, 30L), mon = c(5L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 5L, 5L, 5L, 5L, 11L, 11L, 11L, 5L, 5L, 5L, 11L, 5L, 11L, 11L, 11L, 5L), year = c(72L, 72L, 73L, 74L, 75L, 76L, 77L, 78L, 79L, 81L, 82L, 83L, 85L, 87L, 89L, 90L, 92L, 93L, 94L, 95L, 97L, 98L, 105L, 108L, 112L), wday = c(5L, 0L, 1L, 2L, 3L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 0L, 4L, 0L, 1L, 2L, 3L, 4L, 0L, 1L, 4L, 6L, 3L, 6L), yday = c(181L, 365L, 364L, 364L, 364L, 365L, 364L, 364L, 364L, 180L, 180L, 180L, 180L, 364L, 364L, 364L, 181L, 180L, 180L, 364L, 180L, 364L, 364L, 365L, 181L), isdst = c(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, 1L, 1L, 1L, 0L, 1L, 0L, 0L, 0L, 1L)), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = c('', 'EST', 'EDT')));names(argv[[1]]);");
    }

    @Test
    public void testnames10() {
        assertEval("argv <- list(structure(list(A = c(1L, NA, 1L), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA, NA, NA), E = c(FALSE, NA, TRUE), F = structure(c(1L, NA, 2L), .Label = c('abc', 'def'), class = 'factor')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')));names(argv[[1]]);");
    }

    @Test
    public void testnames11() {
        assertEval("argv <- list(structure(c(NA, NA, NA, NA, NA, 1L, 2L), .Label = c('Ripley', 'Venables & Smith'), class = 'factor'));names(argv[[1]]);");
    }

    @Test
    public void testnames12() {
        assertEval("argv <- list(structure(list(V1 = c(-1L, -2L, 1L, 2L, 3L, 4L, 5L), V2 = c(-3L, -4L, 6L, 7L, 8L, 9L, 10L), V3 = c(-5L, -6L, 11L, 12L, 13L, 14L, 15L), V4 = c(-7L, -8L, 16L, 17L, 18L, 19L, 20L), V5 = c(-9L, -10L, 21L, 22L, 23L, 24L, 25L)), .Names = c('V1', 'V2', 'V3', 'V4', 'V5'), row.names = c(NA, 7L), class = 'data.frame'));names(argv[[1]]);");
    }

    @Test
    public void testnames13() {
        assertEval("argv <- list(structure(list(Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551), GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962), .Names = c('Employed', 'GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year'), class = 'data.frame', row.names = 1947:1962, terms = quote(Employed ~     GNP.deflator + GNP + Unemployed + Armed.Forces + Population + Year)));names(argv[[1]]);");
    }

    @Test
    public void testnames14() {
        assertEval("argv <- list(structure(list(ii = 1:10, xx = c(-9.42477796076938, -6.28318530717959, -3.14159265358979, 0, 3.14159265358979, 6.28318530717959, 9.42477796076938, 12.5663706143592, 15.707963267949, 18.8495559215388)), .Names = c('ii', 'xx')));names(argv[[1]]);");
    }

    @Test
    public void testnames15() {
        assertEval("argv <- list(structure(list(`cbind(X, M)` = structure(c(68, 42, 37, 24, 66, 33, 47, 23, 63, 29, 57, 19, 42, 30, 52, 43, 50, 23, 55, 47, 53, 27, 49, 29), .Dim = c(12L, 2L), .Dimnames = list(NULL, c('X', 'M'))), M.user = structure(c(1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L), .Label = c('N', 'Y'), class = 'factor'), Temp = structure(c(2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L), .Label = c('High', 'Low'), class = 'factor'), Soft = structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L), .Label = c('Hard', 'Medium', 'Soft'), class = 'factor')), .Names = c('cbind(X, M)', 'M.user', 'Temp', 'Soft'), class = 'data.frame', row.names = c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23'), terms = quote(cbind(X, M) ~ M.user + Temp + Soft)));names(argv[[1]]);");
    }

    @Test
    public void testnames16() {
        assertEval("argv <- list(structure(list(), .Names = character(0)));names(argv[[1]]);");
    }

    @Test
    public void testnames17() {
        assertEval("argv <- list(c(1281L, 1283L));names(argv[[1]]);");
    }

    @Test
    public void testnames18() {
        assertEval("argv <- list(structure(list(itemBullet = '• '), .Names = 'itemBullet'));names(argv[[1]]);");
    }

    @Test
    public void testnames19() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils', '/home/lzhao/hg/r-instrumented/library/utils/R/utils'), frow = c(2417L, 2417L, 2418L, 2418L, 2420L, 2420L, 2422L, 2422L, 2423L, 2424L, 2426L, 2426L, 2426L, 2426L), lrow = c(2417L, 2417L, 2419L, 2419L, 2421L, 2421L, 2422L, 2422L, 2434L, 2425L, 2433L, 2433L, 2433L, 2433L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 14L), class = 'data.frame'));names(argv[[1]]);");
    }

    @Test
    public void testnames20() {
        assertEval("argv <- list(c(-21.222245139688+176.377752294836i, -21.222245139688-176.377752294836i, 61.0965873274464+76.7794305756989i, 61.0965873274464-76.7794305756989i, -11.748684375517+0i));names(argv[[1]]);");
    }

    @Test
    public void testnames21() {
        assertEval("argv <- list(structure(list(a = 1), .Dim = 1L, .Dimnames = list('a')));names(argv[[1]]);");
    }

    @Test
    public void testnames22() {
        assertEval("argv <- list(structure(list(`Surv(time, status)` = structure(c(9, 1, 1, 6, 6, 8, 1, 1, 0, 1, 1, 0), .Dim = c(6L, 2L), .Dimnames = list(NULL, c('time', 'status')), class = 'Surv', type = 'right'), x = c(0, 1, 1, 1, 0, 0)), .Names = c('Surv(time, status)', 'x'), class = 'data.frame', row.names = c(1L, 3L, 4L, 5L, 6L, 7L)));names(argv[[1]]);");
    }

    @Test
    public void testnames23() {
        assertEval("argv <- list(structure(list(xlev = structure(list(), .Names = character(0))), .Names = 'xlev'));names(argv[[1]]);");
    }

    @Test
    public void testnames24() {
        assertEval("argv <- list(structure(c(0.434200949779115, NA, 0.907914219551846, 0.907914219551846, 0.907914219551846, 0.434200949779115, 0.434200949779115), .Names = c('1', NA, '3', '4', '5', '6', '7')));names(argv[[1]]);");
    }

    @Test
    public void testnames25() {
        assertEval("argv <- list(c(0, 0, 0, 0, 0, 1.75368801162502e-134, 0, 0, 0, 2.60477585273833e-251, 1.16485035372295e-260, 0, 1.53160350210786e-322, 0.333331382328728, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3.44161262707711e-123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.968811545398e-173, 0, 8.2359965384697e-150, 0, 0, 0, 0, 6.51733217171341e-10, 0, 2.36840184577368e-67, 0, 9.4348408357524e-307, 0, 1.59959906013771e-89, 0, 8.73836857865034e-286, 7.09716190970992e-54, 0, 0, 0, 1.530425353017e-274, 8.57590058044551e-14, 0.333333106397154, 0, 0, 1.36895217898448e-199, 2.0226102635783e-177, 5.50445388209462e-42, 0, 0, 0, 0, 1.07846402051283e-44, 1.88605464411243e-186, 1.09156111051203e-26, 0, 3.0702877273237e-124, 0.333333209689785, 0, 0, 0, 0, 0, 0, 3.09816093866831e-94, 0, 0, 4.7522727332095e-272, 0, 0, 2.30093251441394e-06, 0, 0, 1.27082826644707e-274, 0, 0, 0, 0, 0, 0, 0, 4.5662025456054e-65, 0, 2.77995853978268e-149, 0, 0, 0));names(argv[[1]]);");
    }

    @Test
    public void testnames26() {
        assertEval("argv <- list(structure(c(3.22711508258913, 3.17840134153376, 3.17811325231463, 3.14144977340047, 3.21272015441784, 3.17926446433903, 3.19284611518884, 3.21218760440396, 3.19838213000829, 3.21827285357145, 3.33284226699435, 3.33203427702752, 3.24353410183206, 3.24674470972384, 3.23477029516092, 3.19256745333655, 3.19728055812562, 3.18184358794477, 3.25647720624168, 3.25527250510331, 3.23527587668705, 3.30276370847298, 3.35063560825895, 3.39410130204004, 3.30749603791321, 3.21879799811174, 3.22865695810894, 3.21031851982623, 3.25647720624168, 3.24204423936955, 3.25406445291434, 3.28465628278852, 3.20924684875337, 3.29928933408768, 3.34888872307144, 3.34084054981233, 3.31806333496276, 3.24748226067705, 3.26363606858811, 3.19562294358694, 3.29578694025161, 3.2678754193189, 3.29336255471145, 3.22762964957101, 3.24993175663419, 3.29578694025161, 3.37966803403365, 3.42390091852842, 3.32159843046534, 3.29292029960001, 3.22453306260609, 3.28802553538836, 3.30168094929358, 3.25839780409551, 3.30362797638389, 3.28148788794008, 3.31889771462749, 3.31806333496276, 3.32592595577147, 3.33243845991561, 3.20628604441243, 3.17695898058691, 3.18977095634687, 3.14050804303818, 3.23829706787539, 3.25478968739721, 3.25017594808393, 3.27577190016493, 3.30189771719521, 3.3174364965351, 3.32056168019524, 3.31196566036837, 3.1978316933289, 3.13225968953104, 3.21801004298436, 3.14050804303818, 3.18155777386279, 3.15259407792747, 3.15896526038341, 3.18836592606315, 3.21906033244886, 3.19340290306242, 3.27989498001164, 3.34222522936079, 3.16820274684263, 3.21879799811174, 3.14829409743475, 3.14457420760962, 3.1846914308176, 3.11693964655076, 3.18355453361886, 3.12287092286444, 3.21138755293686, 3.24254142829838, 3.29181268746712, 3.35679046035172, 3.2169572073611, 3.14643813528577, 3.14952701375435, 3.14705767102836, 3.14426277376199, 3.18184358794477, 3.18412335423967, 3.21563756343506, 3.18041263283832, 3.22659990520736, 3.30102999566398, 3.34537373055909, 3.29136885045158, 3.16494737262184, 3.19395897801919, 3.16405529189345, 3.16016829295851, 3.21005084987514, 3.21932250841934, 3.2143138974244, 3.21563756343506, 3.22608411597582, 3.31175386105575, 3.35449260058944, 3.25839780409551, 3.15986784709257, 3.24600590407603, 3.1646502159343, 3.19200959265367, 3.15563963375978, 3.15442397311465, 3.1914510144649, 3.21616590228599, 3.21827285357145, 3.30449052777349, 3.34380233316165, 3.22141423784234, 3.13385812520333, 3.17782497186468, 3.13353890837022, 3.16226561429802, 3.18241465243455, 3.16435285578444, 3.19089171692217, 3.18977095634687, 3.26173854735254, 3.2397998184471, 3.28802553538836, 3.16849748352303, 3.16375752398196, 3.18808437371494, 3.14736710779379, 3.18241465243455, 3.14144977340047, 3.21510858105309, 3.17897694729317, 3.22556771343947, 3.28735377271475, 3.27137687189407, 3.23704079137919, 3.16316137497702, 3.15986784709257, 3.16316137497702, 3.13513265137677, 3.17231096852195, 3.19256745333655, 3.17260293120986, 3.22634208716363, 3.20248831706009, 3.26717172840301, 3.30059548388996, 3.31785448933147), .Tsp = c(1969, 1982.91666666667, 12), class = 'ts'));names(argv[[1]]);");
    }

    @Test
    public void testnames27() {
        assertEval("argv <- list(structure(list(object = structure(3.14159265358979, comment = 'Start with pi'), slots = 'comment', dataPart = TRUE, class = structure('classPrototypeDef', package = 'methods')), .Names = c('object', 'slots', 'dataPart', 'class')));names(argv[[1]]);");
    }

    @Test
    public void testnames28() {
        assertEval("argv <- list(list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render'))));names(argv[[1]]);");
    }

    @Test
    public void testnames29() {
        assertEval("argv <- list(structure(list(zz = complex(0)), .Names = 'zz'));names(argv[[1]]);");
    }

    @Test
    public void testnames31() {
        assertEval("argv <- list(list(structure(list(srcfile = c('/home/lzhao/tmp/RtmpTzriDZ/R.INSTALL30d4108a07be/mgcv/R/gam.fit3.r', '/home/lzhao/tmp/RtmpTzriDZ/R.INSTALL30d4108a07be/mgcv/R/gam.fit3.r'), frow = c(1287L, 1289L), lrow = c(1287L, 1289L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'), structure(list(srcfile = '/home/lzhao/tmp/RtmpTzriDZ/R.INSTALL30d4108a07be/mgcv/R/gam.fit3.r', frow = 1289L, lrow = 1289L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L), class = 'data.frame')));names(argv[[1]]);");
    }

    @Test
    public void testnames32() {
        assertEval("argv <- list(structure(list(trace = 0, fnscale = 1, parscale = c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), ndeps = c(0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001, 0.001), maxit = 100L, abstol = -Inf, reltol = 1.49011611938477e-08, alpha = 1, beta = 0.5, gamma = 2, REPORT = 10, type = 1, lmm = 5, factr = 1e+07, pgtol = 0, tmax = 10, temp = 10), .Names = c('trace', 'fnscale', 'parscale', 'ndeps', 'maxit', 'abstol', 'reltol', 'alpha', 'beta', 'gamma', 'REPORT', 'type', 'lmm', 'factr', 'pgtol', 'tmax', 'temp')));names(argv[[1]]);");
    }

    @Test
    public void testnames33() {
        assertEval("argv <- list(structure(list(Df = c(NA, 0L), Deviance = c(NA, 0), `Resid. Df` = c(10L, 10L), `Resid. Dev` = c(2.74035772634541, 2.74035772634541)), .Names = c('Df', 'Deviance', 'Resid. Df', 'Resid. Dev'), row.names = c('NULL', 'x'), class = c('anova', 'data.frame'), heading = 'Analysis of Deviance Table\\n\\nModel: gaussian, link: identity\\n\\nResponse: y\\n\\nTerms added sequentially (first to last)\\n\\n'));names(argv[[1]]);");
    }

    @Test
    public void testnames34() {
        assertEval("argv <- list(structure(c(100, -1e-13, Inf, -Inf, NaN, 3.14159265358979, NA), .Names = c(' 100', '-1e-13', ' Inf', '-Inf', ' NaN', '3.14', '  NA')));names(argv[[1]]);");
    }

    @Test
    public void testnames35() {
        assertEval("argv <- list(structure(list(y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861), x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), z = 1:10), .Names = c('y', 'x', 'z'), class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ x * z)));names(argv[[1]]);");
    }

    @Test
    public void testnames36() {
        assertEval("argv <- list(structure(c(2671, 6.026e+77, 3.161e+152, 3.501e+299, 2.409e+227, 1.529e+302), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.'), class = 'table'));names(argv[[1]]);");
    }

    @Test
    public void testnames37() {
        assertEval("argv <- list(structure(list(surname = structure(c('McNeil', 'Ripley', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'AsIs'), nationality = structure(c('Australia', 'UK', 'UK', 'US', 'US', 'Australia'), class = 'AsIs'), deceased = structure(c('no', 'no', 'no', 'no', 'yes', 'no'), class = 'AsIs'), title = structure(c('Interactive Data Analysis', 'Spatial Statistics', 'Stochastic Simulation', 'LISP-STAT', 'Exploratory Data Analysis', 'Modern Applied Statistics ...'), class = 'AsIs'), other.author = structure(c(NA, NA, NA, NA, NA, 'Ripley'), class = 'AsIs')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author')));names(argv[[1]]);");
    }

    @Test
    public void testnames38() {
        assertEval("argv <- list(structure(list(head = logical(0)), .Names = 'head', class = 'data.frame', row.names = integer(0)));names(argv[[1]]);");
    }

    @Test
    public void testNames() {
        assertEval("v <- parse(text=\"useDynLib(digest, digest_impl=digest)\"); names(v[[1]][3])");
        assertEval("v <- parse(text=\"useDynLib(digest, digest_impl=digest)\"); names(v[[1]][[3]])");
        assertEval("{ x<-c(1,2,3); dim(x)<-3; dimnames(x)<-list(c(11,12,13)); names(x) }");
        assertEval("{ symNames <- c(\"foobar\", \"bar\"); names(symNames) = symNames; names(names(symNames)); }");
    }

    @Test
    public void testLNames() {
        assertEval("{ x <- quote(plot(x = age, y = weight)); names(x) }");
    }
}
