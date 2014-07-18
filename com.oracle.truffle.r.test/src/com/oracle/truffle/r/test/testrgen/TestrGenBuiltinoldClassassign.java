package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinoldClassassign extends TestBase {
	@Test
		public void testoldClassassign1(){
		assertEval("argv <- list(list(), NULL);`oldClass<-`(argv[[1]],argv[[2]]);");
	}
	@Test
		public void testoldClassassign2(){
		assertEval("argv <- list(NULL, NULL);`oldClass<-`(argv[[1]],argv[[2]]);");
	}
}
