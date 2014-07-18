package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinSysglob extends TestBase {
	@Test
		public void testSysglob1(){
		assertEval("argv <- list(\'/home/lzhao/hg/r-instrumented/src/library/utils/man/unix/*.rd\', FALSE); .Internal(Sys.glob(argv[[1]], argv[[2]]))");
	}
}
