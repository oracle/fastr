package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbitwiseShiftR extends TestBase {
	@Test
		public void testbitwiseShiftR1(){
		assertEval("argv <- list(-1, 1:31); .Internal(bitwiseShiftR(argv[[1]], argv[[2]]))");
	}
}
