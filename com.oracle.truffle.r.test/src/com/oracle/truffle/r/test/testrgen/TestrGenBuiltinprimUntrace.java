package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinprimUntrace extends TestBase {
	@Test
		public void testprimUntrace1(){
		assertEval("argv <- list(.Primitive(\'sum\'));.primUntrace(argv[[1]]);");
	}
}
