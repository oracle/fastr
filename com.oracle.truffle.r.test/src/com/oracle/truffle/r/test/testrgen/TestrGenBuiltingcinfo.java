package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltingcinfo extends TestBase {
	@Test
		public void testgcinfo1(){
		assertEval("argv <- list(list()); .Internal(gcinfo(argv[[1]]))");
	}
	@Test
		public void testgcinfo2(){
		assertEval("argv <- list(FALSE); .Internal(gcinfo(argv[[1]]))");
	}
}
