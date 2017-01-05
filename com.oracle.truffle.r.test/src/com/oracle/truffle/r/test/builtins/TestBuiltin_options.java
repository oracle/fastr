/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_options extends TestBase {

    @Test
    public void testoptions1() {
        assertEval("argv <- list('survfit.print.n'); .Internal(options(argv[[1]]))");
    }

    @Test
    public void testoptions2() {
        assertEval("argv <- list('contrasts'); .Internal(options(argv[[1]]))");
    }

    @Test
    public void testoptions3() {
        assertEval("argv <- list('str'); .Internal(options(argv[[1]]))");
    }

    @Test
    public void testoptions4() {
        assertEval("argv <- list('ts.eps'); .Internal(options(argv[[1]]))");
    }

    @Test
    public void testoptions5() {
        assertEval(Ignored.Unknown, "argv <- list(NULL); .Internal(options(argv[[1]]))");
    }

    @Test
    public void testOptions() {
        assertEval("{ getOption(NULL) }");
        assertEval("{ getOption(character()) }");
        assertEval("{ options(\"timeout\", \"width\") }");
    }

    @Test
    public void testEditor() {
        assertEval("{ f<-function(){}; options(editor=f); identical(getOption(\"editor\"), f) }");
        assertEval("{ options(editor=\"vi\"); identical(getOption(\"editor\"), \"vi\") }");
        assertEval("{ options(editor=NULL); identical(getOption(\"editor\"), NULL) }");
    }

    @Test
    public void testPrompt() {
        assertEval(Ignored.WrongCaller, "{ options(prompt=NULL) }");
        assertEval("{ options(prompt=\"abc\"); identical(getOption(\"prompt\"), \"abc\") }");
    }

    @Test
    public void testContinue() {
        assertEval(Ignored.WrongCaller, "{ options(continue=NULL) }");
        assertEval("{ options(continue=\"abc\"); identical(getOption(\"continue\"), \"abc\") }");
    }
}
