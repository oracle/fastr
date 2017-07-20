/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.instrument.memprof;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.AllocationEvent;
import com.oracle.truffle.api.instrumentation.AllocationEventFilter;
import com.oracle.truffle.api.instrumentation.AllocationListener;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.source.SourceSection;

@Registration(name = "MemAllocProfiler", id = MemAllocProfilerInstrument.ID)
public class MemAllocProfilerInstrument extends TruffleInstrument {

    public static final String ID = "mem-alloc-profiler";

    private EventBinding<MemAllocEventFactory> allocationEventBinding;

    @Override
    protected void onCreate(TruffleInstrument.Env env) {
        env.registerService(this);

        Instrumenter instrumenter = env.getInstrumenter();
        MemAllocEventFactory eventFactory = new MemAllocEventFactory(env);
        SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
        SourceSectionFilter filter = builder.tagIs(StandardTags.StatementTag.class).build();
        instrumenter.attachFactory(filter, eventFactory);
        allocationEventBinding = instrumenter.attachAllocationListener(AllocationEventFilter.newBuilder().build(), eventFactory);

        env.registerService(eventFactory.memAllocStacks);
    }

    @Override
    protected void onDispose(Env env) {
        allocationEventBinding.dispose();
    }

    public static class MemAllocEventFactory implements ExecutionEventNodeFactory, AllocationListener {

        protected final Env env;
        protected final MemAllocProfilerStacks memAllocStacks = MemAllocProfilerStacks.getInstance();

        protected MemAllocEventFactory(final Env env) {
            this.env = env;
        }

        @Override
        @TruffleBoundary
        public void onEnter(AllocationEvent event) {
        }

        @Override
        @TruffleBoundary
        public void onReturnValue(AllocationEvent event) {
            memAllocStacks.reportAllocation(event.getNewSize() - event.getOldSize());
        }

        public MemAllocProfilerStacks getStacks() {
            return memAllocStacks;
        }

        public void dispose() {
            memAllocStacks.clear();
        }

        @Override
        public ExecutionEventNode create(final EventContext ec) {
            return new ExecutionEventNode() {

                @Override
                protected void onEnter(VirtualFrame frame) {
                    pushEntry();
                }

                @Override
                public void onReturnValue(VirtualFrame vFrame, Object result) {
                    popEntry();
                }

                @Override
                protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
                    popEntry();
                }

                @TruffleBoundary
                private void pushEntry() {
                    SourceSection src = ec.getInstrumentedSourceSection();
                    memAllocStacks.push(ec.getInstrumentedNode().getRootNode().getName(), src);
                }

                @TruffleBoundary
                private void popEntry() {
                    memAllocStacks.pop();
                }
            };
        }
    }
}
