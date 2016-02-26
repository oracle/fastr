/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_lm extends TestBase {

    @Test
    public void testlm1() {
        assertEval(Ignored.Unknown, "require(stats); ctl <- c(4.17,5.58,5.18,6.11,4.50,4.61,5.17,4.53,5.33,5.14); trt <- c(4.81,4.17,4.41,3.59,5.87,3.83,6.03,4.89,4.32,4.69); "
                        + "group <- gl(2,10,20, labels=c(\"Ctl\",\"Trt\")); weight <- c(ctl, trt); lm(formula = weight ~ group)");
    }

    @Test
    public void testlm112() {
        assertEval(Ignored.Unknown, "require(stats);" + "lm(formula = weight ~ group - 1)");
    }

    @Test
    public void testlm223() {
        assertEval(Ignored.Unknown, "require(stats);" + "lm(formula = weight ~ group, method = 'model.frame')");
    }

    @Test
    public void testlm334() {
        assertEval(Ignored.Unknown, "require(stats);" + "lm(data = LifeCycleSavings, formula = sr ~ pop15 + pop75 + dpi + ddpi)");
    }

    @Test
    public void testlm445() {
        assertEval(Ignored.Unknown, "require(stats);" + "lm(data = attitude, formula = rating ~ .)");
    }

    @Test
    public void testlm875() {
        assertEval(Ignored.Unknown, "require(stats); lm(data = mtcars, formula = 100/mpg ~ disp + hp + wt + am)");
    }

    @Test
    public void testlm876() {
        assertEval(Ignored.Unknown, "require(stats); lm(data = npk, formula = yield ~ block + N * P * K, singular.ok = TRUE)");
    }

    @Test
    public void testlm877() {
        assertEval(Ignored.Unknown, "require(stats); lm(data = npk, formula = yield ~ block, method = 'qr', qr = TRUE, singular.ok = TRUE)");
    }

    @Test
    public void testlm879() {
        assertEval(Ignored.Unknown, "require(stats); lm(data = npk, formula = yield ~ N + P + K + N:P + N:K + P:K + N:P:K, method = 'model.frame', singular.ok = TRUE)");
    }

    @Test
    public void testlm880() {
        assertEval(Ignored.Unknown, "require(stats); lm(formula = y ~ x)");
    }

    @Test
    public void test() {
        assertEval(Ignored.Unknown,
                        "require(stats); lm(data = structure(list(y = c(43, 63, 71, 61, 81, 43, 58, 71, 72, 67, 64, 67, 69, 68, 77, 81, 74, 65, 65, 50, 50, 64, 53, 40, 63, 66, 82), x1 = c(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), x2 = c(51, 64, 70, 63, 78, 55, 67, 75, 82, 61, 53, 60, 62, 83, 77, 90, 85, 60, 70, 58, 40, 61, 66, 37, 54, 77, 82), x3 = c(30, 51, 68, 45, 56, 49, 42, 50, 72, 45, 53, 47, 57, 83, 54, 50, 64, 65, 46, 68, 33, 52, 52, 42, 42, 66, 39), x4 = c(47, 45, 48, 39, 57, 69, 66, 50, 59, 45, 72, 50, 59, 44, 75, 39, 45, 62, 47, 74, 75, 67, 47, 58, 54, 66, 62), x5 = c(61, 63, 76, 54, 71, 54, 66, 70, 71, 62, 58, 59, 55, 59, 79, 60, 79, 55, 75, 64, 43, 66, 63, 50, 66, 88, 64), x6 = c(92, 73, 86, 84, 83, 49, 68, 66, 83, 80, 67, 74, 63, 77, 77, 54, 79, 80, 85, 78, 64, 80, 80, 57, 75, 76, 78), x7 = c(45, 47, 48, 35, 47, 34, 35, 41, 31, 41, 34, 41, 25, 35, 46, 36, 63, 60, 46, 52, 33, 41, 37, 49, 33, 72, 39)), .Names = c('y', 'x1', 'x2', 'x3', 'x4', 'x5', 'x6', 'x7'), row.names = c('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23', '24', '25', '26', '30'), class = 'data.frame'), formula = y ~ . + 0)");
    }
}
