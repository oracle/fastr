/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_serialize extends TestBase {

    @Test
    public void testserialize() {
        assertEval("options(keep.source=FALSE); serialize(quote(x), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(TRUE), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(FALSE), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote('asdf'), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(NA_character_), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(NA_complex_), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(NA_integer_), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(NA_real_), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(NA_character_ + NA_complex_ + NA_integer_ + NA_real_), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(111L), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(111+8i), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(111+11), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(a+b), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote((a+b)), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote((a %asdf% b)), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(foo(a,b,c)), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote({ foo(a,b,c) }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(if (a) b else c), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(if (a) {b} else {c}), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(if ({a}) {b} else {c}), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(while (a) b), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(repeat {b; if (c) next else break}), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(if (a * 2 < 199) b + foo(x,y,foo=z+1,bar=)), connection=NULL)");
    }
}
