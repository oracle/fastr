package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltingetSymbolInfo extends TestBase {
	@Test
		public void testgetSymbolInfo1(){
		assertEval("argv <- list(\'FALSE\', \'\', FALSE); .Internal(getSymbolInfo(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
