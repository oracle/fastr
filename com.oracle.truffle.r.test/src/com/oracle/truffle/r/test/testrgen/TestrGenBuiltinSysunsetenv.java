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

public class TestrGenBuiltinSysunsetenv extends TestBase {

	@Test
	public void testSysunsetenv1(){
		assertEval("argv <- list(\'_R_NS_LOAD_\'); .Internal(Sys.unsetenv(argv[[1]]))");
	}

	@Test
	public void testSysunsetenv2(){
		assertEval("argv <- list(\'_R_NS_LOAD_\'); .Internal(Sys.unsetenv(argv[[1]]))");
	}

	@Test
	public void testSysunsetenv3(){
		assertEval("argv <- list(character(0)); .Internal(Sys.unsetenv(argv[[1]]))");
	}
}
