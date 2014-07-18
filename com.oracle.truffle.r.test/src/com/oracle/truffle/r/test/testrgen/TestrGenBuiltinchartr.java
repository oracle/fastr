package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinchartr extends TestBase {
	@Test
		public void testchartr1(){
		assertEval("argv <- list(\'.\', \'.\', c(\'0.02\', \'0.06\', \'0.11\', \'0.22\', \'0.56\', \'1.1\')); .Internal(chartr(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testchartr2(){
		assertEval("argv <- list(\'iXs\', \'why\', \'MiXeD cAsE 123\'); .Internal(chartr(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testchartr3(){
		assertEval("argv <- list(\'a-cX\', \'D-Fw\', \'MiXeD cAsE 123\'); .Internal(chartr(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testchartr4(){
		assertEval("argv <- list(\'.\', \'.\', character(0)); .Internal(chartr(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
