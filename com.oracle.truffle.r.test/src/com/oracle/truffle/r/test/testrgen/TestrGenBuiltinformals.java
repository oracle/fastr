package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinformals extends TestBase {
	@Test
		public void testformals1(){
		assertEval("argv <- list(.Primitive(\'length<-\')); .Internal(formals(argv[[1]]))");
	}
	@Test
		public void testformals2(){
		assertEval("argv <- list(logical(0)); .Internal(formals(argv[[1]]))");
	}
	@Test
		public void testformals3(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(formals(argv[[1]]))");
	}
	@Test
		public void testformals4(){
		assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = \'data.frame\')); .Internal(formals(argv[[1]]))");
	}
}
