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
package com.oracle.truffle.r.nodes.builtin.fastr.memprof;

import java.io.PrintStream;
import java.util.Iterator;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.instrument.memprof.MemAllocProfilerPaths;
import com.oracle.truffle.r.runtime.instrument.memprof.MemAllocProfilerStacks;

/**
 * A utility class that can be used for developing simple command line profiling tools. It works on
 * top of the {@link MemAllocProfilerStacks memory allocations model}.
 *
 * <pre>
 * MemAllocProfilerPrinter memAllocPrinter = new MemAllocProfilerPrinter(System.out);
 * // Print the whole hierarchy of allocations
 * memAllocPrinter.show(null, entryId.MAX_VALUE, true, true);
 * // Print the source section associated with the allocation entry 103
 * memAllocPrinter.source(103);
 * </pre>
 */
public final class MemAllocProfilerPrinter {

    private final PrintStream out;

    /**
     * @param out the output stream used to print the output
     */
    public MemAllocProfilerPrinter(PrintStream out) {
        this.out = out;
    }

    /**
     * Show the allocations hierarchy.
     *
     * @param paths the allocation paths
     * @param entryId the id of the top allocation entry. If <code>null</code> the absolute root
     *            entry is used.
     * @param levels the maximum number of levels of the hierarchy to print
     * @param desc if true allocation entries will be sorted in the descending order below their
     *            parent entries
     * @param printParents instructs to print the parent entries of the top entry. It makes sense
     *            only if a sub-hierarchy is printed.
     */
    public void show(MemAllocProfilerPaths paths, Integer entryId, int levels, boolean desc, boolean printParents) {
        MemAllocProfilerPaths.Entry rootEntry = null;
        if (entryId != null) {
            rootEntry = paths.getEntry(entryId);
        }

        paths.traverse(rootEntry, stack -> {
            if (stack.peek().getStats().getCount() == 0 && stack.size() != levels) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            Iterator<MemAllocProfilerPaths.Entry> iter = stack.descendingIterator();
            while (iter.hasNext()) {
                MemAllocProfilerPaths.Entry entry = iter.next();
                if (entry.getId() == 0) {
                    continue;
                }
                sb.append('/').append(entry.getName()).append("[id=").append(entry.getId()).append(']');
            }
            MemAllocProfilerPaths.Entry entry = stack.peek();
            sb.append(" { size: ").append(entry.getAllocatedAggr()).append(", count: ").append(entry.getCountAggr()).append(" }");
            out.println(sb.toString());
        }, new MemAllocProfilerStacks.AlocatedAggrComparator(desc), levels, printParents);
    }

    /**
     * Print the source section associated with the given allocation entry id.
     *
     * @param entryId the entry id
     */
    public void source(MemAllocProfilerPaths paths, int entryId) {
        MemAllocProfilerPaths.Entry entry = paths.getEntry(entryId);
        if (entry == null) {
            out.format("No entry found for id %s\n", entryId);
        } else {
            SourceSection sel = entry.getSourceSection();
            StringBuilder sb = new StringBuilder();
            sb.append("{ size: ").append(entry.getAllocatedAggr()).append(", localSize: ").append(entry.getStats().getAllocated()).append(", count: ").append(entry.getCountAggr()).append(
                            ", localCount: ").append(entry.getStats().getCount()).append(" }");
            out.println(sb);

            if (sel == null) {
                out.println("No source available");
            } else {
                out.format("<<< %s at %s:%s\n", entry.getName(), sel.getStartLine(), sel.getStartColumn());
                out.println(sel.getCode());
                out.format(">>> %s at %s:%s\n", entry.getName(), sel.getEndLine(), sel.getEndColumn());
            }
        }
    }
}
