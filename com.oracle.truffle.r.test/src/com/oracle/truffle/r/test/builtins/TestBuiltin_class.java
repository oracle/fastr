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
public class TestBuiltin_class extends TestBase {

    @Test
    public void testclass1() {
        assertEval("argv <- list(structure(function (x) standardGeneric('exp', .Primitive('exp')), generic = structure('exp', package = 'base'), package = 'base', group = list('Math'), valueClass = character(0), signature = 'x', default = .Primitive('exp'), skeleton = quote(.Primitive('exp')(x)), class = structure('standardGeneric', package = 'methods')));class(argv[[1]]);");
    }

    @Test
    public void testclass2() {
        assertEval("argv <- list(structure(c(1, 0, 0, 0, 1, 0, 0, 0, 1), .Dim = c(3L, 3L), class = structure('mmat2', package = '.GlobalEnv')));class(argv[[1]]);");
    }

    @Test
    public void testclass3() {
        assertEval("argv <- list(structure(3.14159265358979, comment = 'Start with pi', class = structure('num1', package = '.GlobalEnv')));class(argv[[1]]);");
    }

    @Test
    public void testclass4() {
        assertEval("argv <- list(structure(c(1+1i, 2+1.4142135623731i, 3+1.73205080756888i, 4+2i, 5+2.23606797749979i, 6+2.44948974278318i, 7+2.64575131106459i, 8+2.82842712474619i, 9+3i, 10+3.1622776601684i), id = character(0), class = structure('withId', package = '.GlobalEnv')));class(argv[[1]]);");
    }

    @Test
    public void testclass5() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')));class(argv[[1]]);");
    }

    @Test
    public void testclass6() {
        assertEval("argv <- list(structure(list(x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), width = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), height = structure(1, unit = 'npc', valid.unit = 0L, class = 'unit'), justification = 'centre', gp = structure(list(), class = 'gpar'), clip = TRUE, xscale = c(-15.89, 356.89), yscale = c(0.683750615306643, 5.8340977374556), angle = 0, layout = NULL, layout.pos.row = c(21L, 21L), layout.pos.col = c(17L, 17L), valid.just = c(0.5, 0.5), valid.pos.row = c(21L, 21L), valid.pos.col = c(17L, 17L), name = 'plot_01.panel.3.1.vp'), .Names = c('x', 'y', 'width', 'height', 'justification', 'gp', 'clip', 'xscale', 'yscale', 'angle', 'layout', 'layout.pos.row', 'layout.pos.col', 'valid.just', 'valid.pos.row', 'valid.pos.col', 'name'), class = 'viewport'));class(argv[[1]]);");
    }

    @Test
    public void testclass7() {
        assertEval("argv <- list(c(NA, '2', '3'));class(argv[[1]]);");
    }

    @Test
    public void testclass8() {
        assertEval("argv <- list(c(325, 257, 303, 315, 380, 153, 263, 242, 206, 344, 258));class(argv[[1]]);");
    }

    @Test
    public void testclass9() {
        assertEval("argv <- list(structure(list(message = 'Choosing method ‘sparseMatrix#ANY’ from 2 ambiguous possibilities', call = NULL), .Names = c('message', 'call'), class = c('simpleCondition', 'condition')));class(argv[[1]]);");
    }

    @Test
    public void testclass10() {
        assertEval("argv <- list(structure(list(time = 1:10, y = c(1, 1.4142135623731, 1.73205080756888, 2, 2.23606797749979, 2.44948974278318, 2.64575131106459, 2.82842712474619, 3, 3.16227766016838)), .Names = c('time', 'y'), row.names = c(NA, 10L), .S3Class = 'data.frame', date = structure(16045, class = 'Date'), class = structure('dataFrameD', package = '.GlobalEnv')));class(argv[[1]]);");
    }

    @Test
    public void testclass11() {
        assertEval("argv <- list(structure(list(), .Names = character(0), row.names = integer(0), .S3Class = 'data.frame', class = structure('data.frame', package = 'methods')));class(argv[[1]]);");
    }

    @Test
    public void testclass12() {
        assertEval("argv <- list(structure(function (qr, y) .Call(sparseQR_resid_fitted, qr, y, TRUE), target = structure(c('sparseQR', 'ddenseMatrix'), .Names = c('qr', 'y'), package = c('Matrix', 'Matrix'), class = structure('signature', package = 'methods')), defined = structure(c('sparseQR', 'ddenseMatrix'), .Names = c('qr', 'y'), package = c('Matrix', 'Matrix'), class = structure('signature', package = 'methods')), generic = structure('qr.resid', package = 'base'), class = structure('MethodDefinition', package = 'methods')));class(argv[[1]]);");
    }

    @Test
    public void testclass13() {
        assertEval("argv <- list(structure(c(3.1, 6.695, 8.14, 7.50090909091, 8.95, 9.26), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.'), class = c('summaryDefault', 'table')));class(argv[[1]]);");
    }

    @Test
    public void testclass14() {
        assertEval("argv <- list(complex(0));class(argv[[1]]);");
    }

    @Test
    public void testclass15() {
        assertEval("argv <- list(structure(c(-0.00225540511921, -0.00045867962383, -8.86739505379e-06, -1.96554854754e-06, 0.000402346479421, 0.00193962597167), .Names = c('Min.', '1st Qu.', 'Median', 'Mean', '3rd Qu.', 'Max.'), class = c('summaryDefault', 'table')));class(argv[[1]]);");
    }

    @Test
    public void testclass16() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, NA, NA, TRUE, TRUE, TRUE, NA, NA, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE));class(argv[[1]]);");
    }

    @Test
    public void testclass17() {
        assertEval("argv <- list(c(-10, -10, -10, NA, NA, 150, 170, 180, NA, NA, 310, 330, 340, 350, 370, 380));class(argv[[1]]);");
    }

    @Test
    public void testclass18() {
        assertEval("argv <- list(structure(numeric(0), .Dim = 0L));class(argv[[1]]);");
    }

    @Test
    public void testclass19() {
        assertEval("argv <- list(structure(c(0.5, 0, 0.5, 0, 0, 0.5, 0, 0, 0.0740740740740741, 0, 0, 0.5, 1, 0, 0, 0.5, 1, 0.5), unit = c('char', 'grobheight', 'char', 'grobheight', 'grobheight', 'char', 'mm', 'lines', 'null', 'mm', 'mm', 'char', 'grobheight', 'char', 'grobheight', 'char', 'grobheight', 'char'), valid.unit = c(18L, 22L, 18L, 22L, 22L, 18L, 7L, 3L, 5L, 7L, 7L, 18L, 22L, 18L, 22L, 18L, 22L, 18L), data = list(NULL, structure(list(label = '', x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'),     just = 'centre', hjust = NULL, vjust = NULL, rot = 0, check.overlap = FALSE, name = 'GRID.text.1', gp = structure(list(), class = 'gpar'), vp = NULL), .Names = c('label', 'x', 'y', 'just', 'hjust', 'vjust', 'rot', 'check.overlap', 'name', 'gp', 'vp'), class = c('text', 'grob', 'gDesc')), NULL, structure(list(label = '', x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), just = 'centre', hjust = NULL, vjust = NULL,     rot = 0, check.overlap = FALSE, name = 'GRID.text.2', gp = structure(list(), class = 'gpar'), vp = NULL), .Names = c('label', 'x', 'y', 'just', 'hjust', 'vjust', 'rot', 'check.overlap', 'name', 'gp', 'vp'), class = c('text', 'grob', 'gDesc')), structure(list(label = '', x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), just = 'centre', hjust = NULL, vjust = NULL, rot = 0, check.overlap = FALSE, name = 'GRID.text.3',     gp = structure(list(), class = 'gpar'), vp = NULL), .Names = c('label', 'x', 'y', 'just', 'hjust', 'vjust', 'rot', 'check.overlap', 'name', 'gp', 'vp'), class = c('text', 'grob', 'gDesc')), NULL, NULL, NULL, NULL, NULL, NULL, NULL, structure(list(label = 'Column', x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), just = 'centre', hjust = NULL, vjust = NULL, rot = 0, check.overlap = FALSE, name = 'plot_01.xlab',     gp = structure(list(fontface = 1, alpha = 1, cex = 1, col = '#000000', lineheight = 1, font = 1L), .Names = c('fontface', 'alpha', 'cex', 'col', 'lineheight', 'font'), class = 'gpar'), vp = NULL), .Names = c('label', 'x', 'y', 'just', 'hjust', 'vjust', 'rot', 'check.overlap', 'name', 'gp', 'vp'), class = c('text', 'grob', 'gDesc')), NULL, structure(list(label = '', x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'),     just = 'centre', hjust = NULL, vjust = NULL, rot = 0, check.overlap = FALSE, name = 'GRID.text.5', gp = structure(list(), class = 'gpar'), vp = NULL), .Names = c('label', 'x', 'y', 'just', 'hjust', 'vjust', 'rot', 'check.overlap', 'name', 'gp', 'vp'), class = c('text', 'grob', 'gDesc')), NULL, structure(list(label = 'Dimensions: 4 x 54', x = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), y = structure(0.5, unit = 'npc', valid.unit = 0L, class = 'unit'), just = 'centre', hjust = NULL,     vjust = NULL, rot = 0, check.overlap = FALSE, name = 'plot_01.sub', gp = structure(list(fontface = 2, alpha = 1, cex = 1, col = '#000000', lineheight = 1, font = 2L), .Names = c('fontface', 'alpha', 'cex', 'col', 'lineheight', 'font'), class = 'gpar'), vp = NULL), .Names = c('label', 'x', 'y', 'just', 'hjust', 'vjust', 'rot', 'check.overlap', 'name', 'gp', 'vp'), class = c('text', 'grob', 'gDesc')), NULL), class = 'unit'));class(argv[[1]]);");
    }

    @Test
    public void testclass20() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), .Dim = c(24L, 13L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24'), c('(Intercept)', 'block2', 'block3', 'block4', 'block5', 'block6', 'N1', 'P1', 'K1', 'N1:P1', 'N1:K1', 'P1:K1', 'N1:P1:K1')), assign = c(0L, 1L, 1L, 1L, 1L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), contrasts = structure(list(block = 'contr.treatment', N = 'contr.treatment', P = 'contr.treatment', K = 'contr.treatment'), .Names = c('block', 'N', 'P', 'K'))));class(argv[[1]]);");
    }

    @Test
    public void testclass21() {
        assertEval("argv <- list(.Primitive('dimnames<-'));class(argv[[1]]);");
    }

    @Test
    public void testclass22() {
        assertEval("argv <- list(c(17, 289, 4913, 83521, 1419857, 24137569, 410338673, 6975757441, 118587876497, 2015993900449, 34271896307633, 582622237229761, 9904578032905936, 168377826559400928, 2862423051509815808, 48661191875666870272, 8.27240261886337e+20, 1.40630844520677e+22, 2.39072435685151e+23, 4.06423140664757e+24, 6.90919339130087e+25, 1.17456287652115e+27, 1.99675689008595e+28, 3.39448671314612e+29, 5.7706274123484e+30, 9.81006660099228e+31, 1.66771132216869e+33, 2.83510924768677e+34, 4.81968572106751e+35, 8.19346572581477e+36, 1.39288917338851e+38, 2.36791159476047e+39, 4.02544971109279e+40, 6.84326450885775e+41, 1.16335496650582e+43, 1.97770344305989e+44, 3.36209585320181e+45, 5.71556295044308e+46, 9.71645701575324e+47, 1.65179769267805e+49, 2.80805607755269e+50, 4.77369533183957e+51, 8.11528206412726e+52, 1.37959795090163e+54, 2.34531651653278e+55, 3.98703807810572e+56, 6.77796473277973e+57, 1.15225400457255e+59, 1.95883180777334e+60, 3.33001407321468e+61, 5.66102392446496e+62, 9.62374067159043e+63, 1.63603591417037e+65, 2.78126105408963e+66, 4.72814379195238e+67, 8.03784444631904e+68, 1.36643355587424e+70, 2.3229370449862e+71, 3.94899297647655e+72, 6.71328806001013e+73, 1.14125897020172e+75, 1.94014024934293e+76, 3.29823842388298e+77, 5.60700532060106e+78, 9.5319090450218e+79, 1.62042453765371e+81, 2.7547217140113e+82, 4.68302691381921e+83, 7.96114575349266e+84, 1.35339477809375e+86, 2.30077112275938e+87, 3.91131090869094e+88, 6.6492285447746e+89, 1.13036885261168e+91, 1.92162704943986e+92, 3.26676598404776e+93, 5.5535021728812e+94, 9.44095369389803e+95, 1.60496212796267e+97, 2.72843561753653e+98, 4.6383405498121e+99, 7.88517893468058e+100, 1.3404804188957e+102, 2.27881671212269e+103, 3.87398841060857e+104, 6.58578029803456e+105, 1.11958265066588e+107, 1.90329050613199e+108, 3.23559386042438e+109, 5.50050956272145e+110, 9.35086625662646e+111, 1.5896472636265e+113, 2.70240034816505e+114, 4.59408059188058e+115, 7.80993700619699e+116, 1.32768929105349e+118, 2.25707179479093e+119, 3.83702205114458e+120, 6.52293748694579e+121, 1.10889937278078e+123, 5.5535021728812e+94, 3.33001407321468e+61, 1.95883180777334e+60, 1.15225400457255e+59, 6.77796473277973e+57, 3.98703807810572e+56, 2.34531651653278e+55, 1.37959795090163e+54, 8.11528206412726e+52, 4.77369533183957e+51, 2.80805607755269e+50, 1.65179769267805e+49, 2015993900449));class(argv[[1]]);");
    }

    @Test
    public void testclass23() {
        assertEval("argv <- list(structure(function (a, b, ...) standardGeneric('solve'), generic = structure('solve', package = 'base'), package = 'base', group = list(), valueClass = character(0), signature = c('a', 'b'), default = structure(function (a, b, ...) UseMethod('solve'), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'a', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'a', package = 'methods'), generic = structure('solve', package = 'base'), class = structure('derivedDefaultMethod', package = 'methods')), skeleton = quote((function (a, b, ...) UseMethod('solve'))(a, b, ...)), class = structure('standardGeneric', package = 'methods')));class(argv[[1]]);");
    }

    @Test
    public void testclass24() {
        assertEval("argv <- list(c(-0.31833672642477-1.38507061859438i, 1.42379885362755+0.0383231810219i, 0.405090858049187-0.763030162361974i, -0.995386565684023+0.212306135525839i, -0.95881778764026+1.42553796686779i, -0.918087896319951+0.744479822333976i, 0.15096960188161+0.70022940298623i, 1.2230687888662-0.22935461345173i, 0.868824288637794+0.197093861895352i, 1.04248536490429+1.20715377387226i));class(argv[[1]]);");
    }

    @Test
    public void testclass25() {
        assertEval("argv <- list(structure(function (x, type, ...) .Call(dgeMatrix_norm, as(x, 'dgeMatrix'), type), target = structure(c('matrix', 'character'), .Names = c('x', 'type'), package = c('methods', 'methods'), class = structure('signature', package = 'methods')), defined = structure(c('matrix', 'character'), .Names = c('x', 'type'), package = c('methods', 'methods'), class = structure('signature', package = 'methods')), generic = structure('norm', package = 'base'), class = structure('MethodDefinition', package = 'methods')));class(argv[[1]]);");
    }

    @Test
    public void testclass26() {
        assertEval("argv <- list(structure(function (x, mode = 'any') .Internal(as.vector(x, mode)), target = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), defined = structure('ANY', class = structure('signature', package = 'methods'), .Names = 'x', package = 'methods'), generic = character(0), class = structure('MethodDefinition', package = 'methods')));class(argv[[1]]);");
    }

    @Test
    public void testclass27() {
        assertEval("argv <- list(structure(function (x, uplo) {    if (uplo == x@uplo) x else t(x)}, target = structure(c('nsCMatrix', 'character'), .Names = c('x', 'uplo'), package = c('Matrix', 'methods'), class = structure('signature', package = 'methods')), defined = structure(c('nsCMatrix', 'character'), .Names = c('x', 'uplo'), package = c('Matrix', 'methods'), class = structure('signature', package = 'methods')), generic = structure('forceSymmetric', package = 'Matrix'), class = structure('MethodDefinition', package = 'methods')));class(argv[[1]]);");
    }

    @Test
    public void testclass28() {
        assertEval("argv <- list(c(1.1+0i, NA, 3+0i));class(argv[[1]]);");
    }

    @Test
    public void testclass29() {
        assertEval("argv <- list(structure(1:10, .Tsp = c(1959.25, 1961.5, 4), class = 'ts'));class(argv[[1]]);");
    }

    @Test
    public void testclass31() {
        assertEval("argv <- list(c(71.128, 69.70625, 70.9566666666667, 71.7, 71.435,     72.5766666666667, 70.6916666666667));do.call('class', argv)");
    }

    @Test
    public void testclass32() {
        assertEval("argv <- list(structure(c(0.909297426825682, 0.141120008059867,     -0.756802495307928), class = c('foo', 'bar')));do.call('class', argv)");
    }

    @Test
    public void testGetClass() {
        assertEval("{x<-1L;class(x)}");

        assertEval("{x<-c(1L,2L,3L);class(x)}");

        assertEval("{x<-seq(1L,10L);class(x)}");

        assertEval("{x<-seq(1.1,10.1);class(x)}");

        assertEval("{x<-1;class(x)}");

        assertEval("{x<-c(1,2,3);class(x)}");

        assertEval("{x<-seq(1,10);class(x)}");

        assertEval("{  gen<-function(object) 0; setGeneric(\"gen\"); class(gen) }");
    }
}
