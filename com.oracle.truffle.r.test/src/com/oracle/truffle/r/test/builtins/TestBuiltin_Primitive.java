/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_Primitive extends TestBase {

    @Test
    public void testPrimitive1() {
        // our c() primitive is missing an argument
        assertEval(Ignored.ImplementationError, "argv <- list('c');.Primitive(argv[[1]]);");

        assertEval(".Primitive(c('c', 'b'))");
        assertEval(".Primitive('any')");
        assertEval(".Primitive('foo')");
        assertEval(".Primitive('complex')");
        assertEval(".Primitive(1)");
    }
}
