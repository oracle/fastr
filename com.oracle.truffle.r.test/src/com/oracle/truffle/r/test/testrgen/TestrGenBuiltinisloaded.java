package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinisloaded extends TestBase {
	@Test
		public void testisloaded1(){
		assertEval("argv <- list(\'PDF\', \'\', \'External\'); .Internal(is.loaded(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testisloaded2(){
		assertEval("argv <- list(\'supsmu\', \'\', \'\'); .Internal(is.loaded(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
