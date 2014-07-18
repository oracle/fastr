package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinregexec extends TestBase {
	@Test
		public void testregexec1(){
		assertEval("argv <- list(\'^(([^:]+)://)?([^:/]+)(:([0-9]+))?(/.*)\', \'http://stat.umn.edu:80/xyz\', FALSE, FALSE, FALSE); .Internal(regexec(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
}
