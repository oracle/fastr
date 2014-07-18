package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinenvironmentName extends TestBase {
	@Test
		public void testenvironmentName1(){
		assertEval("argv <- list(FALSE); .Internal(environmentName(argv[[1]]))");
	}
	@Test
		public void testenvironmentName2(){
		assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = structure(\'integer(0)\', .Names = \'c0\'))); .Internal(environmentName(argv[[1]]))");
	}
	@Test
		public void testenvironmentName3(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(environmentName(argv[[1]]))");
	}
}
