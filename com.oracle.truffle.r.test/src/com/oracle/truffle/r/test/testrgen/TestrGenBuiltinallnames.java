package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinallnames extends TestBase {
	@Test
		public void testallnames1(){
		assertEval("argv <- list(quote(y ~ ((g1) * exp((log(g2/g1)) * (1 - exp(-k * (x - Ta)))/(1 - exp(-k * (Tb - Ta)))))), FALSE, -1L, TRUE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testallnames2(){
		assertEval("argv <- list(logical(0), logical(0), -1L, TRUE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testallnames3(){
		assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L)), TRUE, -1L, FALSE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testallnames4(){
		assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = \'data.frame\'), structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = \'data.frame\'), -1L, FALSE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
	@Test
		public void testallnames5(){
		assertEval("argv <- list(0.1, FALSE, -1L, TRUE); .Internal(all.names(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
}
