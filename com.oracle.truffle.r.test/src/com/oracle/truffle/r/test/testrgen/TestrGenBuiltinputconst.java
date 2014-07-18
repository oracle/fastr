package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinputconst extends TestBase {
	@Test
		public void testputconst1(){
		assertEval("argv <- list(list(NULL), 0, NULL); .Internal(putconst(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testputconst2(){
		assertEval("argv <- list(list(list(), NULL), 1, list()); .Internal(putconst(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
