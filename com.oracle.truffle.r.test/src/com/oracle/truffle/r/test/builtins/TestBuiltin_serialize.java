/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

        assertEval("options(keep.source=FALSE); serialize(TRUE, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(FALSE, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(c(TRUE, FALSE, TRUE, NA, TRUE), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize('asdf', connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(NA_character_, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(NA_complex_, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(NA_integer_, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(NA_real_, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(111L, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(c(111L, 11L, 990000L, NA_integer_), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(111+8i, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(c(111+8i, 55+9i, NA), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(111, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(c(111, 99, NA, 44), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(as.raw(10), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(as.raw(210), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(as.raw(c(1, 55, 210)), connection=NULL)");

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
        assertEval("options(keep.source=FALSE); serialize(quote(\"bar\"), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote('baz'), connection=NULL)");
        assertEval("setClass('foo', slots = c(x='numeric', y='numeric')); t1 <- new('foo', x=4, y=c(77,88)); options(keep.source=FALSE); serialize(t1, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(a(b(c(d(function (e, ...) { f(g)$h.i}))))), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(f(g)$h.i), connection=NULL)");
        assertEval("options(keep.source=FALSE); val <- new.env(hash=FALSE); serialize(val, connection=NULL)");
        assertEval("options(keep.source=FALSE); val <- list(enclos = new.env(hash=FALSE)); serialize(val, connection=NULL)");
        assertEval("options(keep.source=FALSE); val <- defaultPrototype(); serialize(val, connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function() new(\"foo\", x)), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x) { new(\"BAR\", x) }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x, ...) { new(\"BAR\", x) }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x,y) { new(\"BAR\", x) }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x,y) { TRUE }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x,y,...) { 1 }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x,y=1,...) { NA }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x={1 + a},y,...) { NA }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x={1 + a},y,...) { !!NA }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x={1 + a},y,...) { !1+5i }), connection=NULL)");
        assertEval("options(keep.source=FALSE); serialize(quote(function(x={1 + a},y=c(1,2,3),z=\"foo\",...) { !1+5i }), connection=NULL)");

        assertEval("options(keep.source=FALSE); serialize(quote(function(x) { `+`(`(`(\"BAR\"), x) }), connection=NULL)");

        assertEval("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$a <- 'foo'; serialize(val, connection=NULL)");
        assertEval("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$b <- 123; serialize(val, connection=NULL)");
        assertEval("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$c <- 1233L; serialize(val, connection=NULL)");
        assertEval("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$d <- TRUE; serialize(val, connection=NULL)");
        assertEval("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$e <- 5+9i; serialize(val, connection=NULL)");
        assertEval("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$f <- NA; serialize(val, connection=NULL)");
    }
}
