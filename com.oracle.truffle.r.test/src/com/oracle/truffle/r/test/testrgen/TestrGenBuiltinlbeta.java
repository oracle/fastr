package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinlbeta extends TestBase {
	@Test
		public void testlbeta1(){
		assertEval("argv <- list(FALSE, FALSE); .Internal(lbeta(argv[[1]], argv[[2]]))");
	}
}
