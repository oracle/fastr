/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.stats;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Tests the rxxx functions against common set of arguments. Each such function may have additional
 * tests for its specific corner cases if those are not covered here.
 */
public class TestRandGenerationFunctions extends TestBase {
    private static final String[] FUNCTION2_NAMES = {"rnorm", "runif", "rgamma", "rbeta", "rcauchy", "rf", "rlogis", "rweibull", "rchisq", "rwilcox", "rlnorm", "rnbinom"};
    private static final String[] FUNCTION2_PARAMS = {
                    "10, 10, 10",
                    "20, c(-1, 0, 0.2, 2:5), c(-1, 0, 0.1, 0.9, 3)",
                    "30, c(NA, 0, NaN, 1/0, -1/0), c(NaN, NaN, NA, 0, 1/0, -1/0)",
                    "24, c(0.0653, 0.000123, 32e-80, 8833, 79e70), c(0.0653, 0.000123, 32e-80, 8833, 79e70, 0, -1)"
    };

    private static String[] filterOutFailingTests(String[] allTests, String[] failingTests) {
        Set<String> allTestsSet = new HashSet<>(Arrays.asList(allTests));
        Set<String> failingTestsSet = new HashSet<>(Arrays.asList(failingTests));
        allTestsSet.removeAll(failingTestsSet);
        return allTestsSet.toArray(new String[]{});
    }

    @Test
    public void testFunctions2() {
        // Failing test is "rnbinom(24, c(...), c(...))"
        String[] failingTest = template("set.seed(1); %0(%1)", new String[]{FUNCTION2_NAMES[FUNCTION2_NAMES.length - 1]}, new String[]{FUNCTION2_PARAMS[FUNCTION2_PARAMS.length - 1]});
        String[] allTests = template("set.seed(1); %0(%1)", FUNCTION2_NAMES, FUNCTION2_PARAMS);
        String[] successfullTests = filterOutFailingTests(allTests, failingTest);
        assertEval(Ignored.NewRVersionMigration, failingTest);
        assertEval(Output.IgnoreWhitespace, successfullTests);
    }

    @Test
    public void testFunctions2Infrastructure() {
        // calculating the size of the result:
        assertEval("length(runif(c(1,2,3)))");
        assertEval("length(runif(c('a', 'b', 'b', 'd')))");
        assertEval("length(runif('3'))");
        // wrong size argument
        assertEval("runif('hello')");
        // empty parameters
        assertEval("runif(2, numeric(), 2)");
        assertEval("runif(2, 2, numeric())");
        // wrong parameters
        assertEval("runif(-1, 1, 2)");
    }

    private static final String[] FUNCTION1_NAMES = {"rchisq", "rexp", "rgeom", "rpois", "rt"};

    @Test
    public void testFunctions1() {
        String[] allTests = template("set.seed(2); %0(13, c(NA, NaN, 1/0, -1/0, -1, 1, 0.3, -0.5, 0.0653, 0.000123, 32e-80, 8833, 79e70))", FUNCTION1_NAMES);
        String[] failingTests = template("set.seed(2); %0(13, c(NA, NaN, 1/0, -1/0, -1, 1, 0.3, -0.5, 0.0653, 0.000123, 32e-80, 8833, 79e70))", new String[]{"rgeom", "rpois"});
        String[] successfullTests = filterOutFailingTests(allTests, failingTests);
        assertEval(Ignored.NewRVersionMigration, failingTests);
        assertEval(Output.IgnoreWhitespace, successfullTests);
        // Note: signrank has loop with 'n' iterations: we have to leave out the large numbers
        assertEval(Output.IgnoreWhitespace, "set.seed(10); rsignrank(12, c(NA, NaN, 1/0, -1/0, -1, 1, 0.3, -0.6, 0.0653, 0.000123, 32e-80, 10))");
    }

    @Test
    public void testFunctions3() {
        // error: drawn (20) mare than red + blue (5+5)
        assertEval("rhyper(1, 5, 5, 20)");
        // error: negative number of balls
        assertEval("rhyper(1, -5, 5, 20)");
        // common errors with NA, NaN, Inf
        assertEval("rhyper(1, NA, 5, 20)");
        assertEval("rhyper(1, 5, NaN, 20)");
        assertEval("rhyper(1, 5, 5, 1/0)");
        assertEval("rhyper(1, 5, 5, -1/0)");
        // few simple tests (note: rhyper seems to be quite slow even in GnuR).
        assertEval("set.seed(3); rhyper(3, 10, 10, 5)");
        assertEval("set.seed(3); rhyper(2, 1000, 1000, 5)");
        assertEval("set.seed(3); rhyper(3, 10, 79e70, 2)");
    }

    @Test
    public void testRmultinom() {
        assertEval("set.seed(11); rmultinom(10, 5, c(0.1, 0.1, 0.3, 0.2, 0.3))");
        assertEval("set.seed(11); rmultinom(7, 8, structure(c(0.1, 0.1), .Names=c('a', 'B')))");
        assertEval("set.seed(12); rmultinom('5', 3.1, c(2, 5, 10))");
        // test args validation
        assertEval("rmultinom(1, 1, -0.15)");
        assertEval("rmultinom('string', 1, 0.15)");
        assertEval("rmultinom(1, NA, 0.2)");
        assertEval("rmultinom(NA, 1, 0.2)");
    }

    @Test
    public void testGenerators() {
        assertEval("for(gen in c(\"Buggy Kinderman-Ramage\", \"Ahrens-Dieter\", \"Box-Muller\", \"Inversion\", \"Kinderman-Ramage\", \"default\")) { print(paste0(gen, \":\")); RNGkind(NULL,gen); set.seed(42); print(rnorm(30)); }");
    }

    @Test
    public void testDotRandomSeed() {
        // In shared context, .Random.seed is already initialized. Therefore, all these tests are ignored.
        // TODO: Once the performance of non-shared context is sufficient, run these tests in non-shared context.
        assertEval(Ignored.ImplementationError, Output.IgnoreErrorContext, "{ .Random.seed }");
        assertEval(Ignored.ImplementationError, Output.IgnoreErrorContext, "{ print(.Random.seed) }");
        assertEval(Ignored.ImplementationError, Output.IgnoreErrorContext, "{ get('.Random.seed', envir = .GlobalEnv, inherits = FALSE) }");
        assertEval(Ignored.ImplementationError, "{ get0('.Random.seed', envir = .GlobalEnv, inherits = FALSE) }");
        assertEval(Ignored.ImplementationError, Output.IgnoreErrorContext, "{ get('.Random.seed', envir = .GlobalEnv, inherits = TRUE) }");
        assertEval(Ignored.ImplementationError, "{ get0('.Random.seed', envir = .GlobalEnv, inherits = TRUE) }");
        assertEval(Ignored.ImplementationError, "exists('.Random.seed', envir = .GlobalEnv, inherits = FALSE)");
        assertEval(Ignored.ImplementationError, "{ .GlobalEnv$.Random.seed  }");

        assertEval("{ runif(1); length(.Random.seed) }");
        assertEval("{ runif(1); print(length(.Random.seed)) }");
        assertEval("{ runif(1); length(.GlobalEnv$.Random.seed)  }");

        assertEval("{ .Random.seed <- 1:3; .Random.seed }");
        assertEval("{ .Random.seed <- 1:3; print(.Random.seed) }");
        assertEval("{ .Random.seed <- 1:3; .GlobalEnv$.Random.seed  }");
        // FIXME: GR-35083
        assertEval(Ignored.ImplementationError, "{ .Random.seed <- c(1,2,3); rm(list=ls(all.names=TRUE, envir=.GlobalEnv), envir=.GlobalEnv); set.seed(11) }");
        assertEval(Ignored.ImplementationError, "{ .Random.seed <- c(1,2,3); rm(list=ls(all.names=TRUE, envir=.GlobalEnv), envir=.GlobalEnv); set.seed(11); rnorm(5) }");
    }
}
