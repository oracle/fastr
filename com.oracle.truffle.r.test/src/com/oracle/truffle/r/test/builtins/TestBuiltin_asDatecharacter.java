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
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_asDatecharacter extends TestBase {

    @Test
    public void testasDatecharacter1() {
        assertEval("argv <- structure(list(x = c('2007-11-06', NA)), .Names = 'x');do.call('as.Date.character', argv)");

        assertEval("{ as.Date('2017-02-29') }");
        assertEval("{ as.Date('2017-01-31') }");
        assertEval("{ as.Date('2017-01-32') }");
        assertEval("{ as.Date('2017-02-28') }");
        assertEval("{ as.Date('2016-02-29') }");
        assertEval("{ as.Date('2017-02-30') }");
        assertEval("{ as.Date('2017-03-31') }");
        assertEval("{ as.Date('2017-04-31') }");
        assertEval("{ as.Date('2017-05-31') }");
        assertEval("{ as.Date('2017-04-32') }");
        assertEval("{ as.Date('2017-00-10') }");
        assertEval("{ as.Date('2017-10-00') }");
        assertEval("{ as.Date('2017-12-31') }");
        assertEval("{ as.Date('2017-12-32') }");
        assertEval("{ as.Date('2017-13-01') }");

    }
}
