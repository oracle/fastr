package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltindet extends TestBase {
	@Test
		public void testdet1(){
		assertEval("argv <- list(structure(c(FALSE, TRUE, TRUE, FALSE), .Dim = c(2L, 2L), .Dimnames = list(c(\'A\', \'B\'), c(\'A\', \'B\'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testdet2(){
		assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L), .Dimnames = list(c(\'A\', \'B\'), c(\'A\', \'B\'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
	}
	@Test
		public void testdet3(){
		assertEval("argv <- list(structure(c(2, 1, 1, 2), .Dim = c(2L, 2L), .Dimnames = list(c(\'A\', \'B\'), c(\'A\', \'B\'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
	}
}
