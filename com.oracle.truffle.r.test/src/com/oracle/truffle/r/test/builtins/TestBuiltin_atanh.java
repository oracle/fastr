/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_atanh extends TestBase {

    @Test
    public void testatanh1() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));atanh(argv[[1]]);");
    }

    @Test
    public void testatanh2() {
        assertEval("argv <- list(c(0.667355731370168, 0.757545006911892, 0.835315653735585, 0.984260691393257, 0.997724361757832, 0.998320334761744, 0.995857200407461, 0.978447456936914, 0.924289918318784, 0.794303899816803, 0.772144244057747, 0.886598050753707, 0.927287003572071, 0.862971883028345, 0.864426227271356, 0.927240697865085, 0.892356439729065, 0.753876685479294, 0.834371238466667, 0.856663357154979, 0.836217049107607, 0.820080611345367, 0.881122397467922, 0.964328668319385, 0.870112695225674, 0.897689370465451, 0.872889563044137, 0.716354206299899, 0.634385015212608, 0.77586178284932, 0.639202570327528, 0.710504816816848, 0.825388608284517, 0.812993921221196, 0.705406278672692, 0.577944207218662));atanh(argv[[1]]);");
    }

    @Test
    public void testatanh3() {
        assertEval("argv <- list(-0.133190890463189);atanh(argv[[1]]);");
    }

    @Test
    public void testatanh4() {
        assertEval("argv <- list(c(2+0i, 2-0.0001i, -2+0i, -2+0.0001i));atanh(argv[[1]]);");
    }
}
