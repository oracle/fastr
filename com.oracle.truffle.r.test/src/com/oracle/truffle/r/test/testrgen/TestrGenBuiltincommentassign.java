package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltincommentassign extends TestBase {
	@Test
		public void testcommentassign1(){
		assertEval("argv <- list(structure(1:12, .Dim = 3:4, comment = c(\'This is my very important data from experiment #0234\', \'Jun 5, 1998\')), c(\'This is my very important data from experiment #0234\', \'Jun 5, 1998\')); .Internal(`comment<-`(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testcommentassign2(){
		assertEval("argv <- list(character(0), NULL); .Internal(`comment<-`(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testcommentassign3(){
		assertEval("argv <- list(logical(0), NULL); .Internal(`comment<-`(argv[[1]], argv[[2]]))");
	}
}
