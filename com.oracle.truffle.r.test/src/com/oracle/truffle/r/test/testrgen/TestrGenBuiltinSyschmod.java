package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinSyschmod extends TestBase {
	@Test
		public void testSyschmod1(){
		assertEval("argv <- list(character(0), structure(integer(0), class = \'octmode\'), TRUE); .Internal(Sys.chmod(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
