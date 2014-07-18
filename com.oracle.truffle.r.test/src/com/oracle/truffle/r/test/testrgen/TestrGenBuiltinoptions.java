package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinoptions extends TestBase {
	@Test
		public void testoptions1(){
		assertEval("argv <- list(\'survfit.print.n\'); .Internal(options(argv[[1]]))");
	}
	@Test
		public void testoptions2(){
		assertEval("argv <- list(\'contrasts\'); .Internal(options(argv[[1]]))");
	}
	@Test
		public void testoptions3(){
		assertEval("argv <- list(\'str\'); .Internal(options(argv[[1]]))");
	}
	@Test
		public void testoptions4(){
		assertEval("argv <- list(\'ts.eps\'); .Internal(options(argv[[1]]))");
	}
	@Test
		public void testoptions5(){
		assertEval("argv <- list(NULL); .Internal(options(argv[[1]]))");
	}
	@Test
		public void testoptions6(){
		assertEval("argv <- list(\'ts.eps\'); .Internal(options(argv[[1]]))");
	}
}
