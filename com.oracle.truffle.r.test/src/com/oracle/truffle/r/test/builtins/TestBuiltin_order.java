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
public class TestBuiltin_order extends TestBase {

    @Test
    public void testorder1() {
        assertEval("argv <- list(TRUE, FALSE, structure(c(1, 1, 1, 2, 2, 2, 3, 4), .Names = c('CsparseMatrix', 'nsparseMatrix', 'generalMatrix', 'nMatrix', 'sparseMatrix', 'compMatrix', 'Matrix', 'mMatrix'))); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder2() {
        assertEval("argv <- list(TRUE, FALSE, structure(c(1, 2, 2.1, 2.3, 2.3, 3, 4, 5, 7, 8, 11, 13, 14, 15), .Names = c('\\\\title', '\\\\name', '\\\\alias', '\\\\keyword', '\\\\keyword', '\\\\description', '\\\\usage', '\\\\arguments', '\\\\details', '\\\\value', '\\\\author', '\\\\references', '\\\\seealso', '\\\\examples')), c('', '', 'LOGLIN', '', '', '', '', '', '', '', '', '', '', ''), c('', '', 'loglin', '', '', '', '', '', '', '', '', '', '', '')); .Internal(order(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
    }

    @Test
    public void testorder3() {
        assertEval("argv <- list(TRUE, FALSE, c(NA, 'Ripley', 'Venables & Smith')); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder4() {
        assertEval("argv <- list(TRUE, TRUE, structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 4L, 2L, 1L, 1L, 1L, 2L, 2L, 2L, 1L, 1L, 1L, 2L, 1L, 3L, 4L, 2L, 5L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 3L, 1L, 2L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 1L, 1L, 1L, 1L, 2L, 1L, 1L, 1L, 2L, 1L, 2L, 1L, 1L, 2L, 3L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 1L, 2L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 1L, 2L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 1L, 1L), .Names = c('1008', '1011', '1013', '1014', '1015', '1016', '1027', '1028', '1030', '1032', '1051', '1052', '1083', '1093', '1095', '1096', '110', '1102', '111', '1117', '112', '113', '116', '117', '1219', '125', '1250', '1251', '126', '127', '128', '1291', '1292', '1293', '1298', '1299', '130', '1308', '135', '1376', '1377', '1383', '1408', '1409', '141', '1410', '1411', '1413', '1418', '1422', '1438', '1445', '1456', '1492', '2001', '2316', '262', '266', '269', '270', '2708', '2714', '2715', '272', '2728', '2734', '280', '283', '286', '290', '3501', '411', '412', '475', '5028', '5042', '5043', '5044', '5045', '5047', '5049', '5050', '5051', '5052', '5053', '5054', '5055', '5056', '5057', '5058', '5059', '5060', '5061', '5062', '5066', '5067', '5068', '5069', '5070', '5072', '5073', '5115', '5160', '5165', '655', '724', '885', '931', '942', '952', '955', '958', 'c118', 'c168', 'c203', 'c204', 'c266'))); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder5() {
        assertEval("argv <- list(TRUE, FALSE); .Internal(order(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testorder6() {
        assertEval("argv <- list(TRUE, FALSE, c(25, 50, 100, 250, 500, 1e+05)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder7() {
        assertEval("argv <- list(TRUE, FALSE, c(1L, 2L, NA)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder8() {
        assertEval("argv <- list(TRUE, FALSE, c(-1.90479340955971, 0.152878714793717)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder9() {
        assertEval("argv <- list(TRUE, FALSE, structure(numeric(0), .Dim = c(0L, 0L), .Dimnames = list(NULL, NULL))); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder10() {
        assertEval("argv <- list(TRUE, FALSE, c(1L, 1L), c(5L, 5L)); .Internal(order(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testorder11() {
        assertEval("argv <- list(TRUE, FALSE, structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75')), structure(c(-0.57247143337844, -0.539950178656569, -0.58297623976228, -0.439751638938155, -0.617930515617237, -0.533331856784678, -0.289352731546361, -0.478093191632667, -0.602269400145547, -0.580175322433967, -0.49540432777895, -0.513696058484476, -0.458241390525053, -0.626325772726431, -0.638174891061199, -0.617196984678001, -0.64409821087924, -0.612729425695224, -0.60380450668859, -0.478765912376981, -0.577566470585813, -0.611603489866172, -0.630689749726753, -0.617142933166765, -0.649085924500709, -0.663752033681209, -0.644167702381979, -0.638179039688751, -0.597653338464853, -0.60376057262344, -0.663213279051883, -0.675292341303314, -0.666589501747572, -0.670209631751109, -0.683375351812861, -0.683923564367218, -0.679841016050066, -0.687830656102281, -0.686865903442208, -0.681095489258746, -0.579001929374462, -0.669393058957547, -0.678452540432172, -0.638743740817659, -0.558515347237012, -0.337270659711893, -0.279950203607686, -0.295246094585692, -0.592252570503069, -0.558321756708791, -0.597079745476187, -0.573971559450555, -0.603793132961681, -0.544974758961613, -0.495274888248239, -0.488092985753192, -0.528409363716152, -0.552865045250698, -0.502907194303865, -0.482819909399495, -0.590008262166764, -0.582409343486053, -0.548676506410172, -0.642096376280899, -0.622604552864479, -0.581608072840875, -0.637160558239849, -0.640205884259342, -0.643944208731097, -0.627870005742383, -0.638070667609366, -0.648245104552262, -0.582808968033345, -0.593416716949551, -0.631441868159251), .Names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75'))); .Internal(order(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testorder12() {
        assertEval("argv <- list(TRUE, FALSE, c(FALSE, FALSE)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder13() {
        assertEval("argv <- list(TRUE, FALSE, structure(c(-0.00544194018731062, -0.00542949133552226, -1.20718999105839e-05, -0.00505497198006266, -0.827687885653788, -0.00315385274195005, -0.0023164952286401, -0.00117183915211372, -2.09167441982205, -0.00193959227691399, -0.00358084102808485, -3.39138861812986e-05, -0.00163051710052444, -0.00168735925488057, -0.0167253073891896, -0.237074502262169, -0.0118967636015583, -0.00307437031103621, -0.00114371252369823, -0.000860763872820255, -0.00028432076263802, -0.00329557354736053, -0.000123683950933913, -0.00026114238659798, -0.00471892942651347, -0.00317288091968884, -6.76955217513137e-05, -0.0119061189538054, -0.00233356124758579, -0.00672098496026968, -0.134965372025281, -0.00102115420103838, -0.00114816901125044), .Names = c('Craig Dunain', 'Ben Rha', 'Ben Lomond', 'Goatfell', 'Bens of Jura', 'Cairnpapple', 'Scolty', 'Traprain', 'Lairig Ghru', 'Dollar', 'Lomonds', 'Cairn Table', 'Eildon Two', 'Cairngorm', 'Seven Hills', 'Knock Hill', 'Black Hill', 'Creag Beag', 'Kildcon Hill', 'Meall Ant-Suidhe', 'Half Ben Nevis', 'Cow Hill', 'N Berwick Law', 'Creag Dubh', 'Burnswark', 'Largo Law', 'Criffel', 'Acmony', 'Ben Nevis', 'Knockfarrel', 'Two Breweries', 'Cockleroi', 'Moffat Chase'))); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder14() {
        assertEval("argv <- list(TRUE, FALSE, c(2L, 1L, NA)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder15() {
        assertEval("argv <- list(TRUE, FALSE, structure(c(1, 2, 2, 2, 2), .Names = c('character', 'vector', 'data.frameRowLabels', 'SuperClassMethod', 'atomicVector'))); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder16() {
        assertEval("argv <- list(TRUE, FALSE, c(1, 2, NA)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder17() {
        assertEval("argv <- list(TRUE, FALSE, c(39.39, NA, 60.99, 45.82, 55.4, 59.49, 63.73, 55.09, 57.2, 33.22, 61.18, 59.52, 59.9, 59.06, 65.44, 54.3, 53.28, 70.87, 60.04, 59.33, 55.63, 53.68, 24.46, 49.87, 57.13, 65.51, 57.97, 66.11, 64.68, 57.81, 61.2, 49.34, 61.72, 61.11, 55.13, 55.25, 56.49, 58.31, 25.18, 58.39, 49.21, 55.27, 64.56, 72.18, 55.37, 53.91, 54.08, 61.58)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder18() {
        assertEval("argv <- list(TRUE, FALSE, c(FALSE, TRUE)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder19() {
        assertEval("argv <- list(TRUE, TRUE, c(2572.90550008339, 915.064609071159, 419.980933101553, 456.632018115023, 366.745362912885, 308.301779528581, 258.104614655539, 166.131403923756, 208.43876984087, 174.152165416129, 157.072337453686, 157.072337453686, 93.6287479850051, 145.261798316303, 140.969649074553, 80.9227484547009, 115.221543203043, 115.221543203043, 43.2054741656531, 92.6549620292801, 92.6549620292801, 90.9360919542674, 90.9360919542674, 95.4200045428049, 94.2845517186135, 90.4677652619726, 76.0356310934324, 76.0356310934324, 26.1565478253913, 79.2808369338756, 69.8160310537133, 69.8160310537133, 74.7131802385517, 72.7892236613541, 20.782836979896, 60.876337906218, 60.876337906218, 70.8748882290923, 66.7853490283529, 66.7853490283529, 16.5058357149619, 54.5274774202162, 54.5274774202162, 64.009437139127, 13.2377600042936, 49.2149340764437, 49.2149340764437, 60.372612826631, 55.4228623592615, 55.4228623592615, 55.598407412763, 8.99100195370794, 49.6125680940682, 49.6125680940682, 51.620171425175, 39.8798475138868, 39.8798475138868, 7.62805946796798, 35.458438379179, 35.458438379179, 38.2201699179466, 38.2201699179466, 29.6856293315566, 29.6856293315566, 34.3600931672689, 34.3600931672689, 47.6686025497685, 47.0350049752776, 5.57838460483725, 5.07382264677001, 15.9316203363047, 23.4957538578271, 23.4957538578271, 41.4311176038551, 11.9119569568831, 34.7321001383969, 4.21976444063592, 39.2901755793811, 29.1992830783774, 29.1992830783774, 11.1832867499603, 42.0546965543942, 27.0745572919711, 7.44159556589097, 28.1216021055426, 6.46699019595805, 2.05951624519943, 4.77172378340461, 38.0398197891428, 1.66021670517454, 1.03505989993491, 2.69814683135512, 1.8306332549612, 2.29820560404041, 3.42336057523814e-06, 3.42336062193255e-06, 2.86123099075901, 0.773887754953459, 0.213086170361661, 0.416100454072758)); .Internal(order(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testorder21() {
        assertEval("argv <- structure(list(1, 1, 1, na.last = NA), .Names = c('',     '', '', 'na.last'));do.call('order', argv)");
    }

    @Test
    public void testorder22() {
        assertEval("argv <- structure(list(1, na.last = NA), .Names = c('', 'na.last'));do.call('order', argv)");
    }

    @Test
    public void testOrder() {
        assertEval("{ order(c(\"a\",\"c\",\"b\",\"d\",\"e\",\"f\")) }");
        assertEval("{ order(c(5,2,2,1,7,4)) }");
        assertEval("{ order(c(5,2,2,1,7,4),c(\"a\",\"c\",\"b\",\"d\",\"e\",\"f\")) }");
        assertEval("{ order(c(1,1,1,1),c(4,3,2,1)) }");
        assertEval("{ order(c(1,1,1,1),c(\"d\",\"c\",\"b\",\"a\")) }");
        assertEval("{ order(c(1i,2i,3i)) }");
        assertEval("{ order(c(3i,1i,2i)) }");
        assertEval("{ order(c(3+1i,2+2i,1+3i)) }");
        assertEval("{ order(c(3+1i,2+3i,2+2i,1+3i)) }");

        assertEval("{ order(7) }");
        assertEval("{ order(FALSE) }");
        assertEval("{ order(character()) }");

        assertEval("{ order(1:3) }");
        assertEval("{ order(3:1) }");
        assertEval("{ order(c(1,1,1), 3:1) }");
        assertEval("{ order(c(1,1,1), 3:1, decreasing=FALSE) }");
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=TRUE) }");
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=NA) }");
        assertEval("{ order(c(1,1,1), 3:1, decreasing=TRUE, na.last=FALSE) }");
        assertEval("{ order() }");
        assertEval("{ order(c(NA,NA,1), c(2,1,3)) }"); // infinite loop
        assertEval("{ order(c(NA,NA,1), c(1,2,3)) }"); // infinite loop
        assertEval("{ order(c(1,2,3,NA)) }"); // infinite loop
        assertEval("{ order(c(1,2,3,NA), na.last=FALSE) }");
        assertEval("{ order(c(1,2,3,NA), na.last=FALSE, decreasing=TRUE) }");
        assertEval("{ order(c(0/0, -1/0, 2)) }");
        assertEval("{ order(c(TRUE, FALSE)) }");

        assertEval("{ x<-c(40, 40,  1, 40,  1, 20, 40, 10, 40, 10, 16, 40, 10, 26, 40, 10, 39, 40, 11, 40, 12, 40, 12, 20); order(x, decreasing=TRUE) }");
        assertEval("{ x<-c(40, 40,  1, 40,  1, 20, 40, 10, 40, 10, 16, 40, 10, 26, 40, 10, 39, 40, 11, 40, 12, 40, 12, 20); order(x, decreasing=FALSE) }");

        assertEval("{ order(c(-1480,  -974, -1576,  -970), c(\"a\", \"b\", \"c\", \"d\")) }");

        assertEval("{ order(c(0/0, -1/0, 2), na.last=NA) }");

        assertEval("order(c('40 50', '405', '40 51', '4028', '40 20', '40 30', '404'))");

        assertEval("order(c(1,2,0), decreasing=NA)");

        assertEval("invisible(Sys.setlocale('LC_COLLATE', 'EN_us')); str(as.data.frame(list(a=c('A wo','Far ','abc ')))); invisible(Sys.setlocale('LC_COLLATE', 'C')); str(as.data.frame(list(a=c('A wo','Far ','abc '))));");
    }
}
