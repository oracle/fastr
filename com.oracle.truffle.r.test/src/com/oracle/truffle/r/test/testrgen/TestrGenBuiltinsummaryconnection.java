package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinsummaryconnection extends TestBase {
	@Test
		public void testsummaryconnection1(){
		assertEval("argv <- list(structure(2L, class = c(\'terminal\', \'connection\'))); .Internal(summary.connection(argv[[1]]))");
	}
}
