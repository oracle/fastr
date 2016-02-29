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
public class TestBuiltin_copyDFattr extends TestBase {

    @Test
    public void testcopyDFattr1() {
        assertEval("argv <- list(structure(list(size = 1056, isdir = FALSE, mode = structure(420L, class = 'octmode'), mtime = structure(1395082115.08988, class = c('POSIXct', 'POSIXt')), ctime = structure(1395082122.18188, class = c('POSIXct', 'POSIXt')), atime = structure(1395082175.70988, class = c('POSIXct', 'POSIXt')), uid = 1001L, gid = 1001L, uname = 'roman', grname = 'roman'), .Names = c('size', 'isdir', 'mode', 'mtime', 'ctime', 'atime', 'uid', 'gid', 'uname', 'grname'), class = 'data.frame', row.names = '/home/roman/r-instrumented/tests/myLib/pkgA/R/pkgA'), structure(list(    size = NULL, isdir = NULL, mode = NULL, mtime = NULL, ctime = NULL, atime = NULL, uid = NULL, gid = NULL, uname = NULL, grname = NULL), .Names = c('size', 'isdir', 'mode', 'mtime', 'ctime', 'atime', 'uid', 'gid', 'uname', 'grname'), class = 'data.frame', row.names = '/home/roman/r-instrumented/tests/myLib/pkgA/R/pkgA')); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr2() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(y = c(2.30923841792462, 3.23011719303818, 2.9161246695212, 3.35931329373059, 5.3777049208621, 5.63518136825043, 7.37725908636056, 7.75621985157329, 10.1175871700049, 8.86877085545769), x1 = 1:10, x2 = 1:10, x3 = c(0.1, 0.4, 0.9, 1.6, 2.5, 3.6, 4.9, 6.4, 8.1, 10)), .Names = c('y', 'x1', 'x2', 'x3'), class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ x1 + x2 + x3)), structure(list(y = NULL, x1 = NULL, x2 = NULL, x3 = NULL), .Names = c('y', 'x1', 'x2', 'x3'), class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ x1 + x2 + x3))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr3() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861), x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), z = 1:10), .Names = c('y', 'x', 'z'), class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ x * z)), structure(list(y = NULL, x = NULL, z = NULL), .Names = c('y', 'x', 'z'), class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ x * z))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr4() {
        assertEval("argv <- list(structure(list(surname = structure(integer(0), .Label = c('McNeil', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(integer(0), .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(integer(0), .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = integer(0), class = 'data.frame'), structure(list(surname = NULL, nationality = NULL, deceased = NULL), .Names = c('surname', 'nationality', 'deceased'), row.names = integer(0), class = 'data.frame')); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr5() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(A = 0:10, B = 10:20, `NA` = 20:30), .Names = c('A', 'B', NA), row.names = c(NA, -11L), class = 'data.frame'), structure(list(A = NULL, B = NULL, `NA` = NULL), .Names = c('A', 'B', NA), row.names = c(NA, -11L), class = 'data.frame')); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr6() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(`Surv(stop, status * as.numeric(event), type = 'mstate')` = structure(c(760, 2160, 5441, 277, 1815, 2587, 547, 1125, 2010, 2422, 6155, 1767, 61, 60, 7807, 7732, 6126, 7921, 3590, 5231, 5384, 5934, 6415, 6789, 6778, 3561, 4505, 3987, 4726, 5550, 5216, 5757, 2345, 6931, 6760, 5796, 4810, 5143, 3091, 3316, 700, 1706, 5088, 944, 2466, 1706, 7364, 1857, 9510, 9603, 31, 7479, 2006, 2588, 2983, 8761, 3932, 4201, 5293, 273, 2223, 4249, 5308, 8327, 499, 5789, 7417, 3242, 3275, 10359, 10852, 362, 9993, 1795, 3562, 4139, 4840, 4959, 547, 4119, 8308, 1674, 2953, 3776, 1369, 7911, 7519, 9318, 4370, 7301, 1642, 4169, 7417, 6117, 4536, 7235, 6723, 7397, 7428, 2084, 4066, 1673, 2860, 0, 3773, 4810, 4206, 2314, 4065, 8961, 6143, 517, 3837, 7498, 2815, 8806, 7668, 12457, 8600, 7003, 2435, 1826, 2403, 3805, 4901, 365, 6642, 3318, 3012, 1431, 2223, 4962, 5982, 638, 3346, 4996, 6800, 7454, 8887, 5024, 2833, 4232, 5238, 3186, 3380, 3382, 8100, 1766, 7184, 8059, 6008, 5047, 2236, 8165, 4224, 2844, 6256, 7370, 3560, 4939, 4941, 2230, 3068, 152, 10122, 3226, 3943, 518, 8569, 845, 2099, 8006, 8052, 9560, 0, 7965, 7470, 8133, 809, 153, 1851, 3010, 2121, 7085, 5068, 7093, 5930, 6878, 8080, 791, 6626, 3962, 1116, 1249, 9257, 1077, 566, 174, 4627, 5022, 2070, 3012, 1625, 6607, 8381, 8389, 1005, 3895, 4236, 6970, 8497, 2861, 8487, 3227, 8030, 8023, 31, 2435, 518, 4758, 7958, 7884, 4453, 6349, 7862, 1392, 3167, 6025, 4656, 1767, 7736, 2678, 2191, 3658, 7758, 8009, 2556, 3511, 7954, 822, 4321, 5151, 7545, 7576, 32, 7875, 5236, 7106, 2802, 7898, 3014, 7867, 5354, 2989, 7555, 6089, 8697, 6479, 1826, 5917, 792, 1431, 1434, 4763, 2910, 6209, 5824, 2400, 1400, 3027, 7198, 7247, 2557, 3855, 61, 7410, 1492, 7160, 7899, 5181, 7280, 3448, 7381, 2434, 6763, 7065, 1218, 1554, 7533, 7288, 2922, 5988, 2495, 5234, 9598, 2953, 2961, 4539, 3775, 6524, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0, 0, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 0, 2, 1, 2, 2, 0, 2, 1, 2, 0, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 0, 1, 2, 1, 2, 2, 0, 1, 2, 1, 2, 2, 2, 2, 0, 2, 1, 2, 2, 0, 1, 2, 2, 0, 1, 2, 0, 2, 1, 2, 2, 1, 2, 1, 2, 2, 2, 2, 1, 2, 2, 1, 2, 0, 2, 2, 1, 0, 2, 2, 0, 0, 2, 0, 2, 1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 2, 2, 1, 2, 1, 0, 0, 2, 2, 1, 2, 2, 1, 2, 0, 2, 1, 2, 2, 2, 2, 0, 2, 2, 2, 0, 2, 1, 2, 1, 2, 2, 0, 2, 2, 2, 0, 2, 2, 1, 2, 0, 2, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 0, 2, 2, 0, 2, 2, 2, 1, 2, 0, 2, 2, 2, 1, 2, 1, 2, 2, 2, 0, 0, 2, 1, 2, 1, 0, 1, 0, 2, 0, 0, 2, 2, 2, 2, 0, 0, 2, 2, 0, 2, 2, 2, 2, 2, 0, 2, 1, 2, 0, 0, 1, 2, 0, 2, 1, 2, 1, 2, 2, 0, 1, 2, 1, 0, 2, 0, 2, 2, 0, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1, 2, 0, 0, 1, 2, 2, 0, 2, 0, 2, 2, 0, 2, 0, 2, 2, 0, 1, 2, 0, 0, 1, 2, 1, 2, 0, 1, 2, 2, 1, 2), .Dim = c(300L, 2L), .Dimnames = list(NULL, c('time', 'status')), type = 'mright', states = c('1', '2'), class = 'Surv')), .Names = 'Surv(stop, status * as.numeric(event), type = \\\'mstate\\\')', class = 'data.frame', row.names = c(NA, 300L)), structure(list(`Surv(stop, status * as.numeric(event), type = 'mstate')` = NULL), .Names = 'Surv(stop, status * as.numeric(event), type = \\\'mstate\\\')', class = 'data.frame', row.names = c(NA, 300L))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr7() {
        assertEval("argv <- list(structure(list(size = 284, isdir = FALSE, mode = structure(436L, class = 'octmode'), mtime = structure(1386397148.36693, class = c('POSIXct', 'POSIXt')), ctime = structure(1386397148.36693, class = c('POSIXct', 'POSIXt')), atime = structure(1386397148.36793, class = c('POSIXct', 'POSIXt')), uid = 501L, gid = 501L, uname = 'lzhao', grname = 'lzhao'), .Names = c('size', 'isdir', 'mode', 'mtime', 'ctime', 'atime', 'uid', 'gid', 'uname', 'grname'), class = 'data.frame', row.names = '/home/lzhao/tmp/RtmpvWhahC/Rex4eb8743f75cc'), structure(list(    size = NULL, isdir = NULL, mode = NULL, mtime = NULL, ctime = NULL, atime = NULL, uid = NULL, gid = NULL, uname = NULL, grname = NULL), .Names = c('size', 'isdir', 'mode', 'mtime', 'ctime', 'atime', 'uid', 'gid', 'uname', 'grname'), class = 'data.frame', row.names = '/home/lzhao/tmp/RtmpvWhahC/Rex4eb8743f75cc')); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr8() {
        assertEval("argv <- list(structure(list(File = character(0), Title = character(0), PDF = character(0), Depends = list(), Keywords = list()), .Names = c('File', 'Title', 'PDF', 'Depends', 'Keywords'), row.names = integer(0), class = 'data.frame'), structure(list(File = NULL, Title = NULL, PDF = NULL, Depends = NULL, Keywords = NULL), .Names = c('File', 'Title', 'PDF', 'Depends', 'Keywords'), row.names = integer(0), class = 'data.frame')); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr9() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(Version = c('2.11.0', '2.11.0', '2.11.0', '2.11.0', '2.11.0', '2.11.0', '2.11.0', '2.11.0', '2.11.0'), Date = c(NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_, NA_character_), Category = c('BUG FIXES', 'BUG FIXES', 'BUG FIXES', 'BUG FIXES', 'BUG FIXES', 'BUG FIXES', 'BUG FIXES', 'BUG FIXES', 'BUG FIXES'), Text = c('Using with(), eval() etc with a list with some unnamed elements now\\n works.  (PR#14035)', 'cor(A, B) where A is n x 1 and B a 1-dimensional array segfaulted or\\n gave an internal error.  (The case cor(B, A) was PR#7116.)', 'cut.POSIXt() applied to a start value after the DST transition on a\\n DST-change day could give the wrong time for argument breaks in\\n units of days or longer.  (PR#14208)', 'do_par() UNPROTECTed too early (PR#14214)', 'Subassignment x[[....]] <- y didn't check for a zero-length right\\n hand side, and inserted a rubbish value.  (PR#14217)', 'Extreme tail behavior of, pbeta() {and hence pf()}, e.g., pbeta(x,\\n 3, 2200, lower.tail=FALSE, log.p=TRUE) now returns finite values\\n instead of jumping to -Inf too early.  (PR#14230).', 'read.fwf() misread multi-line records when n was specified.\\n (PR#14241)', 'gzcon( <textConnection> ), an error, no longer damages the\\n connection (in a way to have it segfault).  (PR#14237)', 'If xy[z].coords (used internally by many graphics functions) are\\n given a list as x, they now check that the list has suitable names\\n and give a more informative error message.  (PR#13936)')), .Names = c('Version', 'Date', 'Category', 'Text'), bad = c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), package = 'R', row.names = c(1473L, 1483L, 1484L, 1485L, 1486L, 1493L, 1499L, 1503L, 1505L), class = c('news_db_from_Rd', 'news_db', 'data.frame')), structure(list(Version = NULL, Date = NULL, Category = NULL, Text = NULL), .Names = c('Version', 'Date', 'Category', 'Text'), bad = c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), package = 'R', row.names = c(1473L, 1483L, 1484L, 1485L, 1486L, 1493L, 1499L, 1503L, 1505L), class = c('news_db_from_Rd', 'news_db', 'data.frame'))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr10() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(Df = c(1, 1, 1, 1, 16), `Sum Sq` = c(309.6845, 0.420500000000001, 4.90050000000001, 3.9605, 64.924), `Mean Sq` = c(309.6845, 0.420500000000001, 4.90050000000001, 3.9605, 4.05775), `F value` = c(76.3192656028586, 0.103628858357464, 1.20768899020393, 0.976033516111146, NA), `Pr(>F)` = c(1.73825946976405e-07, 0.751685166772039, 0.288052080502172, 0.337885793589305, NA)), .Names = c('Df', 'Sum Sq', 'Mean Sq', 'F value', 'Pr(>F)'), class = c('anova', 'data.frame'), row.names = c('(Intercept)  ', 'rate         ', 'additive     ', 'rate:additive', 'Residuals    ')), structure(list(Df = NULL, `Sum Sq` = NULL, `Mean Sq` = NULL, `F value` = NULL, `Pr(>F)` = NULL), .Names = c('Df', 'Sum Sq', 'Mean Sq', 'F value', 'Pr(>F)'), class = c('anova', 'data.frame'), row.names = c('(Intercept)  ', 'rate         ', 'additive     ', 'rate:additive', 'Residuals    '))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr11() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(srcfile = c(NA, '/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats'), frow = c(NA, 2228L, 2369L, 2379L), lrow = c(NA, 2228L, 2369L, 2380L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 4L), class = 'data.frame'), structure(list(srcfile = NULL, frow = NULL, lrow = NULL), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 4L), class = 'data.frame')); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr12() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(y = c(73, 73, 70, 74, 75, 115, 105, 107, 124, 107, 116, 125, 102, 144, 178, 149, 177, 124, 157, 128, 169, 165, 186, 152, 181, 139, 173, 151, 138, 181, 152, 188, 173, 196, 180, 171, 188, 174, 198, 172, 176, 162, 188, 182, 182, 141, 191, 190, 159, 170, 163, 197), x = c(1, 1, 1, 1, 1, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 11, 12)), .Names = c('y', 'x'), class = 'data.frame', row.names = c(NA, 52L), terms = quote(~y + x)), structure(list(y = NULL, x = NULL), .Names = c('y', 'x'), class = 'data.frame', row.names = c(NA, 52L), terms = quote(~y + x))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr13() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(x = 1:10, y = c(-0.626453810742332, 0.183643324222082, -0.835628612410047, 1.59528080213779, 0.329507771815361, -0.820468384118015, 0.487429052428485, 0.738324705129217, 0.575781351653492, -0.305388387156356), z = structure(c(9L, 3L, 6L, 2L, 10L, 5L, 1L, 4L, 8L, 7L), .Label = c('a', 'c', 'f', 'h', 'i', 'p', 'v', 'x', 'y', 'z'), class = 'factor')), .Names = c('x', 'y', 'z'), row.names = c(NA, 10L), .S3Class = 'data.frame', timestamps = structure(1386392033.84327, class = c('POSIXct', 'POSIXt')), class = structure('myFrame', package = '.GlobalEnv')), structure(list(x = NULL, y = NULL, z = NULL), .Names = c('x', 'y', 'z'), row.names = c(NA, 10L), .S3Class = 'data.frame', timestamps = structure(1386392033.84327, class = c('POSIXct', 'POSIXt')), class = structure('myFrame', package = '.GlobalEnv'))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr14() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(y = c(-0.667819876370237, 0.170711734013213, 0.552921941721332, -0.253162069270378, -0.00786394222146348, 0.0246733498130512, 0.0730305465518564, -1.36919169254062, 0.0881443844426084, -0.0834190388782434)), .Names = 'y', class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ 0)), structure(list(y = NULL), .Names = 'y', class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ 0))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr15() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(`cbind(w = weight, w2 = weight^2)` = structure(c(4.17, 5.58, 5.18, 6.11, 4.5, 4.61, 5.17, 4.53, 5.33, 5.14, 4.81, 4.17, 4.41, 3.59, 5.87, 3.83, 6.03, 4.89, 4.32, 4.69, 17.3889, 31.1364, 26.8324, 37.3321, 20.25, 21.2521, 26.7289, 20.5209, 28.4089, 26.4196, 23.1361, 17.3889, 19.4481, 12.8881, 34.4569, 14.6689, 36.3609, 23.9121, 18.6624, 21.9961), .Dim = c(20L, 2L), .Dimnames = list(NULL, c('w', 'w2'))), group = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = c('cbind(w = weight, w2 = weight^2)', 'group'), class = 'data.frame', row.names = c(NA, 20L), terms = quote(cbind(w = weight, w2 = weight^2) ~ group)), structure(list(`cbind(w = weight, w2 = weight^2)` = NULL, group = NULL), .Names = c('cbind(w = weight, w2 = weight^2)', 'group'), class = 'data.frame', row.names = c(NA, 20L), terms = quote(cbind(w = weight, w2 = weight^2) ~ group))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr16() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(Y = c(130L, 157L, 174L, 117L, 114L, 161L, 141L, 105L, 140L, 118L, 156L, 61L, 91L, 97L, 100L, 70L, 108L, 126L, 149L, 96L, 124L, 121L, 144L, 68L, 64L, 112L, 86L, 60L, 102L, 89L, 96L, 89L, 129L, 132L, 124L, 74L, 89L, 81L, 122L, 64L, 103L, 132L, 133L, 70L, 89L, 104L, 117L, 62L, 90L, 100L, 116L, 80L, 82L, 94L, 126L, 63L, 70L, 109L, 99L, 53L, 74L, 118L, 113L, 89L, 82L, 86L, 104L, 97L, 99L, 119L, 121L), B = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L), .Label = c('I', 'II', 'III', 'IV', 'V', 'VI'), class = 'factor'), V = structure(c(3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L), .Label = c('Golden.rain', 'Marvellous', 'Victory'), class = 'factor'), N = structure(c(2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt'), class = 'factor')), .Names = c('Y', 'B', 'V', 'N'), terms = quote(Y ~ B + V + N + V:N), row.names = 2:72, class = 'data.frame'), structure(list(Y = NULL, B = NULL, V = NULL, N = NULL), .Names = c('Y', 'B', 'V', 'N'), terms = quote(Y ~ B + V + N + V:N), row.names = 2:72, class = 'data.frame')); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr17() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(Fr = c(32, 53, 10, 3, 11, 50, 10, 30, 10, 25, 7, 5, 3, 15, 7, 8, 36, 66, 16, 4, 9, 34, 7, 64, 5, 29, 7, 5, 2, 14, 7, 8), Hair = structure(c(1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('Black', 'Brown', 'Red', 'Blond'), class = 'factor'), Eye = structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L), .Label = c('Brown', 'Blue', 'Hazel', 'Green'), class = 'factor'), Sex = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Male', 'Female'), class = 'factor')), .Names = c('Fr', 'Hair', 'Eye', 'Sex'), class = 'data.frame', row.names = c(NA, 32L), terms = quote(Fr ~ (Hair + Eye + Sex)^2)), structure(list(Fr = NULL, Hair = NULL, Eye = NULL, Sex = NULL), .Names = c('Fr', 'Hair', 'Eye', 'Sex'), class = 'data.frame', row.names = c(NA, 32L), terms = quote(Fr ~ (Hair + Eye + Sex)^2))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr18() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551), GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962), .Names = c('Employed', 'GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year'), class = 'data.frame', row.names = 1947:1962, terms = quote(Employed ~     GNP.deflator + GNP + Unemployed + Armed.Forces + Population + Year)), structure(list(Employed = NULL, GNP.deflator = NULL, GNP = NULL, Unemployed = NULL, Armed.Forces = NULL, Population = NULL, Year = NULL), .Names = c('Employed', 'GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year'), class = 'data.frame', row.names = 1947:1962, terms = quote(Employed ~ GNP.deflator + GNP + Unemployed + Armed.Forces + Population + Year))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr19() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(Df = c(NA, 1, 1, 2), Deviance = c(12.2441566485997, 32.825622681839, 8.44399377410362, 11.9670615295804), AIC = c(73.9421143635373, 92.5235803967766, 72.1419514890412, 77.665019244518)), .Names = c('Df', 'Deviance', 'AIC'), row.names = c('<none>', '- M.user', '+ Temp', '+ Soft'), class = c('anova', 'data.frame')), structure(list(Df = NULL, Deviance = NULL, AIC = NULL), .Names = c('Df', 'Deviance', 'AIC'), row.names = c('<none>', '- M.user', '+ Temp', '+ Soft'), class = c('anova', 'data.frame'))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testcopyDFattr20() {
        assertEval(Output.ContainsWarning,
                        "argv <- list(structure(list(`cbind(X, M)` = structure(c(68, 42, 37, 24, 66, 33, 47, 23, 63, 29, 57, 19, 42, 30, 52, 43, 50, 23, 55, 47, 53, 27, 49, 29), .Dim = c(12L, 2L), .Dimnames = list(NULL, c('X', 'M'))), M.user = structure(c(1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L), .Label = c('N', 'Y'), class = 'factor'), Temp = structure(c(2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L), .Label = c('High', 'Low'), class = 'factor'), Soft = structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L), .Label = c('Hard', 'Medium', 'Soft'), class = 'factor')), .Names = c('cbind(X, M)', 'M.user', 'Temp', 'Soft'), class = 'data.frame', row.names = c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23'), terms = quote(cbind(X, M) ~ M.user + Temp + Soft)), structure(list(`cbind(X, M)` = NULL, M.user = NULL, Temp = NULL, Soft = NULL), .Names = c('cbind(X, M)', 'M.user', 'Temp', 'Soft'), class = 'data.frame', row.names = c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23'), terms = quote(cbind(X,     M) ~ M.user + Temp + Soft))); .Internal(copyDFattr(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testCopyDDFattr() {
        assertEval("{ x<-7; attr(x, \"foo\")<-\"foo\"; y<-42; z<-.Internal(copyDFattr(x, y)); attributes(z) }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(11,12)); y<-7; z<-.Internal(copyDFattr(y, x)); attributes(z) }");
        assertEval("{ x<-data.frame(a=c(1,2), b=c(11,12)); y<-7; attr(y, \"foo\")<-\"foo\"; z<-.Internal(copyDFattr(y, x)); attributes(z) }");
        assertEval("{ x<-data.frame(c(1,2), c(11,12)); attr(x, \"dim\")<-c(1,2); attr(x, \"dimnames\")<-list(\"a\", c(\"b\", \"c\")); y<-c(7, 42); z<-.Internal(copyDFattr(x, y)); attributes(z) }");
    }
}
