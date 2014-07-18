package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinlistdirs extends TestBase {
	@Test
		public void testlistdirs1(){
		assertEval("argv <- list(\'/home/lzhao/hg/r-instrumented/library/rpart/doc\', TRUE, FALSE); .Internal(list.dirs(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
