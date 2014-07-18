package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltingetconst extends TestBase {
	@Test
		public void testgetconst1(){
		assertEval("argv <- list(list(list(), NULL), 1); .Internal(getconst(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testgetconst2(){
		assertEval("argv <- list(list(FALSE), 1); .Internal(getconst(argv[[1]], argv[[2]]))");
	}
}
