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
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.utilities.CyclicAssumption;

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

    public static RAttributes create() {
        return new RAttributes();
    }

    public static RAttributes createInitialized(String[] names, Object[] values) {
        return new RAttributes(names, values);
    }

    public static void copyAttributes(RAttributable obj, RAttributes attrs) {
        if (attrs == null) {
            return;
        }
        Iterator<RAttribute> iter = attrs.iterator();
        while (iter.hasNext()) {
            RAttribute attr = iter.next();
            obj.setAttr(attr.getName(), attr.getValue());
        }
    }

    /**
     * The implementation class which is separate to avoid a circularity that would result from the
     * {@code Iterable} in the abstract class.
     */

    RAttributes() {
        if (AttributeTracer.enabled) {
            AttributeTracer.reportAttributeChange(AttributeTracer.Change.CREATE, this, null);
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
        int spos = pos;
        if (pos == -1) {
            ensureFreeSpace();
            pos = size++;
            names[pos] = name;
        }
        // TODO: this assertion should hold in general
        // assert value == null || !(value instanceof RShareable) || !((RShareable)
        // value).isTemporary();
        values[pos] = value;
        if (AttributeTracer.enabled) {
            AttributeTracer.reportAttributeChange(spos == -1 ? AttributeTracer.Change.ADD : AttributeTracer.Change.UPDATE, this, name);
        }
    }

    public void ensureFreeSpace() {
        if (size == names.length) {
            names = Arrays.copyOf(names, (size + 1) * 2);
            values = Arrays.copyOf(values, (size + 1) * 2);
            assert names.length == values.length;
            if (AttributeTracer.enabled) {
                AttributeTracer.reportAttributeChange(AttributeTracer.Change.GROW, this, names.length);
            }
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
            if (AttributeTracer.enabled) {
                AttributeTracer.reportAttributeChange(AttributeTracer.Change.REMOVE, this, name);
            }
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

    public void setSize(int size) {
        this.size = size;
    }

    public static final class AttributeTracer {
        private static Deque<Listener> listeners = new ConcurrentLinkedDeque<>();
        @CompilationFinal private static boolean enabled;

        private static final CyclicAssumption noAttributeTracingAssumption = new CyclicAssumption("data copying");

        private AttributeTracer() {
            // only static methods
        }

        /**
         * Adds a listener of attribute events.
         */
        public static void addListener(Listener listener) {
            listeners.addLast(listener);
        }

        /**
         * After calling this method attribute related events will be reported to the listener. This
         * invalidates global assumption and should be used with caution.
         */
        public static void setTracingState(boolean newState) {
            if (enabled != newState) {
                noAttributeTracingAssumption.invalidate();
                enabled = newState;
            }
        }

        public enum Change {
            CREATE,
            ADD,
            UPDATE,
            REMOVE,
            GROW
        }

        public static void reportAttributeChange(Change change, RAttributes attrs, Object value) {
            if (enabled) {
                for (Listener listener : listeners) {
                    listener.reportAttributeChange(change, attrs, value);
                }
            }
        }

        public interface Listener {
            /**
             * Reports attribute events to the listener. If there are no traced objects, this should
             * turn into no-op. {@code valuer} depends on the value of {@code change}. For
             * {@code CREATE} it is {@code null}, for {@code ADD,UPDATE, REMOVE} it is the
             * {@code String} name of the attribute and for {@code GROW} it is the (new) size.
             */
            void reportAttributeChange(Change change, RAttributes attrs, Object value);
        }

    }

}
