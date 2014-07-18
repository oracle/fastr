package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinlistfiles extends TestBase {
	@Test
		public void testlistfiles1(){
		assertEval("argv <- list(\'.\', \'myTst_.*tar\\\\.gz$\', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
	@Test
		public void testlistfiles2(){
		assertEval("argv <- list(\'./myTst/data\', NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
	@Test
		public void testlistfiles3(){
		assertEval("argv <- list(\'.\', \'^CITATION.*\', FALSE, FALSE, TRUE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
	@Test
		public void testlistfiles4(){
		assertEval("argv <- list(\'mgcv\', NULL, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE); .Internal(list.files(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
}
