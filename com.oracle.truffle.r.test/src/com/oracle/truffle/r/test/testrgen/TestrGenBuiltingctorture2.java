package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltingctorture2 extends TestBase {
	@Test
		public void testgctorture21(){
		assertEval("argv <- list(NULL, NULL, FALSE); .Internal(gctorture2(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testgctorture22(){
		assertEval("argv <- list(FALSE, FALSE, FALSE); .Internal(gctorture2(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
