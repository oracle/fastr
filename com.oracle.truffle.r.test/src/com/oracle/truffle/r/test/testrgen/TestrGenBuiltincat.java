package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltincat extends TestBase {
	@Test
		public void testcat1(){
		assertEval("argv <- list(\'head\\n\', 1:2, \'\\n\', 3:4, file = \'foo4\');cat(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]]);");
	}
	@Test
		public void testcat2(){
		assertEval("argv <- list(list(\'Loading required package: splines\\n\'), structure(2L, class = c(\'terminal\', \'connection\')), \'\', FALSE, NULL, FALSE); .Internal(cat(argv[[1]], argv[[2]], argv[[3]], argv[[4]], argv[[5]], argv[[6]]))");
	}
	@Test
		public void testcat3(){
		assertEval("argv <- list(\'%comment\\n\\n%another\\n%\\n%\\n\', \'C1\\tC2\\tC3\\n\\\'Panel\\\'\\t\\\'Area Examined\\\'\\t\\\'% Blemishes\\\'\\n\', \'\\\'1\\\'\\t\\\'0.8\\\'\\t\\\'3\\\'\\n\', \'\\\'2\\\'\\t\\\'0.6\\\'\\t\\\'2\\\'\\n\', \'\\\'3\\\'\\t\\\'0.8\\\'\\t\\\'3\\\'\\n\', file = \'test.dat\', sep = \'\');cat(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]],argv[[7]]);");
	}
	@Test
		public void testcat4(){
		assertEval("argv <- list(\'#comment\\n\\n#another\\n#\\n#\\n\', \'C1\\tC2\\tC3\\n\\\'Panel\\\'\\t\\\'Area Examined\\\'\\t\\\'# Blemishes\\\'\\n\', \'\\\'1\\\'\\t\\\'0.8\\\'\\t\\\'3\\\'\\n\', \'\\\'2\\\'\\t\\\'0.6\\\'\\t\\\'2\\\'\\n\', \'\\\'3\\\'\\t\\\'0.8\\\'\\t\\\'3\\\'\\n\', file = \'test.dat\', sep = \'\');cat(argv[[1]],argv[[2]],argv[[3]],argv[[4]],argv[[5]],argv[[6]],argv[[7]]);");
	}
	@Test
		public void testcat5(){
		assertEval("argv <- list(\'head\\n\', file = \'foo2\');cat(argv[[1]],argv[[2]]);");
	}
}
