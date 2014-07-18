package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltingrowconst extends TestBase {
	@Test
		public void testgrowconst1(){
		assertEval("argv <- list(list(list())); .Internal(growconst(argv[[1]]))");
	}
}
