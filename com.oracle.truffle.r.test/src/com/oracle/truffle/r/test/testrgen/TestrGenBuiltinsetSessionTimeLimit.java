package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsetSessionTimeLimit extends TestBase {
	@Test
		public void testsetSessionTimeLimit1(){
		assertEval("argv <- list(NULL, NULL); .Internal(setSessionTimeLimit(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testsetSessionTimeLimit2(){
		assertEval("argv <- list(FALSE, Inf); .Internal(setSessionTimeLimit(argv[[1]], argv[[2]]))");
	}
}
