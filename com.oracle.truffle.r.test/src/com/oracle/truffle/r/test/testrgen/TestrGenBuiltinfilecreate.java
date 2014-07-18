package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinfilecreate extends TestBase {
	@Test
		public void testfilecreate1(){
		assertEval("argv <- list(\'codetools-manual.log\', TRUE); .Internal(file.create(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testfilecreate2(){
		assertEval("argv <- list(character(0), TRUE); .Internal(file.create(argv[[1]], argv[[2]]))");
	}
}
