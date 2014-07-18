package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbody extends TestBase {
	@Test
		public void testbody1(){
		assertEval("argv <- list(function (x, y) {    c(x, y)}); .Internal(body(argv[[1]]))");
	}
	@Test
		public void testbody2(){
		assertEval("argv <- list(function (object) TRUE); .Internal(body(argv[[1]]))");
	}
	@Test
		public void testbody3(){
		assertEval("argv <- list(function (from, strict = TRUE) from); .Internal(body(argv[[1]]))");
	}
	@Test
		public void testbody4(){
		assertEval("argv <- list(.Primitive(\'/\')); .Internal(body(argv[[1]]))");
	}
	@Test
		public void testbody5(){
		assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = \'data.frame\')); .Internal(body(argv[[1]]))");
	}
	@Test
		public void testbody6(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(body(argv[[1]]))");
	}
}
