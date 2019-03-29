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
package com.oracle.truffle.r.test.tck;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.r.test.tck.ToStringTesterInstrument.Initialize;

@TruffleInstrument.Registration(id = ToStringTesterInstrument.ID, name = ToStringTesterInstrument.ID, version = "1.0", services = Initialize.class)
public class ToStringTesterInstrument extends TruffleInstrument {
    public static final String ID = "ToStringTester";

    static String intAsString;
    static String byteAsString;
    static String doubleAsString;
    static String stringAsString;
    static String booleanAsString;

    @Override
    protected void onCreate(Env env) {
        env.registerService(new Initialize() {
        });

        env.getInstrumenter().attachExecutionEventFactory(SourceSectionFilter.ANY, context -> new ExecutionEventNode() {
            @Override
            protected void onEnter(VirtualFrame frame) {
                LanguageInfo rLanguage = env.getLanguages().get("R");
                intAsString = env.toString(rLanguage, 42);
                byteAsString = env.toString(rLanguage, (byte) 42);
                doubleAsString = env.toString(rLanguage, 42.5);
                stringAsString = env.toString(rLanguage, "Hello");
                booleanAsString = env.toString(rLanguage, true);
            }
        });
    }

    public interface Initialize {
    }
}
