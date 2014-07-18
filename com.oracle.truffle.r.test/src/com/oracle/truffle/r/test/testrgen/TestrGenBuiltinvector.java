package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinvector extends TestBase {
	@Test
		public void testvector1(){
		assertEval("argv <- list(\'integer\', 0L); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector2(){
		assertEval("argv <- list(\'double\', 17.1); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector3(){
		assertEval("argv <- list(\'list\', 1L); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector4(){
		assertEval("argv <- list(\'logical\', 15L); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector5(){
		assertEval("argv <- list(\'double\', 2); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector6(){
		assertEval("argv <- list(\'integer\', 0L); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector7(){
		assertEval("argv <- list(\'logical\', 15L); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector8(){
		assertEval("argv <- list(\'double\', 2); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector9(){
		assertEval("argv <- list(\'raw\', 0L); .Internal(vector(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testvector10(){
		assertEval("argv <- list(\'list\', structure(1L, .Names = \'\\\\c\')); .Internal(vector(argv[[1]], argv[[2]]))");
	}
}
