package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinenvironment extends TestBase {
	@Test
		public void testenvironment1(){
		assertEval("argv <- list(quote(cbind(X, M) ~ M.user + Temp + M.user:Temp + Soft)); .Internal(environment(argv[[1]]))");
	}
	@Test
		public void testenvironment2(){
		assertEval("argv <- list(FALSE); .Internal(environment(argv[[1]]))");
	}
	@Test
		public void testenvironment3(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(environment(argv[[1]]))");
	}
	@Test
		public void testenvironment4(){
		assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = structure(\'integer(0)\', .Names = \'c0\'))); .Internal(environment(argv[[1]]))");
	}
}
