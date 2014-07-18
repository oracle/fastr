package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsinknumber extends TestBase {
	@Test
		public void testsinknumber1(){
		assertEval("argv <- list(FALSE); .Internal(sink.number(argv[[1]]))");
	}
}
