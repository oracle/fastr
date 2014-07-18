package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinnormalizePath extends TestBase {
	@Test
		public void testnormalizePath1(){
		assertEval("argv <- list(c(\'/home/lzhao/hg/r-instrumented/library\', \'/home/lzhao/R/x86_64-unknown-linux-gnu-library/3.0\', \'/home/lzhao/hg/r-instrumented/library\'), \'/\', NA); .Internal(normalizePath(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
