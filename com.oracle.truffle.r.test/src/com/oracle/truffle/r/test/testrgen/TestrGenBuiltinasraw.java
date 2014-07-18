package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinasraw extends TestBase {
	@Test
		public void testasraw1(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)));as.raw(argv[[1]]);");
	}
	@Test
		public void testasraw2(){
		assertEval("argv <- list(integer(0));as.raw(argv[[1]]);");
	}
	@Test
		public void testasraw3(){
		assertEval("argv <- list(logical(0));as.raw(argv[[1]]);");
	}
	@Test
		public void testasraw4(){
		assertEval("argv <- list(character(0));as.raw(argv[[1]]);");
	}
	@Test
		public void testasraw5(){
		assertEval("argv <- list(NULL);as.raw(argv[[1]]);");
	}
	@Test
		public void testasraw6(){
		assertEval("argv <- list(list());as.raw(argv[[1]]);");
	}
}
