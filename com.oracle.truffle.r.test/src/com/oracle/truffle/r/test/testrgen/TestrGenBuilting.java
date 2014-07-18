package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuilting extends TestBase {
	@Test
		public void testg1(){
		assertEval("argv <- list(1);g(argv[[1]]);");
	}
}
