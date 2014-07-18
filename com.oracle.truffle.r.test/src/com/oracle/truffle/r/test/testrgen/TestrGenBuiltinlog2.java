package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinlog2 extends TestBase {
	@Test
		public void testlog21(){
		assertEval("argv <- list(48L);log2(argv[[1]]);");
	}
	@Test
		public void testlog22(){
		assertEval("argv <- list(FALSE);log2(argv[[1]]);");
	}
	@Test
		public void testlog23(){
		assertEval("argv <- list(2.2250738585072e-308);log2(argv[[1]]);");
	}
}
