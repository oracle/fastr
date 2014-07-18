package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinfileremove extends TestBase {
	@Test
		public void testfileremove1(){
		assertEval("argv <- list(character(0)); .Internal(file.remove(argv[[1]]))");
	}
}
