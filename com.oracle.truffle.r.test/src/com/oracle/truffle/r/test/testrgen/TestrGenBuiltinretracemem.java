package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinretracemem extends TestBase {
	@Test
		public void testretracemem1(){
		assertEval("argv <- list(FALSE, FALSE);retracemem(argv[[1]],argv[[2]]);");
	}
	@Test
		public void testretracemem2(){
		assertEval("argv <- list(structure(3.14159265358979, class = structure(\'3.14159265358979\', class = \'testit\')), structure(3.14159265358979, class = structure(\'3.14159265358979\', class = \'testit\')));retracemem(argv[[1]],argv[[2]]);");
	}
}
