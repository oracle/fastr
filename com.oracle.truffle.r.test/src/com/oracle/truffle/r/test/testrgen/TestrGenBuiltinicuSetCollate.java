package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinicuSetCollate extends TestBase {
	@Test
		public void testicuSetCollate1(){
		assertEval(" .Internal(icuSetCollate())");
	}
}
