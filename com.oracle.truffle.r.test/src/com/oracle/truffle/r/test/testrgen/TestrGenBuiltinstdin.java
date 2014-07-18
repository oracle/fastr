package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinstdin extends TestBase {
	@Test
		public void teststdin1(){
		assertEval(" .Internal(stdin())");
	}
	@Test
		public void teststdin2(){
		assertEval(" .Internal(stdin())");
	}
}
