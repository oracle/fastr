/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.methods;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestSubstituteDirect extends TestBase {

    @Test
    public void basicTests() {
        assertEval("substituteDirect(NULL, list(x=1))");
        assertEval("substituteDirect(NA, list(x=1))");
        assertEval("substituteDirect(environment(), list(x=1))");
        assertEval("substituteDirect(1, 1)");
        assertEval("substituteDirect(1, 1, 1)");
        assertEval("substituteDirect(object=1, frame=1)");
        assertEval("substituteDirect(object=1, frame=1, cleanFunction=1)");

        assertEval("substituteDirect(1, frame=NULL)");
        assertEval("substituteDirect(1, frame=NA)");
        assertEval("substituteDirect(object=1, frame=c(list(1)))");
        assertEval("substituteDirect(object=1, frame=list(c(list(1))))");
        assertEval("substituteDirect(object=1, frame=list(list(1)))");

        assertEval("a<-substituteDirect(quote(x+1), NULL); a");
        assertEval("a<-substituteDirect(quote(x+1), NA); a");
        assertEval("a<-substituteDirect(quote(x+1), list(1)); a");
        assertEval("a<-substituteDirect(quote(x+1), list(x=1)); a");
        assertEval("a<-substituteDirect(quote(x+1), list(y=1)); a");
        assertEval("a<-substituteDirect(quote(x+1), c(list(x=1), 'breakme')); a");
        assertEval("a<-substituteDirect(quote(x+1), c(c(list(x=1)))); a");
        assertEval("a<-substituteDirect(quote(x+1), list(c(c(list(x=1))))); a");
        assertEval("a<-substituteDirect(quote(x+1), list(list(x=1))); a");
        assertEval("a<-substituteDirect(quote(x+1), c(list(x=1, 1))); a");
        assertEval("a<-substituteDirect(quote(x+y), c(list(x=1), list(y=1))); a");
        assertEval("substituteDirect(quote(x+1), frame=environment())");
        assertEval("f<-function() {}; substituteDirect(quote(x+1), frame=f)");
        assertEval("substituteDirect(quote(x+1), frame=setClass('a'))");

        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=TRUE)");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=c(TRUE, 'breakme'))");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=c(c(TRUE, 'breakme')))");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=c(TRUE, FALSE))");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=list(c(TRUE), 'breakme'))");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=NA)");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=NULL)");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction='a')");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=c('1'))");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=c(1))");
        assertEval("substituteDirect(quote(x+1), list(x=1), cleanFunction=environment())");
    }
}
