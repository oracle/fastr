package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinngettext extends TestBase {
	@Test
		public void testngettext1(){
		assertEval("argv <- list(1L, \'%s is not TRUE\', \'%s are not all TRUE\', NULL); .Internal(ngettext(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testngettext2(){
		assertEval("argv <- list(2L, \'%s is not TRUE\', \'%s are not all TRUE\', NULL); .Internal(ngettext(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
}
