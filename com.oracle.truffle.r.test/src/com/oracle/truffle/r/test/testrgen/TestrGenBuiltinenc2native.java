package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinenc2native extends TestBase {
	@Test
		public void testenc2native1(){
		assertEval("argv <- list(character(0));enc2native(argv[[1]]);");
	}
	@Test
		public void testenc2native2(){
		assertEval("argv <- list(character(0));enc2native(argv[[1]]);");
	}
	@Test
		public void testenc2native3(){
		assertEval("argv <- list(structure(character(0), .Names = character(0)));enc2native(argv[[1]]);");
	}
	@Test
		public void testenc2native4(){
		assertEval("argv <- list(\'José Pinheiro [aut] (S version)\');enc2native(argv[[1]]);");
	}
}
