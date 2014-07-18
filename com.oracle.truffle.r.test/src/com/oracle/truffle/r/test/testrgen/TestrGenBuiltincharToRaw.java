package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltincharToRaw extends TestBase {
	@Test
		public void testcharToRaw1(){
		assertEval("argv <- list(\'\'); .Internal(charToRaw(argv[[1]]))");
	}
}
