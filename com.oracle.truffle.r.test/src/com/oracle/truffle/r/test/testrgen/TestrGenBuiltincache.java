package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltincache extends TestBase {
	@Test
		public void testcache1(){
		assertEval("argv <- list(\'ddenseMatrix\', c(\'ddenseMatrix\', \'dMatrix\', \'denseMatrix\', \'Matrix\', \'mMatrix\'));.cache_class(argv[[1]],argv[[2]]);");
	}
	@Test
		public void testcache2(){
		assertEval("argv <- list(\'numeric\', c(\'numeric\', \'vector\', \'atomicVector\'));.cache_class(argv[[1]],argv[[2]]);");
	}
}
