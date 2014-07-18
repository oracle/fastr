package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbitwiseXor extends TestBase {
	@Test
		public void testbitwiseXor1(){
		assertEval("argv <- list(-1L, 1L); .Internal(bitwiseXor(argv[[1]], argv[[2]]))");
	}
}
