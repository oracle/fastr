package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbesselI extends TestBase {
	@Test
		public void testbesselI1(){
		assertEval("argv <- list(FALSE, FALSE, 1); .Internal(besselI(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testbesselI2(){
		assertEval("argv <- list(logical(0), logical(0), 1); .Internal(besselI(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
