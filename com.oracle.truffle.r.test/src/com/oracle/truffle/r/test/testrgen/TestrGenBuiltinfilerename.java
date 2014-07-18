package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinfilerename extends TestBase {
	@Test
		public void testfilerename1(){
		assertEval("argv <- list(character(0), character(0)); .Internal(file.rename(argv[[1]], argv[[2]]))");
	}
}
