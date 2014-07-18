package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinargs extends TestBase {
	@Test
		public void testargs1(){
		assertEval("argv <- list(NULL); .Internal(args(argv[[1]]))");
	}
	@Test
		public void testargs2(){
		assertEval("argv <- list(character(0)); .Internal(args(argv[[1]]))");
	}
	@Test
		public void testargs3(){
		assertEval("argv <- list(.Primitive(\':\')); .Internal(args(argv[[1]]))");
	}
	@Test
		public void testargs4(){
		assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = \'data.frame\')); .Internal(args(argv[[1]]))");
	}
	@Test
		public void testargs5(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(args(argv[[1]]))");
	}
}
