package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinnargs extends TestBase {
	@Test
		public void testnargs1(){
		assertEval("nargs( );");
	}
	@Test
		public void testnargs2(){
		assertEval("nargs( );");
	}
}
