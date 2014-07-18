package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinisNamespaceEnv extends TestBase {
	@Test
		public void testisNamespaceEnv1(){
		assertEval("argv <- list(FALSE); .Internal(isNamespaceEnv(argv[[1]]))");
	}
	@Test
		public void testisNamespaceEnv2(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(isNamespaceEnv(argv[[1]]))");
	}
	@Test
		public void testisNamespaceEnv3(){
		assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = \'data.frame\')); .Internal(isNamespaceEnv(argv[[1]]))");
	}
}
