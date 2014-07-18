package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinpushBackLength extends TestBase {
	@Test
		public void testpushBackLength1(){
		assertEval("argv <- list(FALSE); .Internal(pushBackLength(argv[[1]]))");
	}
}
