package com.oracle.truffle.r.test.testrgen;

import java.util.*;
import org.junit.*;
import com.oracle.truffle.r.test.*;
public class  TestrGenBuiltinbasename extends TestBase {
	@Test
		public void testbasename1(){
		assertEval("argv <- list(\'/home/roman/r-instrumented/library/base/help/DateTimeClasses\'); .Internal(basename(argv[[1]]))");
	}
	@Test
		public void testbasename2(){
		assertEval("argv <- list(structure(\'myTst\', .Names = \'\')); .Internal(basename(argv[[1]]))");
	}
	@Test
		public void testbasename3(){
		assertEval("argv <- list(c(\'file55711ba85492.R\', \'/file55711ba85492.R\')); .Internal(basename(argv[[1]]))");
	}
	@Test
		public void testbasename4(){
		assertEval("argv <- list(character(0)); .Internal(basename(argv[[1]]))");
	}
	@Test
		public void testbasename5(){
		assertEval("argv <- list(structure(\'/home/lzhao/hg/r-instrumented/library/utils\', .Names = \'Dir\')); .Internal(basename(argv[[1]]))");
	}
	@Test
		public void testbasename6(){
		assertEval("argv <- list(\'tk_messageBox.Rd\'); .Internal(basename(argv[[1]]))");
	}
	@Test
		public void testbasename7(){
		assertEval("argv <- list(c(\'.\', \'.\', \'.\', \'.\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'R\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'data\', \'inst\', \'inst\', \'inst/doc\', \'inst/doc\', \'inst/doc\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'man\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb/figures\', \'noweb/figures\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb/rates\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'noweb\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'src\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'tests\', \'vignettes\', \'vignettes\', \'vignettes\')); .Internal(basename(argv[[1]]))");
	}
	@Test
		public void testbasename8(){
		assertEval("argv <- list(character(0)); .Internal(basename(argv[[1]]))");
	}
}
