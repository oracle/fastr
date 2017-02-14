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
package com.oracle.truffle.r.library.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.Node;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RObjectSize;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.RprofState;

public abstract class Rprofmem extends RExternalBuiltinNode.Arg3 implements RDataFactory.Listener {

    static {
        Casts casts = new Casts(Rprofmem.class);
        casts.arg(0, "filename").mustBe(stringValue()).asStringVector();
        casts.arg(1, "append").mustBe(instanceOf(byte.class));
        casts.arg(2, "threshold").mustBe(doubleValue()).asDoubleVector();
    }

    @Specialization
    @TruffleBoundary
    public Object doRprofmem(RAbstractStringVector filenameVec, byte appendL, RAbstractDoubleVector thresholdVec) {
        String filename = filenameVec.getDataAt(0);
        if (filename.length() == 0) {
            // disable
            endProfiling();
        } else {
            // enable after ending any previous session
            RprofmemState profmemState = RprofmemState.get();
            if (profmemState != null && profmemState.out() != null) {
                endProfiling();
            }
            boolean append = RRuntime.fromLogical(appendL);
            try {
                PrintStream out = new PrintStream(new FileOutputStream(filename, append));
                profmemState.initialize(out, thresholdVec.getDataAt(0));
                RDataFactory.addListener(this);
                RDataFactory.setTracingState(true);
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, String.format("Rprofmem: cannot open profile file '%s'", filename));
            }
        }
        return RNull.instance;
    }

    private static void endProfiling() {
        RprofmemState profmemState = RprofmemState.get();
        if (profmemState.out() != null) {
            profmemState.cleanup(0);
        }
    }

    private static final int PAGE_SIZE = 2000;
    static final int LARGE_VECTOR = 128;

    /**
     * We ignore nested {@link RTypedValue} instances as these will have been counted already. We
     * also ignore {@link Node} instances, except in {@link RFunction} objects.
     */
    private static class MyIgnoreObjectHandler implements RObjectSize.IgnoreObjectHandler {
        @Override
        public boolean ignore(Object rootObject, Object obj) {
            if (obj == RNull.instance) {
                return true;
            } else {
                Class<?> klass = obj.getClass();
                if (RTypedValue.class.isAssignableFrom(klass)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    static final RObjectSize.IgnoreObjectHandler myIgnoreObjectHandler = new MyIgnoreObjectHandler();

    @Override
    @TruffleBoundary
    public void reportAllocation(RTypedValue data) {
        // We could do some in memory buffering
        // TODO write out full stack
        RprofmemState profmemState = RprofmemState.get();
        Frame frame = Utils.getActualCurrentFrame();
        if (frame == null) {
            // not an R evaluation, some internal use
            return;
        }
        RFunction func = RArguments.getFunction(frame);
        if (func == null) {
            return;
        }
        String name = func.getRootNode().getName();

        long size = RObjectSize.getObjectSize(data, myIgnoreObjectHandler);
        if (data instanceof RAbstractVector && size >= LARGE_VECTOR) {
            if (size > profmemState.threshold) {
                profmemState.out().printf("%d: %s\n", size, name);
            }
        } else {
            int pageCount = profmemState.pageCount;
            long pcs = pageCount + size;
            if (pcs > PAGE_SIZE) {
                profmemState.out().printf("new page: %s\n", name);
                profmemState.pageCount = (int) (pcs - PAGE_SIZE);
            } else {
                profmemState.pageCount = (int) pcs;
            }
        }
    }

    private static final class RprofmemState extends RprofState {
        private double threshold;
        private int pageCount;

        private static RprofmemState get() {
            RprofmemState state = (RprofmemState) RContext.getInstance().stateInstrumentation.getRprofState("mem");
            if (state == null) {
                state = new RprofmemState();
                RContext.getInstance().stateInstrumentation.setRprofState("mem", state);
            }
            return state;
        }

        public void initialize(PrintStream outA, double thresholdA) {
            setOut(outA);
            this.threshold = thresholdA;
        }

        @Override
        public void cleanup(int status) {
            RDataFactory.setTracingState(false);
            closeAndResetOut();
        }
    }
}
