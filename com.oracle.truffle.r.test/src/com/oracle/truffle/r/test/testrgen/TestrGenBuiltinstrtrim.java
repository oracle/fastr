package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinstrtrim extends TestBase {
	@Test
		public void teststrtrim1(){
		assertEval("argv <- list(c(\'\\\'time\\\'\', \'\\\'status\\\'\'), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtrim2(){
		assertEval("argv <- list(\'2014-03-17 14:47:20\', 8); .Internal(strtrim(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtrim3(){
		assertEval("argv <- list(c(\'\\\'1\\\'\', \'\\\'2\\\'\', NA), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtrim4(){
		assertEval("argv <- list(c(\'\\\'gray17\\\'\', \'\\\'grey17\\\'\'), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtrim5(){
		assertEval("argv <- list(structure(\'\\\'@CRAN@\\\'\', .Names = \'CRAN\'), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtrim6(){
		assertEval("argv <- list(\'FALSE\', FALSE); .Internal(strtrim(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtrim7(){
		assertEval("argv <- list(c(\'\\\'1\\\'\', \'\\\'2\\\'\', NA), 128); .Internal(strtrim(argv[[1]], argv[[2]]))");
	}
	@Test
		public void teststrtrim8(){
		assertEval("argv <- list(character(0), 40L); .Internal(strtrim(argv[[1]], argv[[2]]))");
	}
}
