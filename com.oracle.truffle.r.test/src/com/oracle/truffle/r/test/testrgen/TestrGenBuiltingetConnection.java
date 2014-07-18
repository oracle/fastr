package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltingetConnection extends TestBase {
	@Test
		public void testgetConnection1(){
		assertEval("argv <- list(FALSE); .Internal(getConnection(argv[[1]]))");
	}
}
