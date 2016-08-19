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
public class TestBuiltin_isna extends TestBase {

    @Test
    public void testisna1() {
        assertEval("argv <- list(8.21977282218514e-09);is.na(argv[[1]]);");
    }

    @Test
    public void testisna2() {
        assertEval("argv <- list(structure(c(1, 2, 3, 0, 10, NA), .Dim = c(3L, 2L)));is.na(argv[[1]]);");
    }

    @Test
    public void testisna3() {
        assertEval("argv <- list(structure(c(17L, 18L, 18L, 18L), .Dim = 4L, .Dimnames = structure(list(N = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt')), .Names = 'N')));is.na(argv[[1]]);");
    }

    @Test
    public void testisna4() {
        assertEval("argv <- list(structure(0:100, .Tsp = c(1, 101, 1), class = 'ts'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna6() {
        assertEval("argv <- list(list(list(1)));is.na(argv[[1]]);");
    }

    @Test
    public void testisna7() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 9L, mon = 9L, year = 103L, wday = 4L, yday = 281L, isdst = 1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt')));is.na(argv[[1]]);");
    }

    @Test
    public void testisna8() {
        assertEval("argv <- list(structure(c(23L, 24L, 47L, 48L, 71L, 72L, 95L, 96L, 119L, 120L), .Dim = c(2L, 5L), .Dimnames = list(NULL, c('V5', 'V6', 'V7', 'V8', 'V9'))));is.na(argv[[1]]);");
    }

    @Test
    public void testisna10() {
        assertEval("argv <- list(structure(TRUE, .Names = NA_character_));is.na(argv[[1]]);");
    }

    @Test
    public void testisna11() {
        assertEval("argv <- list('•');is.na(argv[[1]]);");
    }

    @Test
    public void testisna12() {
        assertEval("argv <- list(c(1L, 2L, 3L, 4L, 5L, 6L, 7L, NA, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, NA, 16L, NA, NA, 17L, 18L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 19L, 20L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 21L, 22L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 23L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 24L, 25L, NA, NA, NA, NA, NA, 26L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 27L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 28L, NA, NA, NA, NA, NA, NA, NA));is.na(argv[[1]]);");
    }

    @Test
    public void testisna13() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(VAR1 = c(1, 2, 3, 4, 5), VAR3 = c(1, 1, 1, 1, NA)), .Names = c('VAR1', 'VAR3'), class = 'data.frame', row.names = c(NA, -5L)));is.na(argv[[1]]);");
    }

    @Test
    public void testisna14() {
        assertEval("argv <- list(structure(c(2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L), .Label = c('High', 'Low'), class = 'factor'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna15() {
        assertEval("argv <- list(structure('graphics', .Names = 'plot'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna16() {
        assertEval("argv <- list(structure(c(1.47191076131574, 0.586694550701453, NA, 0.258706725324317), .Names = c('(Intercept)', 'x1', 'x2', 'x3')));is.na(argv[[1]]);");
    }

    @Test
    public void testisna17() {
        assertEval("argv <- list(structure(0.0129709025545593, .Names = 'value'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna18() {
        assertEval("argv <- list(c(1.15623864987889e-07, 2.29156215117184e-06, 2.23566813947706e-05, 0.000143191143888661, 0.000677580461489631, 0.00252801907454942, 0.00775156037133752, 0.0201095764491411, 0.0451105149252681, 0.0890234210350955, 0.15678837112652, 0.249535722442692, 0.362988194603088, 0.487807940725587, 0.611969188999548, 0.724126192770213, 0.816469100858263, 0.885981556331846, 0.933947517503216, 0.964353470219262, 0.982092072679401, 0.991629921792979));is.na(argv[[1]]);");
    }

    @Test
    public void testisna19() {
        assertEval("argv <- list(c(-0.560475646552213+0i, 0.7424437487+0.205661411508856i, 1.39139505579429-0.26763356813179i, 0.928710764113827-0.221714979045717i, -0.46926798541295+1.18846175213664i, 0.7424437487-0.205661411508856i, 0.460916205989202+0i, -0.452623703774585+0.170604003753717i, -0.094501186832143+0.54302538277632i, -0.331818442379127+0.612232958468282i, 1.39139505579429+0.26763356813179i, -0.452623703774585-0.170604003753717i, 0.400771450594052+0i, -0.927967220342259+0.479716843914174i, -0.790922791530657+0.043092176305418i, 0.928710764113827+0.221714979045717i, -0.094501186832143-0.54302538277632i, -0.927967220342259-0.479716843914174i, 0.701355901563686+0i, -0.600841318509537+0.213998439984336i, -0.46926798541295-1.18846175213664i, -0.331818442379127-0.612232958468282i, -0.790922791530657-0.043092176305418i, -0.600841318509537-0.213998439984336i, -0.625039267849257+0i));is.na(argv[[1]]);");
    }

    @Test
    public void testisna20() {
        assertEval("argv <- list(structure(list(conc = c(NA, 1.4, NA, NA, NA, NA, NA, NA, 2.2, NA, NA, 0.6)), .Names = 'conc', row.names = 407:418, class = 'data.frame'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna21() {
        assertEval("argv <- list(structure(c(-0.435358622483264, -0.0335050551134034, 0.133034650300067, -0.159998333048696, 0.195871393282558, 0.350272600548034, 0.39175188095922, 0.80136172049434, 0.278604939810076, 0.226807608071186, -0.705366153102363, -0.673376038650154, 1.0575405448001, -0.361896730525493, -0.183063001472974, 0.770224627641576, -0.723327517911943, 0.641508754101234, -0.497966152633253, -0.565883899194175, -0.163154615929682, -1.04605311744929, 0.345822472294285, -0.120642075306238, -0.310915191264717, -0.421459810417331, 0.127313193315093, 0.0460216192070582, -0.571263568938105, -0.255068194977355, 0.466680400648398, -0.577405253130228, 0.427189001707599, -0.117526978398191, 0.338157182237428, 0.106063414615583, 0.0652166343937516, 0.113976880905178, -0.508973211491926, -0.0281367896906678, 0.0255810420505139, -0.0895312259800421, 0.343273059276787, 0.25878029892501, 0.178005594248437, 0.616202813145647, -0.223306051799002, -0.822237650949987, 0.181243736532592, 1.03805050988759, -0.535064558180362, 0.629967292956912, -0.206091625685159, -0.0982523326578774, 0.414371639034748, -0.332128640110978, 0.0280866409684434, -0.53195331347224, -0.0381291521565174, -0.0522348719327905, 0.221019543981438, -0.306904771316101, 0.553064781030607, -0.15705513179628, 0.740342712486913, -0.307570821127478, -0.952143530102099, -0.691835269560791, -0.27190785111766, -0.196035414096589, -0.970405281846342, -0.177170015488179, -0.885843727603163, 0.429176556994819, 0.310459328495626, -0.258604904263182, -1.18516758379134, -0.690975294813695, 0.965849167652318, 0.44535708731974, -0.0846102375086248, -0.32082832908822, 0.528416539604261, 0.620184198607294, 0.317666232431379, 0.283097649168652, 0.223675435302281, -0.697584545802335, 1.0301502006605, 0.452533534739715, 0.264750609900989, 0.267980537616643, 0.0973596082099813, 0.161838610289358, 0.612946027273891, 0.816578471249094, -1.15340096289088, -1.01680545446605, 0.422976667855578, -0.23961110455947, 0.0316786188682291, -0.797164261874229, 0.184311996008136, 0.0876867376986658, 0.312240812855443, 0.0432826980205777, -0.00317515675173313, -0.296692321406956, 0.598755930788477, 0.298681977334167, 0.258864357137695, 0.126248580888692, 0.318393890044881, 0.316636862337678), .Tsp = c(1, 114, 1), class = 'ts'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna22() {
        assertEval("argv <- list(c(-Inf, 2.17292368994844e-311, 4.34584737989688e-311, 8.69169475979376e-311, 1.73833895195875e-310, 3.4766779039175e-310, 6.953355807835e-310, 1.390671161567e-309, 2.781342323134e-309, 5.562684646268e-309, 1.1125369292536e-308, 2.2250738585072e-308, 4.4501477170144e-308, 8.90029543402881e-308, 1.78005908680576e-307, 2.2250738585072e-303, 2.2250738585072e-298, 1.79769313486232e+298, 1.79769313486232e+303, 2.24711641857789e+307, 4.49423283715579e+307, 8.98846567431158e+307, 1.79769313486232e+308, Inf, Inf, NaN, NA));is.na(argv[[1]]);");
    }

    @Test
    public void testisna23() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = 0L, year = 105L, wday = 6L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna24() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna25() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = 0L, year = 60L, wday = 5L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna26() {
        assertEval("argv <- list(structure(list(conc = c(NA, NA, NA, NA, NA, NA, NA, 1.4, NA, NA, NA, NA, NA, NA, NA, 3)), .Names = 'conc', row.names = c(NA, 16L), class = 'data.frame'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna27() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));is.na(argv[[1]]);");
    }

    @Test
    public void testisna28() {
        assertEval("argv <- list(structure(list(conc = c(NA, 3.6)), .Names = 'conc', row.names = 419:420, class = 'data.frame'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna29() {
        assertEval(Output.IgnoreWarningContext, "argv <- list(NULL);is.na(argv[[1]]);");
    }

    @Test
    public void testisna30() {
        assertEval("argv <- list(structure(list(sec = NA_real_, min = NA_integer_, hour = NA_integer_, mday = NA_integer_, mon = NA_integer_, year = NA_integer_, wday = NA_integer_, yday = NA_integer_, isdst = -1L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna31() {
        assertEval("argv <- list(structure(c('R (>= 2.10.0), methods, DBI (>= 0.2-5)', 'methods, DBI (>= 0.2-3)', NA), .Names = c('Depends', 'Imports', 'LinkingTo')));is.na(argv[[1]]);");
    }

    @Test
    public void testisna32() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 12L, mon = 2L, year = 112L, wday = 1L, yday = 71L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna33() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE), .Dim = c(8L, 5L)));is.na(argv[[1]]);");
    }

    @Test
    public void testisna34() {
        assertEval("argv <- list(structure(c('Min.   : 1.00  ', '1st Qu.: 9.00  ', 'Median :18.00  ', 'Mean   :14.74  ', '3rd Qu.:20.00  ', 'Max.   :23.00  ', NA, 'Min.   :5.000  ', '1st Qu.:5.300  ', 'Median :6.100  ', 'Mean   :6.084  ', '3rd Qu.:6.600  ', 'Max.   :7.700  ', NA, '117    :  5  ', '1028   :  4  ', '113    :  4  ', '112    :  3  ', '135    :  3  ', '(Other):147  ', 'NAs   : 16  ', 'Min.   :  0.50  ', '1st Qu.: 11.32  ', 'Median : 23.40  ', 'Mean   : 45.60  ', '3rd Qu.: 47.55  ', 'Max.   :370.00  ', NA, 'Min.   :0.00300  ', '1st Qu.:0.04425  ', 'Median :0.11300  ', 'Mean   :0.15422  ', '3rd Qu.:0.21925  ', 'Max.   :0.81000  ', NA), .Dim = c(7L, 5L), .Dimnames = list(c('', '', '', '', '', '', ''), c('    event', '     mag', '   station', '     dist', '    accel')), class = 'table'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna35() {
        assertEval("argv <- list(NA_complex_);is.na(argv[[1]]);");
    }

    @Test
    public void testisna36() {
        assertEval("argv <- list(complex(0));is.na(argv[[1]]);");
    }

    @Test
    public void testisna37() {
        assertEval("argv <- list(structure(list(sec = 40, min = 24L, hour = 11L, mday = 15L, mon = 11L, year = 100L, wday = 5L, yday = 349L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt')));is.na(argv[[1]]);");
    }

    @Test
    public void testisna38() {
        assertEval("argv <- list(structure(c(2L, 6L, 2L, 5L, 4L, 2L, 5L, 4L), .Dim = 8L, .Dimnames = structure(list(statef = c('act', 'nsw', 'nt', 'qld', 'sa', 'tas', 'vic', 'wa')), .Names = 'statef'), class = 'table'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna39() {
        assertEval("argv <- list(structure(c(2, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0), .Dim = 3:4, .Dimnames = structure(list(x1 = c('a', 'b', 'c'), x2 = c('a', 'b', 'c', NA)), .Names = c('x1', 'x2')), class = c('xtabs', 'table')));is.na(argv[[1]]);");
    }

    @Test
    public void testisna40() {
        assertEval("argv <- list(structure(list(1L, 3L), class = structure('L', package = '.GlobalEnv')));is.na(argv[[1]]);");
    }

    @Test
    public void testisna41() {
        assertEval("argv <- list(c('«Latin-1 accented chars»: éè øØ å<Å æ<Æ é éè', 'éè'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna42() {
        assertEval("argv <- list(c(-Inf, -Inf, -Inf, 0, 1, 2, Inf, Inf));is.na(argv[[1]]);");
    }

    @Test
    public void testisna43() {
        assertEval("argv <- list(structure(c(-3.001e+155, -1.067e+107, -1.976e+62, -9.961e+152, -2.059e+23, 0.5104), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.'), class = c('summaryDefault', 'table')));is.na(argv[[1]]);");
    }

    @Test
    public void testisna44() {
        assertEval("argv <- list(structure(list(sec = 0, min = 0L, hour = 0L, mday = 1L, mon = 0L, year = 70L, wday = 4L, yday = 0L, isdst = 0L), .Names = c('sec', 'min', 'hour', 'mday', 'mon', 'year', 'wday', 'yday', 'isdst'), class = c('POSIXlt', 'POSIXt'), tzone = 'GMT'));is.na(argv[[1]]);");
    }

    @Test
    public void testisna45() {
        assertEval("argv <- list(119:120);is.na(argv[[1]]);");
    }

    @Test
    public void testisna46() {
        assertEval("argv <- list(integer(0));is.na(argv[[1]]);");
    }

    @Test
    public void testIsNA() {
        assertEval("{ is.na(NA) }");
        assertEval("{ is.na(NaN) }");
        assertEval("{ is.na(c(NA)) }");
        assertEval("{ is.na(c(1,2,3,4)) }");
        assertEval("{ is.na(c(1,2,NA,4)) }");
        assertEval("{ is.na(1[10]) }");
        assertEval("{ is.na(c(1[10],2[10],3)) }");
        assertEval("{ is.na(list(1[10],1L[10],list(),integer())) }");
        assertEval(Output.IgnoreWarningContext, "is.na(quote(x()))");
        assertEval("is.na(is.na))");

        // Note: is.na.data.frame calls do.call("cbind", lapply(x, "is.na")) - there is the error
        // Probably the same error as in testisna13
        assertEval(Ignored.Unimplemented, "is.na(data.frame(col1=1:5, col2=c(NA, 1, NA, 2, NA)))");
    }
}
