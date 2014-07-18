package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinmapply extends TestBase {
	@Test
		public void testmapply1(){
		assertEval("argv <- list(.Primitive(\'c\'), list(list(), list(), list()), NULL); .Internal(mapply(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
