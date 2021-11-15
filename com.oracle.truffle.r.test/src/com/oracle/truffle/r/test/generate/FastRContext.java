/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.generate;

import static com.oracle.truffle.r.test.generate.FastRSession.GET_CONTEXT;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Wrapper around the {@link Context} and its input and output streams.
 */
public class FastRContext implements AutoCloseable {
    private final Context context;
    private final TestByteArrayInputStream input;
    private final ByteArrayOutputStream output;

    FastRContext(Context context, TestByteArrayInputStream input, ByteArrayOutputStream output) {
        this.context = context;
        this.input = input;
        this.output = output;
    }

    public Value eval(Source source) {
        return context.eval(source);
    }

    public Value eval(String languageId, CharSequence source) {
        return context.eval(languageId, source);
    }

    public Value getPolyglotBindings() {
        return context.getPolyglotBindings();
    }

    public Engine getEngine() {
        return context.getEngine();
    }

    public Context getContext() {
        return context;
    }

    public RContext getInternalContext() {
        return context.eval(GET_CONTEXT).asHostObject();
    }

    public TestByteArrayInputStream getInput() {
        return input;
    }

    public ByteArrayOutputStream getOutput() {
        return output;
    }

    public void reset() {
    }

    @Override
    public void close() {
        context.close();
        // TODO: maybe input.close(); output.close()?
    }

    /**
     * A context shared between several test runs. We do not close the underlying context, but just
     * try to reset the global state as much as possible.
     */
    public static final class SharedFastRContext extends FastRContext {
        private Value setOptionsFunc;
        private Value oldSearch;
        private final Value baseSearchFun;
        private final Value cleanupFun;
        private final Value defaultOptions;

        SharedFastRContext(Context context, TestByteArrayInputStream input, ByteArrayOutputStream output) {
            super(context, input, output);
            baseSearchFun = context.eval("R", "base::search");
            cleanupFun = context.eval("R", "function (oldSearch) {\n" +
                            "  env <- .GlobalEnv\n" +
                            "  rm(list = ls(envir = env, all.names = TRUE), envir = env)\n" +
                            "  RNGkind('default', 'default', 'default')\n" +
                            "  set.seed(42, 'default')\n" +
                            // " options(warn = 1)\n" +
                            "  sch <- search()\n" +
                            "  newitems <- sch[! sch %in% oldSearch]\n" +
                            "  if(length(newitems)) tools:::detachPackages(newitems, verbose=F)\n" +
                            "  missitems <- oldSearch[! oldSearch %in% sch]\n" +
                            "  return(any(missitems))\n" +
                            "}");
            defaultOptions = context.eval("R", "options()");
            setOptionsFunc = context.eval("R", "options");
        }

        public SharedFastRContext(Context context, TestByteArrayInputStream input, ByteArrayOutputStream output, Value baseSearchFun, Value cleanupFun, Value defaultOptions, Value setOptionsFunc) {
            super(context, input, output);
            this.baseSearchFun = baseSearchFun;
            this.cleanupFun = cleanupFun;
            this.defaultOptions = defaultOptions;
            this.setOptionsFunc = setOptionsFunc;
        }

        @Override
        public void reset() {
            getOutput().reset();
            oldSearch = baseSearchFun.execute();
            setOptionsFunc.execute(defaultOptions);
        }

        @Override
        public void close() {
            Value missing = cleanupFun.execute(oldSearch);
            assert !missing.asBoolean();
        }

        public SharedFastRContext newSession() {
            return new SharedFastRContext(getContext(), getInput(), getOutput(), baseSearchFun, cleanupFun, defaultOptions, setOptionsFunc);
        }
    }

    public static final class TestByteArrayInputStream extends ByteArrayInputStream {

        TestByteArrayInputStream() {
            super(new byte[0]);
        }

        public void setContents(String data) {
            this.buf = data.getBytes(StandardCharsets.UTF_8);
            this.count = this.buf.length;
            this.pos = 0;
        }

        @Override
        public synchronized int read() {
            return super.read();
        }
    }
}
