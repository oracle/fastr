package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinlazyLoadDBflush extends TestBase {
	@Test
		public void testlazyLoadDBflush1(){
		assertEval("argv <- list(\'/home/roman/r-instrumented/library/tools/R/tools.rdb\'); .Internal(lazyLoadDBflush(argv[[1]]))");
	}
	@Test
		public void testlazyLoadDBflush2(){
		assertEval("argv <- list(\'/home/lzhao/hg/r-instrumented/library/stats4/R/stats4.rdb\'); .Internal(lazyLoadDBflush(argv[[1]]))");
	}
}
