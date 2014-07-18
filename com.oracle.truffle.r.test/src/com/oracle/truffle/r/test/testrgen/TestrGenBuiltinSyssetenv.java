package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinSyssetenv extends TestBase {
	@Test
		public void testSyssetenv1(){
		assertEval("argv <- list(\'_R_NS_LOAD_\', \'Matrix\'); .Internal(Sys.setenv(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testSyssetenv2(){
		assertEval("argv <- list(\'_R_NS_LOAD_\', \'methods\'); .Internal(Sys.setenv(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testSyssetenv3(){
		assertEval("argv <- list(c(\'BIBINPUTS\', \'add\'), c(\'.:.:/home/lzhao/hg/r-instrumented/share/texmf/bibtex/bib::/home/lzhao/hg/r-instrumented/share/texmf/bibtex/bib:\', \'TRUE\')); .Internal(Sys.setenv(argv[[1]], argv[[2]]))");
	}
}
