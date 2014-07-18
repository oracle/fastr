package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinSyssetlocale extends TestBase {
	@Test
		public void testSyssetlocale1(){
		assertEval("argv <- list(3L, \'C\'); .Internal(Sys.setlocale(argv[[1]], argv[[2]]))");
	}
}
