package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinseterrmessage extends TestBase {
	@Test
		public void testseterrmessage1(){
		assertEval("argv <- list(\'Error in cor(rnorm(10), NULL) : \\n  supply both 'x' and 'y' or a matrix-like 'x'\\n\'); .Internal(seterrmessage(argv[[1]]))");
	}
	@Test
		public void testseterrmessage2(){
		assertEval("argv <- list(\'Error in as.POSIXlt.character(x, tz, ...) : \\n  character string is not in a standard unambiguous format\\n\'); .Internal(seterrmessage(argv[[1]]))");
	}
	@Test
		public void testseterrmessage3(){
		assertEval("argv <- list(\'Error in validObject(.Object) : \\n  invalid class “trackCurve” object: Unequal x,y lengths: 20, 10\\n\'); .Internal(seterrmessage(argv[[1]]))");
	}
}
