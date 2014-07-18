package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsample extends TestBase {
	@Test
		public void testsample1(){
		assertEval("argv <- list(0L, 0L, FALSE, NULL); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testsample2(){
		assertEval("argv <- list(1L, 1L, FALSE, NULL); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testsample3(){
		assertEval("argv <- list(2L, 499, TRUE, c(0, 0.525)); .Internal(sample(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
}
