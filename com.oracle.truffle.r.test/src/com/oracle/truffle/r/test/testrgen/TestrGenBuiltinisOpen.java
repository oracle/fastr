package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinisOpen extends TestBase {
	@Test
		public void testisOpen1(){
		assertEval("argv <- list(structure(2L, class = c(\'terminal\', \'connection\')), 0L); .Internal(isOpen(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testisOpen2(){
		assertEval("argv <- list(structure(2L, class = c(\'terminal\', \'connection\')), 0L); .Internal(isOpen(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testisOpen3(){
		assertEval("argv <- list(FALSE, 2L); .Internal(isOpen(argv[[1]], argv[[2]]))");
	}
}
