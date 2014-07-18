package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsubstrassign extends TestBase {
	@Test
		public void testsubstrassign1(){
		assertEval("argv <- list(\'(0,5]\', 1L, 1L, \'[\'); .Internal(`substr<-`(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testsubstrassign2(){
		assertEval("argv <- list(c(\'asfef\', \'qwerty\', \'yuiop[\', \'b\', \'stuff.blah.yech\'), 2L, 1000000L, c(\'..\', \'+++\')); .Internal(`substr<-`(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
}
