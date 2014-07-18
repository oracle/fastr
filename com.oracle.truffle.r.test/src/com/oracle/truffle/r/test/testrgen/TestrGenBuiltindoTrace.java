package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltindoTrace extends TestBase {
	@Test
		public void testdoTrace1(){
		assertEval("argv <- list(c(1, 1, 2));.doTrace(argv[[1]]);");
	}
}
