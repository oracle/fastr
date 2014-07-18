package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltincomment extends TestBase {
	@Test
		public void testcomment1(){
		assertEval("argv <- list(NULL); .Internal(comment(argv[[1]]))");
	}
	@Test
		public void testcomment2(){
		assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = structure(\'integer(0)\', .Names = \'c0\'))); .Internal(comment(argv[[1]]))");
	}
	@Test
		public void testcomment3(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(comment(argv[[1]]))");
	}
	@Test
		public void testcomment4(){
		assertEval("argv <- list(structure(1:12, .Dim = 3:4, comment = c(\'This is my very important data from experiment #0234\', \'Jun 5, 1998\'))); .Internal(comment(argv[[1]]))");
	}
}
