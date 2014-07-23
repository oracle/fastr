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

public class TestrGenBuiltinceiling extends TestBase {

	@Test
	public void testceiling1(){
		assertEval("argv <- list(1001);ceiling(argv[[1]]);");
	}

	@Test
	public void testceiling2(){
		assertEval("argv <- list(13990.84);ceiling(argv[[1]]);");
	}

	@Test
	public void testceiling3(){
		assertEval("argv <- list(c(1, 4.5, 8, 11.5, 15));ceiling(argv[[1]]);");
	}

	@Test
	public void testceiling4(){
		assertEval("argv <- list(c(1, 5.5, 10.5, 15.5, 20));ceiling(argv[[1]]);");
	}

	@Test
	public void testceiling5(){
		assertEval("argv <- list(-0.698970004336019);ceiling(argv[[1]]);");
	}

	@Test
	public void testceiling6(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));ceiling(argv[[1]]);");
	}

	@Test
	public void testceiling7(){
		assertEval("argv <- list(1e+05);ceiling(argv[[1]]);");
	}
}
