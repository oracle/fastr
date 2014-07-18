package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinpathexpand extends TestBase {
	@Test
		public void testpathexpand1(){
		assertEval("argv <- list(\'/tmp/RtmptPgrXI/Pkgs/pkgA\'); .Internal(path.expand(argv[[1]]))");
	}
	@Test
		public void testpathexpand2(){
		assertEval("argv <- list(c(\'/home/lzhao/hg/r-instrumented/tests/compiler.Rcheck\', \'/home/lzhao/R/x86_64-unknown-linux-gnu-library/3.0\')); .Internal(path.expand(argv[[1]]))");
	}
	@Test
		public void testpathexpand3(){
		assertEval("argv <- list(character(0)); .Internal(path.expand(argv[[1]]))");
	}
}
