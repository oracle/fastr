/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.test.tck.ToStringTesterInstrument.ToStringTestData;

@TruffleInstrument.Registration(id = ToStringTesterInstrument.ID, name = ToStringTesterInstrument.ID, version = "1.0", services = ToStringTestData.class)
public class ToStringTesterInstrument extends TruffleInstrument {
    public static final String ID = "ToStringTester";

    @Override
    protected void onCreate(Env env) {
        ToStringTestData testData = new ToStringTestData();
        env.registerService(testData);
        SourceSectionFilter sourceFilter = SourceSectionFilter.newBuilder().includeInternal(false).build();
        boolean[] firstExecution = new boolean[]{true};
        env.getInstrumenter().attachExecutionEventFactory(sourceFilter, context -> new ExecutionEventNode() {
            @Override
            protected void onEnter(VirtualFrame frame) {
                if (firstExecution[0]) {
                    firstExecution[0] = false;
                    LanguageInfo rLanguage = env.getLanguages().get("R");
                    Object rObj = null;
                    try {
                        rObj = env.parse(Source.newBuilder("R", "structure(class='myclass', 42L)", "<ToStringTesterInstrument:obj>").build()).call();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    testData.intAsString = toDisplayString(env, rLanguage, 42);
                    testData.byteAsString = toDisplayString(env, rLanguage, (byte) 42);
                    testData.doubleAsString = toDisplayString(env, rLanguage, 42.5);
                    testData.stringAsString = toDisplayString(env, rLanguage, "Hello");
                    testData.trueAsString = toDisplayString(env, rLanguage, true);
                    testData.falseAsString = toDisplayString(env, rLanguage, false);
                    testData.objAsString = toDisplayString(env, rLanguage, rObj);

                    int lazyFrameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(frame.getFrameDescriptor(), "lazy");
                    Object lazyValue;
                    try {
                        lazyValue = FrameSlotChangeMonitor.getObject(frame, lazyFrameIndex);
                    } catch (FrameSlotTypeException e) {
                        throw new RuntimeException(e);
                    }
                    testData.lazyWithoutSideEffects = toDisplayStringWithoutSideEffects(env, rLanguage, lazyValue);
                    testData.lazyWithSideEffects = toDisplayString(env, rLanguage, lazyValue);
                }
            }
        });
    }

    private static String toDisplayString(Env env, LanguageInfo rLang, Object value) {
        return toDisplayString(env, rLang, value, true);
    }

    private static String toDisplayStringWithoutSideEffects(Env env, LanguageInfo rLang, Object value) {
        return toDisplayString(env, rLang, value, false);
    }

    private static String toDisplayString(Env env, LanguageInfo rLang, Object value, boolean sideEffects) {
        Object view = env.getLanguageView(rLang, value);
        InteropLibrary interop = InteropLibrary.getFactory().getUncached();
        try {
            return interop.asString(interop.toDisplayString(view, sideEffects));
        } catch (UnsupportedMessageException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class ToStringTestData {
        public String intAsString;
        public String byteAsString;
        public String doubleAsString;
        public String stringAsString;
        public String trueAsString;
        public String falseAsString;
        public String objAsString;
        public String lazyWithoutSideEffects;
        public String lazyWithSideEffects;
    }
}
