package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinfileinfo extends TestBase {
	@Test
		public void testfileinfo1(){
		assertEval("argv <- list(\'/home/lzhao/hg/r-instrumented/library/codetools/data\'); .Internal(file.info(argv[[1]]))");
	}
	@Test
		public void testfileinfo2(){
		assertEval("argv <- list(character(0)); .Internal(file.info(argv[[1]]))");
	}
}
