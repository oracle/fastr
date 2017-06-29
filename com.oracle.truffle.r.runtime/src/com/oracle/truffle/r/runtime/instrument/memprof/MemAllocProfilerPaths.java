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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

public final class MemAllocProfilerPaths {
    private final AtomicLong version = new AtomicLong();
    private final AtomicInteger idGen = new AtomicInteger();
    private final Map<Integer, Entry> entryMap = new ConcurrentHashMap<>();
    private volatile Entry root = new Entry(this, null, "", null);

    private MemAllocProfilerPaths hsView;
    private long hsViewVersion;

    /**
     * Clear the model.
     */
    public synchronized void clear() {
        idGen.set(0);
        entryMap.clear();
        root = new Entry(this, null, "", null);
    }

    /**
     *
     * @param id the id of the allocation entry
     * @return the allocation entry
     */
    public Entry getEntry(int id) {
        return entryMap.get(id);
    }

    /**
     * @return the root entry.
     */
    public Entry getRootEntry() {
        return root;
    }

    public TruffleObject toTruffleObject() {
        return JavaInterop.asTruffleObject(this);
    }

    public static MemAllocProfilerPaths fromTruffleObject(TruffleObject to) {
        return JavaInterop.asJavaObject(MemAllocProfilerPaths.class, to);
    }

    /**
     * Store the snapshot of the current paths hierarchy.
     *
     * @param name the snapshot name
     * @return the snapshot
     */
    public MemAllocProfilerPaths getOrMakeSnapshot(String name) {
        return clonePaths();
    }

    public synchronized MemAllocProfilerPaths toHS() {
        long curVer = version.get();
        if (curVer == hsViewVersion && hsView != null) {
            return hsView;
        }
        hsViewVersion = curVer;
        hsView = invert().groupBySrcSection();
        return hsView;
    }

    @SuppressWarnings("unused")
    synchronized MemAllocProfilerPaths clonePaths() {
        MemAllocProfilerPaths clonedPaths = new MemAllocProfilerPaths();
        new Entry(clonedPaths, null, root);
        return clonedPaths;
    }

    MemAllocProfilerPaths invert() {
        List<Entry> inverted = new ArrayList<>();
        MemAllocProfilerPaths targetPaths = new MemAllocProfilerPaths();
        for (Entry entry : entryMap.values()) {
            inverted.add(entry.invert(targetPaths));
        }
        return targetPaths;
    }

    MemAllocProfilerPaths groupBySrcSection() {
        MemAllocProfilerPaths targetPaths = new MemAllocProfilerPaths();

        List<Entry> tops = new ArrayList<>();
        for (Entry synthSubRoot : root.children.values()) {
            tops.addAll(synthSubRoot.children.values());
        }

        groupBySrcSection(targetPaths, targetPaths.root, tops);
        return targetPaths;
    }

    private void groupBySrcSection(MemAllocProfilerPaths targetPaths, Entry newParent, Collection<Entry> nonGroupedEntries) {
        Map<SourceSection, List<Entry>> entriesBySection = new HashMap<>();

        for (Entry nonGroupedEntry : nonGroupedEntries) {
            List<Entry> group = entriesBySection.get(nonGroupedEntry.sourceSection);
            if (group == null) {
                group = new ArrayList<>();
                entriesBySection.put(nonGroupedEntry.sourceSection, group);
            }
            group.add(nonGroupedEntry);
        }

        for (Map.Entry<SourceSection, List<Entry>> mapEntry : entriesBySection.entrySet()) {
            List<Entry> nonGroupedChildren = new ArrayList<>();
            Entry reducedEntry = null;
            for (Entry entryForSameSect : mapEntry.getValue()) {
                if (reducedEntry == null) {
                    reducedEntry = new Entry(targetPaths, newParent, entryForSameSect.name, entryForSameSect.sourceSection);
                }
                reducedEntry.stats.add(entryForSameSect.stats);
                nonGroupedChildren.addAll(entryForSameSect.children.values());
            }
            assert reducedEntry != null;
            groupBySrcSection(targetPaths, reducedEntry, nonGroupedChildren);
        }
    }

    /**
     * Traverse the allocations hierarchy. This method is usually used by profiling tools for
     * displaying the allocations.
     *
     * @param startEntry the start entry from which the traversal begins. It can be null to denote
     *            the absolute root.
     * @param consumer the consumer receiving the current stack (i.e. the path of entries to the
     *            root entry)
     * @param childrenEntriesComparator the comparator used to sort children
     * @param levels the maximum number of hierarchy levels to traverse
     * @param prependParentEntries include the parent entries of the root entry
     */
    public void traverse(Entry startEntry, Consumer<Deque<Entry>> consumer, Comparator<Entry> childrenEntriesComparator, int levels, boolean prependParentEntries) {
        Entry se = startEntry;
        if (se == null) {
            se = root;
        }
        traverseEntry(se, consumer, childrenEntriesComparator, levels, prependParentEntries);
    }

    private void traverseEntry(Entry rootEntry, Consumer<Deque<Entry>> consumer, Comparator<Entry> childrenEntriesComparator, int levels, boolean prependParentEntries) {
        ArrayDeque<Entry> stack = new ArrayDeque<>();
        if (prependParentEntries) {
            Entry e = rootEntry;
            while (e != null) {
                stack.addLast(e);
                e = e.parent;
            }
        } else {
            stack.push(rootEntry);
        }
        traverseStack(stack, consumer, childrenEntriesComparator, levels);
    }

    private void traverseStack(Deque<Entry> stack, Consumer<Deque<Entry>> consumer, Comparator<Entry> childrenEntriesComparator, int levels) {
        if (levels <= 0) {
            return;
        }

        consumer.accept(stack);
        Collection<Entry> children = stack.peek().children.values();
        Collection<Entry> sortedChildren;

        if (childrenEntriesComparator != null) {
            sortedChildren = new TreeSet<>(childrenEntriesComparator);
            sortedChildren.addAll(children);
        } else {
            sortedChildren = children;
        }

        for (Entry childEntry : sortedChildren) {
            stack.push(childEntry);
            traverseStack(stack, consumer, childrenEntriesComparator, levels - 1);
            stack.pop();
        }
    }

    public static final class Entry {
        // private static final SourceSection UNAVAILABLE_SECTION =
        // Source.newBuilder("").name("unavailable").mimeType("").build().createUnavailableSection();

        final MemAllocProfilerPaths paths;
        final int id;
        final String name;
        final SourceSection sourceSection;
        final Entry parent;
        final Map<SourceSection, Entry> children = new ConcurrentHashMap<>();
        final Stats stats = new Stats() {

            @Override
            public void set(long allocated, long count) {
                synchronized (paths) {
                    super.set(allocated, count);
                    paths.version.incrementAndGet();
                }
            }

        };

        Entry(MemAllocProfilerPaths paths, Entry parent, String name, SourceSection sourceSection) {
            this.paths = paths;
            synchronized (paths) {
                this.id = paths.idGen.getAndIncrement();
                this.parent = parent;
                this.name = name;
                this.sourceSection = sourceSection == null ? Source.newBuilder("").name(name).mimeType("").build().createUnavailableSection() : sourceSection;
                this.paths.entryMap.put(id, this);
                if (parent != null) {
                    parent.children.put(this.sourceSection, this);
                }
            }
        }

        @SuppressWarnings("unused")
        Entry(MemAllocProfilerPaths paths, Entry parent, Entry original) {
            this.paths = paths;
            this.parent = parent;
            assert parent == null || parent.paths == paths;
            this.id = original.id;
            this.name = original.name;
            this.sourceSection = original.sourceSection;
            this.paths.entryMap.put(id, this);
            this.stats.add(original.stats);

            if (parent != null) {
                parent.children.put(sourceSection, this);
            } else {
                paths.root = this;
            }

            for (Entry origChild : original.children.values()) {
                new Entry(paths, this, origChild);
            }
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public SourceSection getSourceSection() {
            return sourceSection;
        }

        public Entry getParent() {
            return parent;
        }

        public Map<SourceSection, Entry> getChildren() {
            return children;
        }

        public synchronized Entry[] getChildrenAsArray() {
            return children.values().toArray(new Entry[children.size()]);
        }

        public Stats getStats() {
            return stats;
        }

        public long getAllocatedAggr() {
            long a = stats.allocated;
            for (Entry childEntry : children.values()) {
                a += childEntry.getAllocatedAggr();
            }
            return a;
        }

        public long getCountAggr() {
            long a = stats.count;
            for (Entry childEntry : children.values()) {
                a += childEntry.getCountAggr();
            }
            return a;
        }

        public long getAllocated() {
            return stats.allocated;
        }

        public long getCount() {
            return stats.count;
        }

        Entry invert(MemAllocProfilerPaths targetPaths) {
            return invert(targetPaths, new Entry(targetPaths, targetPaths.root, "", null), stats);
        }

        private Entry invert(MemAllocProfilerPaths targetPaths, Entry newParent, Stats rootStat) {
            if (this == paths.root) {
                return targetPaths.root;
            }
            Entry ie = new Entry(targetPaths, newParent, name, sourceSection);
            if (parent.parent != null) {
                Entry invertedParent = parent.invert(targetPaths, ie, rootStat);
                ie.children.put(parent.sourceSection, invertedParent);
            } else {
                // Move the stats from the inverted node to its lowest child (which corresponds to
                // the root in the original hierarchy). This way the stats of a parent entry in the
                // hot-spot paths will be the aggregation of the children stats, i.e. the same fact
                // as in the stacks paths.
                ie.stats.add(rootStat);
            }
            return ie;
        }

        Entry merge(MemAllocProfilerPaths targetPaths, Entry other) {
            Entry merged = new Entry(targetPaths, this.parent, this.name, this.sourceSection);
            merged.children.putAll(this.children);
            merged.children.putAll(other.children);
            merged.stats.add(this.stats);
            merged.stats.add(other.stats);
            return merged;
        }

        @Override
        public String toString() {
            return String.format("{name: %s, alloc: %s, count: %s}", name, stats.allocated, stats.count);
        }
    }

    /**
     * The summary allocation statistic.
     */
    public static class Stats {
        private volatile long allocated;
        private volatile long count;

        /**
         * @return the allocated memory in bytes
         */
        public final long getAllocated() {
            return this.allocated;
        }

        /**
         * @return the number of allocations
         */
        public final long getCount() {
            return count;
        }

        /**
         * Set the allocated memory in bytes and allocations count.
         */
        public void set(long allocated, long count) {
            this.allocated = allocated;
            this.count = count;
        }

        final void clear() {
            set(0, 0);
        }

        final void add(long alloc, long cnt) {
            set(this.allocated + alloc, this.count + cnt);
        }

        final void add(Stats other) {
            add(other.allocated, other.count);
        }

    }

}
