/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RObjectSize;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.MemoryTracer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.RprofState;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.RprofState.MemoryQuad;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Implements the {@code Rprof} external.
 *
 * The output is basically a sequence of call stacks, output at each sample interval, with entries
 * in the stack identified by quoted function names. If memory profiling, the stack is preceded by a
 * auad of numbers {@code :smallv:bigv:nodes:duplicate_counter:} allocated in the interval. If line
 * profiling is enabled source files are listed as
 *
 * <pre>
 * #File N: path
 * </pre>
 *
 * and then the {@code N} is used in line number references of the form {@code N#L},which precede
 * the function name.
 *
 */
public abstract class Rprof extends RExternalBuiltinNode.Arg8 implements RDataFactory.Listener, MemoryTracer.Listener {

    private RprofState profState;

    @SuppressWarnings("unused")
    @Specialization
    public Object doRprof(RAbstractStringVector filenameVec, byte appendL, double intervalD, byte memProfilingL,
                    byte gcProfilingL, byte lineProfilingL, int numFiles, int bufSize) {
        if (!RContext.getInstance().isInitial()) {
            throw RError.error(this, RError.Message.GENERIC, "profiling not supported in created contexts");
        }
        profState = RContext.getInstance().stateInstrumentation.getRprof();
        String filename = filenameVec.getDataAt(0);
        if (filename.length() == 0) {
            // disable
            endProfiling();
        } else {
            // enable after ending any previous session
            if (profState.out() != null) {
                endProfiling();
            }
            boolean append = RRuntime.fromLogical(appendL);
            boolean memProfiling = RRuntime.fromLogical(memProfilingL);
            boolean gcProfiling = RRuntime.fromLogical(gcProfilingL);
            try {
                PrintWriter out = new PrintWriter(new FileWriter(filename, append));
                if (gcProfiling) {
                    RError.warning(this, RError.Message.GENERIC, "Rprof: gc profiling not supported");
                }
                if (memProfiling) {
                    RDataFactory.setListener(this);
                    RDataFactory.setAllocationTracing(true);
                    MemoryTracer.setListener(this);
                    MemoryTracer.reportEvents();
                }
                // interval is in seconds, we convert to millis
                long intervalInMillis = (long) (1E3 * intervalD);
                StatementListener statementListener = new StatementListener();
                ProfileThread profileThread = new ProfileThread(intervalInMillis, statementListener);
                profileThread.setDaemon(true);
                profState.initialize(out, profileThread, statementListener, intervalInMillis, RRuntime.fromLogical(lineProfilingL), memProfiling);
                profileThread.start();
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, String.format("Rprof: cannot open profile file '%s'", filename));
            }
        }
        return RNull.instance;
    }

    @Override
    @TruffleBoundary
    public void reportAllocation(RTypedValue data) {
        long size = RObjectSize.getObjectSize(data, Rprofmem.myIgnoreObjectHandler);
        if (data instanceof RAbstractVector) {
            if (size >= Rprofmem.LARGE_VECTOR) {
                profState.memoryQuad().largeV += size;
            } else {
                profState.memoryQuad().smallV += size;
            }
        } else {
            profState.memoryQuad().nodes += size;
        }

    }

    @Override
    @TruffleBoundary
    public void reportCopying(RAbstractVector source, RAbstractVector dest) {
        profState.memoryQuad().copied += RObjectSize.getObjectSize(source, Rprofmem.myIgnoreObjectHandler);
    }

    private void endProfiling() {
        ProfileThread profileThread = (ProfileThread) profState.profileThread();
        profileThread.running = false;
        HashMap<String, Integer> fileMap = null;
        PrintWriter out = profState.out();
        StatementListener statementListener = (StatementListener) profState.statementListener();
        if (profState.memoryProfiling()) {
            out.print("memory profiling: ");
        }
        if (profState.lineProfiling()) {
            out.print("line profiling: ");
        }
        out.printf("sample.interval=%d\n", profState.intervalInMillis() * 1000);
        if (profState.lineProfiling()) {
            // scan stacks to find files
            fileMap = new HashMap<>();
            int fileIndex = 0;
            for (ArrayList<RSyntaxNode> intervalStack : statementListener.intervalStacks) {
                for (RSyntaxNode node : intervalStack) {
                    String path = getPath(node);
                    if (path != null && fileMap.get(path) == null) {
                        fileMap.put(path, ++fileIndex);
                        out.printf("#File %d: %s\n", fileIndex, path);
                    }
                }
            }
        }
        int index = 0;
        for (ArrayList<RSyntaxNode> intervalStack : statementListener.intervalStacks) {
            if (profState.memoryProfiling()) {
                MemoryQuad mq = statementListener.intervalMemory.get(index);
                out.printf(":%d:%d:%d:%d:", mq.largeV, mq.smallV, mq.nodes, mq.copied);
            }
            for (RSyntaxNode node : intervalStack) {
                RootNode rootNode = node.asRNode().getRootNode();
                if (rootNode instanceof FunctionDefinitionNode) {
                    String name = rootNode.getName();
                    if (profState.lineProfiling()) {
                        Integer fileIndex = fileMap.get(getPath(node));
                        if (fileIndex != null) {
                            out.printf("%d#%d ", fileIndex, node.getSourceSection().getStartLine());
                        }
                    }
                    out.printf("\"%s\" ", name);
                }
            }
            out.println();
            index++;
        }
        out.close();
        profState.setOut(null);
    }

    private static String getPath(RSyntaxNode node) {
        Source source = node.getSourceSection().getSource();
        String path = RSource.getPath(source);
        return path;
    }

    private static final class ProfileThread extends Thread {
        private final long interval;
        private final StatementListener statementListener;
        private volatile boolean running = true;

        private ProfileThread(long interval, StatementListener statementListener) {
            this.interval = interval;
            this.statementListener = statementListener;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(interval);
                    statementListener.intervalElapsed();
                } catch (InterruptedException ex) {

                }
            }
        }

    }

    /**
     * Emulates a sampling timer by checking when the sample interval rolls over and at that point
     * collects the stack of functions.
     */
    private final class StatementListener implements ExecutionEventListener {
        private ArrayList<ArrayList<RSyntaxNode>> intervalStacks = new ArrayList<>();
        private ArrayList<MemoryQuad> intervalMemory = new ArrayList<>();
        private volatile boolean newInterval;

        private StatementListener() {
            SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
            builder.tagIs(StandardTags.StatementTag.class);
            SourceSectionFilter filter = builder.build();
            RInstrumentation.getInstrumenter().attachListener(filter, this);
        }

        private void intervalElapsed() {
            newInterval = true;
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (newInterval) {
                /* context tells here we are now, frame provides callers. */
                final ArrayList<RSyntaxNode> stack = new ArrayList<>();
                stack.add((RSyntaxNode) context.getInstrumentedNode());
                collectStack(stack);
                intervalStacks.add(stack);
                if (profState.memoryProfiling()) {
                    intervalMemory.add(profState.memoryQuad().copyAndClear());
                }

                newInterval = false;
            }
        }

        @TruffleBoundary
        private void collectStack(final ArrayList<RSyntaxNode> stack) {
            Utils.iterateRFrames(FrameAccess.READ_ONLY, new Function<Frame, Object>() {

                @Override
                public Object apply(Frame f) {
                    RCaller call = RArguments.getCall(f);
                    if (call != null && call.isValidCaller()) {
                        while (call.isPromise()) {
                            call = call.getParent();
                        }
                        RSyntaxNode syntaxNode = call.getSyntaxNode();
                        stack.add(syntaxNode);
                    }
                    return null;
                }

            });
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }
}
