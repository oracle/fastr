package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinagrep extends TestBase {
	@Test
		public void testagrep1(){
		assertEval("argv <- list(\'x86_64-linux-gnu\', \'x86_64-linux-gnu\', FALSE, FALSE, c(1L, 1L, 1L), c(0.1, NA, NA, NA, NA), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
	@Test
		public void testagrep2(){
		assertEval("argv <- list(\'x86_64-linux-gnu\', \'x86_64-linux-gnu\', FALSE, FALSE, c(1L, 1L, 1L), c(0.1, NA, NA, NA, NA), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
	@Test
		public void testagrep3(){
		assertEval("argv <- list(\'lasy\', c(\' 1 lazy 2\', \'1 lasy 2\'), FALSE, FALSE, c(1L, 1L, 1L), structure(c(NA, 0.1, 0.1, 0, 0.1), .Names = c(\'cost\', \'insertions\', \'deletions\', \'substitutions\', \'all\')), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
	@Test
		public void testagrep4(){
		assertEval("argv <- list(\'laysy\', c(\'1 lazy\', \'1\', \'1 LAZY\'), FALSE, TRUE, c(1L, 1L, 1L), c(2, NA, NA, NA, NA), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
	@Test
		public void testagrep5(){
		assertEval("argv <- list(\'laysy\', c(\'1 lazy\', \'1\', \'1 LAZY\'), TRUE, FALSE, c(1L, 1L, 1L), c(2, NA, NA, NA, NA), FALSE, TRUE); .Internal(agrep(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]], argv[[7]], argv[[8]]))");
	}
}
