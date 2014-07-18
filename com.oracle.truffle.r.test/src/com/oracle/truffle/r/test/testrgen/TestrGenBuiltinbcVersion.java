package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbcVersion extends TestBase {
	@Test
		public void testbcVersion1(){
		assertEval(" .Internal(bcVersion())");
	}
}
