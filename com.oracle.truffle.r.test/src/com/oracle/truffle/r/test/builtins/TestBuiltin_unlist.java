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
public class TestBuiltin_unlist extends TestBase {

    @Test
    public void testunlist1() {
        assertEval(Ignored.Unknown, "argv <- list(list('yaxp'), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist2() {
        assertEval(Ignored.Unknown, "argv <- list(list(c(13823, NA)), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist3() {
        assertEval(Ignored.Unknown, "argv <- list(structure('A', .Names = 'x', package = '.GlobalEnv'), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist4() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(list(structure(function (e1, e2) standardGeneric('Ops'), generic = structure('Ops', package = 'base'), package = 'base', group = list(), valueClass = character(0), signature = c('e1', 'e2'), default = quote(`\\001NULL\\001`), skeleton = quote((function (e1, e2) stop('invalid call in method dispatch to 'Ops' (no default method)', domain = NA))(e1, e2)), groupMembers = list('Arith', 'Compare', 'Logic'), class = structure('groupGenericFunction', package = 'methods')))), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(sec = c(8.40000009536743, 8.80000019073486), min = c(14L, 14L), hour = c(22L, 22L)), .Names = c('sec', 'min', 'hour')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist6() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(c(NA, 0L)), row.names = c(NA, -2L), class = 'data.frame'), structure(list(c(NA, 0)), row.names = c(NA, -2L), class = 'data.frame'), structure(list(c(10L, 10L)), row.names = c(NA, -2L), class = 'data.frame'), structure(list(c(2.74035772634541, 2.74035772634541)), row.names = c(NA, -2L), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(diagonalMatrix = 1, lMatrix = 1, sparseMatrix = 2, Matrix = 2, mMatrix = 4), .Names = c('diagonalMatrix', 'lMatrix', 'sparseMatrix', 'Matrix', 'mMatrix')), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist8() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(), .Names = character(0), row.names = integer(0), class = 'data.frame'), structure(list(height = numeric(0), weight = numeric(0)), .Names = c('height', 'weight'), row.names = integer(0), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist9() {
        assertEval(Ignored.Unknown, "argv <- list(list(c(TRUE, FALSE, FALSE, FALSE, FALSE), c(TRUE, TRUE, TRUE, TRUE, NA)), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist10() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure('     \\\'Jetz no chli züritüütsch: (noch ein bißchen Zürcher deutsch)\\\')\\n', Rd_tag = 'RCODE'), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist11() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(`1 sec` = 345600, `2 secs` = 172800, `5 secs` = 69120, `10 secs` = 34560, `15 secs` = 23040, `30 secs` = 11520, `1 min` = 5760, `2 mins` = 2880, `5 mins` = 1152, `10 mins` = 576, `15 mins` = 384, `30 mins` = 192, `1 hour` = 96, `3 hours` = 32, `6 hours` = 16, `12 hours` = 8, `1 DSTday` = 4, `2 DSTdays` = 2, `1 week` = 0.571428571428571, halfmonth = 0.262833675564682, `1 month` = 0.131416837782341, `3 months` = 0.0438056125941136, `6 months` = 0.0219028062970568, `1 year` = 0.0109514031485284,     `2 years` = 0.0054757015742642, `5 years` = 0.00219028062970568, `10 years` = 0.00109514031485284, `20 years` = 0.00054757015742642, `50 years` = 0.000219028062970568, `100 years` = 0.000109514031485284, `200 years` = 5.4757015742642e-05, `500 years` = 2.19028062970568e-05, `1000 years` = 1.09514031485284e-05), .Names = c('1 sec', '2 secs', '5 secs', '10 secs', '15 secs', '30 secs', '1 min', '2 mins', '5 mins', '10 mins', '15 mins', '30 mins', '1 hour', '3 hours', '6 hours', '12 hours', '1 DSTday', '2 DSTdays', '1 week', 'halfmonth', '1 month', '3 months', '6 months', '1 year', '2 years', '5 years', '10 years', '20 years', '50 years', '100 years', '200 years', '500 years', '1000 years')), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist12() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(vector = TRUE, atomicVector = TRUE, index = TRUE, numIndex = TRUE, numLike = TRUE, number = TRUE, replValue = TRUE), .Names = c('vector', 'atomicVector', 'index', 'numIndex', 'numLike', 'number', 'replValue')), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist13() {
        assertEval(Ignored.Unknown, "argv <- list(structure(list(a = 6:10), .Names = 'a', row.names = 6:10), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist14() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(`2005` = structure(c(31L, 28L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L), .Dim = 12L, .Dimnames = structure(list(c('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12')), .Names = ''), class = 'table'), `2006` = structure(c(31L, 28L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L), .Dim = 12L, .Dimnames = structure(list(c('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12')), .Names = ''), class = 'table'), `2007` = structure(c(31L, 28L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L), .Dim = 12L, .Dimnames = structure(list(c('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12')), .Names = ''), class = 'table'), `2008` = structure(c(31L, 29L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L), .Dim = 12L, .Dimnames = structure(list(c('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12')), .Names = ''), class = 'table'), `2009` = structure(1L, .Dim = 1L, .Dimnames = structure(list('01'), .Names = ''), class = 'table')), .Names = c('2005', '2006', '2007', '2008', '2009')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist15() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(structure(c(1395082040.29392, 1395082040.29392, 1395082040.29392, 1395082040.29392, 1395082040.29392), class = c('AsIs', 'POSIXct', 'POSIXt'))), row.names = c(NA, -5L), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist16() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure('print(.leap.seconds, tz = \\\'PST8PDT\\\')  # and in Seattle's\\n', Rd_tag = 'RCODE'), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist17() {
        assertEval(Ignored.Unknown, "argv <- list(list(TRUE, TRUE, TRUE, TRUE), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist18() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(c(NA_real_, NA_real_), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'),     c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi'), c('Svansota', 'No. 462', 'Manchuria', 'No. 475', 'Velvet', 'Peatland', 'Glabron', 'No. 457', 'Wisconsin No. 38', 'Trebi')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist19() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list('/home/lzhao/tmp/RtmpTzriDZ/R.INSTALL30d4108a07be/mgcv/R/gamm.r'), row.names = c(NA, -1L), class = 'data.frame'), structure(list(1522L), row.names = c(NA, -1L), class = 'data.frame'), structure(list(1522L), row.names = c(NA, -1L), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist20() {
        assertEval(Ignored.Unknown, "argv <- list(structure(list(`1` = 2.47032822920623e-323), .Names = '1'), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist21() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(`1` = 5900.92307692308, `2` = 6784.76923076923), .Names = c('1', '2')), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist22() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(surname = structure(c('Tukey', 'Venables', 'Tierney', 'Ripley', 'Ripley', 'McNeil'), class = 'AsIs'), nationality = structure(c(3L, 1L, 3L, 2L, 2L, 1L), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(2L, 1L, 1L, 1L, 1L, 1L), .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = c('1', '2', '3', '4', '4.1', '5'), class = 'data.frame'), structure(list(title = structure(c(2L, 5L, 4L, 6L, 7L, 3L), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA, 1L, NA, NA, NA, NA), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('title', 'other.author'), row.names = c(NA, 6L), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist23() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(c(-Inf, -Inf, -Inf, -Inf, 0, 0, 0, 0, 0, -Inf, -Inf, -Inf, -Inf, 0, 0, 0, 0, 0, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, 0, 0, 0, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, 0, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, 0, 0, 0, -Inf, -Inf, -Inf, -Inf, -Inf, 0, 0, 0, 0, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, 0, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, 0, 0, 0, -Inf, -Inf, -Inf, -Inf, -Inf, -Inf, 0, 0, 0), .Dim = c(9L, 9L), .Dimnames = list(c('20%', '30%', '40%', '50%', '60%', '70%', '80%', '90%', '100%'), NULL)), structure(c(-Inf, -Inf, -Inf, 0, 0, 1, 1, Inf, Inf, -Inf, -Inf, -Inf, 0, 0.5, 1, Inf, Inf, Inf, -Inf, -Inf, -Inf, -Inf, 0, 1, 1, 1, Inf, -Inf, -Inf, -Inf, -Inf, 0, 0.5, 1, Inf, Inf, -Inf, -Inf, -Inf, 0, 0.5, 1, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0, 0.6, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0, 0.4, 0.8, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0, 0.533333333333334, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0, 0.525, Inf, Inf, Inf, Inf), .Dim = c(9L, 9L), .Dimnames = list(c('20%', '30%', '40%', '50%', '60%', '70%', '80%', '90%', '100%'), NULL)), structure(c(-Inf, -Inf, 0, 0, 1, 2, Inf, Inf, Inf, -Inf, -Inf, 0, 0.5, 1, 2, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0, 1, 2, 2, Inf, Inf, -Inf, -Inf, -Inf, 0, 0.8, 1.6, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.3, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.4, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.2, 1.9, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.499999999999999, 1.33333333333333, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.325, Inf, Inf, Inf, Inf), .Dim = c(9L, 9L), .Dimnames = list(c('20%', '30%', '40%', '50%', '60%', '70%', '80%', '90%', '100%'), NULL)), structure(c(-Inf, -Inf, 0, 1, 2, 3, Inf, Inf, Inf, -Inf, -Inf, 0, 1, 2, 3, Inf, Inf, Inf, -Inf, -Inf, -Inf, 1, 2, 3, Inf, Inf, Inf, -Inf, -Inf, -Inf, 0.5, 1.6, 2.7, Inf, Inf, Inf, -Inf, -Inf, -Inf, 1, 2.1, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 1, 2.2, Inf, Inf, Inf, Inf, -Inf, -Inf, 0, 1, 2, 3, Inf, Inf, Inf, -Inf, -Inf, -Inf, 1, 2.13333333333333, Inf, Inf, Inf, Inf, -Inf, -Inf, -Inf, 1, 2.125, Inf, Inf, Inf, Inf), .Dim = c(9L, 9L), .Dimnames = list(c('20%', '30%', '40%', '50%', '60%', '70%', '80%', '90%', '100%'), NULL)), structure(c(-Inf, -Inf, 0, 1, 3, 4, Inf, Inf, Inf, -Inf, -Inf, 0, 1.5, 3, 4, Inf, Inf, Inf, -Inf, -Inf, 0, 1, 2, 4, Inf, Inf, Inf, -Inf, -Inf, -Inf, 1, 2.4, 3.8, Inf, Inf, Inf, -Inf, -Inf, 0.100000000000001, 1.5, 2.9, Inf, Inf, Inf, Inf, -Inf, -Inf, 0, 1.5, 3, Inf, Inf, Inf, Inf, -Inf, -Inf, 0.2, 1.5, 2.8, Inf, Inf, Inf, Inf, -Inf, -Inf, 0.0666666666666664, 1.5, 2.93333333333333, Inf, Inf, Inf, Inf, -Inf, -Inf, 0.0750000000000002, 1.5, 2.925, Inf, Inf, Inf, Inf), .Dim = c(9L, 9L), .Dimnames = list(c('20%', '30%', '40%', '50%', '60%', '70%', '80%', '90%', '100%'), NULL)), structure(c(-Inf, -Inf, 0, 2, 4, 5, Inf, Inf, Inf, -Inf, -Inf, 0, 2, 4, 5, Inf, Inf, Inf, -Inf, -Inf, 0, 1, 3, 5, Inf, Inf, Inf, -Inf, -Inf, -Inf, 1.5, 3.2, 4.9, Inf, Inf, Inf, -Inf, -Inf, 0.300000000000001, 2, 3.7, Inf, Inf, Inf, Inf, -Inf, -Inf, 0.2, 2, 3.8, Inf, Inf, Inf, Inf, -Inf, -Inf, 0.4, 2, 3.6, Inf, Inf, Inf, Inf, -Inf, -Inf, 0.266666666666667, 2, 3.73333333333333, Inf, Inf, Inf, Inf, -Inf, -Inf, 0.275, 2, 3.725, Inf, Inf, Inf, Inf), .Dim = c(9L, 9L), .Dimnames = list(c('20%', '30%', '40%', '50%', '60%', '70%', '80%', '90%', '100%'), NULL))), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist24() {
        assertEval(Ignored.Unknown, "argv <- list(structure('# everything ', Rd_tag = 'VERB'), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist25() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(sec = NA_real_, min = NA_integer_, hour = NA_integer_), .Names = c('sec', 'min', 'hour')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist26() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(a = list(1:5, c('A', 'B', 'C', 'D', 'E')), b = 'Z', c = NA), .Names = c('a', 'b', 'c')), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist27() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(structure(c(0.398105880367068, -0.612026393250771, 0.341119691424425, -1.12936309608079, 1.43302370170104, 1.98039989850586, -0.367221476466509, -1.04413462631653, 0.569719627442413, -0.135054603880824, 2.40161776050478, -0.0392400027331692, 0.689739362450777, 0.0280021587806661, -0.743273208882405, 0.188792299514343, -1.80495862889104, 1.46555486156289, 0.153253338211898, 2.17261167036215, 0.475509528899663, -0.709946430921815, 0.610726353489055, -0.934097631644252, -1.2536334002391, 0.291446235517463, -0.443291873218433, 0.00110535163162413, 0.0743413241516641, -0.589520946188072, -0.568668732818502, -0.135178615123832, 1.1780869965732, -1.52356680042976, 0.593946187628422, 0.332950371213518, 1.06309983727636, -0.304183923634301, 0.370018809916288, 0.267098790772231, -0.54252003099165, 1.20786780598317, 1.16040261569495, 0.700213649514998, 1.58683345454085, 0.558486425565304, -1.27659220845804, -0.573265414236886, -1.22461261489836, -0.473400636439312, -0.620366677224124, 0.0421158731442352, -0.910921648552446, 0.158028772404075, -0.654584643918818, 1.76728726937265, 0.716707476017206, 0.910174229495227, 0.384185357826345, 1.68217608051942, -0.635736453948977, -0.461644730360566, 1.43228223854166, -0.650696353310367, -0.207380743601965, -0.392807929441984, -0.319992868548507, -0.279113302976559, 0.494188331267827, -0.177330482269606, -0.505957462114257, 1.34303882517041, -0.214579408546869, -0.179556530043387, -0.100190741213562, 0.712666307051405, -0.0735644041263263, -0.0376341714670479, -0.681660478755657, -0.324270272246319, 0.0601604404345152, -0.588894486259664, 0.531496192632572, -1.51839408178679, 0.306557860789766, -1.53644982353759, -0.300976126836611, -0.528279904445006, -0.652094780680999, -0.0568967778473925, -1.91435942568001, 1.17658331201856, -1.664972436212, -0.463530401472386, -1.11592010504285, -0.750819001193448, 2.08716654562835, 0.0173956196932517, -1.28630053043433, -1.64060553441858), .Label = structure(list(c(-1.91442143130152, -0.573203408615382), c(-0.934159637265755, -0.300914121215107), c(-0.568730738440006, 0.0174576253147555), c(-0.279175308598063, 0.384247363447848), c(0.0279401531591622, 1.16046462131646), c(0.398043874745564, 2.40167976612628)), class = 'shingleLevel'), class = 'shingle')), row.names = c(NA, -100L), class = 'data.frame'), structure(list(c(0.450187101272656, -0.018559832714638, -0.318068374543844, -0.929362147453702, -1.48746031014148, -1.07519229661568, 1.00002880371391, -0.621266694796823, -1.38442684738449, 1.86929062242358, 0.425100377372448, -0.238647100913033, 1.05848304870902, 0.886422651374936, -0.619243048231147, 2.20610246454047, -0.255027030141015, -1.42449465021281, -0.144399601954219, 0.207538339232345, 2.30797839905936, 0.105802367893711, 0.456998805423414, -0.077152935356531, -0.334000842366544, -0.0347260283112762, 0.787639605630162, 2.07524500865228, 1.02739243876377, 1.2079083983867, -1.23132342155804, 0.983895570053379, 0.219924803660651, -1.46725002909224, 0.521022742648139, -0.158754604716016, 1.4645873119698, -0.766081999604665, -0.430211753928547, -0.926109497377437, -0.17710396143654, 0.402011779486338, -0.731748173119606, 0.830373167981674, -1.20808278630446, -1.04798441280774, 1.44115770684428, -1.01584746530465, 0.411974712317515, -0.38107605110892, 0.409401839650934, 1.68887328620405, 1.58658843344197, -0.330907800682766, -2.28523553529247, 2.49766158983416, 0.667066166765493, 0.5413273359637, -0.0133995231459087, 0.510108422952926, -0.164375831769667, 0.420694643254513, -0.400246743977644, -1.37020787754746, 0.987838267454879, 1.51974502549955, -0.308740569225614, -1.25328975560769, 0.642241305677824, -0.0447091368939791, -1.73321840682484, 0.00213185968026965, -0.630300333928146, -0.340968579860405, -1.15657236263585, 1.80314190791747, -0.331132036391221, -1.60551341225308, 0.197193438739481, 0.263175646405474, -0.985826700409291, -2.88892067167955, -0.640481702565115, 0.570507635920485, -0.05972327604261, -0.0981787440052344, 0.560820728620116, -1.18645863857947, 1.09677704427424, -0.00534402827816569, 0.707310667398079, 1.03410773473746, 0.223480414915304, -0.878707612866019, 1.16296455596733, -2.00016494478548, -0.544790740001725, -0.255670709156989, -0.166121036765006, 1.02046390878411)), row.names = c(NA, -100L), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist28() {
        assertEval(Ignored.Unknown, "argv <- list(list(NULL), TRUE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist29() {
        assertEval(Ignored.Unknown, "argv <- list(structure(list(a = 'a', b = 2, c = 3.14159265358979+2i), .Names = c('a', 'b', 'c')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist30() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(`1` = c(2, 1), `2` = c(3, 1.5, 1.5, 4), `3` = c(4, 2.5, 2.5, 5, 1), `4` = c(5, 3.5, 3.5, 7, 1.5, 6, 1.5), `5` = c(5, 3.5, 3.5, 8, 1.5, 6.5, 1.5, 6.5), `6` = c(6, 4.5, 4.5, 10, 2.5, 8.5, 2.5, 8.5, 1, 7), `7` = c(7, 5.5, 5.5, 11, 3.5, 9.5, 3.5, 9.5, 2, 8, 1)), .Dim = 7L, .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7'))), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist31() {
        assertEval(Ignored.Unknown, "argv <- list(list(c(TRUE, TRUE), c(TRUE, TRUE), c(TRUE, TRUE), c(TRUE, TRUE), c(1, 2, 3)), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist32() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(mean = c(0, 1), vcov = structure(c(1, 1, 1, 0), .Dim = c(2L, 2L))), .Names = c('mean', 'vcov'), class = c('relistable', 'list')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist33() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(c('  \\036 The ‘internal’ graphics device invoked by .Call(\\\'R_GD_nullDevice\\\',', '    package = \\\'grDevices\\\') has been removed: use pdf(file = NULL)', '    instead.')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist34() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(surname = structure(c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'AsIs'), nationality = structure(c('Australia', 'UK', 'US', 'US', 'Australia'), class = 'AsIs'), deceased = structure(c('no', 'no', 'no', 'yes', 'no'), class = 'AsIs'), title = structure(c(NA_character_, NA_character_, NA_character_, NA_character_, NA_character_), class = 'AsIs'), other.author = structure(c(NA_character_, NA_character_, NA_character_, NA_character_, NA_character_), class = 'AsIs')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = c('1', '2', '3', '4', '5')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist35() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(Ozone = c(96L, 78L, 73L, 91L, 47L, 32L, 20L, 23L, 21L, 24L, 44L, 21L, 28L, 9L, 13L, 46L, 18L, 13L, 24L, 16L, 13L, 23L, 36L, 7L, 14L, 30L, NA, 14L, 18L, 20L), Solar.R = c(167L, 197L, 183L, 189L, 95L, 92L, 252L, 220L, 230L, 259L, 236L, 259L, 238L, 24L, 112L, 237L, 224L, 27L, 238L, 201L, 238L, 14L, 139L, 49L, 20L, 193L, 145L, 191L, 131L, 223L), Wind = c(6.9, 5.1, 2.8, 4.6, 7.4, 15.5, 10.9, 10.3, 10.9, 9.7, 14.9, 15.5, 6.3, 10.9, 11.5, 6.9, 13.8, 10.3, 10.3, 8, 12.6, 9.2, 10.3, 10.3, 16.6, 6.9, 13.2, 14.3, 8, 11.5), Temp = c(91L, 92L, 93L, 93L, 87L, 84L, 80L, 78L, 75L, 73L, 81L, 76L, 77L, 71L, 71L, 78L, 67L, 76L, 68L, 82L, 64L, 71L, 81L, 69L, 63L, 70L, 77L, 75L, 76L, 68L), Month = c(9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L, 9L), Day = 1:30), .Names = c('Ozone', 'Solar.R', 'Wind', 'Temp', 'Month', 'Day'), row.names = 124:153, class = 'data.frame'), structure(list(c(2.67385465817826, 1.92826057080163, 1.7211511020859, 2.46674518946253, 0.6441818647641, 0.0228534586169083, -0.474209266300845, -0.349943585071407, -0.432787372557699, -0.308521691328261, 0.519916183534662, -0.432787372557699, -0.142834116355676, -0.929850097475453, -0.764162522502868, 0.602759971020954, -0.557053053787138, -0.764162522502868, -0.308521691328261, -0.63989684127343, -0.764162522502868, -0.349943585071407, 0.188541033589493, -1.01269388496175, -0.722740628759722, -0.059990328869384, NA, -0.722740628759722, -0.557053053787138, -0.474209266300845)), row.names = c(NA, -30L), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist36() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(c(5.87030131383818+0i, 1.5889879152884+1.34124369386909i, 2.11222603449395-1.81528547759475i, 2.90982451403972-3.07851581383191i, -0.93444299242086+4.19201264862077i, -2.47319172794455-3.70050127054969i, 3.40387225833387-2.03794044354999i, 0.964146923537224+0.43683199768595i, 0.964146923537223-0.43683199768595i, 3.40387225833387+2.03794044354999i, -2.47319172794455+3.70050127054969i, -0.93444299242086-4.19201264862077i, 2.90982451403972+3.07851581383191i, 2.11222603449395+1.81528547759475i, 1.5889879152884-1.34124369386909i), c(-0.198575181429756+0i, 3.06901469564285-0.28753262878135i, 2.52792606446531+0.34832983414202i, -0.22897831647696+4.34107190550675i, -1.1328140942159+1.10933827962707i, -2.13015831304915-3.19551716353477i, 1.66248610578085-2.34843556657312i, 2.53273081248013+0.345339148259i, 2.53273081248013-0.345339148259i, 1.66248610578085+2.34843556657312i, -2.13015831304915+3.19551716353477i, -1.1328140942159-1.10933827962708i, -0.22897831647696-4.34107190550675i, 2.52792606446531-0.34832983414202i, 3.06901469564285+0.28753262878135i), c(-0.177389766587854+0i, -0.750507869921238-0.968112891774716i, 2.01908494011385-1.61353499070386i, -1.32842557557029+1.87677956172028i, 0.278793972604843+0.060190561256586i, 0.06482045217871+2.780245561063i, -3.05075608405522+4.21179315999883i, -0.12202595251607-1.65218285338028i, -0.12202595251607+1.65218285338028i, -3.05075608405522-4.21179315999883i, 0.06482045217871-2.780245561063i, 0.278793972604844-0.060190561256586i, -1.32842557557029-1.87677956172028i, 2.01908494011385+1.61353499070386i, -0.750507869921237+0.968112891774715i), c(-1.93496831243286+0i, -4.87879352188084-3.06857420991118i, 0.91348359987171+2.30355482564816i, 2.7631069926811+6.2396752311874i, -0.9934286053847-5.99510259160787i, 0.39705745560005+3.84166415349047i, -1.5293697261841+2.76025815484515i, 3.48992984345714-5.88708433976428i, 3.48992984345714+5.88708433976428i, -1.5293697261841-2.76025815484515i, 0.39705745560005-3.84166415349047i, -0.99342860538471+5.99510259160787i, 2.7631069926811-6.2396752311874i, 0.91348359987171-2.30355482564816i, -4.87879352188084+3.06857420991118i), c(1.6954625122129+0i, 0.96480086806796-2.54002409930623i, -3.5054253146275-7.05689416264505i, -2.10114573645889-1.07773818646711i, 1.81179418950692+1.03308206229221i, 0.84721205589596-4.740786425434i, -1.90295630545443-1.68686014535334i, -2.43557705822344-1.63964363160433i, -2.43557705822344+1.63964363160433i, -1.90295630545443+1.68686014535334i, 0.84721205589596+4.740786425434i, 1.81179418950692-1.03308206229221i, -2.10114573645889+1.07773818646711i, -3.50542531462751+7.05689416264504i, 0.96480086806796+2.54002409930623i)), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist37() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(inner = integer(0), outer = integer(0)), .Names = c('inner', 'outer')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist38() {
        assertEval(Ignored.Unknown, "argv <- list(structure(c('mode', 'length', 'x', 'mode', 'x', 'mode'), .Dim = 2:3), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist39() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(b = structure(2L, .Label = c('C', 'D'), class = 'factor')), .Names = 'b', row.names = 2L, class = 'data.frame'), structure(list(a = structure(NA_real_, class = c('POSIXct', 'POSIXt'))), .Names = 'a', row.names = 'NA', class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist40() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(structure(c('0.007239522', '0.014584634', '0.014207936', '0.018442267', '0.011128505', '0.019910082', '0.027072311', '0.034140379', '0.028320657', '0.037525507'), class = 'AsIs')), row.names = c(NA, -10L), class = 'data.frame'), structure(list(structure(c(' 1', ' 6', ' 7', ' 8', '13', '14', '15', '20', '21', '22'), class = 'AsIs')), row.names = c(NA, -10L), class = 'data.frame'), structure(list(structure(c(' 16', ' 16', '144', ' 16', ' 16', '128', ' 16', ' 16', '112', ' 16'), .Dim = 10L, .Dimnames = structure(list(c('1', '6', '7', '8', '13', '14', '15', '20', '21', '22')), .Names = ''))), row.names = c('1', '6', '7', '8', '13', '14', '15', '20', '21', '22'), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist41() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(N = 84L, ZXrows = 18, ZXcols = 5, Q = 1L, StrRows = 18, qvec = structure(c(1, 0, 0), .Names = c('Seed', '', '')), ngrps = structure(c(14L, 1L, 1L), .Names = c('Seed', 'X', 'y')), DmOff = structure(c(0, 1, 10), .Names = c('', 'Seed', '')), ncol = structure(c(1, 3, 1), .Names = c('Seed', '', '')), nrot = structure(c(4, 1, 0), .Names = c('', '', '')), ZXoff = structure(list(Seed = c(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), X = structure(18, .Names = 'Seed'), y = structure(72, .Names = '')), .Names = c('Seed', 'X', 'y')), ZXlen = structure(list(Seed = c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 5), X = 18, y = 18), .Names = c('Seed', 'X', 'y')), SToff = structure(list(Seed = c(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), X = structure(32, .Names = 'Seed'), y = structure(89, .Names = '')), .Names = c('Seed', 'X', 'y')), DecOff = structure(list(Seed = c(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), X = structure(18, .Names = 'Seed'), y = structure(72, .Names = '')), .Names = c('Seed', 'X', 'y')), DecLen = structure(list(    Seed = c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 5), X = 18, y = 18), .Names = c('Seed', 'X', 'y'))), .Names = c('N', 'ZXrows', 'ZXcols', 'Q', 'StrRows', 'qvec', 'ngrps', 'DmOff', 'ncol', 'nrot', 'ZXoff', 'ZXlen', 'SToff', 'DecOff', 'DecLen')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist42() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(structure('\\n', Rd_tag = 'RCODE'), structure('Sys.timezone()\\n', Rd_tag = 'RCODE'), structure('\\n', Rd_tag = 'RCODE'), structure('#ifdef windows not active', Rd_tag = 'COMMENT'), structure(list(structure('\\n', Rd_tag = 'VERB'), structure('## need to supply a suitable file path (if any) for your system\\n', Rd_tag = 'VERB'), structure('tzfile <- \\\'/usr/share/zoneinfo/zone.tab\\\'\\n', Rd_tag = 'VERB'), structure('tzones <- read.delim(tzfile, row.names = NULL, header = FALSE,\\n', Rd_tag = 'VERB'),     structure('    col.names = c(\\\'country\\\', \\\'coords\\\', \\\'name\\\', \\\'comments\\\'),\\n', Rd_tag = 'VERB'), structure('    as.is = TRUE, fill = TRUE, comment.char = \\\'#\\\')\\n', Rd_tag = 'VERB'), structure('str(tzones$name)\\n', Rd_tag = 'VERB')), Rd_tag = '\\\\dontrun'), structure('\\n', Rd_tag = 'RCODE')), Rd_tag = '\\\\examples'), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist43() {
        assertEval(Ignored.Unknown, "argv <- list(structure(list(`1` = 8.91763605923317e+38), .Names = '1'), FALSE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist44() {
        assertEval(Ignored.Unknown, "argv <- list(list(c(0, 0), c(0, 0, 0, 1), NULL, c(1, 1)), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist45() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(structure(c('McNeil', 'Ripley', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'AsIs')), row.names = c(NA, -6L), class = 'data.frame'), structure(list(structure(c('Australia', 'UK', 'UK', 'US', 'US', 'Australia'), class = 'AsIs')), row.names = c(NA, -6L), class = 'data.frame'), structure(list(structure(c('no', 'no', 'no', 'no', 'yes', 'no'), class = 'AsIs')), row.names = c(NA, -6L), class = 'data.frame'), structure(list(structure(c('Interactive Data Analysis', 'Spatial Statistics', 'Stochastic Simulation', 'LISP-STAT', 'Exploratory Data Analysis', 'Modern Applied Statistics ...'), class = 'AsIs')), row.names = c(NA, -6L), class = 'data.frame'), structure(list(structure(c(NA, NA, NA, NA, NA, 'Ripley'), class = 'AsIs')), row.names = c(NA, -6L), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist46() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(surname = structure(c(4L, 5L, 3L, 2L, 2L, 1L, 6L), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables', 'R Core'), class = 'factor'), nationality = structure(c(3L, 1L, 3L, 2L, 2L, 1L, NA), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(2L, 1L, 1L, 1L, 1L, 1L, NA), .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = c('1', '2', '3', '4', '4.1', '5', '7'), class = 'data.frame'),     structure(list(title = structure(c(2L, 5L, 4L, 6L, 7L, 3L, 1L), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA, 1L, NA, NA, NA, NA, 2L), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('title', 'other.author'), row.names = c(NA, 7L), class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist47() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(structure('DateTimeClasses', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('POSIXt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('print.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('print.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('summary.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'),     structure(list(structure('summary.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('+.POSIXt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('-.POSIXt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('Ops.POSIXt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('Math.POSIXt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('Summary.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('Math.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'),     structure(list(structure('Summary.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('[.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('[<-.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('[[.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('[.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('[<-.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('as.data.frame.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'),     structure(list(structure('as.data.frame.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('as.list.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('.leap.seconds', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('is.na.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('c.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('c.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(        structure('as.matrix.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('length.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('mean.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('mean.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('str.POSIXt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('check_tzones', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('duplicated.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'),     structure(list(structure('unique.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('split.POSIXct', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('names.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('names<-.POSIXlt', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('date-time', Rd_tag = 'VERB')), Rd_tag = '\\\\alias')), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist48() {
        assertEval(Ignored.Unknown, "argv <- list(structure(list(c(3L, 0L, 0L)), class = 'numeric_version'), TRUE, TRUE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testunlist49() {
        assertEval(Ignored.Unknown,
                        "argv <- list(list(structure(list(x = 1L, y = structure(1L, .Label = c('A', 'D', 'E'), class = 'factor'), z = 6), .Names = c('x', 'y', 'z'), row.names = 1L, class = 'data.frame'), structure(list(), .Names = character(0), row.names = 1L, class = 'data.frame')), FALSE, FALSE); .Internal(unlist(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testUnlist() {
        assertEval("{ unlist(list(\"hello\", \"hi\")) }");

        assertEval("{ unlist(list(a=\"hello\", b=\"hi\")) }");
        assertEval("{ x <- list(a=1,b=2:3,list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ x <- list(1,z=list(1,b=22,3)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ x <- list(1,z=list(1,b=22,3)) ; unlist(x, recursive=FALSE, use.names=FALSE) }");

        assertEval("{ x <- list(a=1,b=c(x=2, z=3),list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ y<-c(2, 3); names(y)<-c(\"z\", NA); x <- list(a=1,b=y,list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ x <- list(a=1,b=c(x=2, 3),list(x=FALSE)) ; unlist(x, recursive=FALSE) }");
        assertEval("{ unlist(list(a=1, c(b=2,c=3))) }");
        assertEval("{ unlist(list(a=1, c(2,3))) }");
        assertEval("{ unlist(list(a=1, c(2,3), d=4)) }");
        assertEval("{ unlist(list(a=1, c(2,3), 4)) }");
        assertEval("{ unlist(list(1+1i, c(7+7i,42+42i))) }");
        assertEval("{ unlist(list(1+1i, list(7+7i,42+42i)), recursive=FALSE) }");
        assertEval("{ unlist(list(1+1i, c(7,42))) }");
        assertEval("{ unlist(list(1+1i, list(7,42)), recursive=FALSE) }");

        assertEval("{ unlist(list(a=1,b=2, c=list(d=3,e=list(f=7))), recursive=TRUE) }");
        assertEval("{ unlist(list(a=1,b=2, c=list(d=3,list(f=7)))) }");
        assertEval("{ x <- list(list(\"1\",\"2\",b=\"3\",\"4\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",list(\"3\", \"4\"),\"5\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\"))) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\", \"4\"))) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\", \"4\"),\"5\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=c(\"3\", \"4\"),\"5\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\", list(\"10\"), \"4\"),\"5\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",\"2\",b=list(\"3\", list(\"10\", \"11\"), \"4\"),\"5\")) ; unlist(x) }");

        assertEval("{ names(unlist(list(list(list(\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(list(\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(list(\"1\",\"2\"))))) }");

        assertEval("{ unlist(list(a=list(\"0\", list(\"1\")))) }");
        assertEval("{ unlist(list(a=list(b=list(\"1\")))) }");

        assertEval("{ unlist(list(a=list(\"0\", b=list(\"1\")))) }");
        assertEval("{ unlist(list(a=list(b=list(\"1\"), \"2\"))) }");

        assertEval("{ unlist(list(a=list(\"0\", b=list(\"1\"), \"2\"))) }");
        assertEval("{ unlist(list(a=list(\"0\", list(b=list(\"1\"))))) }");

        assertEval("{ unlist(list(a=list(\"-1\", \"0\", b=list(\"1\")))) }");
        assertEval("{ unlist(list(a=list(b=list(\"1\"), \"2\", \"3\"))) }");

        assertEval("{ names(unlist(list(list(b=list(\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(b=list(\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(b=list(\"1\", \"2\"))))) }");

        assertEval("{ names(unlist(list(list(list(c=\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(list(c=\"1\"))))) }");
        assertEval("{ names(unlist(list(a=list(list(c=\"1\", d=\"2\"))))) }");

        assertEval("{ unlist(list()) }");

        assertEval("{ x <- list(\"a\", c(\"b\", \"c\"), list(\"d\", list(\"e\"))) ; unlist(x) }");
        assertEval("{ x <- list(NULL, list(\"d\", list(), character())) ; unlist(x) }");

        assertEval("{ x <- list(a=list(\"1\",\"2\",b=\"3\",\"4\")) ; unlist(x) }");
        assertEval("{ x <- list(a=list(1,FALSE,b=list(2:4))) ; unlist(x) }");
        assertEval("{ x <- list(a=list(\"1\",FALSE,b=list(2:4))) ; unlist(x) }");

        assertEval("{ x <- list(1,list(2,3),4) ; z <- list(x,x) ; u <- list(z,z) ; u[[c(2,2,3)]] <- 6 ; unlist(u) }");

        assertEval("{ x<-quote(f(1,2)); y<-function(z) 42; l<-list(x, y, NULL); y<-unlist(l); c(length(y), typeof(y)) }");
    }
}
