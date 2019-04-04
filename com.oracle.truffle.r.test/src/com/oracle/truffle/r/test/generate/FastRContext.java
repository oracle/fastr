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

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.r.runtime.context.ChildContextInfo;

/**
 * Abstraction of the {@link Context}.
 *
 * This class wraps {@link Context} produced by the internal builtin
 * {@code .fastr.context.testing.new} and calls {@code .fastr.context.testing.close} in
 * {@link #close()} to properly dispose it.
 */
public final class FastRContext implements AutoCloseable {

    private final Context mainContext;
    private final Context context;
    private final Object internalContext;

    private FastRContext(Context mainContext, Context context, Object internalContext) {
        this.context = context;
        this.internalContext = internalContext;
        this.mainContext = mainContext;
    }

    public static FastRContext create(Context mainContext, ChildContextInfo childContextInfo) {
        Value contextData = mainContext.eval("R", ".fastr.context.testing.new").execute(childContextInfo);
        return new FastRContext(mainContext, contextData.getMember("context").as(Context.class), contextData);
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

    @Override
    public void close() {
        if (internalContext != null) {
            mainContext.eval("R", ".fastr.context.testing.close").execute(internalContext);
        }
    }
}
