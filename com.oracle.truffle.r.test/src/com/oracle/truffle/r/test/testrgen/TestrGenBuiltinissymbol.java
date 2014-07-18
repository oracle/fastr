package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinissymbol extends TestBase {
	@Test
		public void testissymbol1(){
		assertEval("argv <- list(0.05);is.symbol(argv[[1]]);");
	}
	@Test
		public void testissymbol2(){
		assertEval("argv <- list(structure(c(1, 1, 1, 1, 1, 1), .Dim = 1:3));is.symbol(argv[[1]]);");
	}
	@Test
		public void testissymbol3(){
		assertEval("argv <- list(numeric(0));is.symbol(argv[[1]]);");
	}
	@Test
		public void testissymbol4(){
		assertEval("argv <- list(structure(3.14159265358979, class = structure(\'3.14159265358979\', class = \'testit\')));is.symbol(argv[[1]]);");
	}
}
