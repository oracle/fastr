package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinflush extends TestBase {
	@Test
		public void testflush1(){
		assertEval("argv <- list(structure(1L, class = c(\'terminal\', \'connection\'))); .Internal(flush(argv[[1]]))");
	}
	@Test
		public void testflush2(){
		assertEval("argv <- list(structure(2L, class = c(\'terminal\', \'connection\'))); .Internal(flush(argv[[1]]))");
	}
}
