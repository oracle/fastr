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
public class TestBuiltin_cumsum extends TestBase {

    @Test
    public void testcumsum1() {
        assertEval("argv <- list(c(9L, 5L, 13L));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum2() {
        assertEval("argv <- list(structure(c(15L, 14L), .Names = c('bibentry', NA)));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum3() {
        assertEval("argv <- list(structure(c(79.3831968838961, 8.55983483385341e+101), .Names = c('', '')));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum4() {
        assertEval("argv <- list(structure(c(-191.999930599838, 7.71626352011359e-309), .Names = c('', '')));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum5() {
        assertEval(Ignored.Unknown, "argv <- list(NULL);cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum6() {
        assertEval("argv <- list(structure(c(5L, 8L, 4L, 19L, 26L, 18L, 41L, 42L, 51L, 90L, 97L, 95L, 122L, 134L, 195L, 215L, 225L, 237L, 274L, 291L, 305L, 333L, 353L, 330L, 363L, 376L, 365L, 393L, 409L, 407L, 376L, 371L, 366L, 337L, 307L, 333L, 290L, 244L, 224L, 218L, 209L, 144L, 147L, 112L, 91L, 79L, 69L, 58L, 54L, 38L, 27L, 17L, 30L, 10L, 7L, 19L), .Dim = 56L, .Dimnames = structure(list(c('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51', '52', '53', '54', '55')), .Names = ''), class = 'table'));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum7() {
        assertEval(Ignored.Unknown,
                        "argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = structure('integer(0)', .Names = 'c0')));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum8() {
        assertEval("argv <- list(c(6, 6, 5));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum9() {
        assertEval("argv <- list(structure(c(7, 7, 7, 7), .Dim = 4L, .Dimnames = list(c('Urban Female', 'Urban Male', 'Rural Female', 'Rural Male'))));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum10() {
        assertEval("argv <- list(structure(c(-0.233567190135781, 1.27766471142225), .Names = c('Low|Medium', 'Medium|High')));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum11() {
        assertEval("argv <- list(c(8L, 2L, 12L, 6L, 4L, 5L, 13L));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum12() {
        assertEval("argv <- list(c(0.535137960496205, -0.371944875163495, -1.02554224849711, -0.582401674605252, 0.342888392897331, -0.450934647056651, 0.51423012023069, -0.334338052169782, -0.105559908794475, -0.730509672807828, 1.9050435849087, 0.332621731470394, 0.230633640499451, -1.69186241488407, 0.659791899549327, -1.02362358887971, -0.891521574354298, 0.918341171021649, -0.45270064650823, -1.74837228000318, 1.76990410988936, -2.37740692539252, 0.572811529859585, 1.01724924908461, -0.630967866660535, 0.444287051411554, 0.439130388388555, 1.04062315291451, 0.484099387952522, -0.244883779092525, 0.915992057940211, 0.800622356509766, -0.936569034135793, -1.40078743399573, 0.160277539993178, -0.273962374775183, -0.985539112562296, 0.0839306795150329, -1.31999652648691, 0.161226351326199, -0.62492838754192, 0.957164274192481, 2.42448914116153, -0.915979243686792, 1.05766417094298, 0.825149727768283, -0.0701942243053587, -0.453646374057015, 1.57530770683748, -2.00545781823625, -0.643194791593663, -1.43684344365778, 1.39531343894608, -0.190703432644857, -0.524671199469671, 3.18404447406633, -0.0500372678876282, -0.443749311866524, 0.299865250136145, -1.56842462075497, 0.490302642672068, -0.0961632010799668, 0.468525122530146, -0.982370635937854, -1.02298384214794, -0.693414663276185, -0.767989573092782, 1.29904996668359, 1.57914556180809, -0.156891953039067, -0.35893656058468, -0.329038830421669, 0.0692364778530165, 0.0969042337010548, 0.290034387765571, -0.746678941046256, -0.846896388820319, 1.19707766374608, -0.548627361103323, 0.303045695225451, -0.056970533803332, -0.957849392150669, 0.591061909411507, 0.173104873492955, 1.39978335621251, 0.117459584626988, -0.331545758200853, 0.278294913305364, -1.18559164903534, -0.835894053393597, 0.510273251139431, -0.333120901223949, -0.0659609463524635, -0.11522170942195, -0.650512618774529, -2.01868865908242, 0.348834970176592, 0.761639507646859, -1.28871623535013, 1.48240271845861));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum13() {
        assertEval("argv <- list(c(23L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 11L, 10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L, 10L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, -7L, 15L, 15L, 15L, 15L, 15L, -4L, -4L, -4L, -4L, -4L, -4L, -4L, -4L, -4L, -4L, -4L, -4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, -9L, -9L, -9L, -9L, -9L, -9L, -9L, -9L, -9L, -9L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, -3L, -3L, -3L, -3L, -3L, -3L, -3L, -3L, -3L, -3L, -3L, -3L, -3L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, -2L, -2L, -2L, -2L, -2L, -2L, -2L, -2L, -2L, -2L, -2L, -6L, -6L, -6L, -6L, -6L, -6L, -6L, -6L, -6L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, NA, NA, 3L, 3L, 3L, -19L, -19L, -19L, -19L, -19L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 8L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum14() {
        assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE));cumsum(argv[[1]]);");
    }

    @Test
    public void testcumsum15() {
        assertEval("argv <- list(character(0));cumsum(argv[[1]]);");
    }

    @Test
    public void testCumulativeSum() {
        assertEval("{ cumsum(1:10) }");
        assertEval("{ cumsum(c(1,2,3)) }");
        assertEval("{ cumsum(NA) }");
        assertEval("{ cumsum(c(2000000000L, NA, 2000000000L)) }");
        assertEval("{ cumsum(c(TRUE,FALSE,TRUE)) }");
        assertEval("{ cumsum(c(TRUE,FALSE,NA,TRUE)) }");
        assertEval("{ cumsum(c(1+1i,2-3i,4+5i)) }");
        assertEval("{ cumsum(c(1+1i, NA, 2+3i)) }");
        assertEval("{ cumsum(as.logical(-2:2)) }");
        assertEval("{ cumsum((1:6)*(1+1i)) }");

        assertEval(Ignored.Unknown, "{ cumsum(c(1,2,3,0/0,5)) }");
        assertEval(Ignored.Unknown, "{ cumsum(c(1,0/0,5+1i)) }");
        assertEval(Ignored.Unknown, "{ cumsum(as.raw(1:6)) }");
        // FIXME 1e+308
        assertEval(Ignored.Unknown, "{ cumsum(rep(1e308, 3) ) }");
        // FIXME 1e+308
        assertEval(Ignored.Unknown, "{ cumsum(c(1e308, 1e308, NA, 1, 2)) }");
        // FIXME missing warning
        assertEval(Ignored.Unknown, "{ cumsum(c(2000000000L, 2000000000L)) }");
        // FIXME missing warning
        assertEval(Ignored.Unknown, "{ cumsum(c(-2147483647L, -1L)) }");
    }
}
