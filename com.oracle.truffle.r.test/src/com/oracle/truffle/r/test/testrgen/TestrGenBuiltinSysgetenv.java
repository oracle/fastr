package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinSysgetenv extends TestBase {
	@Test
		public void testSysgetenv1(){
		assertEval("argv <- list(\'EDITOR\', \'\'); .Internal(Sys.getenv(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testSysgetenv2(){
		assertEval("argv <- list(\'SWEAVE_OPTIONS\', NA_character_); .Internal(Sys.getenv(argv[[1]], argv[[2]]))");
	}
}
