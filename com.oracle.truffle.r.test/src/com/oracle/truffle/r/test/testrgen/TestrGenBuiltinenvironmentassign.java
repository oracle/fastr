package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinenvironmentassign extends TestBase {
	@Test
		public void testenvironmentassign1(){
		assertEval("argv <- list(NULL, NULL);`environment<-`(argv[[1]],argv[[2]]);");
	}
}
