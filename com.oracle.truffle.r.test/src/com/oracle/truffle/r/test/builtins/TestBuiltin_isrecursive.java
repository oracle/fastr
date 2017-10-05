/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_isrecursive extends TestBase {

    @Test
    public void testisrecursive1() {
        assertEval("argv <- list(c(1, 5.75, 10.5, 15.25, 20));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive2() {
        assertEval("argv <- list(structure(c(1+0i, 5+0i, 9+0i, 13+0i, 17+0i, 21+0i, 2+0i, 6+0i, 10+0i, 14+0i, 18+0i, 22+0i, 3+0i, 7+0i, 11+0i, 15+0i, 19+0i, 23+0i, 4+0i, 8+0i, 12+0i, 16+0i, 20+0i, 24+0i), .Dim = c(6L, 4L)));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive3() {
        assertEval("argv <- list(structure(list(var = structure(c(2L, 5L, 1L, 1L, 4L, 5L, 5L, 1L, 3L, 1L, 1L, 2L, 1L, 6L, 1L, 1L, 1L), .Label = c('<leaf>', 'hs.grad', 'illiteracy', 'murder', 'population', 'region'), class = 'factor'), n = c(50L, 10L, 7L, 3L, 40L, 32L, 13L, 3L, 10L, 6L, 4L, 19L, 4L, 15L, 11L, 4L, 8L), wt = c(50, 10, 7, 3, 40, 32, 13, 3, 10, 6, 4, 19, 4, 15, 11, 4, 8), dev = c(18501092, 668812.4, 269049.714285714, 40824, 9908099.1, 5718045.5, 2592172, 97824.6666666667, 1337366.4, 249796.833333333, 155810.75, 2105534.63157895, 276864.75, 1264031.6, 209138.181818182, 224618.75, 2526229.5), yval = c(4435.8, 3639.6, 3515.57142857143, 3929, 4634.85, 4532.875, 4317, 3772.33333333333, 4480.4, 4231.16666666667, 4854.25, 4680.57894736842, 4346.75, 4769.6, 4627.72727272727, 5159.75, 5042.75), complexity = c(0.42830879928601, 0.0194009459395308, 0.01, 0.01, 0.089931129470628, 0.0588430078006851, 0.0588430078006851, 0.01, 0.050362368700543, 0.01, 0.01, 0.0376981247853036, 0.01, 0.0376981247853036, 0.00682341562063013, 0.01, 0.01), ncompete = c(4L, 3L, 0L, 0L, 4L, 4L, 4L, 0L, 4L, 0L, 0L, 4L, 0L, 4L, 0L, 0L, 0L), nsurrogate = c(3L, 0L, 0L, 0L, 2L, 4L, 0L, 0L, 4L, 0L, 0L, 2L, 0L, 3L, 0L, 0L, 0L)), .Names = c('var', 'n', 'wt', 'dev', 'yval', 'complexity', 'ncompete', 'nsurrogate'), row.names = c(1L, 2L, 4L, 5L, 3L, 6L, 12L, 24L, 25L, 50L, 51L, 13L, 26L, 27L, 54L, 55L, 7L), class = 'data.frame'));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive4() {
        assertEval("argv <- list(structure(c(0, 0.693147180559945, 1.09861228866811, 1.38629436111989, 1.6094379124341, 1.79175946922805, 1.94591014905531, 2.07944154167984, 2.19722457733622, 2.30258509299405, 2.39789527279837, 2.484906649788, 2.56494935746154, 2.63905732961526, 2.70805020110221, 2.77258872223978, 2.83321334405622, 2.89037175789616, 2.94443897916644, 2.99573227355399, 3.04452243772342, 3.09104245335832, 3.13549421592915, 3.17805383034795, 3.2188758248682, 3.25809653802148, 3.29583686600433, 3.3322045101752, 3.36729582998647, 3.40119738166216, 3.43398720448515, 3.46573590279973, 3.49650756146648, 3.52636052461616, 3.55534806148941, 3.58351893845611, 3.61091791264422, 3.63758615972639, 3.66356164612965, 3.68887945411394, 3.71357206670431, 3.73766961828337, 3.76120011569356, 3.78418963391826, 3.80666248977032, 3.8286413964891, 3.85014760171006, 3.87120101090789, 3.89182029811063, 3.91202300542815, 3.93182563272433, 3.95124371858143, 3.97029191355212, 3.98898404656427, 4.00733318523247, 4.02535169073515, 4.04305126783455, 4.06044301054642, 4.07753744390572, 4.0943445622221, 4.11087386417331, 4.12713438504509, 4.14313472639153, 4.15888308335967, 4.17438726989564, 4.18965474202643, 4.20469261939097, 4.21950770517611, 4.23410650459726, 4.24849524204936, 4.26267987704132, 4.27666611901606, 4.29045944114839, 4.30406509320417, 4.31748811353631, 4.33073334028633, 4.34380542185368, 4.35670882668959, 4.36944785246702, 4.38202663467388, 4.39444915467244, 4.40671924726425, 4.4188406077966, 4.43081679884331, 4.44265125649032, 4.45434729625351, 4.46590811865458, 4.47733681447821, 4.48863636973214, 4.49980967033027, 4.51085950651685, 4.52178857704904, 4.53259949315326, 4.54329478227, 4.55387689160054, 4.56434819146784, 4.57471097850338, 4.58496747867057, 4.59511985013459, 4.60517018598809), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93', '94', '95', '96', '97', '98', '99', '100')));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive5() {
        assertEval("argv <- list(c(4.2946094590101+0i, -3.25699475013756-2.24807847868298i, -7.95013358893225-2.01799537233252i, -3.67333331331688+0.2922506370947i, 10.1942090590333+0.2633318847587i, -3.52243716497356+3.01559327870726i, 0.840489557961749+0.760755891710788i, 0.34614657901946+3.92303947429563i, 1.25203951718932-0.04113309513059i, 1.25203951718932+0.04113309513059i, 0.34614657901946-3.92303947429563i, 0.840489557961749-0.760755891710788i, -3.52243716497356-3.01559327870726i, 10.1942090590333-0.2633318847587i, -3.67333331331688-0.2922506370947i, -7.95013358893225+2.01799537233252i, -3.25699475013756+2.24807847868298i));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive6() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Names = c('age', 'eet', 'g2', 'grade', 'gleason', 'ploidy')));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive7() {
        assertEval("argv <- list(structure(list(var = structure(c(2L, 1L, 1L), .Label = c('<leaf>', 'Start'), class = 'factor'), n = c(81L, 62L, 19L), wt = c(81, 62, 19), dev = c(24.3, 8.57647058823529, 14.175), yval = c(1, 1, 2), complexity = c(0.0637254901960785, 0, 1e-15), ncompete = c(2L, 0L, 0L), nsurrogate = c(0L, 0L, 0L), yval2 = structure(c(1, 1, 2, 64, 56, 8, 17, 6, 11, 0.7, 0.852610030706244, 0.310704960835509, 0.3, 0.147389969293756, 0.689295039164491, 0.999999999999999, 0.718382352941176, 0.281617647058824), .Dim = c(3L, 6L), .Dimnames = list(NULL, c('', '', '', '', '', 'nodeprob')))), .Names = c('var', 'n', 'wt', 'dev', 'yval', 'complexity', 'ncompete', 'nsurrogate', 'yval2'), row.names = c(NA, 3L), class = 'data.frame'));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive8() {
        assertEval("argv <- list(1e+09);is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive9() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive10() {
        assertEval("argv <- list(logical(0));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive11() {
        assertEval("argv <- list(structure(list(Df = c(1L, 7L), `Sum Sq` = c(158.407612694902, 204.202165082876), `Mean Sq` = c(158.407612694902, 29.1717378689823), `F value` = c(5.43017400630538, NA), `Pr(>F)` = c(0.052592726218915, NA)), .Names = c('Df', 'Sum Sq', 'Mean Sq', 'F value', 'Pr(>F)'), row.names = c('depression', 'Residuals'), class = c('anova', 'data.frame'), heading = c('Analysis of Variance Table\\n', 'Response: weight')));is.recursive(argv[[1]]);");
    }

    @Test
    public void testisrecursive13() {
        assertEval("argv <- list(expression(quote(expression(4, 1.12837916709551))));do.call('is.recursive', argv)");
    }

    @Test
    public void testIsRecursive() {
        assertEval("{ is.recursive(1) }");
        assertEval("{ is.recursive(1L) }");
        assertEval("{ is.recursive(1:3) }");
        assertEval("{ is.recursive(c(1,2,3)) }");
        assertEval("{ is.recursive(NA) }");
        assertEval("{ is.recursive(NULL) }");
        assertEval("{ is.recursive(TRUE) }");
        assertEval("{ !is.recursive(list()) }");
        assertEval("{ !is.recursive(function() {}) }");
        assertEval("{ !is.recursive(tools:::C_parseRd$address) }");
    }
}
