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
public class TestBuiltin_islistfactor extends TestBase {

    @Test
    public void testislistfactor1() {
        assertEval("argv <- list(list(structure(list(structure('R Core', class = 'AsIs')), row.names = c(NA, -1L), class = 'data.frame'), structure(list(structure(NA_character_, class = 'AsIs')), row.names = c(NA, -1L), class = 'data.frame'), structure(list(structure(NA_character_, class = 'AsIs')), row.names = c(NA, -1L), class = 'data.frame'), structure(list(structure('An Introduction to R', class = 'AsIs')), row.names = c(NA, -1L), class = 'data.frame'), structure(list(structure('Venables & Smith', class = 'AsIs')), row.names = c(NA, -1L), class = 'data.frame')), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor2() {
        assertEval("argv <- list(list(c(TRUE, FALSE, FALSE, FALSE, FALSE), c(TRUE, TRUE, TRUE, TRUE, NA)), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor3() {
        assertEval("argv <- list(structure(list(a = 6:10), .Names = 'a', row.names = 6:10), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor4() {
        assertEval("argv <- list(structure(list(k = 0.413311097189709, g1 = 72.8306629700373, g2 = 181.793153976139), .Names = c('k', 'g1', 'g2')), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor5() {
        assertEval("argv <- list(list(structure(list(structure('text', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('text.default', Rd_tag = 'VERB')), Rd_tag = '\\\\alias')), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor6() {
        assertEval("argv <- list(list(structure(list(surname = structure(c(4L, 5L, 3L, 2L, 2L, 1L, 6L), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables', 'R Core'), class = 'factor'), nationality = structure(c(3L, 1L, 3L, 2L, 2L, 1L, NA), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(c(2L, 1L, 1L, 1L, 1L, 1L, NA), .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = c('1', '2', '3', '4', '4.1', '5', '7'), class = 'data.frame'),     structure(list(title = structure(c(2L, 5L, 4L, 6L, 7L, 3L, 1L), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(c(NA, 1L, NA, NA, NA, NA, 2L), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('title', 'other.author'), row.names = c(NA, 7L), class = 'data.frame')), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor7() {
        assertEval("argv <- list(list(c('     The binary arithmetic operators are generic functions: methods can', '     be written for them individually or via the ‘Ops’ group generic', '     function.  (See ‘Ops’ for how dispatch is computed.)')), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor8() {
        assertEval("argv <- list(list(structure(list(structure(1:4, .Label = c('0', '1', '2', '3'), class = 'factor')), row.names = c(NA, -4L), class = 'data.frame')), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor9() {
        assertEval("argv <- list(structure(list(`1` = c(2, 1, 4, 3), `2` = c(3, 1.5, 5, 4, 1.5), `3` = c(6.5, 1.5, 9, 8, 1.5, 6.5, 4, 4, 4), `4` = c(7, 1.5, 10, 9, 1.5, 7, 4, 4, 4, 7)), .Dim = 4L, .Dimnames = list(c('1', '2', '3', '4'))), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor10() {
        assertEval("argv <- list(list(structure(c(0, 1, 1, 2, 2, 4, NA), .Names = c('ddiMatrix', 'diagonalMatrix', 'dMatrix', 'sparseMatrix', 'Matrix', 'mMatrix', 'ANY')), structure(c(0, 1, 1, 1, 2, 2, 2, 3, 4, NA), .Names = c('dgCMatrix', 'CsparseMatrix', 'dsparseMatrix', 'generalMatrix', 'dMatrix', 'sparseMatrix', 'compMatrix', 'Matrix', 'mMatrix', 'ANY'))), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor11() {
        assertEval("argv <- list(list(structure(0, .Dim = c(1L, 1L)), structure(-4.9497224423095e-07, .Dim = c(1L, 1L)), structure(0, .Dim = c(1L, 1L)), structure(-7.44931694456399e-07, .Dim = c(1L, 1L))), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor12() {
        assertEval(Output.IgnoreErrorMessage,
                        "argv <- list(structure(list(event = c('Min.   : 1.00  ', '1st Qu.: 9.00  ', 'Median :18.00  ', 'Mean   :14.74  ', '3rd Qu.:20.00  ', 'Max.   :23.00  ', NA), mag = c('Min.   :5.000  ', '1st Qu.:5.300  ', 'Median :6.100  ', 'Mean   :6.084  ', '3rd Qu.:6.600  ', 'Max.   :7.700  ', NA), station = c('117    :  5  ', '1028   :  4  ', '113    :  4  ', '112    :  3  ', '135    :  3  ', '(Other):147  ', 'NA's   : 16  '), dist = c('Min.   :  0.50  ', '1st Qu.: 11.32  ', 'Median : 23.40  ', 'Mean   : 45.60  ', '3rd Qu.: 47.55  ', 'Max.   :370.00  ', NA), accel = c('Min.   :0.00300  ', '1st Qu.:0.04425  ', 'Median :0.11300  ', 'Mean   :0.15422  ', '3rd Qu.:0.21925  ', 'Max.   :0.81000  ', NA)), .Names = c('event', 'mag', 'station', 'dist', 'accel')), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor13() {
        assertEval("argv <- list(structure(list(`1` = 1.97626258336499e-323), .Names = '1'), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor14() {
        assertEval("argv <- list(structure(list(name = structure(c('McNeil', 'Ripley', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'AsIs'), title = structure(c('Interactive Data Analysis', 'Spatial Statistics', 'Stochastic Simulation', 'LISP-STAT', 'Exploratory Data Analysis', 'Modern Applied Statistics ...'), class = 'AsIs'), other.author = structure(c(NA, NA, NA, NA, NA, 'Ripley'), class = 'AsIs'), nationality = structure(c('Australia', 'UK', 'UK', 'US', 'US', 'Australia'), class = 'AsIs'), deceased = structure(c('no', 'no', 'no', 'no', 'yes', 'no'), class = 'AsIs')), .Names = c('name', 'title', 'other.author', 'nationality', 'deceased'), row.names = c('1', '2', '3', '4', '5', '6')), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor15() {
        assertEval("argv <- list(structure(list(`1` = 8.91763605923317e+38), .Names = '1'), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor16() {
        assertEval("argv <- list(structure(list(V1 = c(0.908207789994776, 0.62911404389888, 0.205974574899301), V2 = c(0.125555095961317, 0.86969084572047, 0.482080115471035), V3 = c(0.553036311641335, 0.7323137386702, 0.477619622135535), V4 = c(0.332394674187526, 0.0842469143681228, 0.339072937844321), V5 = c(0.325352151878178, 0.245488513959572, 0.239629415096715)), .Names = c('V1', 'V2', 'V3', 'V4', 'V5'), row.names = c(4L, 9L, 11L)), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor17() {
        assertEval("argv <- list(structure(list(V1 = c(0.497699242085218, 0.991906094830483), V2 = c(0.668466738192365, 0.107943625887856), V3 = c(0.0994661601725966, 0.518634263193235), V4 = c(0.892198335845023, 0.389989543473348), V5 = c(0.79730882588774, 0.410084082046524)), .Names = c('V1', 'V2', 'V3', 'V4', 'V5'), row.names = c(16L, 18L)), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor18() {
        assertEval("argv <- list(list(structure(list(structure('rm', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('remove', Rd_tag = 'VERB')), Rd_tag = '\\\\alias')), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor19() {
        assertEval("argv <- list(list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render')), structure(c(FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render'))), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor20() {
        assertEval("argv <- list(list(c(0.92317305817397+0i, 0.160449395256071+0.220125597679977i, 0.40353715410585+2.39063261466203i, -3.64092275386503+3.51619480964107i, -0.30877433127864+1.37503901638266i, -0.5590368753986+2.95994484328048i, 2.07117052177259-1.58552086053907i, 5.12796916272868+5.50114308371867i, 0.71791019962021-4.36295436036464i, 3.6182846955548+0.01693946731429i, 5.86560669896785+3.41674024963709i, 7.14153164455803+0i, 5.86560669896785-3.41674024963709i, 3.6182846955548-0.01693946731429i, 0.71791019962021+4.36295436036464i, 5.12796916272868-5.50114308371867i, 2.07117052177259+1.58552086053907i, -0.5590368753986-2.95994484328048i, -0.30877433127864-1.37503901638266i, -3.64092275386503-3.51619480964107i, 0.40353715410585-2.39063261466203i, 0.160449395256071-0.220125597679976i), c(0.994686860835215+0i, -0.711636086238366+0.034977366507257i, -3.47255638259391-3.00654729467177i, -1.61617641806619-2.52564108817258i, -1.83729841635945+1.24025696654912i, -0.05940773912914+1.99807537840182i, 2.14861624215501+1.14547234755584i, -0.18935885218927+5.11711397439959i, 3.55025883223277-3.01463113510177i, 0.37587194655463-4.62160286369829i, -0.57999032040714+3.57394816552023i, -3.22078701201057+0i, -0.57999032040714-3.57394816552023i, 0.37587194655463+4.62160286369829i, 3.55025883223277+3.01463113510177i, -0.18935885218927-5.11711397439959i, 2.14861624215501-1.14547234755584i, -0.05940773912914-1.99807537840182i, -1.83729841635945-1.24025696654912i, -1.61617641806619+2.52564108817258i, -3.47255638259391+3.00654729467177i, -0.711636086238366-0.034977366507256i), c(-0.376031201145236+0i, 0.36561036190112-2.94822783523588i, 2.53378536984825+1.14599403212998i, -0.59345500414631-1.46249091231517i, -5.47371957596241-2.40983118775265i, 0.994698295196402+0.827012883372647i, 4.88614691865207-0.66440097322583i, -1.22869446246947-1.85036568311679i, 4.54719422944744-1.7507307644741i, -1.25805718969215-0.46461775748286i, -6.6950163960079-1.32606545879492i, -1.8510470181104-0i, -6.6950163960079+1.32606545879492i, -1.25805718969215+0.46461775748286i, 4.54719422944744+1.7507307644741i, -1.22869446246947+1.85036568311679i, 4.88614691865207+0.66440097322583i, 0.994698295196402-0.827012883372647i, -5.47371957596241+2.40983118775265i, -0.59345500414631+1.46249091231517i, 2.53378536984825-1.14599403212998i, 0.36561036190112+2.94822783523588i), c(1.86949363581639+0i, 3.2510927680528+3.7297126359622i, 5.77117909703734-0.58113122596059i, -2.73489323319193-2.03739778844743i, 1.59256247378073-3.23882870600546i, -2.21652163259476+3.70287191787544i, -6.80966667821261-4.74346958471693i, -0.48551953206469-3.42445496113818i, -4.95350216815663-1.60107509096991i, -0.651322462114205+0.588393022429161i, 3.32067078328635+3.75999833207777i, -1.35013798358527+0i, 3.32067078328635-3.75999833207777i, -0.651322462114205-0.588393022429161i, -4.95350216815663+1.60107509096991i, -0.48551953206469+3.42445496113818i, -6.80966667821261+4.74346958471693i, -2.21652163259476-3.70287191787544i, 1.59256247378073+3.23882870600546i, -2.73489323319193+2.03739778844743i, 5.77117909703734+0.58113122596059i, 3.2510927680528-3.7297126359622i),     c(-3.90806827793786+0i, -4.10078155861753-4.25996878161911i, -0.63461032994351-2.08074582601136i, -0.10593736514835-3.82022652091785i, 6.14817602783479+2.33657685886581i, 0.64431546852762-1.776774088028i, 3.43771282488202-3.00904523977379i, -3.6812061457129+3.53944567666635i, 3.07722382691467+4.5373840425762i, 3.3679046040028+7.20820407858926i, 7.47003475089893-0.4463480891006i, 13.9322715624418-0i, 7.47003475089893+0.4463480891006i, 3.3679046040028-7.20820407858926i, 3.07722382691467-4.5373840425762i,     -3.6812061457129-3.53944567666635i, 3.43771282488202+3.00904523977379i, 0.64431546852762+1.776774088028i, 6.14817602783479-2.33657685886581i, -0.10593736514835+3.82022652091785i, -0.63461032994351+2.08074582601136i, -4.10078155861753+4.25996878161911i)), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor21() {
        assertEval("argv <- list(structure(list(), .Names = character(0)), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor22() {
        assertEval("argv <- list(structure(list(a = 'a', b = 2, c = 3.14159265358979+2i), .Names = c('a', 'b', 'c')), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor23() {
        assertEval("argv <- list(list(structure(list(surname = structure(2L, .Label = c('McNeil', 'R Core', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(NA_integer_, .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(NA_integer_, .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = 1L, class = 'data.frame'), structure(list(title = structure(1L, .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(2L, .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('title', 'other.author'), row.names = 1L, class = 'data.frame')), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor25() {
        assertEval("argv <- list(list(structure(list(structure(c('', '+ M.user', '+ Temp', '+ M.user:Temp'), class = 'AsIs')), row.names = c(NA, -4L), class = 'data.frame'), structure(list(c(NA, -1, -1, -1)), row.names = c(NA, -4L), class = 'data.frame'), structure(list(c(NA, 20.5814660332393, 3.80016287449608, 2.78794934284365)), row.names = c(NA, -4L), class = 'data.frame'), structure(list(c(11, 10, 9, 8)), row.names = c(NA, -4L), class = 'data.frame'), structure(list(c(32.825622681839, 12.2441566485997, 8.44399377410362, 5.65604443125997)), row.names = c(NA, -4L), class = 'data.frame'), structure(list(c(92.5235803967766, 73.9421143635373, 72.1419514890413, 71.3540021461976)), row.names = c(NA, -4L), class = 'data.frame')), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor26() {
        assertEval("argv <- list(structure(list(`2005` = structure(c(31L, 28L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L), .Dim = 12L, .Dimnames = structure(list(c('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12')), .Names = ''), class = 'table'), `2006` = structure(c(31L, 28L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L), .Dim = 12L, .Dimnames = structure(list(c('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12')), .Names = ''), class = 'table'), `2007` = structure(c(31L, 28L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L), .Dim = 12L, .Dimnames = structure(list(c('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12')), .Names = ''), class = 'table'), `2008` = structure(c(31L, 29L, 31L, 30L, 31L, 30L, 31L, 31L, 30L, 31L, 30L, 31L), .Dim = 12L, .Dimnames = structure(list(c('01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12')), .Names = ''), class = 'table'), `2009` = structure(1L, .Dim = 1L, .Dimnames = structure(list('01'), .Names = ''), class = 'table')), .Names = c('2005', '2006', '2007', '2008', '2009')), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor27() {
        assertEval("argv <- list(structure('mtext(\\\'«Latin-1 accented chars»: éè øØ å<Å æ<Æ\\\', side = 3)\\n', Rd_tag = 'RCODE'), TRUE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor28() {
        assertEval("argv <- list(structure(list(V1 = structure(c('head', '1', '3', '6'), class = 'AsIs'), V2 = structure(c('NA', ' 2', ' 4', ' 7'), class = 'AsIs'), V3 = structure(c('NA', 'NA', ' 5', ' 8'), class = 'AsIs'), V4 = structure(c('NA', 'NA', 'NA', ' 9'), class = 'AsIs')), .Names = c('V1', 'V2', 'V3', 'V4'), row.names = c('1', '2', '3', '4')), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testislistfactor29() {
        assertEval("argv <- list(structure(list(`1 sec` = 345600, `2 secs` = 172800, `5 secs` = 69120, `10 secs` = 34560, `15 secs` = 23040, `30 secs` = 11520, `1 min` = 5760, `2 mins` = 2880, `5 mins` = 1152, `10 mins` = 576, `15 mins` = 384, `30 mins` = 192, `1 hour` = 96, `3 hours` = 32, `6 hours` = 16, `12 hours` = 8, `1 DSTday` = 4, `2 DSTdays` = 2, `1 week` = 0.571428571428571, halfmonth = 0.262833675564682, `1 month` = 0.131416837782341, `3 months` = 0.0438056125941136, `6 months` = 0.0219028062970568, `1 year` = 0.0109514031485284,     `2 years` = 0.0054757015742642, `5 years` = 0.00219028062970568, `10 years` = 0.00109514031485284, `20 years` = 0.00054757015742642, `50 years` = 0.000219028062970568, `100 years` = 0.000109514031485284, `200 years` = 5.4757015742642e-05, `500 years` = 2.19028062970568e-05, `1000 years` = 1.09514031485284e-05), .Names = c('1 sec', '2 secs', '5 secs', '10 secs', '15 secs', '30 secs', '1 min', '2 mins', '5 mins', '10 mins', '15 mins', '30 mins', '1 hour', '3 hours', '6 hours', '12 hours', '1 DSTday', '2 DSTdays', '1 week', 'halfmonth', '1 month', '3 months', '6 months', '1 year', '2 years', '5 years', '10 years', '20 years', '50 years', '100 years', '200 years', '500 years', '1000 years')), FALSE); .Internal(islistfactor(argv[[1]], argv[[2]]))");
    }
}
