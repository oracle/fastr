/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltinlistfiles extends TestBase {

	@Test
	public void testlistfiles1(){
		assertEval("argv <- list(\'.\', \'myTst_.*tar\\\\.gz$\', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}

	@Test
	public void testlistfiles2(){
		assertEval("argv <- list(\'./myTst/data\', NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}

	@Test
	public void testlistfiles3(){
		assertEval("argv <- list(\'.\', \'^CITATION.*\', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}

	@Test
	public void testlistfiles4(){
		assertEval("argv <- list(\'mgcv\', NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
}
