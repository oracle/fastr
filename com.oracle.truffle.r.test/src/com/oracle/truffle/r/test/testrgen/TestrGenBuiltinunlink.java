package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinunlink extends TestBase {
	@Test
		public void testunlink1(){
		assertEval("argv <- list(\'/tmp/RtmptPgrXI/Pkgs\', TRUE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testunlink2(){
		assertEval("argv <- list(character(0), FALSE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testunlink3(){
		assertEval("argv <- list(\'/home/lzhao/tmp/Rtmphu0Cms/file74e1676db2e7\', FALSE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
	}
	@Test
		public void testunlink4(){
		assertEval("argv <- list(character(0), FALSE, FALSE); .Internal(unlink(argv[[1]], argv[[2]], argv[[3]]))");
	}
}
