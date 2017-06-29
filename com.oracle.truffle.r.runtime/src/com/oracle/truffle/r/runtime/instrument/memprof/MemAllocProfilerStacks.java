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

import java.util.Comparator;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.oracle.truffle.r.runtime.instrument.memprof.MemAllocProfilerPaths.Entry;
import com.oracle.truffle.r.runtime.instrument.memprof.MemAllocProfilerPaths.Stats;
import com.oracle.truffle.api.source.SourceSection;

/**
 * This class represents the hierarchical model of memory allocations. It is designed as a
 * singleton, since it is assumed that there is at most one profiling session at any moment.
 */
public final class MemAllocProfilerStacks {

    private static final MemAllocProfilerStacks instance = new MemAllocProfilerStacks();

    final MemAllocProfilerPaths stackPaths = new MemAllocProfilerPaths();
    final ConcurrentHashMap<Thread, Deque<Entry>> stacks = new ConcurrentHashMap<>();
    final MemAllocProfilerPaths.Stats globalStats = new MemAllocProfilerPaths.Stats();

    private MemAllocProfilerStacks() {

    }

    /**
     * @return the single instance of the model
     */
    public static MemAllocProfilerStacks getInstance() {
        return instance;
    }

    /**
     * Clear the model.
     */
    public synchronized void clear() {
        stackPaths.clear();
        globalStats.clear();
        stacks.clear();
    }

    /**
     *
     * @return the stack paths
     */
    public MemAllocProfilerPaths getStackPaths() {
        return stackPaths;
    }

    /**
     *
     * @param id the id of the allocation entry
     * @return the allocation entry
     */
    public Entry getEntry(int id) {
        return stackPaths.getEntry(id);
    }

    /**
     * @return the global (overall) allocations statistic
     */
    public Stats getGlobalStats() {
        return globalStats;
    }

    void push(String name, SourceSection section) {
        Deque<Entry> stack = getStackForThread();
        Entry parentEntry = stack.peek();
        Entry entry = parentEntry.children.get(section);
        if (entry == null) {
            entry = new Entry(stackPaths, parentEntry, name, section);
        }
        stack.push(entry);
    }

    private Deque<Entry> getStackForThread() {
        return stacks.computeIfAbsent(Thread.currentThread(), (t) -> {
            ConcurrentLinkedDeque<Entry> stk = new ConcurrentLinkedDeque<>();
            Entry stackRoot = new Entry(stackPaths, stackPaths.getRootEntry(), "<" + Thread.currentThread().getName() + ">", null);
            stk.add(stackRoot);
            return stk;
        });
    }

    void pop() {
        Deque<Entry> stack = stacks.get(Thread.currentThread());
        if (stack != null) {
            // The stacks map might get cleared during the preceding execution of the instrumented
            // node or its child node. Typically it happens if there is a function in a guest
            // language turning off the profiler.
            stack.pop();
        }
    }

    void reportAllocation(long size) {
        Deque<Entry> stack = stacks.get(Thread.currentThread());
        if (stack != null) {
            Entry entry = stack.peek();
            globalStats.add(size, 1);
            entry.stats.add(size, 1);
        }
    }

    public static final class AlocatedAggrComparator implements Comparator<Entry> {

        private final boolean desc;

        public AlocatedAggrComparator(boolean desc) {
            this.desc = desc;
        }

        @Override
        public int compare(Entry e1, Entry e2) {
            Long a1 = e1.getAllocatedAggr();
            Long a2 = e2.getAllocatedAggr();
            return desc ? a2.compareTo(a1) : a1.compareTo(a2);
        }
    }
}
