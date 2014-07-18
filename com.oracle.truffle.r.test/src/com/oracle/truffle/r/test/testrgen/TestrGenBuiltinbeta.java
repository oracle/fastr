package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbeta extends TestBase {
	@Test
		public void testbeta1(){
		assertEval("argv <- list(FALSE, FALSE); .Internal(beta(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testbeta2(){
		assertEval("argv <- list(logical(0), logical(0)); .Internal(beta(argv[[1]], argv[[2]]))");
	}
}
