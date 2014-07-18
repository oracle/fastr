package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsysparent extends TestBase {
	@Test
		public void testsysparent1(){
		assertEval("argv <- list(2); .Internal(sys.parent(argv[[1]]))");
	}
}
