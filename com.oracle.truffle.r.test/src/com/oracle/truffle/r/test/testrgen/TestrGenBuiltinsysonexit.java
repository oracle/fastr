package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsysonexit extends TestBase {
	@Test
		public void testsysonexit1(){
		assertEval(" .Internal(sys.on.exit())");
	}
}
