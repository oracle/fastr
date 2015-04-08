/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestrGenBuiltinSyssetlocale extends TestBase {

    @Test
    public void testSyssetlocale1() {
        assertEval(Ignored.Unknown, "argv <- list(3L, \'C\'); .Internal(Sys.setlocale(argv[[1]], argv[[2]]))");
    }

	@Test
	public void testSyssetlocale3() {
		assertEval(Ignored.Unknown, "argv <- structure(list(category = \'LC_TIME\', locale = \'C\'), .Names = c(\'category\',     \'locale\'));"+
			"do.call(\'Sys.setlocale\', argv)");
	}

}

