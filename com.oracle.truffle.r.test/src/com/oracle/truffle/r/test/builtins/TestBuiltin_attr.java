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
public class TestBuiltin_attr extends TestBase {

    @Test
    public void testattr1() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1), .Dim = c(32L, 23L), .Dimnames = list(c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31', '32'), c('(Intercept)', 'HairBrown', 'HairRed', 'HairBlond', 'EyeBlue', 'EyeHazel', 'EyeGreen', 'SexFemale', 'HairBrown:EyeBlue', 'HairRed:EyeBlue', 'HairBlond:EyeBlue', 'HairBrown:EyeHazel', 'HairRed:EyeHazel', 'HairBlond:EyeHazel', 'HairBrown:EyeGreen', 'HairRed:EyeGreen', 'HairBlond:EyeGreen', 'HairBrown:SexFemale', 'HairRed:SexFemale', 'HairBlond:SexFemale', 'EyeBlue:SexFemale', 'EyeHazel:SexFemale', 'EyeGreen:SexFemale')), assign = c(0L, 1L, 1L, 1L, 2L, 2L, 2L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 6L, 6L, 6L), contrasts = structure(list(Hair = 'contr.treatment',     Eye = 'contr.treatment', Sex = 'contr.treatment'), .Names = c('Hair', 'Eye', 'Sex'))), 'assign');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr2() {
        assertEval("argv <- list(structure(list(`cbind(X, M)` = structure(c(68, 42, 37, 24, 66, 33, 47, 23, 63, 29, 57, 19, 42, 30, 52, 43, 50, 23, 55, 47, 53, 27, 49, 29), .Dim = c(12L, 2L), .Dimnames = list(NULL, c('X', 'M'))), M.user = structure(c(1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L, 1L, 1L, 2L, 2L), .Label = c('N', 'Y'), class = 'factor'), Temp = structure(c(2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L, 2L, 1L), .Label = c('High', 'Low'), class = 'factor'), Soft = structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L), .Label = c('Hard', 'Medium', 'Soft'), class = 'factor')), .Names = c('cbind(X, M)', 'M.user', 'Temp', 'Soft'), terms = quote(cbind(X, M) ~ M.user + Temp + Soft), row.names = c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23'), class = 'data.frame'), 'terms');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr4() {
        assertEval("argv <- list(structure(list(`cbind(w = weight, w2 = weight^2)` = structure(c(4.17, 5.58, 5.18, 6.11, 4.5, 4.61, 5.17, 4.53, 5.33, 5.14, 4.81, 4.17, 4.41, 3.59, 5.87, 3.83, 6.03, 4.89, 4.32, 4.69, 17.3889, 31.1364, 26.8324, 37.3321, 20.25, 21.2521, 26.7289, 20.5209, 28.4089, 26.4196, 23.1361, 17.3889, 19.4481, 12.8881, 34.4569, 14.6689, 36.3609, 23.9121, 18.6624, 21.9961), .Dim = c(20L, 2L), .Dimnames = list(NULL, c('w', 'w2'))), group = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Ctl', 'Trt'), class = 'factor')), .Names = c('cbind(w = weight, w2 = weight^2)', 'group'), terms = quote(cbind(w = weight, w2 = weight^2) ~ group), row.names = c(NA, 20L), class = 'data.frame'), 'terms');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr5() {
        assertEval("argv <- list(structure(list(title = structure(1L, .Label = c('An Introduction to R', 'Exploratory Data Analysis', 'Interactive Data Analysis', 'LISP-STAT', 'Modern Applied Statistics ...', 'Spatial Statistics', 'Stochastic Simulation'), class = 'factor'), other.author = structure(2L, .Label = c('Ripley', 'Venables & Smith'), class = 'factor')), .Names = c('title', 'other.author'), row.names = 1L, class = 'data.frame'), 'row.names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr6() {
        assertEval("argv <- list(structure(list(Y = c(130L, 157L, 174L, 117L, 114L, 161L, 141L, 105L, 140L, 118L, 156L, 61L, 91L, 97L, 100L, 70L, 108L, 126L, 149L, 96L, 124L, 121L, 144L, 68L, 64L, 112L, 86L, 60L, 102L, 89L, 96L, 89L, 129L, 132L, 124L, 74L, 89L, 81L, 122L, 64L, 103L, 132L, 133L, 70L, 89L, 104L, 117L, 62L, 90L, 100L, 116L, 80L, 82L, 94L, 126L, 63L, 70L, 109L, 99L, 53L, 74L, 118L, 113L, 89L, 82L, 86L, 104L, 97L, 99L, 119L, 121L), B = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 4L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L, 6L), .Label = c('I', 'II', 'III', 'IV', 'V', 'VI'), class = 'factor'), V = structure(c(3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L), .Label = c('Golden.rain', 'Marvellous', 'Victory'), class = 'factor'), N = structure(c(2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('0.0cwt', '0.2cwt', '0.4cwt', '0.6cwt'), class = 'factor')), .Names = c('Y', 'B', 'V', 'N'), terms = quote(Y ~ B + V + N + V:N), row.names = 2:72, class = 'data.frame'), 'na.action');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr7() {
        assertEval("argv <- list(structure('mtext(\\\'«Latin-1 accented chars»: éè øØ å<Å æ<Æ\\\', side = 3)\\n', Rd_tag = 'RCODE'), 'Rd_tag');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr8() {
        assertEval("argv <- list(structure(c(72.8922646699554, 72.8922646699554, 72.8922646699554, 72.8922646699554, 72.8922646699554, 99.6344113579063, 99.6344113579063, 122.561298550713, 122.561298550713, 122.561298550713, 122.561298550713, 122.561298550713, 122.561298550713, 140.590935258431, 140.590935258431, 140.590935258431, 153.976908924618, 153.976908924618, 153.976908924618, 153.976908924618, 163.542360717164, 163.542360717164, 163.542360717164, 163.542360717164, 163.542360717164, 163.542360717164, 170.206309375934, 170.206309375934, 170.206309375934, 170.206309375934, 170.206309375934, 170.206309375934, 170.206309375934, 170.206309375934, 170.206309375934, 174.771283819687, 174.771283819687, 174.771283819687, 174.771283819687, 174.771283819687, 174.771283819687, 174.771283819687, 174.771283819687, 174.771283819687, 174.771283819687, 177.863643456281, 177.863643456281, 177.863643456281, 177.863643456281, 177.863643456281, 181.334266157228, 182.262171017221), gradient = structure(c(0, 0, 0, 0, 0, 57.4245712142609, 57.4245712142609, 92.0663000396056, 92.0663000396056, 92.0663000396056, 92.0663000396056, 92.0663000396056, 92.0663000396056, 102.603355837019, 102.603355837019, 102.603355837019, 96.1864486469068, 96.1864486469068, 96.1864486469068, 96.1864486469068, 80.9127637202515, 80.9127637202515, 80.9127637202515, 80.9127637202515, 80.9127637202515, 80.9127637202515, 62.7567863240641, 62.7567863240641, 62.7567863240641, 62.7567863240641, 62.7567863240641, 62.7567863240641, 62.7567863240641, 62.7567863240641, 62.7567863240641, 45.2165616754423, 45.2165616754423, 45.2165616754423, 45.2165616754423, 45.2165616754423, 45.2165616754423, 45.2165616754423, 45.2165616754423, 45.2165616754423, 45.2165616754423, 29.9327435335998, 29.9327435335998, 29.9327435335998, 29.9327435335998, 29.9327435335998, 7.54853144661603, 0, 0.999999995832131, 0.999999995832131, 0.999999995832131, 0.999999995832131, 0.999999995832131, 0.900752404767751, 0.900752404767751, 0.72805882604922, 0.72805882604922, 0.72805882604922, 0.72805882604922, 0.72805882604922, 0.72805882604922, 0.546325339398677, 0.546325339398677, 0.546325339398677, 0.388712482407919, 0.388712482407919, 0.388712482407919, 0.388712482407919, 0.265313345750221, 0.265313345750221, 0.265313345750221, 0.265313345750221, 0.265313345750221, 0.265313345750221, 0.174363797963106, 0.174363797963106, 0.174363797963106, 0.174363797963106, 0.174363797963106, 0.174363797963106, 0.174363797963106, 0.174363797963106, 0.174363797963106, 0.109797198736723, 0.109797198736723, 0.109797198736723, 0.109797198736723, 0.109797198736723, 0.109797198736723, 0.109797198736723, 0.109797198736723, 0.109797198736723, 0.109797198736723, 0.0650421524858363, 0.0650421524858363, 0.0650421524858363, 0.0650421524858363, 0.0650421524858363, 0.0138547080588812, -2.61666809427403e-08, 0, 0, 0, 0, 0, 0.186415683651876, 0.186415683651876, 0.381271875735367, 0.381271875735367, 0.381271875735367, 0.381271875735367, 0.381271875735367, 0.381271875735367, 0.552874143045278, 0.552874143045278, 0.552874143045278, 0.689351882872836, 0.689351882872836, 0.689351882872836, 0.689351882872836, 0.791184856858009, 0.791184856858009, 0.791184856858009, 0.791184856858009, 0.791184856858009, 0.791184856858009, 0.864120807233518, 0.864120807233518, 0.864120807233518, 0.864120807233518, 0.864120807233518, 0.864120807233518, 0.864120807233518, 0.864120807233518, 0.864120807233518, 0.914989166321596, 0.914989166321596, 0.914989166321596, 0.914989166321596, 0.914989166321596, 0.914989166321596, 0.914989166321596, 0.914989166321596, 0.914989166321596, 0.914989166321596, 0.949854646587551, 0.949854646587551, 0.949854646587551, 0.949854646587551, 0.949854646587551, 0.989368022459573, 0.999999988297389), .Dim = c(52L, 3L))), 'gradient');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr9() {
        assertEval("argv <- list(structure(list(Fr = c(32, 53, 10, 3, 11, 50, 10, 30, 10, 25, 7, 5, 3, 15, 7, 8, 36, 66, 16, 4, 9, 34, 7, 64, 5, 29, 7, 5, 2, 14, 7, 8), Hair = structure(c(1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L), .Label = c('Black', 'Brown', 'Red', 'Blond'), class = 'factor'), Eye = structure(c(1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 3L, 3L, 3L, 3L, 4L, 4L, 4L, 4L), .Label = c('Brown', 'Blue', 'Hazel', 'Green'), class = 'factor'), Sex = structure(c(1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L), .Label = c('Male', 'Female'), class = 'factor')), .Names = c('Fr', 'Hair', 'Eye', 'Sex'), terms = quote(Fr ~ (Hair + Eye + Sex)^2), row.names = c(NA, 32L), class = 'data.frame'), 'terms');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr10() {
        assertEval(Output.IgnoreWhitespace,
                        "argv <- list(structure(list(Employed = c(60.323, 61.122, 60.171, 61.187, 63.221, 63.639, 64.989, 63.761, 66.019, 67.857, 68.169, 66.513, 68.655, 69.564, 69.331, 70.551), GNP.deflator = c(83, 88.5, 88.2, 89.5, 96.2, 98.1, 99, 100, 101.2, 104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9), GNP = c(234.289, 259.426, 258.054, 284.599, 328.975, 346.999, 365.385, 363.112, 397.469, 419.18, 442.769, 444.546, 482.704, 502.601, 518.173, 554.894), Unemployed = c(235.6, 232.5, 368.2, 335.1, 209.9, 193.2, 187, 357.8, 290.4, 282.2, 293.6, 468.1, 381.3, 393.1, 480.6, 400.7), Armed.Forces = c(159, 145.6, 161.6, 165, 309.9, 359.4, 354.7, 335, 304.8, 285.7, 279.8, 263.7, 255.2, 251.4, 257.2, 282.7), Population = c(107.608, 108.632, 109.773, 110.929, 112.075, 113.27, 115.094, 116.219, 117.388, 118.734, 120.445, 121.95, 123.366, 125.368, 127.852, 130.081), Year = 1947:1962), .Names = c('Employed', 'GNP.deflator', 'GNP', 'Unemployed', 'Armed.Forces', 'Population', 'Year'), terms = quote(Employed ~ GNP.deflator + GNP + Unemployed +     Armed.Forces + Population + Year), row.names = 1947:1962, class = 'data.frame'), 'terms');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr11() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0), .Dim = c(12L, 3L), .Dimnames = list(c('1', '3', '5', '7', '9', '11', '13', '15', '17', '19', '21', '23'), c('(Intercept)', 'M.userY', 'TempLow')), assign = 0:2, contrasts = structure(list(M.user = 'contr.treatment', Temp = 'contr.treatment'), .Names = c('M.user', 'Temp'))), 'contrasts');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr12() {
        assertEval("argv <- list(structure(list(`1000/MPG.city` = c(40, 55.5555555555556, 50, 52.6315789473684, 45.4545454545455, 45.4545454545455, 52.6315789473684, 62.5, 52.6315789473684, 62.5, 62.5, 40, 40, 52.6315789473684, 47.6190476190476, 55.5555555555556, 66.6666666666667, 58.8235294117647, 58.8235294117647, 50, 43.4782608695652, 50, 34.4827586206897, 43.4782608695652, 45.4545454545455, 58.8235294117647, 47.6190476190476, 55.5555555555556, 34.4827586206897, 50, 32.258064516129, 43.4782608695652, 45.4545454545455, 45.4545454545455, 41.6666666666667, 66.6666666666667, 47.6190476190476, 55.5555555555556, 21.7391304347826, 33.3333333333333, 41.6666666666667, 23.8095238095238, 41.6666666666667, 34.4827586206897, 45.4545454545455, 38.4615384615385, 50, 58.8235294117647, 55.5555555555556, 55.5555555555556, 58.8235294117647, 55.5555555555556, 34.4827586206897, 35.7142857142857, 38.4615384615385, 55.5555555555556, 58.8235294117647, 50, 52.6315789473684, 43.4782608695652, 52.6315789473684, 34.4827586206897, 55.5555555555556, 34.4827586206897, 41.6666666666667, 58.8235294117647, 47.6190476190476, 41.6666666666667, 43.4782608695652, 55.5555555555556, 52.6315789473684, 43.4782608695652, 32.258064516129, 43.4782608695652, 52.6315789473684, 52.6315789473684, 52.6315789473684, 50, 35.7142857142857, 30.3030303030303, 40, 43.4782608695652, 25.6410256410256, 31.25, 40, 45.4545454545455, 55.5555555555556, 40, 58.8235294117647, 47.6190476190476, 55.5555555555556, 47.6190476190476, 50), Weight = c(2705L, 3560L, 3375L, 3405L, 3640L, 2880L, 3470L, 4105L, 3495L, 3620L, 3935L, 2490L, 2785L, 3240L, 3195L, 3715L, 4025L, 3910L, 3380L, 3515L, 3085L, 3570L, 2270L, 2670L, 2970L, 3705L, 3080L, 3805L, 2295L, 3490L, 1845L, 2530L, 2690L, 2850L, 2710L, 3735L, 3325L, 3950L, 1695L, 2475L, 2865L, 2350L, 3040L, 2345L, 2620L, 2285L, 2885L, 4000L, 3510L, 3515L, 3695L, 4055L, 2325L, 2440L, 2970L, 3735L, 2895L, 2920L, 3525L, 2450L, 3610L, 2295L, 3730L, 2545L, 3050L, 4100L, 3200L, 2910L, 2890L, 3715L, 3470L, 2640L, 2350L, 2575L, 3240L, 3450L, 3495L, 2775L, 2495L, 2045L, 2490L, 3085L, 1965L, 2055L, 2950L, 3030L, 3785L, 2240L, 3960L, 2985L, 2810L, 2985L, 3245L), Cylinders = structure(c(2L, 4L, 4L, 4L, 2L, 2L, 4L, 4L, 4L, 5L, 5L, 2L, 2L, 4L, 2L, 4L, 4L, 5L, 5L, 4L, 2L, 4L, 2L, 2L, 2L, 4L, 2L, 4L, 2L, 4L, 2L, 2L, 2L, 2L, 2L, 4L, 4L, 5L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 5L, 4L, 4L, 4L, 5L, 2L, 2L, 2L, 4L, 6L, 2L, 4L, 2L, 4L, 2L, 4L, 2L, 2L, 4L, 4L, 2L, 2L, 4L, 4L, 2L, 2L, 2L, 4L, 4L, 4L, 2L, 2L, 1L, 2L, 2L, 1L, 2L, 2L, 2L, 2L, 2L, 3L, 2L, 4L, 2L, 3L), .Label = c('3', '4', '5', '6', '8', 'rotary'), class = 'factor'), Type = structure(c(4L, 3L, 1L, 3L, 3L, 3L, 2L, 2L, 3L, 2L, 3L, 1L, 1L, 5L, 3L, 6L, 6L, 2L, 5L, 2L, 1L, 2L, 4L, 4L, 1L, 6L, 3L, 5L, 4L, 2L, 4L, 4L, 1L, 5L, 5L, 6L, 3L, 2L, 4L, 5L, 5L, 4L, 1L, 4L, 4L, 5L, 3L, 3L, 3L, 3L, 3L, 2L, 4L, 4L, 1L, 6L, 5L, 1L, 3L, 5L, 3L, 4L, 3L, 4L, 1L, 6L, 3L, 1L, 3L, 6L, 2L, 5L, 4L, 1L, 5L, 3L, 2L, 1L, 4L, 4L, 4L, 1L, 4L, 4L, 5L, 3L, 6L, 4L, 6L, 1L, 5L, 1L, 3L), .Label = c('Compact', 'Large', 'Midsize', 'Small', 'Sporty', 'Van'), class = 'factor'), EngineSize = c(1.8, 3.2, 2.8, 2.8, 3.5, 2.2, 3.8, 5.7, 3.8, 4.9, 4.6, 2.2, 2.2, 3.4, 2.2, 3.8, 4.3, 5, 5.7, 3.3, 3, 3.3, 1.5, 2.2, 2.5, 3, 2.5, 3, 1.5, 3.5, 1.3, 1.8, 2.3, 2.3, 2, 3, 3, 4.6, 1, 1.6, 2.3, 1.5, 2.2, 1.5, 1.8, 1.5, 2, 4.5, 3, 3, 3.8, 4.6, 1.6, 1.8, 2.5, 3, 1.3, 2.3, 3.2, 1.6, 3.8, 1.5, 3, 1.6, 2.4, 3, 3, 2.3, 2.2, 3.8, 3.8, 1.8, 1.6, 2, 3.4, 3.4, 3.8, 2.1, 1.9, 1.2, 1.8, 2.2, 1.3, 1.5, 2.2, 2.2, 2.4, 1.8, 2.5, 2, 2.8, 2.3, 2.4), DriveTrain = structure(c(2L, 2L, 2L, 2L, 3L, 2L, 2L, 3L, 2L, 2L, 2L, 2L, 2L, 3L, 2L, 2L, 1L, 3L, 3L, 2L, 2L, 2L, 2L, 2L, 2L, 1L, 2L, 1L, 2L, 2L, 2L, 2L, 2L, 3L, 2L, 1L, 2L, 3L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 2L, 3L, 2L, 3L, 2L, 2L, 2L, 1L, 3L, 3L, 3L, 2L, 3L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 3L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 2L, 2L, 3L, 2L), .Label = c('4WD', 'Front', 'Rear'), class = 'factor')), .Names = c('1000/MPG.city', 'Weight', 'Cylinders', 'Type', 'EngineSize', 'DriveTrain'), terms = quote(1000/MPG.city ~ Weight + Cylinders + Type + EngineSize + DriveTrain), row.names = c(NA, 93L), class = 'data.frame'), 'row.names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr13() {
        assertEval("argv <- list(quote(cbind(X, M) ~ 1), 'term.labels');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr14() {
        assertEval("argv <- list(structure(c(1L, 1L, 1L, 2L, 1L), .Label = c('no', 'yes'), class = 'factor'), 'levels');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr15() {
        assertEval("argv <- list(structure(list(Df = c(NA, 1), Deviance = c(5.65604443125997, 8.44399377410362), AIC = c(71.3540021461976, 72.1419514890413)), .Names = c('Df', 'Deviance', 'AIC'), row.names = c('<none>', '- M.user:Temp'), class = c('anova', 'data.frame'), heading = c('Single term deletions', '\\nModel:', 'cbind(X, M) ~ M.user + Temp + M.user:Temp')), 'row.names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr16() {
        assertEval("argv <- list(structure(list(`1000/MPG.city` = c(40, 55.5555555555556, 50, 52.6315789473684, 45.4545454545455, 45.4545454545455, 52.6315789473684, 62.5, 52.6315789473684, 62.5, 62.5, 40, 40, 52.6315789473684, 47.6190476190476, 55.5555555555556, 66.6666666666667, 58.8235294117647, 58.8235294117647, 50, 43.4782608695652, 50, 34.4827586206897, 43.4782608695652, 45.4545454545455, 58.8235294117647, 47.6190476190476, 55.5555555555556, 34.4827586206897, 50, 32.258064516129, 43.4782608695652, 45.4545454545455, 45.4545454545455, 41.6666666666667, 66.6666666666667, 47.6190476190476, 55.5555555555556, 21.7391304347826, 33.3333333333333, 41.6666666666667, 23.8095238095238, 41.6666666666667, 34.4827586206897, 45.4545454545455, 38.4615384615385, 50, 58.8235294117647, 55.5555555555556, 55.5555555555556, 58.8235294117647, 55.5555555555556, 34.4827586206897, 35.7142857142857, 38.4615384615385, 55.5555555555556, 58.8235294117647, 50, 52.6315789473684, 43.4782608695652, 52.6315789473684, 34.4827586206897, 55.5555555555556, 34.4827586206897, 41.6666666666667, 58.8235294117647, 47.6190476190476, 41.6666666666667, 43.4782608695652, 55.5555555555556, 52.6315789473684, 43.4782608695652, 32.258064516129, 43.4782608695652, 52.6315789473684, 52.6315789473684, 52.6315789473684, 50, 35.7142857142857, 30.3030303030303, 40, 43.4782608695652, 25.6410256410256, 31.25, 40, 45.4545454545455, 55.5555555555556, 40, 58.8235294117647, 47.6190476190476, 55.5555555555556, 47.6190476190476, 50), Weight = c(2705L, 3560L, 3375L, 3405L, 3640L, 2880L, 3470L, 4105L, 3495L, 3620L, 3935L, 2490L, 2785L, 3240L, 3195L, 3715L, 4025L, 3910L, 3380L, 3515L, 3085L, 3570L, 2270L, 2670L, 2970L, 3705L, 3080L, 3805L, 2295L, 3490L, 1845L, 2530L, 2690L, 2850L, 2710L, 3735L, 3325L, 3950L, 1695L, 2475L, 2865L, 2350L, 3040L, 2345L, 2620L, 2285L, 2885L, 4000L, 3510L, 3515L, 3695L, 4055L, 2325L, 2440L, 2970L, 3735L, 2895L, 2920L, 3525L, 2450L, 3610L, 2295L, 3730L, 2545L, 3050L, 4100L, 3200L, 2910L, 2890L, 3715L, 3470L, 2640L, 2350L, 2575L, 3240L, 3450L, 3495L, 2775L, 2495L, 2045L, 2490L, 3085L, 1965L, 2055L, 2950L, 3030L, 3785L, 2240L, 3960L, 2985L, 2810L, 2985L, 3245L), Cylinders = structure(c(2L, 4L, 4L, 4L, 2L, 2L, 4L, 4L, 4L, 5L, 5L, 2L, 2L, 4L, 2L, 4L, 4L, 5L, 5L, 4L, 2L, 4L, 2L, 2L, 2L, 4L, 2L, 4L, 2L, 4L, 2L, 2L, 2L, 2L, 2L, 4L, 4L, 5L, 1L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 5L, 4L, 4L, 4L, 5L, 2L, 2L, 2L, 4L, 6L, 2L, 4L, 2L, 4L, 2L, 4L, 2L, 2L, 4L, 4L, 2L, 2L, 4L, 4L, 2L, 2L, 2L, 4L, 4L, 4L, 2L, 2L, 1L, 2L, 2L, 1L, 2L, 2L, 2L, 2L, 2L, 3L, 2L, 4L, 2L, 3L), .Label = c('3', '4', '5', '6', '8', 'rotary'), class = 'factor'), Type = structure(c(4L, 3L, 1L, 3L, 3L, 3L, 2L, 2L, 3L, 2L, 3L, 1L, 1L, 5L, 3L, 6L, 6L, 2L, 5L, 2L, 1L, 2L, 4L, 4L, 1L, 6L, 3L, 5L, 4L, 2L, 4L, 4L, 1L, 5L, 5L, 6L, 3L, 2L, 4L, 5L, 5L, 4L, 1L, 4L, 4L, 5L, 3L, 3L, 3L, 3L, 3L, 2L, 4L, 4L, 1L, 6L, 5L, 1L, 3L, 5L, 3L, 4L, 3L, 4L, 1L, 6L, 3L, 1L, 3L, 6L, 2L, 5L, 4L, 1L, 5L, 3L, 2L, 1L, 4L, 4L, 4L, 1L, 4L, 4L, 5L, 3L, 6L, 4L, 6L, 1L, 5L, 1L, 3L), .Label = c('Compact', 'Large', 'Midsize', 'Small', 'Sporty', 'Van'), class = 'factor'), EngineSize = c(1.8, 3.2, 2.8, 2.8, 3.5, 2.2, 3.8, 5.7, 3.8, 4.9, 4.6, 2.2, 2.2, 3.4, 2.2, 3.8, 4.3, 5, 5.7, 3.3, 3, 3.3, 1.5, 2.2, 2.5, 3, 2.5, 3, 1.5, 3.5, 1.3, 1.8, 2.3, 2.3, 2, 3, 3, 4.6, 1, 1.6, 2.3, 1.5, 2.2, 1.5, 1.8, 1.5, 2, 4.5, 3, 3, 3.8, 4.6, 1.6, 1.8, 2.5, 3, 1.3, 2.3, 3.2, 1.6, 3.8, 1.5, 3, 1.6, 2.4, 3, 3, 2.3, 2.2, 3.8, 3.8, 1.8, 1.6, 2, 3.4, 3.4, 3.8, 2.1, 1.9, 1.2, 1.8, 2.2, 1.3, 1.5, 2.2, 2.2, 2.4, 1.8, 2.5, 2, 2.8, 2.3, 2.4), DriveTrain = structure(c(2L, 2L, 2L, 2L, 3L, 2L, 2L, 3L, 2L, 2L, 2L, 2L, 2L, 3L, 2L, 2L, 1L, 3L, 3L, 2L, 2L, 2L, 2L, 2L, 2L, 1L, 2L, 1L, 2L, 2L, 2L, 2L, 2L, 3L, 2L, 1L, 2L, 3L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 3L, 2L, 3L, 2L, 3L, 2L, 2L, 2L, 1L, 3L, 3L, 3L, 2L, 3L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 3L, 2L, 2L, 2L, 2L, 1L, 1L, 1L, 2L, 2L, 2L, 2L, 1L, 2L, 2L, 2L, 2L, 3L, 2L), .Label = c('4WD', 'Front', 'Rear'), class = 'factor')), .Names = c('1000/MPG.city', 'Weight', 'Cylinders', 'Type', 'EngineSize', 'DriveTrain'), terms = quote(1000/MPG.city ~ Weight + Cylinders + Type + EngineSize + DriveTrain), row.names = c(NA, 93L), class = 'data.frame'), 'na.action');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr17() {
        assertEval("argv <- list(structure(c(0.5, 0.5, 0.5, 0.5), gradient = structure(c(NaN, NaN, NaN, NaN), .Dim = c(4L, 1L), .Dimnames = list(NULL, 'L75'))), 'gradient');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr18() {
        assertEval("argv <- list(c(NA, NA, '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/histogram.R', '/home/lzhao/tmp/RtmpGUHe0I/R.INSTALL2aa51f3e9d31/lattice/R/histogram.R'), 'levels');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr19() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats', '/home/lzhao/hg/r-instrumented/library/stats/R/stats'), frow = c(2418L, 2418L, 2418L, 2421L, 2422L, 2424L), lrow = c(2418L, 2418L, 2418L, 2421L, 2426L, 2424L)), .Names = c('srcfile', 'frow', 'lrow'), row.names = c(NA, 6L), class = 'data.frame'), 'row.names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr20() {
        assertEval("argv <- list(structure(c('[[.bibentry', '[[.data.frame', '[[.Date', '[[.dendrogram', '[[.factor', '[[.numeric_version', '[[.pdf_doc', '[[.person', '[[.POSIXct'), class = 'MethodsFunction', info = structure(list(visible = c(FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, TRUE), from = structure(c(9L, 2L, 2L, 9L, 2L, 2L, 9L, 9L, 2L), .Label = c('CheckExEnv', 'package:base', 'package:datasets', 'package:graphics', 'package:grDevices', 'package:methods', 'package:stats', 'package:utils', 'registered S3method for [['), class = 'factor')), .Names = c('visible', 'from'), row.names = c('[[.bibentry', '[[.data.frame', '[[.Date', '[[.dendrogram', '[[.factor', '[[.numeric_version', '[[.pdf_doc', '[[.person', '[[.POSIXct'), class = 'data.frame')), 'info');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr21() {
        assertEval("argv <- list(structure(list(ID = c(63L, 63L, 63L, 63L, 63L), Age = c(30L, 30L, 30L, 30L, 30L), OME = structure(c(3L, 3L, 3L, 3L, 3L), .Label = c('N/A', 'high', 'low'), class = 'factor'), Loud = c(35L, 40L, 45L, 50L, 55L), Noise = structure(c(2L, 2L, 2L, 2L, 2L), .Label = c('coherent', 'incoherent'), class = 'factor'), Correct = c(1L, 1L, 1L, 3L, 1L), Trials = c(2L, 1L, 1L, 3L, 1L), UID = c(67L, 67L, 67L, 67L, 67L), UIDn = c(67.1, 67.1, 67.1, 67.1, 67.1)), .Names = c('ID', 'Age', 'OME', 'Loud', 'Noise', 'Correct', 'Trials', 'UID', 'UIDn'), row.names = c(635L, 639L, 643L, 647L, 651L), class = 'data.frame'), 'na.action');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr22() {
        assertEval("argv <- list(structure(c(0, 3, 3, 0), .Dim = c(2L, 2L), counts = structure(c(0L, 0L, 1L, 0L, 0L, 1L, 0L, 0L, 0L, 2L, 2L, 0L), .Dim = c(2L, 2L, 3L), .Dimnames = list(NULL, NULL, c('ins', 'del', 'sub'))), trafos = structure(c('MMMMMM', 'SMMMSMD', 'SMMMSMI', 'MMMMMMM'), .Dim = c(2L, 2L))), 'trafos');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr23() {
        assertEval("argv <- list(structure(list(y = c(73, 73, 70, 74, 75, 115, 105, 107, 124, 107, 116, 125, 102, 144, 178, 149, 177, 124, 157, 128, 169, 165, 186, 152, 181, 139, 173, 151, 138, 181, 152, 188, 173, 196, 180, 171, 188, 174, 198, 172, 176, 162, 188, 182, 182, 141, 191, 190, 159, 170, 163, 197), x = c(1, 1, 1, 1, 1, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 9, 9, 9, 9, 9, 11, 12), Ta = 1, Tb = 12), .Names = c('y', 'x', 'Ta', 'Tb'), terms = quote(~y +     x)), 'terms');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr24() {
        assertEval("argv <- list(structure(c(804.851443135267, 3.3994157758076, 28.3699038266834, 1.84375046462573), .Dim = c(2L, 2L), .Dimnames = list(c('(Intercept)', 'day'), c('Variance', 'StdDev')), formStr = 'pdLogChol(day)', corr = structure(c('(Intr)', '-0.555'), .Dim = c(2L, 1L), .Dimnames = list(c('(Intercept)', 'day'), 'Corr'))), which = 'corr');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr25() {
        assertEval("argv <- list(structure(c(0.0319339476375948, 0.0319339476375948, 0.114405269727263, 0.114405269727263, 0.211060625790557, 0.211060625790557, 0.375868556643391, 0.375868556643391, 0.631987388405009, 0.631987388405009, 0.977771587733117, 0.977771587733117, 1.3655287091085, 1.3655287091085, 1.71941659701549, 1.71941659701549), gradient = structure(c(0.013379193963099, 0.013379193963099, 0.0479317593757097, 0.0479317593757097, 0.0884269330704518, 0.0884269330704518, 0.157475623779182, 0.157475623779182, 0.264780350605592, 0.264780350605592, 0.409651693312349, 0.409651693312349, 0.572108204994814, 0.572108204994814, 0.720374706438093, 0.720374706438093, -0.0298119721009501, -0.0298119721009501, -0.103062799493893, -0.103062799493893, -0.182048260790464, -0.182048260790464, -0.299644491810901, -0.299644491810901, -0.439656344689613, -0.439656344689613, -0.546177195068236, -0.546177195068236, -0.552869486814534, -0.552869486814534, -0.454930857067767, -0.454930857067767), .Dim = c(16L, 2L), .Dimnames = list(NULL, c('Asym', 'xmid')))), 'gradient');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr26() {
        assertEval("argv <- list(structure(list(structure(list(structure('Update varFunc Object', Rd_tag = 'TEXT')), Rd_tag = '\\\\title'), structure(list(structure('update.varFunc', Rd_tag = 'VERB')), Rd_tag = '\\\\name'), structure(list(structure('update.varExp', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('update.varFunc', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('update.varComb', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('update.varConstPower', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'),     structure(list(structure('update.varExpon', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('update.varPower', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('models', Rd_tag = 'TEXT')), Rd_tag = '\\\\keyword'), structure(list(structure('\\n', Rd_tag = 'TEXT'), structure('  If the ', Rd_tag = 'TEXT'), structure(list(structure('formula(object)', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' includes a ', Rd_tag = 'TEXT'), structure(list(structure('\\\'.\\\'', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'),         structure(' term, representing\\n', Rd_tag = 'TEXT'), structure('  a fitted object, the variance covariate needs to be updated upon\\n', Rd_tag = 'TEXT'), structure('  completion of an optimization cycle (in which the variance function\\n', Rd_tag = 'TEXT'), structure('  weights are kept fixed). This method function allows a reevaluation of\\n', Rd_tag = 'TEXT'), structure('  the variance covariate using the current fitted object and,\\n', Rd_tag = 'TEXT'), structure('  optionally, other variables in the original data.\\n', Rd_tag = 'TEXT')), Rd_tag = '\\\\description'),     structure(list(structure('\\n', Rd_tag = 'RCODE'), structure(list(list(structure('update', Rd_tag = 'TEXT')), list(structure('varFunc', Rd_tag = 'TEXT'))), Rd_tag = '\\\\method'), structure('(object, data, ', Rd_tag = 'RCODE'), structure(list(), Rd_tag = '\\\\dots'), structure(')\\n', Rd_tag = 'RCODE')), Rd_tag = '\\\\usage'), structure(list(structure('\\n', Rd_tag = 'TEXT'), structure('  ', Rd_tag = 'TEXT'), structure(list(list(structure('object', Rd_tag = 'TEXT')), list(structure('an object inheriting from class ', Rd_tag = 'TEXT'),         structure(list(structure('varFunc', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(',\\n', Rd_tag = 'TEXT'), structure('    representing a variance function structure.', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure(' \\n', Rd_tag = 'TEXT'), structure('  ', Rd_tag = 'TEXT'), structure(list(list(structure('data', Rd_tag = 'TEXT')), list(structure('a list with a component named ', Rd_tag = 'TEXT'), structure(list(structure('\\\'.\\\'', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' with the current\\n', Rd_tag = 'TEXT'),         structure('    version of the fitted object (from which fitted values, coefficients,\\n', Rd_tag = 'TEXT'), structure('    and residuals can be extracted) and, if necessary, other variables\\n', Rd_tag = 'TEXT'), structure('    used to evaluate the variance covariate(s).', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure('\\n', Rd_tag = 'TEXT'), structure(' ', Rd_tag = 'TEXT'), structure(list(list(structure(list(), Rd_tag = '\\\\dots')), list(structure('some methods for this generic require additional\\n', Rd_tag = 'TEXT'),         structure('    arguments.  None are used in this method.', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure(' \\n', Rd_tag = 'TEXT')), Rd_tag = '\\\\arguments'), structure(list(structure('\\n', Rd_tag = 'TEXT'), structure('  if ', Rd_tag = 'TEXT'), structure(list(structure('formula(object)', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' includes a ', Rd_tag = 'TEXT'), structure(list(structure('\\\'.\\\'', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' term, an\\n', Rd_tag = 'TEXT'), structure('  ', Rd_tag = 'TEXT'),         structure(list(structure('varFunc', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' object similar to ', Rd_tag = 'TEXT'), structure(list(structure('object', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(', but with the \\n', Rd_tag = 'TEXT'), structure('  variance covariate reevaluated at the current fitted object value;\\n', Rd_tag = 'TEXT'), structure('  else ', Rd_tag = 'TEXT'), structure(list(structure('object', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' is returned unchanged.\\n', Rd_tag = 'TEXT')), Rd_tag = '\\\\value'),     structure(list(structure('José Pinheiro and Douglas Bates ', Rd_tag = 'TEXT'), structure(list(structure('bates@stat.wisc.edu', Rd_tag = 'TEXT')), Rd_tag = '\\\\email')), Rd_tag = '\\\\author'), structure(list(structure(list(structure(list(structure('needUpdate', Rd_tag = 'TEXT')), Rd_tag = '\\\\link')), Rd_tag = '\\\\code'), structure(',\\n', Rd_tag = 'TEXT'), structure('  ', Rd_tag = 'TEXT'), structure(list(structure(list(structure('covariate<-.varFunc', Rd_tag = 'TEXT')), Rd_tag = '\\\\link')), Rd_tag = '\\\\code'),         structure('\\n', Rd_tag = 'TEXT')), Rd_tag = '\\\\seealso')), Rdfile = '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/man/update.varFunc.Rd', class = 'Rd', meta = structure(list(docType = character(0)), .Names = 'docType'), prepared = 3L), 'prepared');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr27() {
        assertEval("argv <- list(structure(list(structure(list(structure('Print a varFunc Object', Rd_tag = 'TEXT')), Rd_tag = '\\\\title'), structure(list(structure('print.varFunc', Rd_tag = 'VERB')), Rd_tag = '\\\\name'), structure(list(structure('print.varFunc', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('print.varComb', Rd_tag = 'VERB')), Rd_tag = '\\\\alias'), structure(list(structure('models', Rd_tag = 'TEXT')), Rd_tag = '\\\\keyword'), structure(list(structure('\\n', Rd_tag = 'TEXT'), structure('  The class and the coefficients associated with ', Rd_tag = 'TEXT'),     structure(list(structure('x', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(' are printed.\\n', Rd_tag = 'TEXT')), Rd_tag = '\\\\description'), structure(list(structure('\\n', Rd_tag = 'RCODE'), structure(list(list(structure('print', Rd_tag = 'TEXT')), list(structure('varFunc', Rd_tag = 'TEXT'))), Rd_tag = '\\\\method'), structure('(x, ', Rd_tag = 'RCODE'), structure(list(), Rd_tag = '\\\\dots'), structure(')\\n', Rd_tag = 'RCODE')), Rd_tag = '\\\\usage'), structure(list(structure('\\n', Rd_tag = 'TEXT'),     structure(' ', Rd_tag = 'TEXT'), structure(list(list(structure('x', Rd_tag = 'TEXT')), list(structure('an object inheriting from class ', Rd_tag = 'TEXT'), structure(list(structure('varFunc', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure(', representing a\\n', Rd_tag = 'TEXT'), structure(' variance function structure.', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure('\\n', Rd_tag = 'TEXT'), structure(' ', Rd_tag = 'TEXT'), structure(list(list(structure(list(), Rd_tag = '\\\\dots')), list(structure('optional arguments passed to ', Rd_tag = 'TEXT'),         structure(list(structure('print.default', Rd_tag = 'RCODE')), Rd_tag = '\\\\code'), structure('; see\\n', Rd_tag = 'TEXT'), structure('   the documentation on that method function.', Rd_tag = 'TEXT'))), Rd_tag = '\\\\item'), structure('\\n', Rd_tag = 'TEXT')), Rd_tag = '\\\\arguments'), structure(list(structure('Jos<U+00E9> Pinheiro and Douglas Bates ', Rd_tag = 'TEXT'), structure(list(structure('bates@stat.wisc.edu', Rd_tag = 'TEXT')), Rd_tag = '\\\\email')), Rd_tag = '\\\\author'), structure(list(structure(list(    structure(list(structure('summary.varFunc', Rd_tag = 'TEXT')), Rd_tag = '\\\\link')), Rd_tag = '\\\\code')), Rd_tag = '\\\\seealso'), structure(list(structure('\\n', Rd_tag = 'RCODE'), structure('vf1 <- varPower(0.3, form = ~age)\\n', Rd_tag = 'RCODE'), structure('vf1 <- Initialize(vf1, Orthodont)\\n', Rd_tag = 'RCODE'), structure('print(vf1)\\n', Rd_tag = 'RCODE')), Rd_tag = '\\\\examples')), Rdfile = '/home/lzhao/tmp/RtmptukZK0/R.INSTALL2ccc3a5cba55/nlme/man/print.varFunc.Rd', class = 'Rd', meta = structure(list(    docType = character(0)), .Names = 'docType'), prepared = 3L), 'meta');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr28() {
        assertEval("argv <- list(structure(list(variable1 = c(1, 2, 2), variable2 = c(1, 1, 3)), .Names = c('variable1', 'variable2'), row.names = c(NA, -3L), class = 'data.frame', variable.labels = structure(c('variable1', 'variable2'), .Names = c('variable1', 'variable2')), codepage = 20127L), 'variable.labels');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr29() {
        assertEval("argv <- list(structure(list(A = c(1L, NA, 1L), B = c(1.1, NA, 2), C = c(1.1+0i, NA, 3+0i), D = c(NA, NA, NA), E = c(FALSE, NA, TRUE), F = structure(c(1L, NA, 2L), .Label = c('abc', 'def'), class = 'factor')), .Names = c('A', 'B', 'C', 'D', 'E', 'F'), class = 'data.frame', row.names = c('1', '2', '3')), 'row.names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr30() {
        assertEval("argv <- list(structure(list(srcfile = c('/home/lzhao/hg/r-instrumented/library/graphics/R/graphics', '/home/lzhao/hg/r-instrumented/library/graphics/R/graphics'), frow = 4262:4263, lrow = 4262:4263), .Names = c('srcfile', 'frow', 'lrow'), row.names = 1:2, class = 'data.frame'), 'row.names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr31() {
        assertEval("argv <- list(structure(c(2L, 1L, 3L), .Label = c('1', '2', NA), class = c('ordered', 'factor')), 'levels');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr32() {
        assertEval("argv <- list(structure(1:10, date = structure(200171400, class = c('POSIXct', 'POSIXt'), tzone = ''), class = 'stamped'), 'date');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr33() {
        assertEval("argv <- list(structure(c(49.9, 52.3, 49.4, 51.1, 49.4, 47.9, 49.8, 50.9, 49.3, 51.9, 50.8, 49.6, 49.3, 50.6, 48.4, 50.7, 50.9, 50.6, 51.5, 52.8, 51.8, 51.1, 49.8, 50.2, 50.4, 51.6, 51.8, 50.9, 48.8, 51.7, 51, 50.6, 51.7, 51.5, 52.1, 51.3, 51, 54, 51.4, 52.7, 53.1, 54.6, 52, 52, 50.9, 52.6, 50.2, 52.6, 51.6, 51.9, 50.5, 50.9, 51.7), .Tsp = c(1, 53, 1)), 'tsp');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr34() {
        assertEval("argv <- list(structure(list(y = c(-0.0561287395290008, -0.155795506705329, -1.47075238389927, -0.47815005510862, 0.417941560199702, 1.35867955152904, -0.102787727342996, 0.387671611559369, -0.0538050405829051, -1.37705955682861), x = c(TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE, TRUE, FALSE), z = 1:10), .Names = c('y', 'x', 'z'), class = 'data.frame', row.names = c(NA, 10L), terms = quote(y ~ x * z)), 'row.names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr35() {
        assertEval("argv <- list(structure(c(123.48457192908, 239.059434652297, 290.055338401838, 18.397281603467, 6.57585722655537, 0.670931786731845, 0.178466148156965, 0.245410750178149, 0.363167328274208, 0.194808268742596, 2172.67583033103, 8.91763605923317e+38), .Dim = c(1L, 12L), .Dimnames = list(NULL, c('1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1'))), 'class');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr36() {
        assertEval("argv <- list(structure(c(8.85169533448293e-238, 2.77884205079773e-237, 8.5330427463164e-242, 7.89244209468013e-215, 6.74732964729372e-231, 1.30818670504849e-217, 1.39113376416096e-208, 1.35683278955814e-215, 7.74002099666521e-219, 3.64254537730231e-220, 6.75916981442421e-296, 0), .Dim = c(1L, 12L), .Dimnames = list(NULL, c('1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1'))), 'class');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr37() {
        assertEval("argv <- list(structure(c(0, 0, 0, 0, 0, 0, 1.90299264808673e-318, 0, 0, 0, 0, 0), .Dim = c(1L, 12L), .Dimnames = list(NULL, c('1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1'))), 'class');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr38() {
        assertEval("argv <- list(list(c(57, 95, 8, 69, 92, 90, 15, 2, 84, 6, 127, 36, 51, 2, 69, 71, 87, 72, 5, 39, 22, 16, 72, 4, 130, 4, 114, 9, 20, 24, 10, 51, 43, 28, 60, 5, 17, 7, 81, 71, 12, 29, 44, 77, 4, 27, 47, 76, 8, 72, 13, 57, 4, 81, 20, 61, 80, 114, 39, 14, 86, 55, 3, 19)), 'names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr39() {
        assertEval("argv <- list(structure(list(coef = c(0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099, 0.0099009900990099), m = 50L), .Names = c('coef', 'm'), name = 'Daniell(50)', class = 'tskernel'), 'name');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr40() {
        assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0.0519265581680438, 0.0688412118115477, 0.0907383424760159, 0.11871257874736, 0.153851867351129, 0.197066627437879, 0.248849916275927, 0.309003712276245, 0.376411716520019, 0.448970177554117, 0.523767544415284, 0.560977234455458), .Dim = c(12L, 2L), gradient = structure(c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -0.00738389705040961, -0.00961449255414965, -0.0123746759217436, -0.0156916432890164, -0.0195255505224529, -0.0237326760503604, -0.0280361473696848, -0.032025323792521, -0.035205879677649, -0.0371062207595128, -0.0374120660881388, -0.0369391076611127, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.142994094104604, 0.166962078687643, 0.190145150639427, 0.209729276540067, 0.221920901693929, 0.222272332097609, 0.206504915766748, 0.17183720786829, 0.11849125053358, 0.0506747206505475, -0.0237317230350864, -0.0603708169587119), .Dim = c(12L, 2L, 2L))), 'gradient');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr41() {
        assertEval("argv <- list(structure(list(A = 0:10, B = 10:20, `NA` = 20:30), .Names = c('A', 'B', NA), row.names = c(NA, -11L), class = 'data.frame'), 'row.names');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr42() {
        assertEval("argv <- list(structure(list(coef = c(0.1, 0.1, 0.1, 0.1, 0.1, 0.05), m = 5L), .Names = c('coef', 'm'), name = 'mDaniell(5)', class = 'tskernel'), 'name');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr43() {
        assertEval("argv <- list(structure(c(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), .Dim = c(10L, 2L), .Dimnames = list(NULL, c('tt', 'tt + 1')), .Tsp = c(1920.5, 1921.25, 12), class = c('mts', 'ts', 'matrix')), 'tsp');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr44() {
        assertEval("argv <- list(structure(c(2L, NA, NA, 4L, 3L, 2L, 1L, 5L, 5L, 6L), .Label = c('NA', 'a', 'b', 'c', 'd', NA), class = 'factor'), 'levels');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testattr45() {
        assertEval("argv <- list(c(35.2589338684655, 59.5005803666983, 12.4529321610302, 2.53579570262684, 10.370198579714, 42.0067149618146, 8.14319638132861, 34.0508943233725, 7.78517191057496, 26.9998965458032, 6.70435391953205, 3.62502215105156, 2.59277105754344, 14.4998960151485, 6.70435391953205, 5.8000097831969, 32.741875696675, 59.5015090627504, 13.5512565366133, 4.46460764999704, 9.62989278443572, 42.0073706103832, 8.86141045052292, 59.9511558158597, 7.22940551532861, 27.0003179651772, 7.29566488446303, 6.38233656214029, 2.40767880256155, 14.5001223322046, 7.29566488446303, 10.2116933242272), 'dim');attr(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testExactMatch() {
        assertEval("x <- c(1, 3); attr(x, 'abc') <- 42; attr(x, 'ab', exact=TRUE)");
        assertEval(Ignored.Unimplemented, "x <- c(1,2); attr(x, 'row.namess') <- 42; attr(x, 'row.names')");
    }
}
