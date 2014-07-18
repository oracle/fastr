package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltingetRestart extends TestBase {
	@Test
		public void testgetRestart1(){
		assertEval("argv <- list(2L); .Internal(.getRestart(argv[[1]]))");
	}
	@Test
		public void testgetRestart2(){
		assertEval("argv <- list(1L); .Internal(.getRestart(argv[[1]]))");
	}
}
