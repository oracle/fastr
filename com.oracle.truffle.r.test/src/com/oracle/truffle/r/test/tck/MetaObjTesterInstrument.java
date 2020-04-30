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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.r.test.tck.MetaObjTesterInstrument.MetaObjTestData;

@TruffleInstrument.Registration(id = MetaObjTesterInstrument.ID, name = MetaObjTesterInstrument.ID, version = "1.0", services = MetaObjTestData.class)
public class MetaObjTesterInstrument extends TruffleInstrument {
    public static final String ID = "MetaObjsTester";

    @Override
    protected void onCreate(Env env) {
        MetaObjTestData testData = new MetaObjTestData();
        env.registerService(testData);
        SourceSectionFilter sourceFilter = SourceSectionFilter.newBuilder().includeInternal(false).build();
        boolean[] firstExecution = new boolean[]{true};
        env.getInstrumenter().attachExecutionEventFactory(sourceFilter, context -> new ExecutionEventNode() {
            @Override
            protected void onEnter(VirtualFrame frame) {
                if (firstExecution[0]) {
                    firstExecution[0] = false;
                    runTest(frame.materialize());
                }
            }

            @TruffleBoundary
            private void runTest(MaterializedFrame frame) {
                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                LanguageInfo rLang = env.getLanguages().get("R");

                Object intView = env.getLanguageView(rLang, 42);
                Object doubleView = env.getLanguageView(rLang, 33.33);
                try {
                    Object intMeta = interop.getMetaObject(intView);
                    assertTrue(interop.isMetaInstance(intMeta, intView));
                    assertFalse(interop.isMetaInstance(intMeta, doubleView));
                    checkName(interop, intMeta, "integer");
                } catch (UnsupportedMessageException e) {
                    throw new RuntimeException(e);
                }

                FrameSlot lazySlot = frame.getFrameDescriptor().findFrameSlot("lazy");
                Object lazyValue;
                try {
                    lazyValue = frame.getObject(lazySlot);
                } catch (FrameSlotTypeException e) {
                    throw new RuntimeException(e);
                }

                try {
                    Object lazyMeta = interop.getMetaObject(lazyValue);
                    assertFalse(interop.isMetaInstance(lazyMeta, doubleView));
                    assertTrue(interop.isMetaInstance(lazyMeta, lazyValue));
                    checkName(interop, lazyMeta, "promise");

                    // toDisplayString with side effects evaluates the promise
                    interop.asString(interop.toDisplayString(lazyValue, true));

                    // evaluating the promise changes the meta-type to the type of the
                    // underlying value
                    lazyMeta = interop.getMetaObject(lazyValue);
                    checkName(interop, lazyMeta, "character");
                } catch (UnsupportedMessageException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static void checkName(InteropLibrary interop, Object metaObj, String expected) throws UnsupportedMessageException {
        assertEquals(interop.toDisplayString(metaObj, false), expected);
        assertEquals(interop.toDisplayString(metaObj, true), expected);
        assertEquals(interop.getMetaSimpleName(metaObj), expected);
        assertEquals(interop.getMetaQualifiedName(metaObj), expected);
    }

    public static final class MetaObjTestData {
    }
}
