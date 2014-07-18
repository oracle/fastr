package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltingettext extends TestBase {
	@Test
		public void testgettext1(){
		assertEval("argv <- list(NULL, \'Loading required package: %s\'); .Internal(gettext(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testgettext2(){
		assertEval("argv <- list(NULL, \'\'); .Internal(gettext(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testgettext3(){
		assertEval("argv <- list(NULL, \'The following object is masked from ‘package:base’:\\n\\n    det\\n\'); .Internal(gettext(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testgettext4(){
		assertEval("argv <- list(NULL, c(\'/\', \' not meaningful for factors\')); .Internal(gettext(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testgettext5(){
		assertEval("argv <- list(NULL, character(0)); .Internal(gettext(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testgettext6(){
		assertEval("argv <- list(NULL, NULL); .Internal(gettext(argv[[1]], argv[[2]]))");
	}
}
