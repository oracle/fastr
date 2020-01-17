/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tck;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.instrumentation.ContextsListener;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.test.tck.ParseWithArgsTesterInstrument.ParseWithArgsTestData;

@TruffleInstrument.Registration(id = ParseWithArgsTesterInstrument.ID, name = ParseWithArgsTesterInstrument.ID, version = "1.0", services = ParseWithArgsTestData.class)
public class ParseWithArgsTesterInstrument extends TruffleInstrument {
    public static final String ID = "ParseWithArgsTester";

    @Override
    protected void onCreate(Env env) {
        ParseWithArgsTestData testData = new ParseWithArgsTestData();
        env.registerService(testData);
        env.getInstrumenter().attachContextsListener(new TestListener(testData, env), true);
    }

    public static final class ParseWithArgsTestData {
        private boolean wasRun = false;
        public Object additionResult;
        public Object helloWorld;
        public Object sumResult;
    }

    private static class TestListener implements ContextsListener {
        private final ParseWithArgsTestData testData;
        private final Env env;

        TestListener(ParseWithArgsTestData testData, Env env) {
            this.testData = testData;
            this.env = env;
        }

        @Override
        public void onLanguageContextInitialized(TruffleContext context, LanguageInfo language) {
            if (!language.getName().toLowerCase().equals("r") || testData.wasRun) {
                return;
            }
            testData.wasRun = true;
            try {
                // Basic smoke test
                CallTarget target1 = env.parse(Source.newBuilder("R", "a + b", "parse-with-args-test1").build(), "a", "b");
                testData.additionResult = target1.call(1, 3);

                // Accessing builtin from the base package works
                CallTarget target2 = env.parse(Source.newBuilder("R", "sum(argName)", "parse-with-args-test2").build(), "argName");
                // Note: how to wrap Java array as host value here?
                testData.sumResult = target2.call(6);

                // Accessing internal function from the base packages
                CallTarget target3 = env.parse(Source.newBuilder("R", "paste(argName, 'world')", "parse-with-args-test3").build(), "argName");
                testData.helloWorld = target3.call("Hello");
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onContextCreated(TruffleContext context) {
            // nop
        }

        @Override
        public void onLanguageContextCreated(TruffleContext context, LanguageInfo language) {
            // nop
        }

        @Override
        public void onLanguageContextFinalized(TruffleContext context, LanguageInfo language) {
            // nop
        }

        @Override
        public void onLanguageContextDisposed(TruffleContext context, LanguageInfo language) {
            // nop
        }

        @Override
        public void onContextClosed(TruffleContext context) {
            // nop
        }
    }
}
