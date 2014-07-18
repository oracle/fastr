package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinPrimitive extends TestBase {
	@Test
		public void testPrimitive1(){
		assertEval("argv <- list(\'c\');.Primitive(argv[[1]]);");
	}
	@Test
		public void testPrimitive2(){
		assertEval("argv <- list(\'c\');.Primitive(argv[[1]]);");
	}
}
