package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinfileexists extends TestBase {
	@Test
		public void testfileexists1(){
		assertEval("argv <- list(\'/home/lzhao/hg/r-instrumented/library/methods/data/Rdata.rdb\'); .Internal(file.exists(argv[[1]]))");
	}
	@Test
		public void testfileexists2(){
		assertEval("argv <- list(c(\'src/Makevars\', \'src/Makevars.in\')); .Internal(file.exists(argv[[1]]))");
	}
	@Test
		public void testfileexists3(){
		assertEval("argv <- list(character(0)); .Internal(file.exists(argv[[1]]))");
	}
}
