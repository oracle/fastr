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
public class TestBuiltin_agrep extends TestBase {

    @Test
    public void testagrep1() {
        assertEval("argv <- list('x86_64-linux-gnu', 'x86_64-linux-gnu', FALSE, FALSE, c(1L, 1L, 1L), c(0.1, NA, NA, NA, NA), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testagrep3() {
        assertEval(Ignored.Unknown,
                        "argv <- list('lasy', c(' 1 lazy 2', '1 lasy 2'), FALSE, FALSE, c(1L, 1L, 1L), structure(c(NA, 0.1, 0.1, 0, 0.1), .Names = c('cost', 'insertions', 'deletions', 'substitutions', 'all')), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testagrep4() {
        assertEval(Ignored.Unknown,
                        "argv <- list('laysy', c('1 lazy', '1', '1 LAZY'), FALSE, TRUE, c(1L, 1L, 1L), c(2, NA, NA, NA, NA), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testagrep5() {
        assertEval(Ignored.Unknown,
                        "argv <- list('laysy', c('1 lazy', '1', '1 LAZY'), TRUE, FALSE, c(1L, 1L, 1L), c(2, NA, NA, NA, NA), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testAgrep() {
        assertEval("{ .Internal(agrep(7, \"42\", F, F, NULL, NULL, F, F)) }");
        assertEval("{ .Internal(agrep(character(), \"42\", F, F, NULL, NULL, F, F)) }");
        assertEval("{ .Internal(agrep(\"7\", 42, F, F, NULL, NULL, F, F)) }");

        assertEval("{ .Internal(agrepl(7, \"42\", F, F, NULL, NULL, F, F)) }");
        assertEval("{ .Internal(agrepl(character(), \"42\", F, F, NULL, NULL, F, F)) }");
        assertEval("{ .Internal(agrepl(\"7\", 42, F, F, NULL, NULL, F, F)) }");
    }
}
