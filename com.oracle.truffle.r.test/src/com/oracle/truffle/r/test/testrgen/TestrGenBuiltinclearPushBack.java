package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinclearPushBack extends TestBase {
	@Test
		public void testclearPushBack1(){
		assertEval("argv <- list(FALSE); .Internal(clearPushBack(argv[[1]]))");
	}
}
