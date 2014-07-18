package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinlchoose extends TestBase {
	@Test
		public void testlchoose1(){
		assertEval("argv <- list(FALSE, FALSE); .Internal(lchoose(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testlchoose2(){
		assertEval("argv <- list(50L, 0:48); .Internal(lchoose(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testlchoose3(){
		assertEval("argv <- list(0.5, 1:9); .Internal(lchoose(argv[[1]], argv[[2]]))");
	}
}
