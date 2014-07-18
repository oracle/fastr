package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinlengthassign extends TestBase {
	@Test
		public void testlengthassign1(){
		assertEval("argv <- list(c(\'A\', \'B\'), value = 5);`length<-`(argv[[1]],argv[[2]]);");
	}
	@Test
		public void testlengthassign2(){
		assertEval("argv <- list(list(list(2, 2, 6), list(2, 2, 0)), value = 0);`length<-`(argv[[1]],argv[[2]]);");
	}
	@Test
		public void testlengthassign3(){
		assertEval("argv <- list(list(list(2, 2, 6), list(1, 3, 9), list(1, 3, -1)), value = 1);`length<-`(argv[[1]],argv[[2]]);");
	}
	@Test
		public void testlengthassign4(){
		assertEval("argv <- list(c(28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1), value = 0L);`length<-`(argv[[1]],argv[[2]]);");
	}
	@Test
		public void testlengthassign5(){
		assertEval("argv <- list(c(0L, 1L, 2L, 3L, 4L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 5L, 6L, 0L, 1L, 2L, 3L, 4L, 5L, 6L), value = 0L);`length<-`(argv[[1]],argv[[2]]);");
	}
	@Test
		public void testlengthassign6(){
		assertEval("argv <- list(list(), value = 0L);`length<-`(argv[[1]],argv[[2]]);");
	}
}
