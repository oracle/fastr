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

public class TestrGenBuiltinintToUtf8 extends TestBase {

	@Test
	public void testintToUtf81(){
		assertEval("argv <- list(NULL, FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
	}

	@Test
	public void testintToUtf82(){
		assertEval("argv <- list(list(), FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
	}

	@Test
	public void testintToUtf83(){
		assertEval("argv <- list(FALSE, FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
	}
}
