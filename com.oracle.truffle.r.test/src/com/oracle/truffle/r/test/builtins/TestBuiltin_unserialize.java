/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.truffle.r.test.builtins;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.generate.FastRSession;
import com.oracle.truffle.r.test.generate.GnuROneShotRSession;

public class TestBuiltin_unserialize extends TestBase {
    private static final String[] BASIC_TYPE_VALUES = new String[]{
                    "c(1,2,3,4)", "3L", "c(1L,2L,99L,NA)", "1:15", "(1:15)+0.1", "42", "\"Hello world\"", "3+2i", "c(3+2i, 5+944i, NA)", "TRUE", "c(TRUE, FALSE, NA, FALSE, TRUE)",
                    "head(mtcars)", "data.frame(col1=c(9,8,7), col2=1:3)", "expression(x+1)", "list(1,2)", "NULL"
    };

    private static final String[] VERSIONS = new String[]{"2", "3"};

    private static final String DEFERED_STRING_PATH = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/builtins/data/serializedDeferredStrings";
    private static final String WRAPPER_PATH = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/builtins/data/serializedWrappers";

    @Before
    public void init() throws Throwable {
        File f = new File(DEFERED_STRING_PATH);
        if (!f.exists()) {
            f.getParentFile().mkdirs();

            GnuROneShotRSession gnur = new GnuROneShotRSession();

            String create = "e <- new.env(parent = baseenv())\n" +
                            "assign('is', as.character(1L:3L), envir = e)\n" +
                            "assign('isa', as.character(1L:3L), envir = e)\n" +
                            "attr(e$isa, 'testattr') <- 'testattrValue'\n" +
                            "assign('iv', as.character(c(2L, 4L, 6L)), envir = e)\n" +
                            "assign('iva', as.character(c(2L, 4L, 6L)), envir = e)\n" +
                            "attr(e$iva, 'testattr') <- 'testattrValue'\n" +
                            "assign('rs', as.character(1.1:3.1), envir = e)\n" +
                            "assign('rsa', as.character(1.1:3.1), envir = e)\n" +
                            "attr(e$rsa, 'testattr') <- 'testattrValue'\n" +
                            "assign('rv', as.character(c(2.1, 4.1, 6.1)), envir = e)\n" +
                            "assign('rva', as.character(c(2.1, 4.1, 6.1)), envir = e)\n" +
                            "attr(e$rva, 'testattr') <- 'testattrValue'\n" +
                            "fw <- file('" + DEFERED_STRING_PATH + "', open='wb');\n" +
                            "serialize(e, fw);\n" +
                            "flush(fw)";
            gnur.eval(null, create, null, FastRSession.USE_DEFAULT_TIMEOUT);

            create = "e <- new.env(parent = baseenv())\n" +
                            "assign(\"iv\", sort(c(22L, 33L, 11L)), envir = e)\n" +
                            "assign(\"iva\", sort(c(22L, 33L, 11L)), envir = e)\n" +
                            "attr(e$iva, \"testattr\") <- \"testattrValue\"\n" +
                            "assign(\"rv\", sort(c(22.1, 33.1, 11.1)), envir = e)\n" +
                            "assign(\"rva\", sort(c(22.1, 33.1, 11.1)), envir = e)\n" +
                            "attr(e$rva, \"testattr\") <- \"testattrValue\"\n" +
                            "fw <- file('" + WRAPPER_PATH + "', open=\"wb\"); serialize(e, fw); flush(fw)";

            gnur.eval(null, create, null, FastRSession.USE_DEFAULT_TIMEOUT);
        }

    }

    @Test
    public void tests() {
        assertEval(template("unserialize(serialize(%0, NULL, version=%1))", BASIC_TYPE_VALUES, VERSIONS));
    }

    @Test
    public void testserializeAndUnserializeClosure() {
        // N.B.: FastR does not preserve code formatting like GNU R does
        assertEval(Output.IgnoreWhitespace, template("unserialize(serialize(function (x) { x }, NULL, version=%0))", VERSIONS));
        assertEval(template(
                        "f <- function() x; e <- new.env(); e$x <- 123; environment(f) <- e; expr <- substitute({ FUN() }, list(FUN=f)); eval(expr); expr <- unserialize(serialize(expr, NULL, version=%0)); eval(expr)",
                        VERSIONS));
    }

    @Test
    public void testunserializeSeq() {
        assertEval(template("unserialize(serialize(1L:10L, connection=NULL, version=%0))", VERSIONS));
        assertEval(template("unserialize(serialize(1:10, connection=NULL, version=%0))", VERSIONS));
        assertEval(template("unserialize(serialize(1.1:10.1, connection=NULL, version=%0))", VERSIONS));
        assertEval(template("s <- 1L:10L; attr(s, 'testattr') <- 'attrvalue'; unserialize(serialize(s, connection=NULL, version=%0))", VERSIONS));
        assertEval(template("s <- 1:10; attr(s, 'testattr') <- 'attrvalue'; unserialize(serialize(s, connection=NULL, version=%0))", VERSIONS));
        assertEval(template("s <- 1.1:10.1; attr(s, 'testattr') <- 'attrvalue'; unserialize(serialize(s, connection=NULL, version=%0))", VERSIONS));

        assertEvalFastR("v <- unserialize(serialize(1L:10L, connection=NULL, version=2)); .fastr.inspect(v)", "cat('com.oracle.truffle.r.runtime.data.RIntVector\n')");
        assertEvalFastR("v <- unserialize(serialize(1L:10L, connection=NULL, version=3)); .fastr.inspect(v, inspectVectorData=T)", "cat('com.oracle.truffle.r.runtime.data.RIntSeqVectorData\n')");
        assertEvalFastR("v <- unserialize(serialize(1:10, connection=NULL, version=2)); .fastr.inspect(v)", "cat('com.oracle.truffle.r.runtime.data.RIntVector\n')");
        assertEvalFastR("v <- unserialize(serialize(1:10, connection=NULL, version=3)); .fastr.inspect(v, inspectVectorData=T)", "cat('com.oracle.truffle.r.runtime.data.RIntSeqVectorData\n')");
        assertEvalFastR("v <- unserialize(serialize(1.1:10.1, connection=NULL, version=2)); .fastr.inspect(v)", "cat('com.oracle.truffle.r.runtime.data.RDoubleVector\n')");
        assertEvalFastR("v <- unserialize(serialize(1.1:10.1, connection=NULL, version=3)); .fastr.inspect(v)", "cat('com.oracle.truffle.r.runtime.data.RDoubleVector\n')");

        // FastR can't create such long sequences
        assertEvalFastR(Ignored.Unimplemented, "s <- unserialize(serialize(1:2147483648, connection=NULL, version=3)); .fastr.inspect(s)", "cat('com.oracle.truffle.r.runtime.data.RIntSequence\n')");
    }

    @Test
    public void testunserializeDeferredString() {
        assertEval("e <- unserialize(file('" + DEFERED_STRING_PATH + "', open='rb')); e$is; e$iv; e$rs; e$rv; e$isa; e$iva; e$rsa; e$rva");
    }

    @Test
    public void testunserializeWrappers() {
        assertEval("e <- unserialize(file('" + WRAPPER_PATH + "', open='rb')); e$iv; e$rv; e$iva; e$rva");
    }

    @Test
    public void testunserialize() {
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(x), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(TRUE), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(FALSE), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote('asdf'), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(NA_character_), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(NA_complex_), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(NA_integer_), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(NA_real_), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(NA_character_ + NA_complex_ + NA_integer_ + NA_real_), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(111L), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(111+8i), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(111+11), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(a+b), connection=NULL, version=%0))", VERSIONS));

        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote((a+b)), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote((a %asdf% b)), connection=NULL, version=%0))", VERSIONS));

        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(foo(a,b,c)), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote({ foo(a,b,c) }), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(if (a) b else c), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(if (a) {b} else {c}), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(if ({a}) {b} else {c}), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(while (a) b), connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(repeat {b; if (c) next else break}), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(if (a * 2 < 199) b + foo(x,y,foo=z+1,bar=)), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(\"bar\"), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote('baz'), connection=NULL, version=%0))", VERSIONS));
        assertEval(template(
                        "setClass('foo', slots = c(x='numeric', y='numeric')); t1 <- new('foo', x=4, y=c(77,88)); options(keep.source=FALSE); unserialize(serialize(t1, connection=NULL, version=%0))",
                        VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(a(b(c(d(function (e, ...) { f(g)$h.i}))))), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(f(g)$h.i), connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.ContainsReferences, template("options(keep.source=FALSE); val <- new.env(hash=FALSE); unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.ContainsReferences, template("options(keep.source=FALSE); val <- list(enclos = new.env(hash=FALSE)); unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); val <- defaultPrototype(); unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(function() new(\"foo\", x)), connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(function(x) { new(\"BAR\", x) }), connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(function(x, ...) { new(\"BAR\", x) }), connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(function(x,y) { new(\"BAR\", x) }), connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(function(x,y) { TRUE }), connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(function(x,y,...) { 1 }), connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.IgnoreWhitespace, template("options(keep.source=FALSE); unserialize(serialize(quote(function(x,y=1,...) { NA }), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(function(x={1 + a},y,...) { NA }), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(function(x={1 + a},y,...) { !!NA }), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(function() { !T&F }), connection=NULL, version=%0)", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(function(x={1 + a},y,...) { !1+5i }), connection=NULL, version=%0))", VERSIONS));
        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(function(x={1 + a},y=c(1,2,3),z=\"foo\",...) { !1+5i }), connection=NULL, version=%0))", VERSIONS));

        assertEval(template("options(keep.source=FALSE); unserialize(serialize(quote(function(x) { `+`(`(`(\"BAR\"), x) }), connection=NULL, version=%0))", VERSIONS));

        assertEval(Output.ContainsReferences, template("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$a <- 'foo'; unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.ContainsReferences, template("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$b <- 123; unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.ContainsReferences, template("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$c <- 1233L; unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.ContainsReferences, template("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$d <- TRUE; unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.ContainsReferences, template("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$e <- 5+9i; unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
        assertEval(Output.ContainsReferences, template("options(keep.source=FALSE); val <- new.env(hash=FALSE); val$f <- NA; unserialize(serialize(val, connection=NULL, version=%0))", VERSIONS));
    }
}
