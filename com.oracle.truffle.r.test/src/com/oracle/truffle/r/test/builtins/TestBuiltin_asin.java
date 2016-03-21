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
public class TestBuiltin_asin extends TestBase {

    @Test
    public void testasin1() {
        assertEval("argv <- list(c(0.185157057377868, 0.15968866445196, 0.190428414477965, 0.0799378829516562, 0.043979457119882, 0.0348843282121068, 0.0484793103572122, 0.109221220908651, 0.264364324223884, 0.211999913632203, 0.141157385938914, 0.143099951254224, 0.193270515700824, 0.217573738375349, 0.216954681783302, 0.291716047319384, 0.387858840434923, 0.461666520261029, 0.49992310387409, 0.421641232345205, 0.419256648241403, 0.442116045838704, 0.375354272383531, 0.416333612927645, 0.506835005179142, 0.408511923588378, 0.442160540547329, 0.59649385178332, 0.729919018318794, 0.811421169963513, 0.896290688103034, 0.752346465072037, 0.654905104838795, 0.821242494513718, 0.91715624670646, 0.885960209053628));asin(argv[[1]]);");
    }

    @Test
    public void testasin2() {
        assertEval("argv <- list(c(2+0i, 2-0.0001i, -2+0i, -2+0.0001i));asin(argv[[1]]);");
    }

    @Test
    public void testasin3() {
        assertEval("argv <- list(logical(0));asin(argv[[1]]);");
    }

    @Test
    public void testasin4() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));asin(argv[[1]]);");
    }

    @Test
    public void testasin5() {
        assertEval("argv <- list(c(0.34345+233i,-0.34345+0.3334i));asin(argv[[1]]);");
    }

    @Test
    public void testTrigExp() {
        assertEval("{ asin(0.4) }");
        assertEval("{ asin(c(0.3,0.6,0.9)) }");
        assertEval("{ asin() }");
        assertEval("{ asin(2+0i) }");
    }
}
