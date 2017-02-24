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
public class TestBuiltin_aperm extends TestBase {

    @Test
    public void testaperm1() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(5L, 14L), .Dimnames = list(c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render'), NULL)), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm2() {
        assertEval("argv <- list(structure(c('[', 'as.data.frame', 'plot', 'print', 'summary', 'as.character', 'print', 'print', 'plot', 'update', 'dim', 'dimnames', 'dimnames<-', '[', 't', 'summary', 'print', 'barchart', 'barchart', 'barchart', 'barchart', 'barchart', 'barchart', 'bwplot', 'bwplot', 'densityplot', 'densityplot', 'dotplot', 'dotplot', 'dotplot', 'dotplot', 'dotplot', 'dotplot', 'histogram', 'histogram', 'histogram', 'qqmath', 'qqmath', 'stripplot', 'stripplot', 'qq', 'xyplot', 'xyplot', 'levelplot', 'levelplot', 'levelplot', 'levelplot', 'contourplot', 'contourplot', 'contourplot', 'contourplot', 'cloud', 'cloud', 'cloud', 'wireframe', 'wireframe', 'splom', 'splom', 'splom', 'parallelplot', 'parallelplot', 'parallelplot', 'parallel', 'parallel', 'parallel', 'tmd', 'tmd', 'llines', 'ltext', 'lpoints', 'shingle', 'shingle', 'shingle', 'shingle', 'shingle', 'shingleLevel', 'shingleLevel', 'trellis', 'trellis', 'trellis', 'trellis', 'trellis', 'trellis', 'trellis', 'trellis', 'trellis', 'summary.trellis', 'formula', 'array', 'default', 'matrix', 'numeric', 'table', 'formula', 'numeric', 'formula', 'numeric', 'formula', 'array', 'default', 'matrix', 'numeric', 'table', 'formula', 'factor', 'numeric', 'formula', 'numeric', 'formula', 'numeric', 'formula', 'formula', 'ts', 'formula', 'table', 'array', 'matrix', 'formula', 'table', 'array', 'matrix', 'formula', 'matrix', 'table', 'formula', 'matrix', 'formula', 'matrix', 'data.frame', 'formula', 'matrix', 'data.frame', 'formula', 'matrix', 'data.frame', 'formula', 'trellis', 'default', 'default', 'default', NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), .Dim = c(70L, 3L)), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm3() {
        assertEval("argv <- list(structure(c(0.666666666666667, 0.666666666666667, 0.666666666666667, 0.666666666666667, 0.666666666666667, 0.666666666666667, 0.666666666666667, 0.666666666666667, 0.666666666666667), .Dim = c(1L, 9L)), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm4() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0L, 0L, 0L, 1L, 0L, 1L, 1L, 1L), .Dim = c(1L, 8L), .Dimnames = list('strata(enum)', c('rx', 'size', 'number', 'strata(enum)', 'cluster(id)', 'rx:strata(enum)', 'size:strata(enum)', 'number:strata(enum)'))), 1:2, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm5() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(9.2319289524956, -0.470372045488369, 186.857050189827), .Dim = c(1L, 3L), .Dimnames = list('118', c('age', 'sex', 'meal.cal'))), 1:2, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm6() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(414L, 439L, 382L, 388L, 383L, 364L, 364L, 363L, 349L, 371L, 102L, 388L, 388L, 363L, 367L, 365L, 382L, 362L, 373L, 385L, 376L, 360L, 306L, 160L, 316L, 316L, 315L, 357L, 360L, 347L, 353L, 308L, 327L, 329L, 343L, 251L, 318L, 304L, 316L, 335L, 365L, 336L, 350L, 356L, 339L, 301L, 330L, 300L, 300L, 312L, 334L, 270L, 347L, 293L, 303L, 337L, 287L, 293L, 293L, 318L, 359L, 351L, 322L, 343L, 269L, 286L, 286L, 273L, 297L, 273L, 273L, 273L, 294L, 303L, 281L, 273L, 255L, 269L, 270L, 270L, 276L, 264L, 245L, 261L, 270L, 273L, 306L, 274L, 279L, 278L, 278L, 284L, 276L, 265L, 294L, 277L, 259L, 287L, 263L, 240L, 217L, 271L, 252L, 331L, 255L, 271L, 254L, 185L, 213L, 210L, 203L, 288L, 269L, 269L, 91L, 91L, 192L, 199L, 195L, 198L, 207L, 200L, 197L, 243L, 203L, 197L, 227L, 227L, 219L, 8L, NA, NA, 246L, NA, 292L, NA, 294L, NA, 19L, 373L, NA, 211L, 82L, NA, 334L, 18L, NA, 280L, NA, NA, NA, NA, 146L, NA, NA, NA, 267L, 206L, 175L, NA, NA, NA, NA, 118L, NA, NA, NA, NA, 274L, NA, NA, 187L, NA, 6L, NA, NA, 146L, 304L, NA, 52L, 67L, NA, 265L, NA, 91L, NA, NA, NA, 318L, 57L, 226L, 65L, NA, 264L, NA, NA, NA, 236L, NA, 207L, NA, NA, NA, NA, NA, NA, 23L, NA, NA, NA, NA, NA, NA, 113L, 99L, NA, NA, 14L, NA, NA, NA, NA, NA, NA, NA, 4L, NA, 167L, NA, NA, NA, NA, NA, NA, NA, NA, NA, 165L, NA, NA, NA, NA, NA, NA, NA, NA, 11L, NA, NA, 168L, NA, NA, 120L, NA, 104L, NA, 373L, 26L, NA, NA, 253L, NA, NA, NA, NA, NA, NA, NA, NA, 260L, 114L, NA, 370L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 280L, NA, NA, NA, NA, 240L, NA, NA, NA, NA, 361L, NA, NA, NA, NA, NA, NA, NA, 188L, NA, NA, 65L, 248L, NA, NA, NA, 121L, NA, NA, NA, NA, 121L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 306L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 159L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 22L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 152L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 265L, 337L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 255L, 250L, NA, NA, NA, 203L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 213L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 169L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 241L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 269L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 284L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 249L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 307L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 322L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, 350L, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), .Dim = c(128L, 8L), .Dimnames = list(    NULL, c('futime', 'e1', 'e2', 'e3', 'e4', 'e5', 'e6', 'e7'))), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm7() {
        assertEval("argv <- list(structure(c(0, -10, 0, -10, -10, 0, NA, NA, 0, 0, 0, 0, 0, 150, 0, 170, 180, 0, 0, 0, NA, 0, 0, 0, 0, 0, NA, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 310, 0, 330, 340, 0, 0, 350, 0, 370, 380, 0), .Dim = c(6L, 8L), .Dimnames = list(NULL, NULL)), 1:2, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm8() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(544.790381900886, 398.486952468991, 440.879079007027, 273.26068924187, -165.547292067734, -289.908895455829, -336.563851641157, -433.491123254512, -446.830170210184, -229.698549757081, 7.43503106965538, 237.187718724823, 544.790381900886, 398.486952468991, 440.879079007027, 273.26068924187, -165.547292067734, -289.908895455829, -336.563851641157, -433.491123254512, -446.830170210184, -229.698549757081, 7.43503106965538, 237.187718724823, 544.790381900886, 398.486952468991, 440.879079007027, 273.26068924187, -165.547292067734, -289.908895455829, -336.563851641157, -433.491123254512, -446.830170210184, -229.698549757081, 7.43503106965538, 237.187718724823, 544.790381900886, 398.486952468991, 440.879079007027, 273.26068924187, -165.547292067734, -289.908895455829, -336.563851641157, -433.491123254512, -446.830170210184, -229.698549757081, 7.43503106965538, 237.187718724823, 544.790381900886, 398.486952468991, 440.879079007027, 273.26068924187, -165.547292067734, -289.908895455829, -336.563851641157, -433.491123254512, -446.830170210184, -229.698549757081, 7.43503106965538, 237.187718724823, 544.790381900886, 398.486952468991, 440.879079007027, 273.26068924187, -165.547292067734, -289.908895455829, -336.563851641157, -433.491123254512, -446.830170210184, -229.698549757081, 7.43503106965538, 237.187718724823, 1539.44739946315, 1548.66655077773, 1557.88570209231, 1566.45228027983, 1575.01885846735, 1583.14083472285, 1591.26281097836, 1599.26100149451, 1607.25919201066, 1611.39689466313, 1615.5345973156, 1612.61129444623, 1609.68799157686, 1603.44643859537, 1597.20488561388, 1588.73002343463, 1580.25516125537, 1570.86127478964, 1561.46738832392, 1549.89535441445, 1538.32332050498, 1524.62526591843, 1510.92721133189, 1499.65830819836, 1488.38940506483, 1479.31388700637, 1470.23836894792, 1460.03887936132, 1449.83938977473, 1441.27547309544, 1432.71155641615, 1432.51830671501, 1432.32505701387, 1433.15763708544, 1433.99021715701, 1434.96142536256, 1435.9326335681, 1435.47421580154, 1435.01579803498, 1433.4368629411, 1431.85792784722, 1430.85617066215, 1429.85441347709, 1432.59097206397, 1435.32753065085, 1440.49425642708, 1445.66098220331, 1448.76676550395, 1451.87254880459, 1452.9163236715, 1453.96009853841, 1454.6961768366, 1455.43225513478, 1452.22362902495, 1449.01500291512, 1442.43484036078, 1435.85467780644, 1426.50159512644, 1417.14851244644, 1409.58997614642, 1402.0314398464, 1397.59624058751, 1393.16104132862, 1386.64426440334, 1380.12748747807, 1371.71107833433, 1363.2946691906, 1354.59002807904, 1345.88538696748, 1336.94914699242, 1328.01290701735, 1318.64960669271, 49.7622186359663, -84.1535032467218, -121.764781099341, 37.2870304782966, 82.528433600382, -44.2319392670254, 25.3010406627996, -34.7698782399993, 48.5709781995188, 110.301655093951, -1.96962838525201, -3.7990131710535, -51.4783734777507, 135.066608935635, 114.916035379091, -28.990712676497, -11.7078691876363, 7.04762066618673, -38.9035366827579, 16.5957688400649, -38.4931502947952, 52.0732838386475, 26.6377575984557, 329.153973076816, -13.1797869657194, 872.199160524634, 371.882552045056, -254.299568603192, -95.2920977069916, 8.63342236039193, 16.852295225008, -29.0271834604991, 13.5051131963112, 4.54091267164154, 25.5747517733375, 386.850855912621, 259.276984531009, -199.961168270532, -153.894877042003, 94.302447817031, -20.3106357794875, 21.0527247936745, -6.29056183593116, 13.9001511905426, -29.4973604406664, -31.7957066699985, -224.096013272965, -30.9544842287708, 22.3370692945275, 432.596723859509, 47.1608224545594, -304.956866078466, 50.1150369329559, 24.6852664308792, -14.4511512739648, -4.94371710626865, -19.024507596255, -56.8030453693573, -314.583543516094, 165.222305128756, 316.17817825271, 23.9168069434991, 11.9598796643579, -128.904953645213, 0.419804589665318, -6.80218287850425, 29.2691824505584, 53.9010951754703, 40.9447832426993, -26.2505972353374, -41.4479380870087, -214.837325417531, 2134, 1863, 1877, 1877, 1492, 1249, 1280, 1131, 1209, 1492, 1621, 1846, 2103, 2137, 2153, 1833, 1403, 1288, 1186, 1133, 1053, 1347, 1545, 2066, 2020, 2750, 2283, 1479, 1189, 1160, 1113, 970, 999, 1208, 1467, 2059, 2240, 1634, 1722, 1801, 1246, 1162, 1087, 1013, 959, 1179, 1229, 1655, 2019, 2284, 1942, 1423, 1340, 1187, 1098, 1004, 970, 1140, 1110, 1812, 2263, 1820, 1846, 1531, 1215, 1075, 1056, 975, 940, 1081, 1294, 1341), .Dim = c(72L, 4L), .Dimnames = list(NULL, c('STL.seasonal', 'STL.trend', 'STL.remainder', 'data')), .Tsp = c(1974, 1979.91666666667, 12), class = c('mts', 'ts', 'matrix')), 1:2, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm9() {
        assertEval("argv <- list(structure(c(0.36376697930799, 0.252815298286177, 0.144820268657847, 0.059950033165656, 0.0137701755391906, 0.00220408917547991, 6.22489401973083e-05, -0.36376697846279, -0.252815298708777, -0.144820267390048, -0.0599500327958813, -0.0137701747732286, -0.00220408987788688, -6.22486118855004e-05), .Dim = c(7L, 2L, 1L)), 1:3, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm10() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(32, 53, 10, 3, 11, 50, 10, 30, 10, 25, 7, 5, 3, 15, 7, 8, 36, 66, 16, 4, 9, 34, 7, 64, 5, 29, 7, 5, 2, 14, 7, 8), .Dim = c(4L, 4L, 2L), .Dimnames = structure(list(Hair = c('Black', 'Brown', 'Red', 'Blond'), Eye = c('Brown', 'Blue', 'Hazel', 'Green'), Sex = c('Male', 'Female')), .Names = c('Hair', 'Eye', 'Sex')), class = 'table'), c(3L, 1L, 2L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm11() {
        assertEval("argv <- list(structure(list(3, 3, 3, 3, 3, 'fred'), .Dim = 2:3), NULL, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm12() {
        assertEval("argv <- list(structure(c(1, 0, -1, 0.5, -0.5, NA, NA, NA, 0), .Dim = c(3L, 3L)), 1:2, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm13() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('    Null deviance:', 'Residual deviance:', '3.118557', '0.012672', ' on', ' on', '8', '7', ' degrees of freedom\\n', ' degrees of freedom\\n'), .Dim = c(2L, 5L), .Dimnames = list(c('null.deviance', 'deviance'), NULL)), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm14() {
        assertEval("argv <- list(structure(character(0), .Dim = c(3L, 0L, 2L)), 1:3, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm15() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE), .Dim = c(5L, 20L), .Dimnames = list(c('#ifdef', '\\\\Sexpr', 'build', 'install', 'render'), NULL)), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm16() {
        assertEval("argv <- list(structure(c(0.537261763078809+0i, 0.305061935059249+0.040985454461732i, 0.320062315956695-0.375563080684187i, 0.339383913939873+0.23302799386284i, -0.286918674221019+0.348301421162371i, 0.333809303929022+0i, -0.026432475532662-0.117484096686937i, 0.337897321317337+0.476009430788475i, -0.104431629205049-0.683873316213355i, -0.076600108155357+0.221030150757328i, 0.0283375771475593+0i, -0.439625821284244+0.725562264268455i, -0.093252555843956-0.328135936730845i, 0.099659684890077-0.362886081139892i, -0.146024566266657+0.013219412797458i, 0.437826208287688+0i, -0.047393587739568+0.297523229473226i, 0.053640336864496+0.244704251340016i, 0.189395328272566+0.197948900656662i, 0.744900728861518-0.157648587806964i, -0.63829956885596+0i, 0.190923866036828+0.209348060979014i, 0.478761262752136+0.086103851005322i, 0.365383456834977-0.041833555661111i, 0.222902888615007+0.301211043305794i), .Dim = c(5L, 5L)), 1:2, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm17() {
        assertEval("argv <- list(structure(c('add1', 'anova', 'coef', 'confint', 'drop1', 'extractAIC', 'logLik', 'model.frame', 'predict', 'print', 'print', 'summary', 'vcov', 'coef', 'predict', 'print', 'print', 'summary', 'nnet', 'nnet', 'multinom', 'multinom', 'multinom', 'multinom', 'multinom', 'multinom', 'multinom', 'multinom', 'multinom', 'multinom', 'summary.multinom', 'multinom', 'multinom', 'nnet', 'nnet', 'nnet', 'summary.nnet', 'nnet', 'default', 'formula', NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA, NA), .Dim = c(20L, 3L)), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm18() {
        assertEval("argv <- list(structure(c(NA, NA, NA), .Dim = 3L), 1L, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm19() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 4000, 8000, 12000, 16000, 20000, 24000, 28000, 32000, 36000, 40000, 44000, 48000, 8, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 72000, 80000, 88000, 96000, 12, 24, 36, 48, 60, 72, 84, 96, 108, 120, 132, 144, 12000, 24000, 36000, 48000, 60000, 72000, 84000, 96000, 108000, 120000, 132000, 144000, 16, 32, 48, 64, 80, 96, 112, 128, 144, 160, 176, 192, 16000, 32000, 48000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 176000, 192000, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240, 20000, 40000, 60000, 80000, 1e+05, 120000, 140000, 160000, 180000, 2e+05, 220000, 240000, 24, 48, 72, 96, 120, 144, 168, 192, 216, 240, 264, 288, 24000, 48000, 72000, 96000, 120000, 144000, 168000, 192000, 216000, 240000, 264000, 288000, 28, 56, 84, 112, 140, 168, 196, 224, 252, 280, 308, 336, 28000, 56000, 84000, 112000, 140000, 168000, 196000, 224000, 252000, 280000, 308000, 336000, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 32000, 64000, 96000, 128000, 160000, 192000, 224000, 256000, 288000, 320000, 352000, 384000, 36, 72, 108, 144, 180, 216, 252, 288, 324, 360, 396, 432, 36000, 72000, 108000, 144000, 180000, 216000, 252000, 288000, 324000, 360000, 396000, 432000, 40, 80, 120, 160, 200, 240, 280, 320, 360, 400, 440, 480, 40000, 80000, 120000, 160000, 2e+05, 240000, 280000, 320000, 360000, 4e+05, 440000, 480000, 44, 88, 132, 176, 220, 264, 308, 352, 396, 440, 484, 528, 44000, 88000, 132000, 176000, 220000, 264000, 308000, 352000, 396000, 440000, 484000, 528000, 48, 96, 144, 192, 240, 288, 336, 384, 432, 480, 528, 576, 48000, 96000, 144000, 192000, 240000, 288000, 336000, 384000, 432000, 480000, 528000, 576000, 52, 104, 156, 208, 260, 312, 364, 416, 468, 520, 572, 624, 52000, 104000, 156000, 208000, 260000, 312000, 364000, 416000, 468000, 520000, 572000, 624000, 56, 112, 168, 224, 280, 336, 392, 448, 504, 560, 616, 672, 56000, 112000, 168000, 224000, 280000, 336000, 392000, 448000, 504000, 560000, 616000, 672000, 60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 660, 720, 60000, 120000, 180000, 240000, 3e+05, 360000, 420000, 480000, 540000, 6e+05, 660000, 720000, 64, 128, 192, 256, 320, 384, 448, 512, 576, 640, 704, 768, 64000, 128000, 192000, 256000, 320000, 384000, 448000, 512000, 576000, 640000, 704000, 768000, 68, 136, 204, 272, 340, 408, 476, 544, 612, 680, 748, 816, 68000, 136000, 204000, 272000, 340000, 408000, 476000, 544000, 612000, 680000, 748000, 816000, 72, 144, 216, 288, 360, 432, 504, 576, 648, 720, 792, 864, 72000, 144000, 216000, 288000, 360000, 432000, 504000, 576000, 648000, 720000, 792000, 864000, 76, 152, 228, 304, 380, 456, 532, 608, 684, 760, 836, 912, 76000, 152000, 228000, 304000, 380000, 456000, 532000, 608000, 684000, 760000, 836000, 912000, 80, 160, 240, 320, 400, 480, 560, 640, 720, 800, 880, 960, 80000, 160000, 240000, 320000, 4e+05, 480000, 560000, 640000, 720000, 8e+05, 880000, 960000, 84, 168, 252, 336, 420, 504, 588, 672, 756, 840, 924, 1008, 84000, 168000, 252000, 336000, 420000, 504000, 588000, 672000, 756000, 840000, 924000, 1008000, 88, 176, 264, 352, 440, 528, 616, 704, 792, 880, 968, 1056, 88000, 176000, 264000, 352000, 440000, 528000, 616000, 704000, 792000, 880000, 968000, 1056000, 92, 184, 276, 368, 460, 552, 644, 736, 828, 920, 1012, 1104, 92000, 184000, 276000, 368000, 460000, 552000, 644000, 736000, 828000, 920000, 1012000, 1104000, 96, 192, 288, 384, 480, 576, 672, 768, 864, 960, 1056, 1152, 96000, 192000, 288000, 384000, 480000, 576000, 672000, 768000, 864000, 960000, 1056000, 1152000), .Dim = c(3L, 4L, 2L, 3L, 4L, 2L), .Dimnames = list(c('A', 'B', 'C'), c('D', 'E', 'F', 'G'), c('frequentist', 'bayesian'), NULL, NULL, c('happy', 'sad'))), c(4L, 1L, 5L, 2L, 6L, 3L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm20() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(1:24, .Dim = c(4L, 6L), .Dimnames = structure(list(happy = c('a', 'b', 'c', 'd'), sad = c('A', 'B', 'C', 'D', 'E', 'F')), .Names = c('happy', 'sad'))), c(2, 1), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm21() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(1:120, .Dim = 2:5, .Dimnames = list(NULL, c('a', 'b', 'c'), NULL, c('V5', 'V6', 'V7', 'V8', 'V9'))), 1:4, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm22() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c('    Null deviance:', 'Residual deviance:', '67.5316', ' 4.5512', ' on', ' on', '9', '7', ' degrees of freedom\\n', ' degrees of freedom\\n'), .Dim = c(2L, 5L), .Dimnames = list(c('null.deviance', 'deviance'), NULL)), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm23() {
        assertEval("argv <- list(structure(c(FALSE, FALSE, FALSE), .Dim = c(3L, 1L)), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm24() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 2, 1, 2, 3, 4, 5), .Dim = c(8L, 2L), .Dimnames = list(c('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'), c('x1', 'x2'))), c(2L, 1L), TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm25() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(c(0, 1.23517986278748, 1.95771376416406, 2.47035972557496, 2.86799882564536, 3.19289362695154, 3.46758826742286, 3.70553958836244, 3.91542752832811, 4.10317868843284, 4.27302027203108, 4.42807348973901, 4.57070862330685, 4.70276813021034, 4.82571258980942, 4.94071945114991, 5.04875179140451, 5.15060739111559, 5.2469545231864, 5.33835855122032, 5.42530203158692, 5.50820013481856, 5.58741263619316, 5.66325335252649, 5.73599765129073, 5.80588848609433, 5.87314129249217, 5.93794799299782, 6.00048029898585, 6.0608924525969, 6.11932351888589, 6.17589931393739, 6.23073403619514, 6.28393165419199, 6.33558709306823, 6.38578725390307, 6.43461189335533, 6.48213438597388, 6.52842238747091, 6.5735384140078, 6.61754034994095, 6.6604818943744, 6.70241295516147, 6.74337999760604, 6.78342635397348, 6.82259249898064, 6.86091629565604, 6.89843321531397, 6.93517653484573, 6.97117751407821, 7.00646555556857, 7.04106834888181, 7.07501200112497, 7.10832115527965, 7.14101909767645, 7.1731278557853, 7.20466828735046, 7.23566016177333, 7.26612223453848, 7.29607231538438, 7.3255273308395, 7.35450338167337, 7.38301579575098, 7.41107917672487, 7.43870744895221, 7.46591389898262, 7.49271121392624, 7.51911151697947, 7.54512640035722, 7.57076695585571, 7.59604380324749, 7.62096711669055, 7.64554664931599, 7.66979175614282, 7.69371141545478, 7.71731424876136, 7.74060853945395, 7.76360225025839, 7.78630303957574, 7.80871827679528, 7.83085505665623, 7.85272021272842, 7.87432033007586, 7.89566175716188, 7.91675061704988, 7.93759281794895, 7.95819406314991, 7.97855986039352, 7.99869553070936, 8.01860621676096, 8.03829689072971, 8.05777236176812, 8.07703728304995, 8.09609615844352, 8.11495334883177, 8.13361307810145, 8.15207943882202, 8.17035639763321, 8.1884478003592, 8.20635737686569, 5.76558893216369, 5.5342688729893, 5.69366159038267, 6.17674775070929, 6.08762735966107, 6.68653280779044, 6.70253591217234, 6.32938323618963, 6.81735284786279, 6.64835766778347, 6.91213030655848, 7.1496842781073, 7.25682341590407, 7.46164094256645, 7.37149913131863, 7.56470707593246, 7.71334191900841, 7.71375128844693, 7.82793409372511, 7.90749319121623, 7.96255733207686, 8.11381187364273, 8.21211505208663, 8.18427543602736, 8.29133399017863, 8.31295002652197, 8.345677476918, 8.39053879616249, 8.40857122007675, 8.48086068897741, 8.7064475146364, 8.66563269607315, 8.79435721712053, 8.7996087849725, 8.82443395257555, 8.91314507957224, 8.8999544270272, 8.96760168103462, 8.93548690078514, 9.01332239000153, 9.07083338712431, 9.15422051683385, 9.20109302097792, 9.2062218972166, 9.31170984199071, 9.30909253379462, 9.35447695163181, 9.45333740615033, 9.41458248768079, 9.46983861007334, 9.51652628670815, 9.5301888386762, 9.59497468213833, 9.61268143770055, 9.64141492393412, 9.68857453461133, 9.77580537125637, 9.79816256416163, 9.79128849346381, 9.80699184934282, 9.91833626833319, 9.95487179604373, 9.88086373278725, 9.93505313047982, 9.97034080826287, 9.97752630228797, 10.1165750634827, 10.0977558023188, 10.1414502841663, 10.129071787117, 10.166774063688, 10.1792762662323, 10.2172491181904, 10.2670710204409, 10.2742314938915, 10.287876622612, 10.3447249333494, 10.4075370351282, 10.3465199067119, 10.4404223214255, 10.422301774768, 10.4739543513507, 10.5314461891317, 10.4813429169605, 10.5097541699286, 10.5389544549716, 10.5752633644781, 10.6133054015308, 10.6776080133421, 10.6266190058322, 10.6657950921482, 10.7067723709738, 10.7424707425861, 10.7418659657784, 10.7335163259687, 10.780101845273, 10.8334343829096, 10.8616735406708, 10.8535694508523, 10.8900668188725), .Dim = c(100L, 2L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59', '60', '61', '62', '63', '64', '65', '66', '67', '68', '69', '70', '71', '72', '73', '74', '75', '76', '77', '78', '79', '80', '81', '82', '83', '84', '85', '86', '87', '88', '89', '90', '91', '92', '93', '94', '95', '96', '97', '98', '99', '100'), c('log(x)', 'log(z)'))), 1:2, TRUE); .Internal(aperm(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testaperm27() {
        assertEval(Ignored.Unknown,
                        "argv <- structure(list(a = structure(c(0.124248979591837, 0.266432653061224,     0.404342857142857, 0.0992163265306122, 0.0851836734693878,     0.0937632653061225, 0.0163551020408163, 0.182897959183673,     0.303289795918367, 0.010330612244898, 0.0557795918367347,     0.0490938775510204, 0.0992163265306122, 0.0851836734693878,     0.0937632653061225, 0.143689795918367, 0.098469387755102,     0.104004081632653, 0.0116979591836735, 0.0826530612244898,     0.0713795918367347, 0.00929795918367347, 0.0412040816326531,     0.0476285714285714, 0.0163551020408163, 0.182897959183673,     0.303289795918367, 0.0116979591836735, 0.0826530612244898,     0.0713795918367347, 0.0301591836734694, 0.220816326530612,     0.304587755102041, 0.00606938775510204, 0.0731020408163265,     0.0488244897959184, 0.010330612244898, 0.0557795918367347,     0.0490938775510204, 0.00929795918367347, 0.0412040816326531,     0.0476285714285714, 0.00606938775510204, 0.0731020408163265,     0.0488244897959184, 0.0111061224489796, 0.0391061224489796,     0.0754326530612245), .Dim = c(3L, 4L, 4L), .Dimnames = structure(list(Species = c('setosa',     'versicolor', 'virginica'), c('Sepal.Length', 'Sepal.Width',     'Petal.Length', 'Petal.Width'), c('Sepal.Length', 'Sepal.Width',     'Petal.Length', 'Petal.Width')), .Names = c('Species', '',     ''))), perm = c(2, 3, 1)), .Names = c('a', 'perm'));" +
                                        "do.call('aperm', argv)");
    }

    @Test
    public void testAperm() {
        // default argument for permutation is transpose
        assertEval("{ a = array(1:4,c(2,2)); b = aperm(a); c(a[1,1] == b[1,1], a[1,2] == b[2,1], a[2,1] == b[1,2], a[2,2] == b[2,2]) }");

        // default for resize is true
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a); c(dim(b)[1],dim(b)[2],dim(b)[3]) }");

        // no resize does not change the dimensions
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, c(3,2,1), resize=FALSE); c(dim(b)[1],dim(b)[2],dim(b)[3]) }");

        // correct structure with resize
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, c(2,3,1)); a[1,2,3] == b[2,3,1] }");

        // correct structure on cubic array
        assertEval("{ a = array(1:24,c(3,3,3)); b = aperm(a, c(2,3,1)); c(a[1,2,3] == b[2,3,1], a[2,3,1] == b[3,1,2], a[3,1,2] == b[1,2,3]) }");

        // correct structure on cubic array with no resize
        assertEval("{ a = array(1:24,c(3,3,3)); b = aperm(a, c(2,3,1), resize = FALSE); c(a[1,2,3] == b[2,3,1], a[2,3,1] == b[3,1,2], a[3,1,2] == b[1,2,3]) }");

        // correct structure without resize
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a, c(2,3,1), resize = FALSE); a[1,2,3] == b[2,1,2] }");

        // no resize does not change the dimensions
        assertEval("{ a = array(1:24,c(2,3,4)); b = aperm(a,, resize=FALSE); c(dim(b)[1],dim(b)[2],dim(b)[3]) }");

        assertEval("{ aperm(array(c(TRUE, FALSE, TRUE, TRUE, FALSE), c(2, 5, 2))) }");
        assertEval("{ aperm(array(c('FASTR', 'IS', 'SO', 'FAST'), c(3,1,2))) }");

        // perm specified in complex numbers produces warning
        assertEval("{ aperm(array(1:27,c(3,3,3)), c(1+1i,3+3i,2+2i))[1,2,3] == array(1:27,c(3,3,3))[1,3,2]; }");

        // perm is not a permutation vector
        assertEval("{ aperm(array(1,c( 3,3,3)), c(1,2,1)); }");

        // perm value out of bounds
        assertEval("{ aperm(array(1,c(3,3,3)), c(1,2,0)); }");

        // first argument not an array
        assertEval("{ aperm(c(1,2,3)); }");

        // Invalid first argument, not array
        assertEval("{ aperm(c(c(2,3), c(4,5), c(6,7)), c(3,4)) }");

        // invalid perm length
        assertEval("{ aperm(array(1,c(3,3,3)), c(1,2)); }");

        // Complex Vector
        assertEval("{ aperm(array(c(3+2i, 5+0i, 1+3i, 5-3i), c(2,2,2))) }");
    }
}
