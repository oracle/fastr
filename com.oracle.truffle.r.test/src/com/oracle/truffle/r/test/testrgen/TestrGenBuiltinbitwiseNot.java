package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbitwiseNot extends TestBase {
	@Test
		public void testbitwiseNot1(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(bitwiseNot(argv[[1]]))");
	}
}
