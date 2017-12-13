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
public class TestBuiltin_regexec extends TestBase {

    @Test
    public void testregexec1() {
        // FIXME Outputs match but in addition GnuR outputs:
        // attr(,"useBytes")
        // [1] TRUE
        // docs do not mention that so ReferenceError for now.
        assertEval(Ignored.ReferenceError,
                        "argv <- list('^(([^:]+)://)?([^:/]+)(:([0-9]+))?(/.*)', 'http://stat.umn.edu:80/xyz', FALSE, FALSE, FALSE); .Internal(regexec(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");

        assertEval("regexpr(\"^(?:(?:^\\\\[([^\\\\]]+)\\\\])?(?:'?([^']+)'?!)?([a-zA-Z0-9:\\\\-$\\\\[\\\\]]+)|(.*))$\", 'A1', perl=T)");
        assertEval("regexpr(\"^(?:(?:^\\\\[([^\\\\]]+)\\\\])?(?:'?([^']+)'?!)?([a-zA-Z0-9:\\\\-$\\\\[\\\\]]+)|(.*))$\", 'A1A1', perl=T)");
        assertEval("regexpr(\"^(?:(?:^\\\\[([^\\\\]]+)\\\\])?(?:'?([^']+)'?!)?([a-zA-Z0-9:\\\\-$\\\\[\\\\]]+)|(.*))$\", 'A1 A1', perl=T)");
        assertEval("regexpr(\"^(?<n1>:(?:^\\\\[([^\\\\]]+)\\\\])?(?:'?([^']+)'?!)?([a-zA-Z0-9:\\\\-$\\\\[\\\\]]+)|(.*))$\", 'A1', perl=T)");
        assertEval("regexpr(\"^(?<n1>:(?:^\\\\[([^\\\\]]+)\\\\])?(?<n2>:'?([^']+)'?!)?([a-zA-Z0-9:\\\\-$\\\\[\\\\]]+)|(.*))$\", 'A1', perl=T)");
        assertEval("regexpr(\"^(?:(?:^\\\\[([^\\\\]]+)\\\\])?(?<n2>:'?([^']+)'?!)?([a-zA-Z0-9:\\\\-$\\\\[\\\\]]+)|(.*))$\", 'A1', perl=T)");
        assertEval("regexpr(\"^((.*))$\", 'A1', perl=T)");
        assertEval("regexpr(\"^(?<n>(.*))$\", 'A1', perl=T)");
        assertEval("regexpr(\"^(.*)$\", 'A1', perl=T)");
        assertEval("regexpr(\"^(?<n>.*)$\", 'A1', perl=T)");

        assertEval("regexpr(\"^(([A-Z)|([a-z]))$\", 'Aa', perl=T)");
        assertEval("regexpr(\"^(([A-Z)|([a-z]))$\", c('A', 'Aa'), perl=T)");
        assertEval("regexpr(\"^(([A-Z)|([a-z]))$\", c('Aa', 'A'), perl=T)");
    }
}
