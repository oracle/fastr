package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinencodeString extends TestBase {
	@Test
		public void testencodeString1(){
		assertEval("argv <- list(c(\'1\', \'2\', NA), 0L, \'\\\'\', 0L, FALSE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString3(){
		assertEval("argv <- list(c(\'a\', \'ab\', \'abcde\'), NA, \'\', 0L, TRUE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString4(){
		assertEval("argv <- list(c(\'a\', \'ab\', \'abcde\'), NA, \'\', 1L, TRUE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString5(){
		assertEval("argv <- list(c(\'1\', \'2\', NA), 0L, \'\\\'\', 0L, FALSE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString6(){
		assertEval("argv <- list(c(\'NA\', \'a\', \'b\', \'c\', \'d\', NA), 0L, \'\', 0L, TRUE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString8(){
		assertEval("argv <- list(c(\'FALSE\', NA), 0L, \'\', 0L, TRUE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString9(){
		assertEval("argv <- list(structure(\'integer(0)\', .Names = \'c0\', row.names = character(0)), 0L, \'\', 0L, TRUE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString11(){
		assertEval("argv <- list(structure(character(0), .Dim = c(0L, 0L)), 0L, \'\', 0L, TRUE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString12(){
		assertEval("argv <- list(character(0), logical(0), \'\', 0L, TRUE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
	@Test
		public void testencodeString13(){
		assertEval("argv <- list(structure(\'integer(0)\', .Names = \'c0\', row.names = character(0)), structure(list(c0 = structure(integer(0), .Label = character(0), class = \'factor\')), .Names = \'c0\', row.names = character(0), class = structure(\'integer(0)\', .Names = \'c0\')), \'\', 0L, TRUE); .Internal(encodeString(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]]))");
	}
}
