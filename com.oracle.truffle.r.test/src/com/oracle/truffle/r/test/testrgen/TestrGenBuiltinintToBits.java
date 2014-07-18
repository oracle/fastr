package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinintToBits extends TestBase {
	@Test
		public void testintToBits1(){
		assertEval("argv <- list(list()); .Internal(intToBits(argv[[1]]))");
	}
	@Test
		public void testintToBits2(){
		assertEval("argv <- list(NULL); .Internal(intToBits(argv[[1]]))");
	}
}
