package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinSysunsetenv extends TestBase {
	@Test
		public void testSysunsetenv1(){
		assertEval("argv <- list(\'_R_NS_LOAD_\'); .Internal(Sys.unsetenv(argv[[1]]))");
	}
	@Test
		public void testSysunsetenv2(){
		assertEval("argv <- list(\'_R_NS_LOAD_\'); .Internal(Sys.unsetenv(argv[[1]]))");
	}
	@Test
		public void testSysunsetenv3(){
		assertEval("argv <- list(character(0)); .Internal(Sys.unsetenv(argv[[1]]))");
	}
}
