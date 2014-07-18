package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinisdouble extends TestBase {
	@Test
		public void testisdouble1(){
		assertEval("argv <- list(list(1, list(3, \'A\')));is.double(argv[[1]]);");
	}
	@Test
		public void testisdouble2(){
		assertEval("argv <- list(structure(1:7, .Names = c(\'a1\', \'a2\', \'a3\', \'a4\', \'a5\', \'a6\', \'a7\')));is.double(argv[[1]]);");
	}
	@Test
		public void testisdouble3(){
		assertEval("argv <- list(structure(3.14159265358979, class = structure(\'3.14159265358979\', class = \'testit\')));is.double(argv[[1]]);");
	}
	@Test
		public void testisdouble4(){
		assertEval("argv <- list(structure(list(`character(0)` = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'character(0)\', row.names = character(0), class = \'data.frame\'));is.double(argv[[1]]);");
	}
	@Test
		public void testisdouble5(){
		assertEval("argv <- list(structure(1:24, .Dim = 2:4));is.double(argv[[1]]);");
	}
}
