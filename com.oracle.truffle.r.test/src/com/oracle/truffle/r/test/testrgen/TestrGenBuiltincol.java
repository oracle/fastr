package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltincol extends TestBase {
	@Test
		public void testcol1(){
		assertEval("argv <- list(c(2L, 2L)); .Internal(col(argv[[1]]))");
	}
	@Test
		public void testcol2(){
		assertEval("argv <- list(c(2L, 2L)); .Internal(col(argv[[1]]))");
	}
	@Test
		public void testcol3(){
		assertEval("argv <- list(c(1L, 0L)); .Internal(col(argv[[1]]))");
	}
}
