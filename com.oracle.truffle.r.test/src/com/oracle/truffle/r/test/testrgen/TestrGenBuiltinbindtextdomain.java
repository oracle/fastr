package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbindtextdomain extends TestBase {
	@Test
		public void testbindtextdomain1(){
		assertEval("argv <- list(\'splines\', \'/home/roman/r-instrumented/library/translations\'); .Internal(bindtextdomain(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testbindtextdomain2(){
		assertEval("argv <- list(\'utils\', \'/home/lzhao/hg/r-instrumented/library/translations\'); .Internal(bindtextdomain(argv[[1]], argv[[2]]))");
	}
}
