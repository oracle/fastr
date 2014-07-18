package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinstrtoi extends TestBase {
	@Test
		public void teststrtoi1(){
		assertEval("argv <- list(\'0777\', 8L); .Internal(strtoi(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtoi2(){
		assertEval("argv <- list(\'700\', 8L); .Internal(strtoi(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtoi3(){
		assertEval("argv <- list(c(\'0xff\', \'077\', \'123\'), 0L); .Internal(strtoi(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtoi4(){
		assertEval("argv <- list(\'1.3\', 16L); .Internal(strtoi(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtoi5(){
		assertEval("argv <- list(character(0), 8L); .Internal(strtoi(argv[[1]], argv[[2]]))");
	}
}
