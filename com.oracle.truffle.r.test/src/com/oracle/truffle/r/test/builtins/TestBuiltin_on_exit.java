/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_on_exit extends TestBase {

    @Test
    public void testOnExit() {
        assertEval("n = function() { on.exit(print(\"test\")); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\", TRUE)); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\")); on.exit(); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\")); on.exit(print(\"test2\", TRUE)); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\")); on.exit(print(\"test2\")); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\", TRUE)); on.exit(print(\"test2\")); print(\"some\") }; n()");
        assertEval("n = function() { on.exit(print(\"test\")); on.exit(print(\"test2\")); print(\"some\"); on.exit() }; n()");
        assertEval("n = function() { on.exit() }; n()");
    }
}
