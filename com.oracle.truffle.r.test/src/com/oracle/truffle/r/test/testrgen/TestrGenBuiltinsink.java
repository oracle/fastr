package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsink extends TestBase {
	@Test
		public void testsink1(){
		assertEval("argv <- list(structure(2L, class = c(\'terminal\', \'connection\')), FALSE, TRUE, FALSE); .Internal(sink(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testsink2(){
		assertEval("argv <- list(-1L, FALSE, FALSE, FALSE); .Internal(sink(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testsink3(){
		assertEval("argv <- list(-1L, FALSE, FALSE, FALSE); .Internal(sink(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testsink4(){
		assertEval("argv <- list(structure(2L, class = c(\'terminal\', \'connection\')), FALSE, TRUE, FALSE); .Internal(sink(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
}
