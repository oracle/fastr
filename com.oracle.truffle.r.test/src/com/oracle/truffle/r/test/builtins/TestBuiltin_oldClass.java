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
public class TestBuiltin_oldClass extends TestBase {

    @Test
    public void testoldClass1() {
        assertEval("argv <- list(structure(list(`cbind(w = weight, w2 = weight^2)` = structure(c(4.17, 5.58, 5.18, 6.11, 4.5, 4.61, 5.17, 4.53, 5.33, 5.14, 4.81, 4.17, 4.41, 3.59, 5.87, 3.83, 6.03, 4.89, 4.32, 4.69, 17.3889, 31.1364, 26.8324, 37.3321, 20.25, 21.2521, 26.7289, 20.5209, 28.4089, 26.4196, 23.1361, 17.3889, 19.4481, 12.8881, 34.4569, 14.6689, 36.3609, 23.9121, 18.6624, 21.9961), .Dim = c(20L, 2L), .Dimnames = list(NULL, c('w', 'w2'))), group = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = c('cbind(w = weight, w2 = weight^2)', 'group'), terms = quote(cbind(w = weight, w2 = weight^2) ~ group), row.names = c(NA, 20L), class = 'data.frame'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass2() {
        assertEval("argv <- list(structure(list(y = structure(c(8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536, 9.79424), .Tsp = c(1962.25, 1971.75, 4), class = 'ts'), lag.quarterly.revenue = c(8.79636, 8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536), price.index = c(4.70997, 4.70217, 4.68944, 4.68558, 4.64019, 4.62553, 4.61991, 4.61654, 4.61407, 4.60766, 4.60227, 4.5896, 4.57592, 4.58661, 4.57997, 4.57176, 4.56104, 4.54906, 4.53957, 4.51018, 4.50352, 4.4936, 4.46505, 4.44924, 4.43966, 4.42025, 4.4106, 4.41151, 4.3981, 4.38513, 4.3732, 4.3277, 4.32023, 4.30909, 4.30909, 4.30552, 4.29627, 4.27839, 4.27789), income.level = c(5.8211, 5.82558, 5.83112, 5.84046, 5.85036, 5.86464, 5.87769, 5.89763, 5.92574, 5.94232, 5.95365, 5.9612, 5.97805, 6.00377, 6.02829, 6.03475, 6.03906, 6.05046, 6.05563, 6.06093, 6.07103, 6.08018, 6.08858, 6.10199, 6.11207, 6.11596, 6.12129, 6.122, 6.13119, 6.14705, 6.15336, 6.15627, 6.16274, 6.17369, 6.16135, 6.18231, 6.18768, 6.19377, 6.2003), market.potential = c(12.9699, 12.9733, 12.9774, 12.9806, 12.9831, 12.9854, 12.99, 12.9943, 12.9992, 13.0033, 13.0099, 13.0159, 13.0212, 13.0265, 13.0351, 13.0429, 13.0497, 13.0551, 13.0634, 13.0693, 13.0737, 13.077, 13.0849, 13.0918, 13.095, 13.0984, 13.1089, 13.1169, 13.1222, 13.1266, 13.1356, 13.1415, 13.1444, 13.1459, 13.152, 13.1593, 13.1579, 13.1625, 13.1664)), .Names = c('y', 'lag.quarterly.revenue', 'price.index', 'income.level', 'market.potential'), row.names = c('1962.25', '1962.5', '1962.75', '1963', '1963.25', '1963.5', '1963.75', '1964', '1964.25', '1964.5', '1964.75', '1965', '1965.25', '1965.5', '1965.75', '1966', '1966.25', '1966.5', '1966.75', '1967', '1967.25', '1967.5', '1967.75', '1968', '1968.25', '1968.5', '1968.75', '1969', '1969.25', '1969.5', '1969.75', '1970', '1970.25', '1970.5', '1970.75', '1971', '1971.25', '1971.5', '1971.75'), class = 'data.frame'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass3() {
        assertEval("argv <- list(c(7.50863122075491e-09, 1.87762632589663e-07, 2.29589583061716e-06, 1.83002461474278e-05, 0.000106962770210119, 0.000488992941332962, 0.00182154707835978, 0.0056884235091347, 0.0152093632759767, 0.0353957474549943, 0.0726726073416657, 0.13316547411151));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass4() {
        assertEval("argv <- list(c(0.2853725+0.3927816i, -0.07283992154231+0.224178134292i, -0.10883955678256+0.035364093700981i, -0.0449501817243521-0.0326582354266614i, 8.2299281e-09-2.69753665872767e-02i, 0.0105954299973322-0.0076980245688633i, 0.00604728675391113+0.00196488543076221i, 0.00095395849586903+0.00293598723445021i, -0.00088096824266454+0.00121254736140417i, -7.27670402517897e-04-4.44010655e-10i, -2.07656947543323e-04-2.85815671682054e-04i, 5.3003554565545e-05-1.6312776087427e-04i, 7.9199339795869e-05-2.57333559721504e-05i, 3.27089023280074e-05+2.37644512768026e-05i, -1.79660253e-11+1.96291758626278e-05i, -7.70998422901389e-06+5.60161993213361e-06i, -4.4004307139296e-06-1.42979165736404e-06i, -6.9416605906477e-07-2.13643143624753e-06i, 6.41055054129141e-07-8.82334435385704e-07i, 5.29504214700362e-07+6.46186824e-13i));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass5() {
        assertEval("argv <- list(structure(list(Subject = structure(c(1L, 3L, 6L, 2L, 4L, 5L), .Label = c('1', '4', '2', '5', '6', '3'), class = c('ordered', 'factor')), conc.0.25 = c(1.5, 2.03, 2.72, 1.85, 2.05, 2.31), conc.0.5 = c(0.94, 1.63, 1.49, 1.39, 1.04, 1.44), conc.0.75 = c(0.78, 0.71, 1.16, 1.02, 0.81, 1.03), conc.1 = c(0.48, 0.7, 0.8, 0.89, 0.39, 0.84), conc.1.25 = c(0.37, 0.64, 0.8, 0.59, 0.3, 0.64), conc.2 = c(0.19, 0.36, 0.39, 0.4, 0.23, 0.42)), row.names = c(1L, 12L, 23L, 34L, 45L, 56L), .Names = c('Subject', 'conc.0.25', 'conc.0.5', 'conc.0.75', 'conc.1', 'conc.1.25', 'conc.2'), class = c('nfnGroupedData', 'nfGroupedData', 'groupedData', 'data.frame')));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass6() {
        assertEval("argv <- list(structure(c(-0.560475646552213+0i, 0.7424437487+0.205661411508856i, 1.39139505579429-0.26763356813179i, 0.928710764113827-0.221714979045717i, -0.46926798541295+1.18846175213664i, 0.7424437487-0.205661411508856i, 0.460916205989202+0i, -0.452623703774585+0.170604003753717i, -0.094501186832143+0.54302538277632i, -0.331818442379127+0.612232958468282i, 1.39139505579429+0.26763356813179i, -0.452623703774585-0.170604003753717i, 0.400771450594052+0i, -0.927967220342259+0.479716843914174i, -0.790922791530657+0.043092176305418i, 0.928710764113827+0.221714979045717i, -0.094501186832143-0.54302538277632i, -0.927967220342259-0.479716843914174i, 0.701355901563686+0i, -0.600841318509537+0.213998439984336i, -0.46926798541295-1.18846175213664i, -0.331818442379127-0.612232958468282i, -0.790922791530657-0.043092176305418i, -0.600841318509537-0.213998439984336i, -0.625039267849257+0i), .Dim = c(5L, 5L)));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass7() {
        assertEval("argv <- list(3.18309886183791e-301);oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass8() {
        assertEval("argv <- list(structure(list(visible = c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), from = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L), .Label = 'registered S3method for predict', class = 'factor')), .Names = c('visible', 'from'), row.names = c('predict.Arima', 'predict.HoltWinters', 'predict.StructTS', 'predict.ar', 'predict.arima0', 'predict.bSpline', 'predict.bs', 'predict.glm', 'predict.glmmPQL', 'predict.lda', 'predict.lm', 'predict.loess', 'predict.lqs', 'predict.mca', 'predict.mlm', 'predict.nbSpline', 'predict.nls', 'predict.npolySpline', 'predict.ns', 'predict.pbSpline', 'predict.polr', 'predict.poly', 'predict.polySpline', 'predict.ppolySpline', 'predict.ppr', 'predict.prcomp', 'predict.princomp', 'predict.qda', 'predict.rlm', 'predict.smooth.spline', 'predict.smooth.spline.fit'), class = 'data.frame'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass9() {
        assertEval("argv <- list(structure(c(4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L), .Label = c('C', 'E', 'D', 'A', 'F', 'B'), class = 'factor', scores = structure(c(14, 16.5, 1.5, 5, 3, 15), .Dim = 6L, .Dimnames = list(c('A', 'B', 'C', 'D', 'E', 'F')))));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass10() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass11() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/tmp/RtmpS45wYI/R.INSTALL2aa62411bcd3/rpart/R/rsq.rpart.R', '/home/lzhao/tmp/RtmpS45wYI/R.INSTALL2aa62411bcd3/rpart/R/rsq.rpart.R'), frow = c(7L, 9L), lrow = c(7L, 9L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass12() {
        assertEval("argv <- list(structure(1:5, .Tsp = c(1, 5, 1), class = 'ts'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass13() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass14() {
        assertEval("argv <- list(structure(list(`cbind(A, B, C, D)` = structure(c(0.696706709347165, 0.362357754476673, -0.0291995223012888, 0.696706709347165, 0.696706709347165, -0.0291995223012888, 0.696706709347165, -0.0291995223012888, 0.362357754476673, 0.696706709347165, -0.0291995223012888, 0.362357754476673, -0.416146836547142, 0.362357754476673, 0.696706709347165, 0.696706709347165, 0.362357754476673, -0.416146836547142, -0.0291995223012888, -0.416146836547142, 0.696706709347165, -0.416146836547142, 0.362357754476673, -0.0291995223012888, 0.717356090899523, 0.932039085967226, 0.999573603041505, 0.717356090899523, 0.717356090899523, 0.999573603041505, 0.717356090899523, 0.999573603041505, 0.932039085967226, 0.717356090899523, 0.999573603041505, 0.932039085967226, 0.909297426825682, 0.932039085967226, 0.717356090899523, 0.717356090899523, 0.932039085967226, 0.909297426825682, 0.999573603041505, 0.909297426825682, 0.717356090899523, 0.909297426825682, 0.932039085967226, 0.999573603041505, -0.0291995223012888, -0.737393715541246, -0.998294775794753, -0.0291995223012888, -0.0291995223012888, -0.998294775794753, -0.0291995223012888, -0.998294775794753, -0.737393715541246, -0.0291995223012888, -0.998294775794753, -0.737393715541246, -0.653643620863612, -0.737393715541246, -0.0291995223012888, -0.0291995223012888, -0.737393715541246, -0.653643620863612, -0.998294775794753, -0.653643620863612, -0.0291995223012888, -0.653643620863612, -0.737393715541246, -0.998294775794753, 0.999573603041505, 0.67546318055115, -0.0583741434275801, 0.999573603041505, 0.999573603041505, -0.0583741434275801, 0.999573603041505, -0.0583741434275801, 0.67546318055115, 0.999573603041505, -0.0583741434275801, 0.67546318055115, -0.756802495307928, 0.67546318055115, 0.999573603041505, 0.999573603041505, 0.67546318055115, -0.756802495307928, -0.0583741434275801, -0.756802495307928, 0.999573603041505, -0.756802495307928, 0.67546318055115, -0.0583741434275801), .Dim = c(24L, 4L), .Dimnames = list(NULL, c('A', 'B', 'C', 'D'))), groups = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('1', '2', '3'), class = 'factor')), .Names = c('cbind(A, B, C, D)', 'groups'), terms = quote(cbind(A, B, C, D) ~ groups), row.names = c(NA, 24L), class = 'data.frame'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass15() {
        assertEval("argv <- list(structure(list(), .Names = character(0), class = 'data.frame', row.names = c(NA, -10L)));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass16() {
        assertEval("argv <- list(structure(list(y = c(1.08728092481538, 0.0420572471552261, 0.787502161306819, 0.512717751544676, 3.35376639535311, 0.204341510750309, -0.334930602487435, 0.80049208412789, -0.416177803375218, -0.777970346246018, 0.934996808181635, -0.678786709127108, 1.52621589791412, 0.5895781228122, -0.744496121210548, -1.99065153885627, 1.51286447692396, -0.750182409847851), A = c(0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1), U = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('a', 'b', 'c'), class = 'factor'), V = structure(c(1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L, 1L, 1L, 2L, 2L, 3L, 3L), .Label = c('a', 'b', 'c'), class = 'factor')), .Names = c('y', 'A', 'U', 'V'), class = 'data.frame', row.names = c(NA, 18L), terms = quote(y ~ A:U + A:V - 1)));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass17() {
        assertEval("argv <- list(structure(list(surname = structure(integer(0), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(integer(0), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(integer(0), .Label = c('no', 'yes'), class = 'factor'), title = structure(integer(0), .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(integer(0), .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased', 'title', 'other.author'), row.names = integer(0), class = 'data.frame'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass18() {
        assertEval("argv <- list(structure(list(srcfile = c(NA, NA, '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R', '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/R/pdMat.R'), frow = c(NA, NA, 1739L, 1741L, 1807L, 1868L, 1868L, 1868L, 1870L, 1873L, 1888L, 1888L, 1888L, 1898L, 1898L, 1898L, 1899L, 1905L), lrow = c(NA, NA, 1739L, 1742L, 1807L, 1868L, 1868L, 1868L, 1872L, 1873L, 1888L, 1888L, 1888L, 1898L, 1898L, 1898L, 1901L, 1905L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 18L), class = 'data.frame'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass19() {
        assertEval("argv <- list(structure(list(double.eps = 2.22044604925031e-16, double.neg.eps = 1.11022302462516e-16, double.xmin = 2.2250738585072e-308, double.xmax = 1.79769313486232e+308, double.base = 2L, double.digits = 53L, double.rounding = 5L, double.guard = 0L, double.ulp.digits = -52L, double.neg.ulp.digits = -53L, double.exponent = 11L, double.min.exp = -1022L, double.max.exp = 1024L, integer.max = 2147483647L, sizeof.long = 8L, sizeof.longlong = 8L, sizeof.longdouble = 16L, sizeof.pointer = 8L), .Names = c('double.eps', 'double.neg.eps', 'double.xmin', 'double.xmax', 'double.base', 'double.digits', 'double.rounding', 'double.guard', 'double.ulp.digits', 'double.neg.ulp.digits', 'double.exponent', 'double.min.exp', 'double.max.exp', 'integer.max', 'sizeof.long', 'sizeof.longlong', 'sizeof.longdouble', 'sizeof.pointer')));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass20() {
        assertEval("argv <- list(structure(list(Model = 1:2, df = c(5, 6), AIC = c('1571.455', '1570.925'), BIC = c('1590.056', '1593.247'), logLik = c(-780.727255295109, -779.462624623506), Test = structure(1:2, .Label = c('', '1 vs 2'), class = 'factor'), L.Ratio = c(NA, 2.52926134320705), `p-value` = c(NA, 0.111752518719366)), .Names = c('Model', 'df', 'AIC', 'BIC', 'logLik', 'Test', 'L.Ratio', 'p-value'), row.names = c('fm1', 'fm2'), class = 'data.frame'));oldClass(argv[[1]]);");
    }

    @Test
    public void testoldClass22() {
        assertEval("argv <- list(c('1.537e+04', '1.54e+04', '1.546e+04'));do.call('oldClass', argv)");
    }

    @Test
    public void testGetClass() {
        assertEval("{ x<-1; oldClass(x) }");
        assertEval("{ oldClass(NULL) }");
    }
}
