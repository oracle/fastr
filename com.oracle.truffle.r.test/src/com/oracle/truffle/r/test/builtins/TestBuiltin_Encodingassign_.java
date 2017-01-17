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

public class TestBuiltin_Encodingassign_ extends TestBase {

    @Test
    public void testEncodingassign_1() {
        assertEval("argv <- structure(list(x = 'abc', value = 'UTF-8'), .Names = c('x',     'value'));do.call('Encoding<-', argv)");
    }

    @Test
    public void testEncoding() {
        assertEval(Output.IgnoreErrorContext, "{ x<-42; Encoding(x)<-\"UTF-8\" }");
        assertEval(Output.IgnoreErrorContext, "{ x<-\"foo\"; Encoding(x)<-NULL }");
        assertEval(Output.IgnoreErrorContext, "{ x<-\"foo\"; Encoding(x)<-42 }");
        assertEval(Output.IgnoreErrorContext, "{ x<-\"foo\"; Encoding(x)<-character() }");
    }
}
