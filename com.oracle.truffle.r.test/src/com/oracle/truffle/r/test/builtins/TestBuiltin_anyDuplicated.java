/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_anyDuplicated extends TestBase {

    @Test
    public void testanyDuplicated1() {
        assertEval("argv <- list(c('U', 'V'), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated2() {
        assertEval("argv <- list(c('refClassA', 'envRefClass', '.environment', 'refClass', 'environment', 'refObject'), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated3() {
        assertEval("argv <- list(c(0, 0.00165722279589047, 0.00331444559178095, 0.00497166838767142, 0.00662889118356189, 0.00828611397945236, 0.00994333677534284, 0.0116005595712333, 0.0132577823671238, 0.0149150051630143, 0.0165722279589047, 0.0182294507547952, 0.0198866735506857, 0.0215438963465761, 0.0232011191424666, 0.0248583419383571, 0.0265155647342476, 0.028172787530138, 0.0298300103260285, 0.031487233121919, 0.0331444559178094, 0.0348016787136999, 0.0364589015095904, 0.0381161243054809, 0.0397733471013713, 0.0414305698972618, 0.0430877926931523, 0.0447450154890428, 0.0464022382849332, 0.0480594610808237, 0.0497166838767142, 0.0513739066726046, 0.0530311294684951, 0.0546883522643856, 0.0563455750602761, 0.0580027978561665, 0.059660020652057, 0.0613172434479475, 0.062974466243838, 0.0646316890397284, 0.0662889118356189, 0.0679461346315094, 0.0696033574273998, 0.0712605802232903, 0.0729178030191808, 0.0745750258150713, 0.0762322486109617, 0.0778894714068522, 0.0795466942027427, 0.0812039169986331, 0.0828611397945236, 0.0845183625904141, 0.0861755853863046, 0.087832808182195, 0.0894900309780855, 0.091147253773976, 0.0928044765698665, 0.0944616993657569, 0.0961189221616474, 0.0977761449575379, 0.0994333677534284, 0.101090590549319, 0.102747813345209, 0.1044050361411, 0.10606225893699, 0.107719481732881, 0.109376704528771, 0.111033927324662, 0.112691150120552, 0.114348372916443, 0.116005595712333, 0.117662818508224, 0.119320041304114, 0.120977264100004, 0.122634486895895, 0.124291709691785, 0.125948932487676, 0.127606155283566, 0.129263378079457, 0.130920600875347, 0.132577823671238, 0.134235046467128, 0.135892269263019, 0.137549492058909, 0.1392067148548, 0.14086393765069, 0.142521160446581, 0.144178383242471, 0.145835606038362, 0.147492828834252, 0.149150051630143, 0.150807274426033, 0.152464497221923, 0.154121720017814, 0.155778942813704, 0.157436165609595, 0.159093388405485, 0.160750611201376, 0.162407833997266, 0.164065056793157, 0.165722279589047, 0.167379502384938, 0.169036725180828, 0.170693947976719, 0.172351170772609, 0.1740083935685, 0.17566561636439, 0.177322839160281, 0.178980061956171, 0.180637284752062, 0.182294507547952, 0.183951730343842, 0.185608953139733, 0.187266175935623, 0.188923398731514, 0.190580621527404, 0.192237844323295, 0.193895067119185, 0.195552289915076, 0.197209512710966, 0.198866735506857, 0.200523958302747, 0.202181181098638, 0.203838403894528, 0.205495626690419, 0.207152849486309, 0.2088100722822, 0.21046729507809, 0.21212451787398, 0.213781740669871, 0.215438963465761, 0.217096186261652, 0.218753409057542, 0.220410631853433, 0.222067854649323, 0.223725077445214, 0.225382300241104, 0.227039523036995, 0.228696745832885, 0.230353968628776, 0.232011191424666, 0.233668414220557, 0.235325637016447, 0.236982859812338, 0.238640082608228, 0.240297305404119, 0.241954528200009, 0.243611750995899, 0.24526897379179, 0.24692619658768, 0.248583419383571, 0.250240642179461, 0.251897864975352, 0.253555087771242, 0.255212310567133, 0.256869533363023, 0.258526756158914, 0.260183978954804, 0.261841201750695, 0.263498424546585, 0.265155647342476, 0.266812870138366, 0.268470092934257, 0.270127315730147, 0.271784538526038, 0.273441761321928, 0.275098984117818, 0.276756206913709, 0.278413429709599, 0.28007065250549, 0.28172787530138, 0.283385098097271, 0.285042320893161, 0.286699543689052, 0.288356766484942, 0.290013989280833, 0.291671212076723, 0.293328434872614, 0.294985657668504, 0.296642880464395, 0.298300103260285, 0.299957326056176, 0.301614548852066, 0.303271771647956, 0.304928994443847, 0.306586217239737, 0.308243440035628, 0.309900662831518, 0.311557885627409, 0.313215108423299, 0.31487233121919, 0.31652955401508, 0.318186776810971, 0.319843999606861, 0.321501222402752, 0.323158445198642, 0.324815667994533, 0.326472890790423, 0.328130113586314, 0.329787336382204, 0.331444559178095, 0.333101781973985, 0.334759004769875, 0.336416227565766, 0.338073450361656, 0.339730673157547, 0.341387895953437, 0.343045118749328, 0.344702341545218, 0.346359564341109, 0.348016787136999, 0.34967400993289, 0.35133123272878, 0.352988455524671, 0.354645678320561, 0.356302901116452, 0.357960123912342, 0.359617346708233, 0.361274569504123, 0.362931792300013, 0.364589015095904, 0.366246237891794, 0.367903460687685, 0.369560683483575, 0.371217906279466, 0.372875129075356, 0.374532351871247, 0.376189574667137, 0.377846797463028, 0.379504020258918, 0.381161243054809, 0.382818465850699, 0.38447568864659, 0.38613291144248, 0.387790134238371, 0.389447357034261, 0.391104579830152, 0.392761802626042, 0.394419025421932, 0.396076248217823, 0.397733471013713, 0.399390693809604, 0.401047916605494, 0.402705139401385, 0.404362362197275, 0.406019584993166, 0.407676807789056, 0.409334030584947, 0.410991253380837, 0.412648476176728, 0.414305698972618, 0.415962921768509, 0.417620144564399, 0.41927736736029, 0.42093459015618, 0.42259181295207, 0.424249035747961), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated4() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'), structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame'), FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated5() {
        assertEval("argv <- list(c(NA, 9L, 13L), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated6() {
        assertEval("argv <- list(structure(c('5.1\\r3.5\\r1.4\\r0.2', '4.9\\r3\\r1.4\\r0.2', '4.7\\r3.2\\r1.3\\r0.2', '4.6\\r3.1\\r1.5\\r0.2', '5\\r3.6\\r1.4\\r0.2', '5.4\\r3.9\\r1.7\\r0.4', '4.6\\r3.4\\r1.4\\r0.3', '5\\r3.4\\r1.5\\r0.2', '4.4\\r2.9\\r1.4\\r0.2', '4.9\\r3.1\\r1.5\\r0.1', '5.4\\r3.7\\r1.5\\r0.2', '4.8\\r3.4\\r1.6\\r0.2', '4.8\\r3\\r1.4\\r0.1', '4.3\\r3\\r1.1\\r0.1', '5.8\\r4\\r1.2\\r0.2', '5.7\\r4.4\\r1.5\\r0.4', '5.4\\r3.9\\r1.3\\r0.4', '5.1\\r3.5\\r1.4\\r0.3', '5.7\\r3.8\\r1.7\\r0.3', '5.1\\r3.8\\r1.5\\r0.3', '5.4\\r3.4\\r1.7\\r0.2', '5.1\\r3.7\\r1.5\\r0.4', '4.6\\r3.6\\r1\\r0.2', '5.1\\r3.3\\r1.7\\r0.5', '4.8\\r3.4\\r1.9\\r0.2', '5\\r3\\r1.6\\r0.2', '5\\r3.4\\r1.6\\r0.4', '5.2\\r3.5\\r1.5\\r0.2', '5.2\\r3.4\\r1.4\\r0.2', '4.7\\r3.2\\r1.6\\r0.2', '4.8\\r3.1\\r1.6\\r0.2', '5.4\\r3.4\\r1.5\\r0.4', '5.2\\r4.1\\r1.5\\r0.1', '5.5\\r4.2\\r1.4\\r0.2', '4.9\\r3.1\\r1.5\\r0.2', '5\\r3.2\\r1.2\\r0.2', '5.5\\r3.5\\r1.3\\r0.2', '4.9\\r3.6\\r1.4\\r0.1', '4.4\\r3\\r1.3\\r0.2', '5.1\\r3.4\\r1.5\\r0.2', '5\\r3.5\\r1.3\\r0.3', '4.5\\r2.3\\r1.3\\r0.3', '4.4\\r3.2\\r1.3\\r0.2', '5\\r3.5\\r1.6\\r0.6', '5.1\\r3.8\\r1.9\\r0.4', '4.8\\r3\\r1.4\\r0.3', '5.1\\r3.8\\r1.6\\r0.2', '4.6\\r3.2\\r1.4\\r0.2', '5.3\\r3.7\\r1.5\\r0.2', '5\\r3.3\\r1.4\\r0.2', '7\\r3.2\\r4.7\\r1.4', '6.4\\r3.2\\r4.5\\r1.5', '6.9\\r3.1\\r4.9\\r1.5', '5.5\\r2.3\\r4\\r1.3', '6.5\\r2.8\\r4.6\\r1.5', '5.7\\r2.8\\r4.5\\r1.3', '6.3\\r3.3\\r4.7\\r1.6', '4.9\\r2.4\\r3.3\\r1', '6.6\\r2.9\\r4.6\\r1.3', '5.2\\r2.7\\r3.9\\r1.4', '5\\r2\\r3.5\\r1', '5.9\\r3\\r4.2\\r1.5', '6\\r2.2\\r4\\r1', '6.1\\r2.9\\r4.7\\r1.4', '5.6\\r2.9\\r3.6\\r1.3', '6.7\\r3.1\\r4.4\\r1.4', '5.6\\r3\\r4.5\\r1.5', '5.8\\r2.7\\r4.1\\r1', '6.2\\r2.2\\r4.5\\r1.5', '5.6\\r2.5\\r3.9\\r1.1', '5.9\\r3.2\\r4.8\\r1.8', '6.1\\r2.8\\r4\\r1.3', '6.3\\r2.5\\r4.9\\r1.5', '6.1\\r2.8\\r4.7\\r1.2', '6.4\\r2.9\\r4.3\\r1.3', '6.6\\r3\\r4.4\\r1.4', '6.8\\r2.8\\r4.8\\r1.4', '6.7\\r3\\r5\\r1.7', '6\\r2.9\\r4.5\\r1.5', '5.7\\r2.6\\r3.5\\r1', '5.5\\r2.4\\r3.8\\r1.1', '5.5\\r2.4\\r3.7\\r1', '5.8\\r2.7\\r3.9\\r1.2', '6\\r2.7\\r5.1\\r1.6', '5.4\\r3\\r4.5\\r1.5', '6\\r3.4\\r4.5\\r1.6', '6.7\\r3.1\\r4.7\\r1.5', '6.3\\r2.3\\r4.4\\r1.3', '5.6\\r3\\r4.1\\r1.3', '5.5\\r2.5\\r4\\r1.3', '5.5\\r2.6\\r4.4\\r1.2', '6.1\\r3\\r4.6\\r1.4', '5.8\\r2.6\\r4\\r1.2', '5\\r2.3\\r3.3\\r1', '5.6\\r2.7\\r4.2\\r1.3', '5.7\\r3\\r4.2\\r1.2', '5.7\\r2.9\\r4.2\\r1.3', '6.2\\r2.9\\r4.3\\r1.3', '5.1\\r2.5\\r3\\r1.1', '5.7\\r2.8\\r4.1\\r1.3', '6.3\\r3.3\\r6\\r2.5', '5.8\\r2.7\\r5.1\\r1.9', '7.1\\r3\\r5.9\\r2.1', '6.3\\r2.9\\r5.6\\r1.8', '6.5\\r3\\r5.8\\r2.2', '7.6\\r3\\r6.6\\r2.1', '4.9\\r2.5\\r4.5\\r1.7', '7.3\\r2.9\\r6.3\\r1.8', '6.7\\r2.5\\r5.8\\r1.8', '7.2\\r3.6\\r6.1\\r2.5', '6.5\\r3.2\\r5.1\\r2', '6.4\\r2.7\\r5.3\\r1.9', '6.8\\r3\\r5.5\\r2.1', '5.7\\r2.5\\r5\\r2', '5.8\\r2.8\\r5.1\\r2.4', '6.4\\r3.2\\r5.3\\r2.3', '6.5\\r3\\r5.5\\r1.8', '7.7\\r3.8\\r6.7\\r2.2', '7.7\\r2.6\\r6.9\\r2.3', '6\\r2.2\\r5\\r1.5', '6.9\\r3.2\\r5.7\\r2.3', '5.6\\r2.8\\r4.9\\r2', '7.7\\r2.8\\r6.7\\r2', '6.3\\r2.7\\r4.9\\r1.8', '6.7\\r3.3\\r5.7\\r2.1', '7.2\\r3.2\\r6\\r1.8', '6.2\\r2.8\\r4.8\\r1.8', '6.1\\r3\\r4.9\\r1.8', '6.4\\r2.8\\r5.6\\r2.1', '7.2\\r3\\r5.8\\r1.6', '7.4\\r2.8\\r6.1\\r1.9', '7.9\\r3.8\\r6.4\\r2', '6.4\\r2.8\\r5.6\\r2.2', '6.3\\r2.8\\r5.1\\r1.5', '6.1\\r2.6\\r5.6\\r1.4', '7.7\\r3\\r6.1\\r2.3', '6.3\\r3.4\\r5.6\\r2.4', '6.4\\r3.1\\r5.5\\r1.8', '6\\r3\\r4.8\\r1.8', '6.9\\r3.1\\r5.4\\r2.1', '6.7\\r3.1\\r5.6\\r2.4', '6.9\\r3.1\\r5.1\\r2.3', '5.8\\r2.7\\r5.1\\r1.9', '6.8\\r3.2\\r5.9\\r2.3', '6.7\\r3.3\\r5.7\\r2.5', '6.7\\r3\\r5.2\\r2.3', '6.3\\r2.5\\r5\\r1.9', '6.5\\r3\\r5.2\\r2', '6.2\\r3.4\\r5.4\\r2.3', '5.9\\r3\\r5.1\\r1.8'), .Dim = c(50L, 3L), .Dimnames = list(NULL, c('Setosa', 'Versicolor', 'Virginica'))), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated7() {
        assertEval("argv <- list(c(-6, -3, 0, 3, 6, 9, 12, 15), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated8() {
        assertEval("argv <- list(c(9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 1L, 2L, 3L, 4L, 5L, 3L, 4L, 5L, 6L, 7L, 0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), FALSE, TRUE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated9() {
        assertEval("argv <- list(c(-1.001, -1, -0.999), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated10() {
        assertEval("argv <- list(c(12784, 12874, 12965, 13057, 13149, 13239, 13330, 13422, 13514, 13604, 13695, 13787, 13879, 13970, 14061, 14153, 14245, 14335), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated11() {
        assertEval("argv <- list(structure(c('A', NA), .Names = c('1', '3')), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated12() {
        assertEval("argv <- list(c('A', NA), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated13() {
        assertEval("argv <- list(c(12784, 13149, 13514, 13879, 14245, 14610), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated14() {
        assertEval("argv <- list(character(0), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated15() {
        assertEval("argv <- list(c(1L, 1L, 1L, 1L, 1L), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated16() {
        assertEval("argv <- list(c('1', '4', '6', '9', '11', NA, '15', '16', '17', '20', '21', '23', '29', '41', '45', '48', '55', '62', '63', '65', '70', '74', '82', '83', '85', '86', '92', '93', '97', '98', '99', '103', '104', '106', '108', '109', '112', '113', '120', '126', '127', '128', '132', '139', '142', '145', '148', '151', '159', '164', '165', '169', '171', '173', '175', '189', '191', '193', '194', '195', '198', '200', '202', '209', '212', '213', '215', '216', '221', '223', '224', '227'), FALSE, FALSE); .Internal(anyDuplicated(argv[[1]], argv[[2]], argv[[3]]))");
    }

    @Test
    public void testanyDuplicated18() {
        assertEval("argv <- structure(list(x = structure(c(3, 2, 7, 2, 6, 2, 7, 2),     .Dim = c(4L, 2L), .Dimnames = list(c('A', 'B', 'C', 'D'),         c('M', 'F'))), MARGIN = 0), .Names = c('x', 'MARGIN'));do.call('anyDuplicated', argv)");
    }

    @Test
    public void testanyDuplicated19() {
        assertEval("argv <- structure(list(x = c(1, NA, 3, NA, 3), incomparables = c(3,     NA)), .Names = c('x', 'incomparables'));do.call('anyDuplicated', argv)");
    }

    @Test
    public void testAnyDuplicated() {
        assertEval("{ anyDuplicated(c(1L, 2L, 3L, 4L, 2L, 3L)) }");
        assertEval("{ anyDuplicated(c(1L, 2L, 3L, 4L, 2L, 3L), incomparables = TRUE )}");
        assertEval("{ anyDuplicated(c(1L, 2L, 3L, 4L, 2L, 3L), fromLast = TRUE) }");

        // strings
        assertEval("{anyDuplicated(c(\"abc\"))}");
        assertEval("{anyDuplicated(c(\"abc\", \"def\", \"abc\"))}");
        assertEval("{anyDuplicated(c(\"abc\", \"def\", \"ghi\", \"jkl\"))}");

        // boolean
        assertEval("{anyDuplicated(c(FALSE))}");
        assertEval("{anyDuplicated(c(FALSE, TRUE))}");
        assertEval("{anyDuplicated(c(FALSE, TRUE, FALSE))}");

        // complex
        assertEval("{anyDuplicated(c(2+2i)) }");
        assertEval("{anyDuplicated(c(2+2i, 3+3i, 2+2i)) }");
        assertEval("{anyDuplicated(c(2+2i, 3+3i, 4+4i, 5+5i)) }");

        // Double Vector
        assertEval("{ anyDuplicated(c(27.2, 68.4, 94.3, 22.2)) }");
        assertEval("{ anyDuplicated(c(1, 1, 4, 5, 4), TRUE, TRUE) }");
        assertEval("{ anyDuplicated(c(1,2,1)) }");
        assertEval("{ anyDuplicated(c(1)) }");
        assertEval("{ anyDuplicated(c(1,2,3,4)) }");
        assertEval("{ anyDuplicated(list(76.5, 5L, 5L, 76.5, 5, 5), incomparables = c(5L, 76.5)) }");

        // Logical Vector
        assertEval("{ anyDuplicated(c(TRUE, FALSE, TRUE), TRUE) }");
        assertEval("{ anyDuplicated(c(TRUE, FALSE, TRUE), TRUE, fromLast = 1) }");

        // String Vector
        assertEval("{ anyDuplicated(c(\"abc\", \"good\", \"hello\", \"hello\", \"abc\")) }");
        assertEval("{ anyDuplicated(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), FALSE) }");
        assertEval("{ anyDuplicated(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), TRUE) }");
        assertEval("{ anyDuplicated(c(\"TRUE\", \"TRUE\", \"FALSE\", \"FALSE\"), 1) }");

        // Complex Vector
        assertEval("{ anyDuplicated(c(1+0i, 6+7i, 1+0i), TRUE)}");
        assertEval("{ anyDuplicated(c(1+1i, 4-6i, 4-6i, 6+7i)) }");
        assertEval("{ anyDuplicated(c(1, 4+6i, 7+7i, 1), incomparables = c(1, 2)) }");

        assertEval("{ anyDuplicated(c(1L, 2L, 1L, 1L, 3L, 2L), incomparables = \"cat\") }");
        assertEval("{ anyDuplicated(c(1,2,3,2), incomparables = c(2+6i)) }");
    }
}
