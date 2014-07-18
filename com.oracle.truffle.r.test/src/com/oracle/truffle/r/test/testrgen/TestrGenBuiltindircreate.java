package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltindircreate extends TestBase {
	@Test
		public void testdircreate1(){
		assertEval("argv <- list(\'/home/lzhao/tmp/RtmptS6o2G/translations\', FALSE, FALSE, structure(511L, class = \'octmode\')); .Internal(dir.create(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
	}
}
