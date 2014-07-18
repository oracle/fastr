package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinprintDeferredWarnings extends TestBase {
	@Test
		public void testprintDeferredWarnings1(){
		assertEval(" .Internal(printDeferredWarnings())");
	}
	@Test
		public void testprintDeferredWarnings2(){
		assertEval(" .Internal(printDeferredWarnings())");
	}
}
