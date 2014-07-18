package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinrow extends TestBase {
	@Test
		public void testrow1(){
		assertEval("argv <- list(c(14L, 14L)); .Internal(row(argv[[1]]))");
	}
	@Test
		public void testrow2(){
		assertEval("argv <- list(c(4L, 3L)); .Internal(row(argv[[1]]))");
	}
	@Test
		public void testrow3(){
		assertEval("argv <- list(0:1); .Internal(row(argv[[1]]))");
	}
}
