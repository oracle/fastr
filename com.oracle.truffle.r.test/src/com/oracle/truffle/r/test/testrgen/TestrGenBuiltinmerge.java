package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinmerge extends TestBase {
	@Test
		public void testmerge1(){
		assertEval("argv <- list(c(0L, 0L, 0L, 0L, 0L), 0L, FALSE, TRUE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testmerge2(){
		assertEval("argv <- list(c(0L, 0L, 0L, 0L, 0L), 0L, TRUE, FALSE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testmerge3(){
		assertEval("argv <- list(c(0L, 0L, 0L, 3L, 4L), c(0L, 0L, 0L, 3L, 4L), FALSE, FALSE); .Internal(merge(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
}
