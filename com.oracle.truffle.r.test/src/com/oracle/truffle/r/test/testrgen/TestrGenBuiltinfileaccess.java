package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinfileaccess extends TestBase {
	@Test
		public void testfileaccess1(){
		assertEval("argv <- list(character(0), 0); .Internal(file.access(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testfileaccess2(){
		assertEval("argv <- list(\'/home/lzhao/R/x86_64-unknown-linux-gnu-library/3.0/FALSE\', 5); .Internal(file.access(argv[[1]], argv[[2]]))");
	}
}
