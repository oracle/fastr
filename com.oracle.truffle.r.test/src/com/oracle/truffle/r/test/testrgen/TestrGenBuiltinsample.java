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

public class TestrGenBuiltinsample extends TestBase {

	@Test
	public void testsample1(){
		assertEval("argv <- list(0L, 0L, FALSE, NULL); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}

	@Test
	public void testsample2(){
		assertEval("argv <- list(1L, 1L, FALSE, NULL); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}

	@Test
	public void testsample3(){
		assertEval("argv <- list(2L, 499, TRUE, c(0, 0.525)); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
}
