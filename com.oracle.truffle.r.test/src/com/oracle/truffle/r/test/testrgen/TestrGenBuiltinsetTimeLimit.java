package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsetTimeLimit extends TestBase {
	@Test
		public void testsetTimeLimit1(){
		assertEval("argv <- list(FALSE, Inf, FALSE); .Internal(setTimeLimit(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
