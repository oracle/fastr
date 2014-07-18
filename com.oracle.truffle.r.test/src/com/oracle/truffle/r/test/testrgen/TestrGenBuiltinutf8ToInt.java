package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinutf8ToInt extends TestBase {
	@Test
		public void testutf8ToInt1(){
		assertEval("argv <- list(\'lasy\'); .Internal(utf8ToInt(argv[[1]]))");
	}
}
