/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop LineLength

/**
 * Tests for the S4 object model implementation.
 */
public class TestS4 extends TestBase {
    @Test
    public void testSlotAccess() {
        assertEval("{ `@`(getClass(\"ClassUnionRepresentation\"), virtual) }");
        assertEval("{ `@`(getClass(\"ClassUnionRepresentation\"), \"virtual\") }");
        assertEval(Output.ContainsError, "{ `@`(getClass(\"ClassUnionRepresentation\"), c(\"virtual\", \"foo\")) }");
        assertEval("{ getClass(\"ClassUnionRepresentation\")@virtual }");
        assertEval("{ getClass(\"ClassUnionRepresentation\")@.S3Class }");
        assertEval("{ c(42)@.Data }");
        assertEval("{ x<-42; `@`(x, \".Data\") }");
        assertEval("{ x<-42; `@`(x, .Data) }");
        assertEval(Output.ContainsError, "{ getClass(\"ClassUnionRepresentation\")@foo }");
        assertEval(Output.ContainsError, "{ c(42)@foo }");
        assertEval(Output.ContainsError, "{ x<-c(42); class(x)<-\"bar\"; x@foo }");
        assertEval("{ x<-getClass(\"ClassUnionRepresentation\"); slot(x, \"virtual\") }");
        assertEval(Output.ContainsError, "{ x<-getClass(\"ClassUnionRepresentation\"); slot(x, virtual) }");
        assertEval("{ x<-function() 42; attr(x, \"foo\")<-7; y<-asS4(x); y@foo }");
        assertEval(Output.ContainsError, "{ x<-NULL; `@`(x, foo) }");
        assertEval(Output.ContainsError, "{ x<-NULL; x@foo }");
        assertEval("{ x<-paste0(\".\", \"Data\"); y<-42; slot(y, x) }");

        // test from Hadley Wickham's book
        assertEval("{ setClass(\"Person\", representation(name = \"character\", age = \"numeric\"), prototype(name = NA_character_, age = NA_real_)); getSlots(\"Person\") }");

    }

    @Test
    public void testSlotUpdate() {
        assertEval("{ x<-getClass(\"ClassUnionRepresentation\"); x@virtual<-TRUE; x@virtual }");
        assertEval("{ x<-getClass(\"ClassUnionRepresentation\"); slot(x, \"virtual\", check=TRUE)<-TRUE; x@virtual }");
        assertEval("{ x<-initialize@valueClass; initialize@valueClass<-\"foo\"; initialize@valueClass<-x }");

        assertEval(Output.ContainsError, "{ x<-function() 42; attr(x, \"foo\")<-7; y<-asS4(x); y@foo<-42 }");
        assertEval(Output.ContainsError, "{ x<-NULL; `@<-`(x, foo, \"bar\") }");
        assertEval(Output.ContainsError, "{ x<-NULL; x@foo<-\"bar\" }");

    }

    @Test
    public void testConversions() {
        assertEval("{ x<-42; isS4(x) }");
        assertEval("{ x<-42; y<-asS4(x); isS4(y) }");
        assertEval("{ isS4(NULL) }");
        assertEval("{ asS4(NULL); isS4(NULL }");
        assertEval("{  asS4(7:42) }");
    }

    @Test
    public void testAllocation() {
        assertEval("{ new(\"numeric\") }");
        assertEval("{ setClass(\"foo\", representation(j=\"numeric\")); new(\"foo\", j=42) }");

        // tests from Hadley Wickham's book
        assertEval("{ setClass(\"Person\", representation(name = \"character\", age = \"numeric\")); setClass(\"Employee\", representation(boss = \"Person\"), contains = \"Person\"); hadley <- new(\"Person\", name = \"Hadley\", age = 31); hadley }");
        assertEval(Output.ContainsError,
                        "{ setClass(\"Person\", representation(name = \"character\", age = \"numeric\")); setClass(\"Employee\", representation(boss = \"Person\"), contains = \"Person\"); hadley <- new(\"Person\", name = \"Hadley\", age = \"thirty\") }");
        assertEval(Output.ContainsError,
                        "{ setClass(\"Person\", representation(name = \"character\", age = \"numeric\")); setClass(\"Employee\", representation(boss = \"Person\"), contains = \"Person\"); hadley <- new(\"Person\", name = \"Hadley\", sex = \"male\") }");
        assertEval("{ setClass(\"Person\", representation(name = \"character\", age = \"numeric\")); setClass(\"Employee\", representation(boss = \"Person\"), contains = \"Person\"); hadley <- new(\"Person\", name = \"Hadley\"); hadley@age }");
        assertEval("{ setClass(\"Person\", representation(name = \"character\", age = \"numeric\"), prototype(name = NA_character_, age = NA_real_)); hadley <- new(\"Person\", name = \"Hadley\"); hadley@age }");

        // testw from Hadley Wickham's book
        assertEval(Output.ContainsError,
                        "{ check_person <- function(object) { errors <- character(); length_age <- length(object@age); if (length_age != 1) { msg <- paste(\"Age is length \", length_age, \".  Should be 1\", sep = \"\"); errors <- c(errors, msg) }; if (length(errors) == 0) TRUE else errors }; setClass(\"Person\", representation(name = \"character\", age = \"numeric\"), validity = check_person); new(\"Person\", name = \"Hadley\") }");
        assertEval(Output.ContainsError,
                        "{ check_person <- function(object) { errors <- character(); length_age <- length(object@age); if (length_age != 1) { msg <- paste(\"Age is length \", length_age, \".  Should be 1\", sep = \"\"); errors <- c(errors, msg) }; if (length(errors) == 0) TRUE else errors }; setClass(\"Person\", representation(name = \"character\", age = \"numeric\"), validity = check_person); new(\"Person\", name = \"Hadley\", age = 1:10) }");
        assertEval(Output.ContainsError,
                        "{ check_person <- function(object) { errors <- character(); length_age <- length(object@age); if (length_age != 1) { msg <- paste(\"Age is length \", length_age, \".  Should be 1\", sep = \"\"); errors <- c(errors, msg) }; if (length(errors) == 0) TRUE else errors }; setClass(\"Person\", representation(name = \"character\", age = \"numeric\"), validity = check_person); hadley <- new(\"Person\", name = \"Hadley\", age = 31); hadley@age <- 1:10; validObject(hadley) }");
    }

    @Test
    public void testClassCreation() {
        // output slightly different from GNU R even though we use R's "show" method to print it
        assertEval(Ignored.OutputFormatting, "{ setClass(\"foo\", representation(j=\"numeric\")); getClass(\"foo\") }");
    }

    @Test
    public void testMethods() {
        // output slightly different from GNU R even though we use R's "show" method to print it
        assertEval(Ignored.OutputFormatting, "{ setGeneric(\"gen\", function(object) standardGeneric(\"gen\")); gen }");
        assertEval(Ignored.OutputFormatting, "{ gen<-function(object) 0; setGeneric(\"gen\"); gen }");

        assertEval("{ gen<-function(object) 0; setGeneric(\"gen\"); setClass(\"foo\", representation(d=\"numeric\")); setMethod(\"gen\", signature(object=\"foo\"), function(object) object@d); gen(new(\"foo\", d=42)) }");

        assertEval("{ setClass(\"foo\", representation(d=\"numeric\")); setClass(\"bar\",  contains=\"foo\"); setGeneric(\"gen\", function(o) standardGeneric(\"gen\")); setMethod(\"gen\", signature(o=\"foo\"), function(o) \"FOO\"); setMethod(\"gen\", signature(o=\"bar\"), function(o) \"BAR\"); c(gen(new(\"foo\", d=7)), gen(new(\"bar\", d=42))) }");

        // additional cleanup (generic removal) was needed to get the methods listing to work
        // properly (impossible to reproduce on command-line, only in the test harness, even with
// the same
        // sequence of tests - likely due to tests being run in a somewhat non-standard way via
        // semi-isolated contexts)
        assertEval("{ setGeneric(\"gen\", function(o) standardGeneric(\"gen\")); setGeneric(\"gen\", function(o) standardGeneric(\"gen\")); ; removeGeneric(\"gen\"); }");

        // test from Hadley Wickham's book
        assertEval("{ setClass(\"A\"); setClass(\"A1\", contains = \"A\"); setClass(\"A2\", contains = \"A1\"); setClass(\"A3\", contains = \"A2\"); setGeneric(\"foo\", function(a, b) standardGeneric(\"foo\")); setMethod(\"foo\", signature(\"A1\", \"A2\"), function(a, b) \"1-2\"); setMethod(\"foo\", signature(\"A2\", \"A1\"), function(a, b) \"2-1\"); x<-print(foo(new(\"A2\"), new(\"A2\"))); removeGeneric(\"foo\"); x }");
        assertEval("{ setGeneric(\"sides\", function(object) standardGeneric(\"sides\")); setClass(\"Shape\"); setClass(\"Polygon\", representation(sides = \"integer\"), contains = \"Shape\"); setClass(\"Triangle\", contains = \"Polygon\"); setMethod(\"sides\", signature(\"Triangle\"), function(object) 3); showMethods(\"sides\") }");
        assertEval("{ setGeneric(\"sides\", function(object) standardGeneric(\"sides\")); setClass(\"Shape\"); setClass(\"Polygon\", representation(sides = \"integer\"), contains = \"Shape\"); setClass(\"Triangle\", contains = \"Polygon\"); setMethod(\"sides\", signature(\"Triangle\"), function(object) 3); showMethods(class = \"Polygon\") }");
        assertEval(Output.ContainsError,
                        "{ setClass(\"Shape\"); setClass(\"Polygon\", representation(sides = \"integer\"), contains = \"Shape\"); setClass(\"Triangle\", contains = \"Polygon\"); setGeneric(\"sides\", valueClass = \"numeric\", function(object) standardGeneric(\"sides\")); setMethod(\"sides\", signature(\"Triangle\"), function(object) \"three\"); sides(new(\"Triangle\")) }");

    }
}
