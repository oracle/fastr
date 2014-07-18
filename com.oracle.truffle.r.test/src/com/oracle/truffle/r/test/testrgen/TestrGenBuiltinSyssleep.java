package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinSyssleep extends TestBase {
	@Test
		public void testSyssleep1(){
		assertEval("argv <- list(0.5); .Internal(Sys.sleep(argv[[1]]))");
	}
	@Test
		public void testSyssleep2(){
		assertEval("argv <- list(FALSE); .Internal(Sys.sleep(argv[[1]]))");
	}
}
