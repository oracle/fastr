package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinwhich extends TestBase {
	@Test
		public void testwhich1(){
		assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE)); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich2(){
		assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L), .Dimnames = list(c(\'A\', \'B\'), c(\'A\', \'B\')))); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich3(){
		assertEval("argv <- list(c(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE)); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich4(){
		assertEval("argv <- list(structure(TRUE, .Names = \'V1\')); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich5(){
		assertEval("argv <- list(logical(0)); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich6(){
		assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L))); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich7(){
		assertEval("argv <- list(c(FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE)); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich8(){
		assertEval("argv <- list(structure(c(FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE), .Dim = 12L)); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich9(){
		assertEval("argv <- list(structure(FALSE, .Names = \'signature-class.Rd\')); .Internal(which(argv[[1]]))");
	}
	@Test
		public void testwhich10(){
		assertEval("argv <- list(logical(0)); .Internal(which(argv[[1]]))");
	}
}
