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
public class TestBuiltin_unclass extends TestBase {

    @Test
    public void testunclass1() {
        assertEval("argv <- list(c(-1, -1));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass2() {
        // FIXME "Year" printed on a new line it probably should be on the same line with quote()
        assertEval(Output.IgnoreWhitespace,
                        "argv <- list(structure(list(Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551), GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962), .Names = c('Employed', 'GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year'), terms = quote(Employed ~ GNP.deflator + GNP + Unemployed +     Armed.Forces + Population + Year), row.names = 1947:1962));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass3() {
        // GnuR does not retain row.names vector and substitutes it with 1L:10L
        // Probably GnuR error since for row.names=c(NA, 9L, 10L) GnuR retains the row.names vector
        // as is
        assertEval(Ignored.ReferenceError,
                        "argv <- list(structure(list(x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE)), .Names = 'x', row.names = c(NA, 10L)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass4() {
        assertEval(Ignored.ReferenceError, // similar problem with row.names like above
                        "argv <- list(structure(list(X1.10 = 1:10, z = structure(list(x = 1:10, yyy = 11:20), .Names = c('x', 'yyy'), row.names = c(NA, -10L), class = 'data.frame')), .Names = c('X1.10', 'z'), row.names = c(NA, -10L)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass5() {
        assertEval("argv <- list(structure(list(Df = c(1, 1, 1, NA, 1), `Sum of Sq` = c(0.109090049888117, 0.246974722154086, 2.97247824113524, NA, 25.9509113775335), RSS = c(47.9727294003871, 48.1106140726531, 50.8361175916342, 47.863639350499, 73.8145507280325), AIC = c(24.9738836085411, 25.0111950072736, 25.7275503692601, 26.9442879283302, 30.5758847476115)), .Names = c('Df', 'Sum of Sq', 'RSS', 'AIC'), row.names = c('- x3', '- x4', '- x2', '<none>', '- x1')));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass6() {
        assertEval("argv <- list(structure(list(surname = structure(2L, .Label = c('McNeil', 'R Core', 'Ripley', 'Tierney', 'Tukey', 'Venables'), class = 'factor'), nationality = structure(NA_integer_, .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(NA_integer_, .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = 7L));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass7() {
        assertEval("argv <- list(structure(list(A = c(1L, NA, 1L), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA, NA, NA), E = c(FALSE, NA, TRUE), F = structure(c(1L, NA, 2L), .Label = c('abc', 'def'), class = 'factor')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), row.names = c('1', '2', '3')));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass8() {
        assertEval(Ignored.ReferenceError, // similar problem with row.names like above
                        "argv <- list(structure(list(`cbind(w = weight, w2 = weight^2)` = structure(c(4.17, 5.58, 5.18, 6.11, 4.5, 4.61, 5.17, 4.53, 5.33, 5.14, 4.81, 4.17, 4.41, 3.59, 5.87, 3.83, 6.03, 4.89, 4.32, 4.69, 17.3889, 31.1364, 26.8324, 37.3321, 20.25, 21.2521, 26.7289, 20.5209, 28.4089, 26.4196, 23.1361, 17.3889, 19.4481, 12.8881, 34.4569, 14.6689, 36.3609, 23.9121, 18.6624, 21.9961), .Dim = c(20L, 2L), .Dimnames = list(NULL, c('w', 'w2'))), group = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = c('cbind(w = weight, w2 = weight^2)', 'group'), terms = quote(cbind(w = weight, w2 = weight^2) ~ group), row.names = c(NA, 20L)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass9() {
        assertEval("argv <- list(structure(c('Min.   : 1.00  ', '1st Qu.: 3.25  ', 'Median : 5.50  ', 'Mean   : 5.50  ', '3rd Qu.: 7.75  ', 'Max.   :10.00  ', 'Min.   : 1.00    Min.   :11.00  ', '1st Qu.: 3.25    1st Qu.:13.25  ', 'Median : 5.50    Median :15.50  ', 'Mean   : 5.50    Mean   :15.50  ', '3rd Qu.: 7.75    3rd Qu.:17.75  ', 'Max.   :10.00    Max.   :20.00  '), .Dim = c(6L, 2L), .Dimnames = list(c('', '', '', '', '', ''), c('    X1.10', '      z.x             z.yyy     '))));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass10() {
        assertEval("argv <- list(structure(list(GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962), .Names = c('GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year'), row.names = 1947:1962));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass11() {
        assertEval(Output.IgnoreWhitespace,
                        "argv <- list(structure(list(y = c(0.219628047744843, 0.360454661130887, -1.14267533343616, 0.772374419482067, 0.681741904304867, 0.171869265068012, 2.08409180391906, 0.367547276775469), x1 = c(1L, 2L, 5L, 6L, 7L, 8L, 9L, 10L), x2 = c(1L, 2L, 5L, 6L, 7L, 8L, 9L, 10L), `(weights)` = c(0, 1, 1, 1, 1, 1, 1, 1)), .Names = c('y', 'x1', 'x2', '(weights)'), terms = quote(y ~ x1 + x2), row.names = c('a', 'b', 'e', 'f', 'g', 'h', 'i', 'j'), na.action = structure(3:4, .Names = c('c', 'd'), class = 'omit')));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass12() {
        assertEval(Ignored.ReferenceError, // similar problem with row.names like above
                        "argv <- list(structure(list(`log(x)` = c(0, 0.693147180559945, 1.09861228866811, 1.38629436111989, 1.6094379124341, 1.79175946922805, 1.94591014905531, 2.07944154167984, 2.19722457733622, 2.30258509299405, 2.39789527279837, 2.484906649788, 2.56494935746154, 2.63905732961526, 2.70805020110221, 2.77258872223978, 2.83321334405622, 2.89037175789616, 2.94443897916644, 2.99573227355399, 3.04452243772342, 3.09104245335832, 3.13549421592915, 3.17805383034795, 3.2188758248682, 3.25809653802148, 3.29583686600433, 3.3322045101752, 3.36729582998647, 3.40119738166216, 3.43398720448515, 3.46573590279973, 3.49650756146648, 3.52636052461616, 3.55534806148941, 3.58351893845611, 3.61091791264422, 3.63758615972639, 3.66356164612965, 3.68887945411394, 3.71357206670431, 3.73766961828337, 3.76120011569356, 3.78418963391826, 3.80666248977032, 3.8286413964891, 3.85014760171006, 3.87120101090789, 3.89182029811063, 3.91202300542815, 3.93182563272433, 3.95124371858143, 3.97029191355212, 3.98898404656427, 4.00733318523247, 4.02535169073515, 4.04305126783455, 4.06044301054642, 4.07753744390572, 4.0943445622221, 4.11087386417331, 4.12713438504509, 4.14313472639153, 4.15888308335967, 4.17438726989564, 4.18965474202643, 4.20469261939097, 4.21950770517611, 4.23410650459726, 4.24849524204936, 4.26267987704132, 4.27666611901606, 4.29045944114839, 4.30406509320417, 4.31748811353631, 4.33073334028633, 4.34380542185368, 4.35670882668959, 4.36944785246702, 4.38202663467388, 4.39444915467244, 4.40671924726425, 4.4188406077966, 4.43081679884331, 4.44265125649032, 4.45434729625351, 4.46590811865458, 4.47733681447821, 4.48863636973214, 4.49980967033027, 4.51085950651685, 4.52178857704904, 4.53259949315326, 4.54329478227, 4.55387689160054, 4.56434819146784, 4.57471097850338, 4.58496747867057, 4.59511985013459, 4.60517018598809), `log(z)` = c(2.39789527279837, 2.484906649788, 2.56494935746154, 2.63905732961526, 2.70805020110221, 2.77258872223978, 2.83321334405622, 2.89037175789616, 2.94443897916644, 2.99573227355399, 3.04452243772342, 3.09104245335832, 3.13549421592915, 3.17805383034795, 3.2188758248682, 3.25809653802148, 3.29583686600433, 3.3322045101752, 3.36729582998647, 3.40119738166216, 3.43398720448515, 3.46573590279973, 3.49650756146648, 3.52636052461616, 3.55534806148941, 3.58351893845611, 3.61091791264422, 3.63758615972639, 3.66356164612965, 3.68887945411394, 3.71357206670431, 3.73766961828337, 3.76120011569356, 3.78418963391826, 3.80666248977032, 3.8286413964891, 3.85014760171006, 3.87120101090789, 3.89182029811063, 3.91202300542815, 3.93182563272433, 3.95124371858143, 3.97029191355212, 3.98898404656427, 4.00733318523247, 4.02535169073515, 4.04305126783455, 4.06044301054642, 4.07753744390572, 4.0943445622221, 4.11087386417331, 4.12713438504509, 4.14313472639153, 4.15888308335967, 4.17438726989564, 4.18965474202643, 4.20469261939097, 4.21950770517611, 4.23410650459726, 4.24849524204936, 4.26267987704132, 4.27666611901606, 4.29045944114839, 4.30406509320417, 4.31748811353631, 4.33073334028633, 4.34380542185368, 4.35670882668959, 4.36944785246702, 4.38202663467388, 4.39444915467244, 4.40671924726425, 4.4188406077966, 4.43081679884331, 4.44265125649032, 4.45434729625351, 4.46590811865458, 4.47733681447821, 4.48863636973214, 4.49980967033027, 4.51085950651685, 4.52178857704904, 4.53259949315326, 4.54329478227, 4.55387689160054, 4.56434819146784, 4.57471097850338, 4.58496747867057, 4.59511985013459, 4.60517018598809, 4.61512051684126, 4.62497281328427, 4.63472898822964, 4.64439089914137, 4.65396035015752, 4.66343909411207, 4.67282883446191, 4.68213122712422, 4.69134788222914, 4.70048036579242)), .Names = c('log(x)', 'log(z)'), row.names = c(NA, 100L), terms = quote(~log(x) + log(z))));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass13() {
        assertEval("argv <- list(structure(list(Df = c(NA, 0L), Deviance = c(NA, 0), `Resid. Df` = c(10L, 10L), `Resid. Dev` = c(2.74035772634541, 2.74035772634541)), .Names = c('Df', 'Deviance', 'Resid. Df', 'Resid. Dev'), row.names = c('NULL', 'x'), heading = 'Analysis of Deviance Table\\n\\nModel: gaussian, link: identity\\n\\nResponse: y\\n\\nTerms added sequentially (first to last)\\n\\n'));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass14() {
        assertEval("argv <- list(list(structure(list(title = 'foreign: Read Data Stored by Minitab, S, SAS, SPSS, Stata, Systat, dBase,\\n...', author = structure(list(structure(list(given = 'R Core Team', family = NULL, role = c('aut', 'cph', 'cre'), email = 'R-core@R-project.org', comment = NULL), .Names = c('given', 'family', 'role', 'email', 'comment'))), class = 'person'), year = '2013', note = 'R package version 0.8-53', url = 'http://CRAN.R-project.org/package=foreign'), .Names = c('title', 'author', 'year', 'note', 'url'), bibtype = 'Manual', textVersion = 'R Core Team (2013). foreign: Read Data Stored by Minitab, S, SAS, SPSS, Stata, Systat, dBase,\\n.... R package version 0.8-53. http://CRAN.R-project.org/package=foreign', header = 'To cite package ‘foreign’ in publications use:')));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass15() {
        assertEval("argv <- list(structure(c(8.79236, 8.79137, 8.81486, 8.81301, 8.90751, 8.93673, 8.96161, 8.96044, 9.00868, 9.03049, 9.06906, 9.05871, 9.10698, 9.12685, 9.17096, 9.18665, 9.23823, 9.26487, 9.28436, 9.31378, 9.35025, 9.35835, 9.39767, 9.4215, 9.44223, 9.48721, 9.52374, 9.5398, 9.58123, 9.60048, 9.64496, 9.6439, 9.69405, 9.69958, 9.68683, 9.71774, 9.74924, 9.77536, 9.79424), .Tsp = c(1962.25, 1971.75, 4)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass16() {
        assertEval("argv <- list(structure(list(structure(9L, members = 1L, height = 0, label = 9L, leaf = TRUE, value = 2L), structure(list(structure(10L, label = 10L, members = 1L, height = 0, leaf = TRUE, value = 1L), structure(1L, label = 1L, members = 1L, height = 0, leaf = TRUE, value = 10L)), members = 2L, midpoint = 0.5, height = 0.114813676452255, value = 5.5)), members = 3L, midpoint = 0.75, height = 0.241190881793568, value = 3.75));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass17() {
        assertEval("argv <- list(c(-1.6, -0.9));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass18() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/base/R/base', '/home/lzhao/hg/r-instrumented/library/base/R/base'), frow = 5852:5853, lrow = c(5852L, 5854L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass19() {
        assertEval("argv <- list(structure(list(b = structure(c(3L, 1L, 2L), .Label = c('A', 'B', 'C'), class = 'factor'), a = structure(c(1386423981.90268, 1386403981.90268, 1386413981.90268), class = c('POSIXct', 'POSIXt'))), .Names = c('b', 'a'), row.names = c(3L, 1L, 2L)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass20() {
        assertEval("argv <- list(c(-2.20207097779183e-13, -2.19098062290287e-13, -2.17989026801391e-13, -2.16879991312495e-13, -2.15770955823599e-13, -2.14661920334703e-13, -2.13552884845807e-13, -2.12443849356911e-13, -2.11334813868015e-13, -2.10225778379119e-13, -2.09116742890223e-13, -2.08007707401327e-13, -2.06898671912432e-13, -2.05789636423536e-13, -2.0468060093464e-13, -2.03571565445744e-13, -2.02462529956848e-13, -2.01353494467952e-13, -2.00244458979056e-13, -1.9913542349016e-13, -1.98026388001264e-13, -1.96917352512368e-13, -1.95808317023472e-13, -1.94699281534576e-13, -1.93590246045681e-13, -1.92481210556785e-13, -1.91372175067889e-13, -1.90263139578993e-13, -1.89154104090097e-13, -1.88045068601201e-13, -1.86936033112305e-13, -1.85826997623409e-13, -1.84717962134513e-13, -1.83608926645617e-13, -1.82499891156721e-13, -1.81390855667826e-13, -1.8028182017893e-13, -1.79172784690034e-13, -1.78063749201138e-13, -1.76954713712242e-13, -1.75845678223346e-13, -1.7473664273445e-13, -1.73627607245554e-13, -1.72518571756658e-13, -1.71409536267762e-13, -1.70300500778866e-13, -1.6919146528997e-13, -1.68082429801075e-13, -1.66973394312179e-13, -1.65864358823283e-13, -1.64755323334387e-13, -1.63646287845491e-13, -1.62537252356595e-13, -1.61428216867699e-13, -1.60319181378803e-13, -1.59210145889907e-13, -1.58101110401011e-13, -1.56992074912115e-13, -1.5588303942322e-13, -1.54774003934324e-13, -1.53664968445428e-13, -1.52555932956532e-13, -1.51446897467636e-13, -1.5033786197874e-13, -1.49228826489844e-13, -1.48119791000948e-13, -1.47010755512052e-13, -1.45901720023156e-13, -1.4479268453426e-13, -1.43683649045365e-13, -1.42574613556469e-13, -1.41465578067573e-13, -1.40356542578677e-13, -1.39247507089781e-13, -1.38138471600885e-13, -1.37029436111989e-13, -1.35920400623093e-13, -1.34811365134197e-13, -1.33702329645301e-13, -1.32593294156405e-13, -1.3148425866751e-13, -1.30375223178614e-13, -1.29266187689718e-13, -1.28157152200822e-13, -1.27048116711926e-13, -1.2593908122303e-13, -1.24830045734134e-13, -1.23721010245238e-13, -1.22611974756342e-13, -1.21502939267446e-13, -1.2039390377855e-13, -1.19284868289654e-13, -1.18175832800759e-13, -1.17066797311863e-13, -1.15957761822967e-13, -1.14848726334071e-13, -1.13739690845175e-13, -1.12630655356279e-13, -1.11521619867383e-13, -1.10412584378487e-13, -1.09303548889591e-13, -1.08194513400695e-13, -1.07085477911799e-13, -1.05976442422904e-13, -1.04867406934008e-13, -1.03758371445112e-13, -1.02649335956216e-13, -1.0154030046732e-13, -1.00431264978424e-13, -9.9322229489528e-14, -9.82131940006321e-14, -9.71041585117362e-14, -9.59951230228403e-14, -9.48860875339444e-14, -9.37770520450484e-14, -9.26680165561525e-14, -9.15589810672566e-14, -9.04499455783607e-14, -8.93409100894648e-14, -8.82318746005689e-14, -8.7122839111673e-14, -8.60138036227771e-14, -8.49047681338812e-14, -8.37957326449853e-14, -8.26866971560894e-14, -8.15776616671935e-14, -8.04686261782975e-14, -7.93595906894016e-14, -7.82505552005057e-14, -7.71415197116098e-14, -7.60324842227139e-14, -7.49234487338179e-14, -7.3814413244922e-14, -7.27053777560261e-14, -7.15963422671302e-14, -7.04873067782343e-14, -6.93782712893384e-14, -6.82692358004425e-14, -6.71602003115466e-14, -6.60511648226507e-14, -6.49421293337547e-14, -6.38330938448588e-14, -6.27240583559629e-14, -6.1615022867067e-14, -6.05059873781711e-14, -5.93969518892752e-14, -5.82879164003793e-14, -5.71788809114834e-14, -5.60698454225874e-14, -5.49608099336915e-14, -5.38517744447956e-14, -5.27427389558997e-14, -5.16337034670038e-14, -5.05246679781079e-14, -4.9415632489212e-14, -4.83065970003161e-14, -4.71975615114202e-14, -4.60885260225244e-14, -4.49794905336287e-14, -4.38704550447331e-14, -4.27614195558379e-14, -4.16523840669435e-14, -4.05433485780505e-14, -3.94343130891604e-14, -3.83252776002761e-14, -3.72162421114035e-14, -3.61072066225542e-14, -3.49981711337514e-14, -3.38891356450417e-14, -3.27801001565183e-14, -3.16710646683675e-14, -3.05620291809617e-14, -2.9452993695046e-14, -2.83439582121106e-14, -2.72349227351356e-14, -2.61258872700815e-14, -2.50168518288693e-14, -2.39078164353409e-14, -2.27987811371798e-14, -2.16897460297536e-14, -2.05807113037972e-14, -1.94716773407802e-14, -1.83626449036421e-14, -1.72536155182618e-14, -1.61445922363971e-14, -1.50355811615637e-14, -1.39265945007928e-14, -1.28176566681469e-14, -1.1708816491751e-14, -1.0600171627855e-14, -9.49191738895913e-15));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass21() {
        assertEval("argv <- list(structure(c(325, 285, 706, 885), .Dim = c(1L, 4L), row.vars = structure(list(), .Names = character(0)), col.vars = structure(list(Class = c('1st', '2nd', '3rd', 'Crew')), .Names = 'Class')));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass22() {
        assertEval("argv <- list(c(10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 1e+05, 110000, 120000, 130000, 140000, 150000, 160000, 170000, 180000, 190000, 2e+05, 210000, 220000, 230000, 240000, 250000, 260000, 270000, 280000, 290000, 3e+05, 310000, 320000, 330000, 340000, 350000, 360000, 370000, 380000, 390000, 4e+05, 410000, 420000, 430000, 440000, 450000, 460000, 470000, 480000, 490000, 5e+05, 510000, 520000, 530000, 540000, 550000, 560000, 570000, 580000, 590000, 6e+05, 610000, 620000, 630000, 640000, 650000, 660000, 670000, 680000, 690000, 7e+05, 710000, 720000, 730000, 740000, 750000, 760000, 770000, 780000, 790000, 8e+05, 810000, 820000, 830000, 840000, 850000, 860000, 870000, 880000, 890000, 9e+05, 910000, 920000, 930000, 940000, 950000, 960000, 970000, 980000, 990000, 1e+06));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass23() {
        assertEval("argv <- list(quote(y ~ a + b:c + d + e + e:d));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass24() {
        assertEval("argv <- list(structure(c('Min.   :14.00  ', '1st Qu.:26.00  ', 'Median :29.50  ', 'Mean   :36.39  ', '3rd Qu.:49.25  ', 'Max.   :70.00  ', 'A:9  ', 'B:9  ', NA, NA, NA, NA), .Dim = c(6L, 2L), .Dimnames = list(c('', '', '', '', '', ''), c('    breaks', 'wool'))));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass25() {
        // GnuR does not retain row.names vector and substitutes it with 1L while FastR keeps c(NA,
        // -1L)
        assertEval(Ignored.ReferenceError,
                        "argv <- list(structure(list(srcfile = '/home/lzhao/tmp/RtmpYl9n1I/R.INSTALL2aa24b6697e5/MASS/R/negbin.R', frow = 135L, lrow = 137L), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, -1L)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass26() {
        assertEval("argv <- list(structure(list(a = 1), .Dim = 1L, .Dimnames = list('a')));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass27() {
        assertEval("argv <- list(list(structure(list(label = 'FALSE', x = structure(0, unit = 'npc', valid.unit = 0L, class = 'r_unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'r_unit'), just = c('left', 'centre'), hjust = NULL, vjust = NULL, rot = 0, check.overlap = FALSE, name = 'GRID.text.106', gp = structure(list(), class = 'r_gpar'), vp = NULL), .Names = c('label', 'x', 'y', 'just', 'hjust', 'vjust', 'rot', 'check.overlap', 'name', 'gp', 'vp'), class = c('r_text', 'r_grob', 'r_gDesc'))));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass28() {
        assertEval("argv <- list(structure(c('0', 'list', 'list'), .Names = c('Length', 'Class', 'Mode')));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass29() {
        assertEval("argv <- list(structure(list(surname = structure('R Core', class = 'AsIs'), nationality = structure(NA_integer_, .Label = c('Australia', 'UK', 'US'), class = 'factor'), deceased = structure(NA_integer_, .Label = c('no', 'yes'), class = 'factor')), .Names = c('surname', 'nationality', 'deceased'), row.names = 7L));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass31() {
        // GnuR does not retain row.names vector and substitutes it with 1L:10L while FastR keeps
        // c(NA, -10L)
        assertEval(Ignored.ReferenceError, "argv <- list(structure(list(), .Names = character(0), row.names = c(NA, -10L), terms = quote(~0)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass32() {
        assertEval("argv <- list(quote(breaks ~ (wool + tension)^2));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass33() {
        assertEval("argv <- list(structure(c(1L, 2L, 1L), .Dim = 3L, .Dimnames = structure(list(c('1', '2', NA)), .Names = '')));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass34() {
        // GnuR does not retain row.names vector and substitutes it with 1L:24L while FastR keeps
        // c(NA, 24L)
        assertEval(Ignored.ReferenceError,
                        "argv <- list(structure(list(`cbind(A, B, C, D)` = structure(c(0.696706709347165, 0.362357754476673, -0.0291995223012888, 0.696706709347165, 0.696706709347165, -0.0291995223012888, 0.696706709347165, -0.0291995223012888, 0.362357754476673, 0.696706709347165, -0.0291995223012888, 0.362357754476673, -0.416146836547142, 0.362357754476673, 0.696706709347165, 0.696706709347165, 0.362357754476673, -0.416146836547142, -0.0291995223012888, -0.416146836547142, 0.696706709347165, -0.416146836547142, 0.362357754476673, -0.0291995223012888, 0.717356090899523, 0.932039085967226, 0.999573603041505, 0.717356090899523, 0.717356090899523, 0.999573603041505, 0.717356090899523, 0.999573603041505, 0.932039085967226, 0.717356090899523, 0.999573603041505, 0.932039085967226, 0.909297426825682, 0.932039085967226, 0.717356090899523, 0.717356090899523, 0.932039085967226, 0.909297426825682, 0.999573603041505, 0.909297426825682, 0.717356090899523, 0.909297426825682, 0.932039085967226, 0.999573603041505, -0.0291995223012888, -0.737393715541246, -0.998294775794753, -0.0291995223012888, -0.0291995223012888, -0.998294775794753, -0.0291995223012888, -0.998294775794753, -0.737393715541246, -0.0291995223012888, -0.998294775794753, -0.737393715541246, -0.653643620863612, -0.737393715541246, -0.0291995223012888, -0.0291995223012888, -0.737393715541246, -0.653643620863612, -0.998294775794753, -0.653643620863612, -0.0291995223012888, -0.653643620863612, -0.737393715541246, -0.998294775794753, 0.999573603041505, 0.67546318055115, -0.0583741434275801, 0.999573603041505, 0.999573603041505, -0.0583741434275801, 0.999573603041505, -0.0583741434275801, 0.67546318055115, 0.999573603041505, -0.0583741434275801, 0.67546318055115, -0.756802495307928, 0.67546318055115, 0.999573603041505, 0.999573603041505, 0.67546318055115, -0.756802495307928, -0.0583741434275801, -0.756802495307928, 0.999573603041505, -0.756802495307928, 0.67546318055115, -0.0583741434275801), .Dim = c(24L, 4L), .Dimnames = list(NULL, c('A', 'B', 'C', 'D'))), groups = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L), .Label = c('1', '2', '3'), class = 'factor')), .Names = c('cbind(A, B, C, D)', 'groups'), terms = quote(cbind(A, B, C, D) ~ groups), row.names = c(NA, 24L)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass35() {
        assertEval("argv <- list(structure(list(group = structure(c(1L, 1L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = 'group', row.names = 1:2, terms = quote(~group)));unclass(argv[[1]]);");
    }

    @Test
    public void testunclass36() {
        assertEval("argv <- list(structure(c(2671, 6.026e+77, 3.161e+152, 3.501e+299, 2.409e+227, 1.529e+302), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.')));unclass(argv[[1]]);");
    }

    @Test
    public void testOther() {
        assertEval("{ setClass(\"foo\", representation(j=\"numeric\")); x<-new(\"foo\", j=42); unclass(x) }");
        // before S4 objects became shareable, the test below was merging two class representations
        assertEval("{ setClass(\"foo\", representation(j=\"numeric\")); setClass(\"foo\", representation(d=\"numeric\")); x<-new(\"foo\", d=42); unclass(x) }");

    }
}
