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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;
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
import com.oracle.truffle.r.runtime.data.MemoryCopyTracer;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RObjectSize;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;

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
public abstract class Rprof extends RExternalBuiltinNode.Arg8 implements RDataFactory.Listener, MemoryCopyTracer.Listener {

    static {
        Casts casts = new Casts(Rprof.class);
        casts.arg(0, "filename").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        casts.arg(1, "append_mode").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).map(toBoolean());
        casts.arg(2, "dinterval").asDoubleVector().findFirst(RRuntime.DOUBLE_NA);
        casts.arg(3, "mem_profiling").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).map(toBoolean());
        casts.arg(4, "gc_profiling").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).map(toBoolean());
        casts.arg(5, "line_profiling").asLogicalVector().findFirst(RRuntime.LOGICAL_NA).map(toBoolean());
        casts.arg(6, "numfiles").asIntegerVector().findFirst().mustBe(gte(0));
        casts.arg(7, "bufsize").asIntegerVector().findFirst().mustBe(gte(0));
    }

    @Specialization
    @TruffleBoundary
    public Object doRprof(String filename, boolean append, double intervalD, boolean memProfiling, boolean gcProfiling, boolean lineProfiling, @SuppressWarnings("unused") int numFiles,
                    @SuppressWarnings("unused") int bufSize) {
        RprofState profState = RprofState.get();
        if (filename.length() == 0) {
            // disable
            endProfiling();
        } else {
            // enable after ending any previous session
            if (profState != null && profState.out() != null) {
                endProfiling();
            }
            try {
                PrintStream out = new PrintStream(new FileOutputStream(filename, append));
                if (gcProfiling) {
                    RError.warning(this, RError.Message.GENERIC, "Rprof: gc profiling not supported");
                }
                if (memProfiling) {
                    RDataFactory.addListener(this);
                    RDataFactory.setTracingState(true);
                    MemoryCopyTracer.addListener(this);
                    MemoryCopyTracer.setTracingState(true);
                }
                // interval is in seconds, we convert to millis
                long intervalInMillis = (long) (1E3 * intervalD);
                StatementListener statementListener = new StatementListener();
                ProfileThread profileThread = new ProfileThread(intervalInMillis, statementListener);
                profileThread.setDaemon(true);
                profState.initialize(out, profileThread, statementListener, intervalInMillis, lineProfiling, memProfiling);
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
        RprofState profState = RprofState.get();
        long size = RObjectSize.getObjectSize(data, Rprofmem.myIgnoreObjectHandler);
        if (data instanceof RAbstractVector) {
            if (size >= Rprofmem.LARGE_VECTOR) {
                profState.memoryQuad.largeV += size;
            } else {
                profState.memoryQuad.smallV += size;
            }
        } else {
            profState.memoryQuad.nodes += size;
        }
    }

    @Override
    @TruffleBoundary
    public void reportCopying(RAbstractVector source, RAbstractVector dest) {
        RprofState profState = RprofState.get();
        profState.memoryQuad.copied += RObjectSize.getObjectSize(source, Rprofmem.myIgnoreObjectHandler);
    }

    private static void endProfiling() {
        RprofState profState = RprofState.get();
        if (profState.out() != null) {
            profState.cleanup(0);
        }
    }

    private static String getPath(RSyntaxElement node) {
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
        private final ArrayList<ArrayList<RSyntaxElement>> intervalStacks = new ArrayList<>();
        private final ArrayList<RprofState.MemoryQuad> intervalMemory = new ArrayList<>();
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
                final ArrayList<RSyntaxElement> stack = new ArrayList<>();
                stack.add((RSyntaxElement) context.getInstrumentedNode());
                collectStack(stack);
                intervalStacks.add(stack);
                RprofState profState = RprofState.get();
                if (profState.memoryProfiling) {
                    intervalMemory.add(profState.memoryQuad.copyAndClear());
                }

                newInterval = false;
            }
        }

        @TruffleBoundary
        private void collectStack(final ArrayList<RSyntaxElement> stack) {
            Utils.iterateRFrames(FrameAccess.READ_ONLY, new Function<Frame, Object>() {

                @Override
                public Object apply(Frame f) {
                    RCaller call = RArguments.getCall(f);
                    if (call != null && call.isValidCaller()) {
                        while (call.isPromise()) {
                            call = call.getParent();
                        }
                        RSyntaxElement syntaxNode = call.getSyntaxNode();
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

    /**
     * State used by {@code Rprof}.
     *
     */
    private static final class RprofState extends InstrumentationState.RprofState {
        private ProfileThread profileThread;
        private StatementListener statementListener;
        private long intervalInMillis;
        private boolean lineProfiling;
        private boolean memoryProfiling;
        private MemoryQuad memoryQuad;

        public static final class MemoryQuad {
            public long smallV;
            public long largeV;
            public long nodes;
            public long copied;

            public MemoryQuad copyAndClear() {
                MemoryQuad result = new MemoryQuad();
                result.copied = copied;
                result.largeV = largeV;
                result.smallV = smallV;
                result.nodes = nodes;
                copied = 0;
                largeV = 0;
                smallV = 0;
                nodes = 0;
                return result;
            }
        }

        private static RprofState get() {
            RprofState state = (RprofState) RContext.getInstance().stateInstrumentation.getRprofState("prof");
            if (state == null) {
                state = new RprofState();
                RContext.getInstance().stateInstrumentation.setRprofState("prof", state);
            }
            return state;
        }

        public void initialize(PrintStream outA, ProfileThread profileThreadA, StatementListener statementListenerA, long intervalInMillisA,
                        boolean lineProfilingA, boolean memoryProfilingA) {
            setOut(outA);
            this.profileThread = profileThreadA;
            this.statementListener = statementListenerA;
            this.intervalInMillis = intervalInMillisA;
            this.lineProfiling = lineProfilingA;
            this.memoryProfiling = memoryProfilingA;
            this.memoryQuad = memoryProfilingA ? new MemoryQuad() : null;
        }

        @Override
        public void cleanup(int status) {
            profileThread.running = false;
            HashMap<String, Integer> fileMap = null;
            PrintStream out = this.out();
            if (this.memoryProfiling) {
                out.print("memory profiling: ");
            }
            if (this.lineProfiling) {
                out.print("line profiling: ");
            }
            out.printf("sample.interval=%d\n", this.intervalInMillis * 1000);
            if (this.lineProfiling) {
                // scan stacks to find files
                fileMap = new HashMap<>();
                int fileIndex = 0;
                for (ArrayList<RSyntaxElement> intervalStack : statementListener.intervalStacks) {
                    for (RSyntaxElement node : intervalStack) {
                        String path = getPath(node);
                        if (path != null && fileMap.get(path) == null) {
                            fileMap.put(path, ++fileIndex);
                            out.printf("#File %d: %s\n", fileIndex, path);
                        }
                    }
                }
            }
            int index = 0;
            for (ArrayList<RSyntaxElement> intervalStack : statementListener.intervalStacks) {
                if (this.memoryProfiling) {
                    RprofState.MemoryQuad mq = statementListener.intervalMemory.get(index);
                    out.printf(":%d:%d:%d:%d:", mq.largeV, mq.smallV, mq.nodes, mq.copied);
                }
                for (RSyntaxElement node : intervalStack) {
                    RootNode rootNode = ((RSyntaxNode) node).asRNode().getRootNode();
                    if (rootNode instanceof FunctionDefinitionNode) {
                        String name = rootNode.getName();
                        if (this.lineProfiling) {
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
            this.setOut(null);
            if (this.memoryProfiling) {
                RDataFactory.setTracingState(false);
                MemoryCopyTracer.setTracingState(false);
            }
        }
    }
}
