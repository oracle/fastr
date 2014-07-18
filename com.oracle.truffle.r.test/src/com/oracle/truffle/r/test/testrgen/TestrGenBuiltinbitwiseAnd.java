package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbitwiseAnd extends TestBase {
	@Test
		public void testbitwiseAnd1(){
		assertEval("argv <- list(structure(c(420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 493L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L), class = \'octmode\'), structure(256L, class = \'octmode\')); .Internal(bitwiseAnd(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testbitwiseAnd2(){
		assertEval("argv <- list(structure(integer(0), class = \'hexmode\'), structure(integer(0), class = \'hexmode\')); .Internal(bitwiseAnd(argv[[1]], argv[[2]]))");
	}
}
