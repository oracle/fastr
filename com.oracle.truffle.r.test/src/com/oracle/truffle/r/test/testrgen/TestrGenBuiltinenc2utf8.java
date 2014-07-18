package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinenc2utf8 extends TestBase {
	@Test
		public void testenc2utf81(){
		assertEval("argv <- list(\'Add Text to a Plot\');enc2utf8(argv[[1]]);");
	}
	@Test
		public void testenc2utf82(){
		assertEval("argv <- list(\'Modes\');enc2utf8(argv[[1]]);");
	}
	@Test
		public void testenc2utf83(){
		assertEval("argv <- list(c(\'\', \'(De)compress I/O Through Connections\'));enc2utf8(argv[[1]]);");
	}
	@Test
		public void testenc2utf84(){
		assertEval("argv <- list(character(0));enc2utf8(argv[[1]]);");
	}
}
