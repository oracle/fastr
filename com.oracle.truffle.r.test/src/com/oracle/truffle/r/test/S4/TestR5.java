/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.S4;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestR5 extends TestBase {

    @Test
    public void testAllocation() {
        assertEval("{ Person <- setRefClass('Person'); Person$new() }");
        assertEval("{ fooClass <- setRefClass('Foo', fields = list( a = 'numeric')); fooClass$new(a = 1) }");
        assertEval(Ignored.ImplementationError, "{ fooClass <- setRefClass('Foo', fields = list( a = 'numeric')); fooClass$new(1) }");
    }

    @Test
    public void testAttributes() {
        assertEval("{ Person <- setRefClass('Person'); Person$methods() }");
    }

    @Test
    public void testReferenceSemantics() {
        assertEval("fooClass <- setRefClass('Foo', fields = list( a = 'numeric')); obj0 <- fooClass$new(a = 1); obj1 <- obj0; obj0$a; obj1$a; obj0$a <- 999; obj0$a; obj1$a");
    }

    @Test
    public void testInstanceMethods() {
        assertEval("{ clazz <- setRefClass('Foo', c('a', 'b')); clazz$methods(mean = function() { (a + b) / 2 }); obj <- clazz$new(a = 1, b = 5); obj$mean() }");
        assertEval("{ clazz <- setRefClass('Foo', c('a', 'b')); obj <- clazz$new(a = 1, b = 5); cobj <- obj$copy(); obj$a; cobj$a; obj$a <- 10; obj$a; cobj$a}");
        assertEval("clazz <- setRefClass('Foo', c('a', 'b')); obj <- clazz$new(a = 1, b = 5); obj$field('a'); obj$field('b')");
        assertEval("clazz <- setRefClass('Foo', c('a', 'b')); obj <- clazz$new(); obj$initFields(a = 5, b = 6)");
        assertEval(Output.IgnoreErrorContext, "{ clazz <- setRefClass('Foo', c('a', 'b')); clazz$accessors() }");
        assertEval("{ clazz <- setRefClass('Foo', c('a', 'b')); obj <- clazz$new(a = 1, b = 5); attributes(obj$getRefClass())$className }");
    }

    @Test
    public void testCheckedAssign() {
        assertEval("clazz <- setRefClass('Foo', fields = list(a = 'numeric')); obj <- clazz$new(); obj$a <- 10; obj$a; obj$a <- 'hello'; obj$a");
        assertEval("clazz <- setRefClass('Foo', fields = list(a = 'character')); obj <- clazz$new(); obj$a <- 'hello'; obj$a; obj$a <- 10; obj$a");
    }
}
