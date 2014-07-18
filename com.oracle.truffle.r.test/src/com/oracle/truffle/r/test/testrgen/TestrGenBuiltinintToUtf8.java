package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinintToUtf8 extends TestBase {
	@Test
		public void testintToUtf81(){
		assertEval("argv <- list(NULL, FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testintToUtf82(){
		assertEval("argv <- list(list(), FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testintToUtf83(){
		assertEval("argv <- list(FALSE, FALSE); .Internal(intToUtf8(argv[[1]], argv[[2]]))");
	}
}
