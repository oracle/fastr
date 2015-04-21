/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.control;

import java.util.concurrent.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;

@NodeInfo(cost = NodeCost.NONE)
public class SequenceNode extends RNode {

    @Children private final RNode[] sequence;

    private final long[] timing;

    public SequenceNode(RNode[] sequence) {
        this.sequence = sequence;
        this.timing = null;
    }

    private SequenceNode(RNode[] sequence, SourceSection src) {
        this.sequence = sequence;
        this.timing = PerfHandler.initTiming(src, this);
        if (src != null) {
            assignSourceSection(src);
        }
    }

    public SequenceNode(SourceSection src, RNode node) {
        this(convert(node), src);
    }

    public SequenceNode(SourceSection src, RNode[] sequence) {
        this(sequence, src);
    }

    /**
     * Similar to {@link #ensureSequence} but for subclasses, where we have to extract any existing
     * array.
     *
     * @param node
     */
    protected SequenceNode(RNode node) {
        this(convert(node));
    }

    public RNode[] getSequence() {
        return sequence;
    }

    /**
     * Ensures that {@code node} is a {@link SequenceNode} by converting any other node to a single
     * length sequence. This is important because sequences are meaningful to the instrumentation
     * framework.
     */
    public static RNode ensureSequence(RNode node) {
        if (node == null || node instanceof SequenceNode) {
            return node;
        } else {
            return new SequenceNode(new RNode[]{node});
        }
    }

    private static RNode[] convert(RNode node) {
        if (node instanceof SequenceNode) {
            return ((SequenceNode) node).sequence;
        } else {
            return new RNode[]{node};
        }
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object lastResult = null;
        long t1 = timing == null ? 0 : System.nanoTime();
        for (int i = 0; i < sequence.length; i++) {
            lastResult = sequence[i].execute(frame);
            if (timing != null) {
                long t2 = System.nanoTime();
                timing[i] += t2 - t1;
                t1 = t2;
            }
        }
        return lastResult;
    }

    @TruffleBoundary
    @Override
    public void deparse(RDeparse.State state) {
        for (int i = 0; i < sequence.length; i++) {
            state.mark();
            sequence[i].deparse(state);
            if (state.changed()) {
                // not all nodes will produce output
                state.writeline();
                state.mark(); // in case last
            }
        }
    }

    @Override
    public void serialize(RSerialize.State state) {
        /*
         * In GnuR there are no empty statement sequences, because "{" is really a function in R, so
         * it is represented as a LANGSXP with symbol "{" and a NULL cdr, representing the empty
         * sequence. This is an unpleasant special case in FastR that we can only detect by
         * re-examining the original source.
         * 
         * A sequence of length 1, i.e. a single statement, is represented at itself, e.g. a SYMSXP
         * for "x" or a LANGSXP for a function call. Otherwise, the representation is a LISTSXP
         * pairlist, where the car is the statement and the cdr is either NILSXP or a LISTSXP for
         * the next statement. Typically the statement (car) is itself a LANGSXP pairlist but it
         * might be a simple value, e.g. SYMSXP.
         */
        if (sequence.length == 0) {
            state.setNull();
        } else {
            for (int i = 0; i < sequence.length; i++) {
                state.serializeNodeSetCar(sequence[i]);
                if (i != sequence.length - 1) {
                    state.openPairList();
                }
            }
            state.linkPairList(sequence.length);
        }
    }

    @TruffleBoundary
    @Override
    public RNode substitute(REnvironment env) {
        RNode[] sequenceSubs = new RNode[sequence.length];
        for (int i = 0; i < sequence.length; i++) {
            sequenceSubs[i] = sequence[i].substitute(env);
        }
        return new SequenceNode(sequenceSubs);
    }

    // Performance analysis

    static {
        RPerfStats.register(new PerfHandler());
    }

    public static class PerfHandler implements RPerfStats.Handler {

        private static ConcurrentLinkedDeque<SequenceNode> timedSequences;

        private static long[] initTiming(SourceSection src, SequenceNode sequence) {
            CompilerAsserts.neverPartOfCompilation();
            if (timedSequences != null && src != null) {
                timedSequences.add(sequence);
                return new long[sequence.sequence.length];
            }
            return null;
        }

        public void initialize(String optionText) {
            timedSequences = new ConcurrentLinkedDeque<>();
        }

        public String getName() {
            return "sequences";
        }

        public void report() {
            if (timedSequences != null) {
                for (SequenceNode sequence : timedSequences) {
                    long total = 0;
                    if (sequence.getSourceSection() == null) {
                        for (int i = 0; i < sequence.timing.length; i++) {
                            total += sequence.timing[i];
                        }
                        FunctionDefinitionNode rootNode = (FunctionDefinitionNode) sequence.getRootNode();
                        RDeparse.State state = RDeparse.State.createPrintableState();
                        rootNode.deparse(state);
                        RPerfStats.out().println("Source (" + rootNode + "): " + (total / 1000000) + "ms");
                        RPerfStats.out().println(state.toString());
                    } else {
                        Source source = sequence.getSourceSection().getSource();
                        long[] time = new long[source.getLineCount() + 1];
                        for (int i = 0; i < sequence.timing.length; i++) {
                            SourceSection src = sequence.sequence[i].getSourceSection();
                            if (src != null) {
                                time[src.getLineLocation().getLineNumber()] += sequence.timing[i];
                                total += sequence.timing[i];
                            }
                        }
                        if (total > 0L) {
                            int startLine = sequence.getSourceSection().getStartLine();
                            int endLine = startLine + sequence.getSourceSection().getCode().split("\n").length - 1;
                            RPerfStats.out().println("File " + source.getName() + " lines " + startLine + "-" + endLine + ", total: " + (total / 1000000) + "ms");
                            for (int i = startLine; i <= endLine; i++) {
                                RPerfStats.out().printf("%2d%%: %s%n", (time[i] + (total / 200)) * 100 / total, source.getCode(i));
                            }
                        }
                    }
                }
            }
        }
    }
}
