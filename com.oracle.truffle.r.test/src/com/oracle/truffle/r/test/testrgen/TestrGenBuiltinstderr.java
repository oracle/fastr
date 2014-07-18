package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinstderr extends TestBase {
	@Test
		public void teststderr1(){
		assertEval(" .Internal(stderr())");
	}
	@Test
		public void teststderr2(){
		assertEval(" .Internal(stderr())");
	}
}
