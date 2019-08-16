/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
        assertEval("options(list(NULL));");
        assertEval("options(NA);");
        // IMHO ReferenceError since NULL makes little sense as parameter value
        // here just like NA which outputs 'invalid argument' both in GnuR and FastR.
        // Expected output:
        // FastR output: Error in options(NULL) : invalid argument
        assertEval(Ignored.ReferenceError, "options(NULL);");
        assertEval(Ignored.ReferenceError, "argv <- list(NULL); .Internal(options(argv[[1]]))");
    }

    @Test
    public void testOptions() {
        assertEval("{ getOption(NULL) }");
        assertEval("{ getOption(character()) }");
        assertEval("{ options(\"timeout\", \"width\") }");
        assertEval("{ options(options(digits = 5)) }");
        assertEval("{ options(list(aaa = 42, bbb = 'hello')); x <- options('aaa', 'bbb'); options(aaa=NULL, bbb=NULL); x }");
        assertEval("{ options(lll = list(aaa = 42, bbb = 'hello')); x <- options('lll', 'aaa', 'bbb'); options(aaa=NULL, bbb=NULL, lll=NULL); x }");
    }

    @Test
    public void testEditor() {
        assertEval("{ f<-function(){}; options(editor=f); identical(getOption(\"editor\"), f) }");
        assertEval("{ options(editor=\"vi\"); identical(getOption(\"editor\"), \"vi\") }");
        assertEval("{ options(editor=NULL); identical(getOption(\"editor\"), NULL) }");
        assertEval("{ options(editor=\"\") }");
    }

    @Test
    public void testPrompt() {
        assertEval(Ignored.NewRVersionMigration, "{ options(prompt=NULL) }");
        assertEval("{ options(prompt=\"abc\"); identical(getOption(\"prompt\"), \"abc\") }");
    }

    @Test
    public void testContinue() {
        assertEval(Ignored.NewRVersionMigration, "{ options(continue=NULL) }");
        assertEval("{ options(continue=\"abc\"); identical(getOption(\"continue\"), \"abc\") }");
    }
}
