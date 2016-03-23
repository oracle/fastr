/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RPerfStats;

/**
 * Provides the generic mechanism for associating attributes with a R object. It does no special
 * analysis of the "name" of the attribute; that is left to other classes, e.g. {@link RVector}.
 */
public final class RAttributes implements Iterable<RAttributes.RAttribute> {

    public interface RAttribute {
        String getName();

        Object getValue();
    }

    @ValueType
    private static class AttrInstance implements RAttribute {
        private final String name;
        private Object value;

        AttrInstance(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }
    }

    private static final ConditionProfile statsProfile = ConditionProfile.createBinaryProfile();

    public static RAttributes create() {
        return new RAttributes();
    }

    public static RAttributes createInitialized(String[] names, Object[] values) {
        return new RAttributes(names, values);
    }

    /**
     * The implementation class which is separate to avoid a circularity that would result from the
     * {@code Iterable} in the abstract class.
     */

    RAttributes() {
        if (statsProfile.profile(stats != null)) {
            stats.init();
        }
    }

    private RAttributes(RAttributes attrs) {
        if (attrs.size != 0) {
            size = attrs.size;
            names = attrs.names.clone();
            values = attrs.values.clone();
        }
    }

    private RAttributes(String[] names, Object[] values) {
        this.names = names;
        this.values = values;
        this.size = names.length;
    }

    public int find(String name) {
        for (int i = 0; i < size; i++) {
            assert names[i] != null;
            if (names[i] == name) {
                return i;
            }
        }
        return -1;
    }

    public void put(String name, Object value) {
        assert isInterned(name);
        int pos = find(name);
        if (pos == -1) {
            ensureFreeSpace();
            pos = size++;
            names[pos] = name;
        }
        // TODO: this assertion should hold in general
        // assert value == null || !(value instanceof RShareable) || !((RShareable)
        // value).isTemporary();
        values[pos] = value;
        if (statsProfile.profile(stats != null)) {
            stats.update(this);
        }
    }

    public void ensureFreeSpace() {
        if (size == names.length) {
            names = Arrays.copyOf(names, (size + 1) * 2);
            values = Arrays.copyOf(values, (size + 1) * 2);
            assert names.length == values.length;
        }
    }

    @TruffleBoundary
    private static boolean isInterned(String name) {
        assert name == name.intern() : name;
        return true;
    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private String[] names = EMPTY_STRING_ARRAY;
    private Object[] values = EMPTY_OBJECT_ARRAY;
    private int size;

    public int size() {
        return size;
    }

    public String getNameAtIndex(int i) {
        return names[i];
    }

    public Object getValueAtIndex(int i) {
        return values[i];
    }

    public void setNameAtIndex(int i, String v) {
        names[i] = v;
    }

    public void setValueAtIndex(int i, Object v) {
        // TODO: this assertion should hold in general
        // assert v == null || !(v instanceof RShareable) || !((RShareable) v).isTemporary();
        values[i] = v;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void remove(String name) {
        assert isInterned(name);
        int pos = find(name);
        if (pos != -1) {
            size--;
            for (int i = pos; i < size; i++) {
                names[i] = names[i + 1];
                values[i] = values[i + 1];
            }
            names[size] = null;
            values[size] = null;
        }
    }

    public Object get(String name) {
        assert isInterned(name);
        int pos = find(name);
        return pos == -1 ? null : values[pos];
    }

    public void clear() {
        names = EMPTY_STRING_ARRAY;
        values = EMPTY_OBJECT_ARRAY;
        size = 0;
    }

    public RAttributes copy() {
        return new RAttributes(this);
    }

    /**
     * An iterator for the attributes, specified in terms of {@code Map.Entry<String, Object> } to
     * avoid copying in the normal case.
     */
    @Override
    public Iterator<RAttribute> iterator() {
        return new Iter();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        StringBuffer sb = new StringBuffer().append('{');
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(names[i]).append('=').append(values[i]);
        }
        sb.append('}');
        return sb.toString();
    }

    private class Iter implements Iterator<RAttribute> {
        int index;

        Iter() {
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public RAttribute next() {
            return new AttrInstance(names[index], values[index++]);
        }
    }

    @TruffleBoundary
    private static NoSuchElementException noSuchElement() {
        throw new NoSuchElementException();
    }

    // Performance analysis

    @CompilationFinal private static PerfHandler stats;

    static {
        RPerfStats.register(new PerfHandler());
    }

    /**
     * Collects data on the maximum size of the attribute set. So only interested in adding not
     * removing attributes.
     */
    private static class PerfHandler implements RPerfStats.Handler {
        private static final RPerfStats.Histogram hist = new RPerfStats.Histogram(5);

        @TruffleBoundary
        void init() {
            hist.inc(0);
        }

        @TruffleBoundary
        void update(RAttributes attr) {
            // incremented size by 1
            int s = attr.size();
            int effectivePrevSize = hist.effectiveBucket(s - 1);
            int effectiveSizeNow = hist.effectiveBucket(s);
            hist.dec(effectivePrevSize);
            hist.inc(effectiveSizeNow);
        }

        @Override
        public void initialize(String optionText) {
            stats = this;
        }

        @Override
        public String getName() {
            return "attributes";

        }

        @Override
        public void report() {
            RPerfStats.out().printf("RAttributes: %d, max size %d%n", hist.getTotalCount(), hist.getMaxSize());
            hist.report();
        }
    }

    public void setSize(int size) {
        this.size = size;
    }
}
