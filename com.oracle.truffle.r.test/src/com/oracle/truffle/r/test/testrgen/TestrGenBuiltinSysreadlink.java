package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinSysreadlink extends TestBase {
	@Test
		public void testSysreadlink1(){
		assertEval("argv <- list(character(0)); .Internal(Sys.readlink(argv[[1]]))");
	}
}
