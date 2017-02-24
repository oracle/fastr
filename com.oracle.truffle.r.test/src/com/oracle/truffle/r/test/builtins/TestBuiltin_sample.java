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
public class TestBuiltin_sample extends TestBase {

    @Test
    public void testsample1() {
        assertEval("argv <- list(0L, 0L, FALSE, NULL); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testsample2() {
        assertEval("argv <- list(1L, 1L, FALSE, NULL); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testsample3() {
        assertEval("argv <- list(2L, 499, TRUE, c(0, 0.525)); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testsample5() {
        assertEval("argv <- structure(list(x = c(0, 0)), .Names = 'x');do.call('sample', argv)");
    }

    @Test
    public void testSample() {
        assertEval("{  set.seed(4357, \"default\"); x <- 5 ; sample(x, 5, TRUE, NULL) ;}");
        assertEval("{  set.seed(4357, \"default\"); x <- 5 ; sample(x, 5, FALSE, NULL) ;}");

        assertEval("{ set.seed(4357, \"default\");  x <- c(5, \"cat\"); sample(x, 2, TRUE, NULL) ;}");
        assertEval("{ set.seed(4357, \"default\"); x <- c(5, \"cat\"); sample(x, 2, FALSE, NULL) ;}");
        assertEval("{ set.seed(4357, \"default\"); x <- c(5, \"cat\"); sample(x, 3, TRUE, NULL) ;}");

        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5; sample(x, 5, TRUE, NULL) ;}");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5; sample(x, 5, FALSE, NULL) ;}");

        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- c(5, \"cat\") ; sample(x, 2, TRUE, NULL) ;}");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- c(5, \"cat\") ; sample(x, 2, FALSE, NULL) ;}");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- c(5, \"cat\") ; sample(x, 3, TRUE, NULL) ;}");

        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; prob <- c(.1, .2, .3, .2, .1) ; sample(x, 10, TRUE, prob) ; }");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; prob <- c(.5, .5, .5, .5, .5) ; sample(x, 5, FALSE, prob) ; }");
        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; prob <- c(.2, .2, .2, .2, .2 ) ; sample(x, 5, FALSE, prob) ; }");

        assertEval("{ set.seed(4357, \"default\"); x <- c(\"Heads\", \"Tails\"); prob <- c(.3, .7) ; sample(x, 10, TRUE, prob) ; }");
        assertEval("{ set.seed(4357, \"default\"); x <- 5 ; prob <- c(.1, .2, .3, .2, .1); sample(x, 10, TRUE, prob) ; }");
        assertEval("{ set.seed(4357, \"default\"); x <- 5 ; prob <- c(.5, .5, .5, .5, .5); sample(x, 5, FALSE, prob) ; }");
        assertEval("{ set.seed(4357, \"default\"); x <- 5 ; prob <- c(.2, .2, .2, .2, .2 ); sample(x, 5, FALSE, prob) ; }");

        assertEval("{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(\"Heads\", \"Tails\") ; prob <- c(.3, .7) ; sample(x, 10, TRUE, prob) ; }");

        assertEval(Ignored.Unknown, "{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, TRUE, prob) ; }");
        assertEval(Ignored.Unknown, "{ set.seed(9567, \"Marsaglia-Multicarry\");x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, FALSE, prob) ; }");
        assertEval(Ignored.Unknown, "{ set.seed(4357, \"default\"); x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, TRUE, prob) ; }");
        assertEval(Ignored.Unknown, "{ set.seed(4357, \"default\"); x <- c(5) ; prob <- c(1, 2, 3, 4, 5) ; sample(x, 5, FALSE, prob) ; }");

        // Fails because of error message mismatch.
        assertEval(Ignored.Unknown, "{ set.seed(4357, \"default\"); x <- 5 ; sample(x, 6, FALSE, NULL) ;}");
        assertEval(Ignored.Unknown, "{ set.seed(9567, \"Marsaglia-Multicarry\"); x <- 5 ; sample(x, 6, FALSE, NULL) ;}");
    }

    @Test
    public void testArgsCasts() {
        // x
        assertEval("set.seed(42); sample(-1)");
        assertEval("set.seed(42); .Internal(sample(4.5e20, 4.5e20, FALSE, NULL))");
        assertEval("set.seed(42); .Internal(sample(NA, NA, FALSE, NULL))");
        assertEval("set.seed(42); .Internal(sample(NaN, NaN, FALSE, NULL))");
        // Note: we treat Infinity in NaN check
        assertEval(Output.IgnoreErrorMessage, "set.seed(42); .Internal(sample(1/0, 1, FALSE, NULL))");
        // size
        assertEval("set.seed(42); sample(3, '2')");
        assertEval("set.seed(42); sample(3, 2.0)");
        assertEval("set.seed(42); sample(3, c(2,3))");
        assertEval("set.seed(42); sample(3, TRUE)");
        assertEval("set.seed(42); sample(3, -3)");
        assertEval("set.seed(42); sample(3, NA)");
        assertEval("set.seed(42); sample(2, 0)");
        assertEval("set.seed(42); sample(0, 0)");
        // replace
        assertEval("set.seed(42); sample(4, replace=c(T,F))");
        assertEval("set.seed(42); sample(4, replace=1)");
        assertEval("set.seed(42); sample(4, replace=1.2)");
        assertEval(Output.IgnoreErrorMessage, "set.seed(42); sample(4, replace='s')");
        // prob
        assertEval("set.seed(42); sample(4, prob=c(1,2))");
        assertEval("set.seed(42); sample(4, prob=c(-1,1,1,2))");
    }
}
