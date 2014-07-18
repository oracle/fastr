package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbitwiseOr extends TestBase {
	@Test
		public void testbitwiseOr1(){
		assertEval("argv <- list(15L, 7L); .Internal(bitwiseOr(argv[[1]], argv[[2]]))");
	}
}
