package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinchoose extends TestBase {
	@Test
		public void testchoose1(){
		assertEval("argv <- list(-1, 3); .Internal(choose(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testchoose2(){
		assertEval("argv <- list(9L, c(-14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24)); .Internal(choose(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testchoose3(){
		assertEval("argv <- list(logical(0), logical(0)); .Internal(choose(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testchoose4(){
		assertEval("argv <- list(0.5, 0:10); .Internal(choose(argv[[1]], argv[[2]]))");
	}
}
