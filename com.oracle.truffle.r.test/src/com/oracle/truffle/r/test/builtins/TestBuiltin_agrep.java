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
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
        // FIXME: GnuR seems to be right, 1 edit necessary for first string of x
        // and when deletions set to 0 then only second string of x should pass.
        // TODO verify expected algorithm behavior according to input args values
        assertEval(Ignored.ImplementationError,
                        "argv <- list('lasy', c(' 1 lazy 2', '1 lasy 2'), FALSE, FALSE, c(1L, 1L, 1L), structure(c(NA, 0.1, 0.1, 0, 0.1), .Names = c('cost', 'insertions', 'deletions', 'substitutions', 'all')), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testagrep4() {
        // FIXME not yet implemented: value == true
        assertEval(Ignored.Unimplemented,
                        "argv <- list('laysy', c('1 lazy', '1', '1 LAZY'), FALSE, TRUE, c(1L, 1L, 1L), c(2, NA, NA, NA, NA), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
    }

    @Test
    public void testagrep5() {
        // FIXME not yet implemented: ignoreCase == true
        assertEval(Ignored.Unimplemented,
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

        assertEval("{ agrep('a', c('a'), ignore.case=T) }");
        assertEval("{ agrep('A', c('a'), ignore.case=T) }");
        assertEval("{ agrep('a', c('A'), ignore.case=T) }");
        assertEval(Ignored.ImplementationError, "{ agrep('a', c('a', 'b'), ignore.case=T) }");
        assertEval(Ignored.ImplementationError, "{ agrep('a', c('b', 'a'), ignore.case=T) }");
        assertEval(Ignored.ImplementationError, "{ agrep('a', c('b', 'a'), ignore.case=F) }");

        assertEval("{ agrep('a', c('b', 'a', 'z'), max.distance=0, ignore.case=T) }");
        assertEval("{ agrep('A', c('b', 'a', 'z'), max.distance=0, ignore.case=T) }");
        assertEval("{ agrep('a', c('b', 'a', 'z'), max.distance=1, ignore.case=T) }");
        assertEval("{ agrep('A', c('b', 'a', 'z'), max.distance=1, ignore.case=T) }");

    }
}
