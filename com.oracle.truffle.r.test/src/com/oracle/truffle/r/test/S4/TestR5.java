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
package com.oracle.truffle.r.test.S4;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.TestTrait;

// Checkstyle: stop LineLength

public class TestR5 extends TestBase {

    /**
     * {@code suppressMessages} is needed to suppress messages about codetools not being available.
     */
    private void assertSuppress(String test) {
        assertEval("print(suppressMessages({" + test + "}))");
    }

    private void assertSuppress(TestTrait trait, String test) {
        assertEval(trait, "print(suppressMessages({" + test + "}))");
    }

    @Test
    public void testAllocation() {
        assertEval("{ DummyClass0 <- setRefClass('DummyClass0'); DummyClass0$new() }");
        assertEval("{ DummyClass1 <- setRefClass('DummyClass1'); is(DummyClass1, 'refClass') }");
        assertEval("{ DummyClass2 <- setRefClass('DummyClass2'); obj <- DummyClass2$new(); is(obj, 'refObject') }");
        assertEval("{ fooClass <- setRefClass('Foo6R5', fields = list( a = 'numeric')); fooClass$new(a = 1) }");
        assertEval("{ fooClass <- setRefClass('Foo7R5', fields = list( a = 'numeric')); fooClass$new(1) }");
        assertEval("{ setRefClass('Foo16R5'); ls(topenv(parent.frame()), all.names = T) }");
        assertEval("env0 <- new.env(); setRefClass('Foo17R5', where = env0); ls(topenv(parent.frame()), all.names = T); ls(env0, all.names = T)");
    }

    @Test
    public void testAttributes() {
        assertEval("{ clazz <- setRefClass('Foo13R5'); clazz$methods() }");
    }

    @Test
    public void testReferenceSemantics() {
        assertEval("fooClass <- setRefClass('Foo12R5', fields = list( a = 'numeric')); obj0 <- fooClass$new(a = 1); obj1 <- obj0; obj0$a; obj1$a; obj0$a <- 999; obj0$a; obj1$a");
    }

    @Test
    public void testInstanceMethods() {
        assertSuppress("{ clazz <- setRefClass('Foo0R5', c('a', 'b')); clazz$methods(mean = function() { (a + b) / 2 }); obj <- clazz$new(a = 1, b = 5); obj$mean() }");
        assertEval("{ clazz <- setRefClass('Foo1R5', c('a', 'b')); obj <- clazz$new(a = 1, b = 5); cobj <- obj$copy(); obj$a; cobj$a; obj$a <- 10; obj$a; cobj$a}");
        assertEval("clazz <- setRefClass('Foo2R5', c('a', 'b')); obj <- clazz$new(a = 1, b = 5); obj$field('a'); obj$field('b')");
        assertEval("clazz <- setRefClass('Foo3R5', c('a', 'b')); obj <- clazz$new(); obj$initFields(a = 5, b = 6)");
        assertEval(Output.IgnoreErrorContext, "{ clazz <- setRefClass('Foo4R5', c('a', 'b')); clazz$accessors() }");
        assertEval("{ clazz <- setRefClass('Foo5R5', c('a', 'b')); obj <- clazz$new(a = 1, b = 5); attributes(obj$getRefClass())$className }");
        assertSuppress("{ clazz <- setRefClass('Foo12R5', fields = list(a = 'numeric'), methods = list(inc = function() { a <<- a+1 })); obj <- clazz$new(a = 0); obj$inc(); obj$a }");
        assertEval("{ clazz <- setRefClass('Foo13R5'); obj <- clazz$new(); obj$inexistingMethod() }");
        assertSuppress("{ clazz <- setRefClass('Foo28R5', fields = list(a = 'numeric'), methods = list(inc = function() { a <<- a+1 })); obj <- clazz$new(a = 0); obj$inc(); obj$a }");

        // constructor
        assertSuppress("{ clazz <- setRefClass('Foo15R5', fields = c('a'), methods = list(initialize = function() { a <<- 123 })); obj <- clazz$new(); obj$a }");
        assertSuppress("{ setRefClass('A6R5', fields = c('a'), methods = list(initialize = function() { a <<- 'hello' })); clazz <- setRefClass('B6R5', fields = c('a'), contains = 'A6R5'); obj <- clazz$new(); obj$a }");

        // FIXME FastR memory statistics missing completely (NA)
        // Assuming that they should be provided so ImplementationError.
        assertEval(Ignored.ImplementationError,
                        "{ clazz <- setRefClass('Foo18R5', fields = c('a'), methods = list(initialize = function() a <<- 456, finalize = function() { print(sprintf('finalizer: %d', a)) } )); (function () { x <- clazz$new(); print('fun') })(); gc() }");
    }

    @Test
    public void testCheckedAssign() {
        assertEval("clazz <- setRefClass('Foo8R5', fields = list(a = 'numeric')); obj <- clazz$new(); bindingIsActive('a', as.environment(obj))");
        assertEval("clazz <- setRefClass('Foo9R5', fields = list(a = 'numeric')); obj <- clazz$new(); bindingIsActive('a', as.environment(obj)); obj$a <- 123; bindingIsActive('a', as.environment(obj))");
        assertEval("clazz <- setRefClass('Foo10R5', fields = list(a = 'numeric')); obj <- clazz$new(); obj$a <- 10; obj$a; obj$a <- 'hello'; obj$a");
        assertEval("clazz <- setRefClass('Foo11R5', fields = list(a = 'character')); obj <- clazz$new(); obj$a <- 'hello'; obj$a; obj$a <- 10; obj$a");
    }

    @Test
    public void testFieldAccess() {
        assertEval("{ clazz <- setRefClass('Foo14R5'); obj <- clazz$new(); obj$inexistingMethod() }");
        assertEval("{ clazz <- setRefClass('Foo14R5', fields = c('a')); obj <- clazz$new(); obj$a }");
        assertEval("{ clazz <- setRefClass('Foo14R5', fields = c('a')); obj <- clazz$new(a = 1); print(obj$a); obj$a <- 'hello'; print(obj$a) }");

        // list/modify methods
        assertEval("{ clazz <- setRefClass('Foo19R5'); clazz$methods('inexistingMethod') }");
        assertSuppress(Ignored.OutputFormatting, "clazz <- setRefClass('Foo20R5', methods = list(foo = function() NULL)); clazz$methods('foo')");
        assertSuppress("clazz <- setRefClass('Foo21R5', methods = list(foo = function() NULL)); clazz$new()$foo(); clazz$methods(foo = function(x) x); clazz$new()$foo(3)");

        // list available fields
        assertEval("{ clazz <- setRefClass('Foo22R5'); clazz$fields() }");
        assertEval("{ clazz <- setRefClass('Foo23R5', fields = c('a', 'b', 'c')); clazz$fields() }");
        assertEval("{ clazz <- setRefClass('Foo24R5', fields = list(a = 'numeric')); clazz$fields() }");
        assertEval("{ clazz <- setRefClass('Foo25R5', fields = list(a = 'numeric', b = 'character', c = 'raw')); clazz$fields() }");

        // make field read-only; one assignment is allowed
        assertEval("{ clazz <- setRefClass('Foo26R5', fields = list(a = 'numeric')); clazz$lock('a'); obj <- clazz$new(); obj$a <- 10; obj$a }");
        assertEval("{ clazz <- setRefClass('Foo27R5', fields = list(a = 'numeric')); clazz$lock('a'); obj <- clazz$new(); obj$a <- 10; obj$a <- 20 }");

        // generate getter and setter
        assertEval(Output.ImprovedErrorContext, "{ clazz <- setRefClass('Foo28R5', fields = list(a = 'numeric', b = 'character')); clazz$accessors(); clazz$methods() }");
    }

    @Test
    public void testInheritance() {
        assertEval("A0R5 <- setRefClass('A0R5', field = list(a = 'numeric')); B0R5 <- setRefClass('B0R5', contains = 'A0R5'); obj <- B0R5$new(a = 1); obj$a");
        assertSuppress("A1R5 <- setRefClass('A1R5', methods = list(foo = function() { print('hello') })); B1R5 <- setRefClass('B1R5', contains = 'A1R5'); obj <- B1R5$new(); obj$foo()");
        assertSuppress("A2R5 <- setRefClass('A2R5', methods = list(foo = function() { print('hello') })); B2R5 <- setRefClass('B2R5', methods = list(foo = function() { print('world') }), contains = 'A2R5'); obj <- B2R5$new(); obj$foo()");
        assertSuppress("A3R5 <- setRefClass('A3R5', methods = list(foo = function() { print('hello') })); B3R5 <- setRefClass('B3R5', methods = list(foo = function() { callSuper(); print('world') }), contains = 'A3R5'); obj <- B3R5$new(); obj$foo()");
        assertSuppress(Ignored.OutputFormatting, "{ setRefClass('A4R5', fields = c('a')); setRefClass('B4R5', contains = 'A4R5', methods = list(set_a = function(a){ a <<- a })) }");
        assertEval("A5R5 <- setRefClass('A5R5', field = c('a'), methods = list(initialize <- function() { a <<- 0 })); B5R5 <- setRefClass('B5R5', field = c('a'), methods = list(initialize <- function() { a <<- 'hello' })); C5R5 <- setRefClass('C5R5', contains = c('A5R5', 'B5R5')); obj <- C5R5$new(); obj$a <- raw(0)");
    }
}
