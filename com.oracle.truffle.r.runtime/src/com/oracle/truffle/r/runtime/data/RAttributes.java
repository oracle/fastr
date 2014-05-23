/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.r.runtime.*;

/**
 * Provides the generic mechanism for associating attributes with a R object. It does no special
 * analysis of the "name" of the attribute; that is left to other classes, e.g. {@link RVector}.
 *
 * Experimentally, the typical size of an attribute set is 1, so the default implementation is
 * optimized for that case and encodes the singleton attribute directly in the fields
 * {@link RAttributesImpl#name1}and {@link RAttributesImpl#value1}. A {@code null} value for
 * {@code value1} indicates no attributes set. If the number of attributes exceeds 1, {@code value1}
 * is an {@link ArrayList} that stores instances of {@link AttrInstance}.
 */
public abstract class RAttributes implements Iterable<RAttributes.RAttribute> {

    public interface RAttribute {
        String getName();

        Object getValue();
    }

    private static class AttrInstance implements RAttribute {
        private String name;
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

    public abstract void put(String name, Object value);

    public abstract Object get(String name);

    public abstract RAttributes copy();

    public abstract int size();

    public abstract boolean isEmpty();

    public abstract void clear();

    public abstract void remove(String name);

    public abstract Iterator<RAttribute> iterator();

    public static RAttributes create() {
        RAttributes result = stats ? new RAttributesStatsImpl() : new RAttributesImpl();
        return result;
    }

    /**
     * The implementation class which is separate to avoid a circularity that would result from the
     * {@code Iterable} in the abstract class.
     */
    private static class RAttributesImpl extends RAttributes implements RAttribute {

        RAttributesImpl() {
        }

        private RAttributesImpl(RAttributesImpl attrs) {
            if (attrs.value1 != null) {
                if (attrs.value1 instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<AttrInstance> list = (ArrayList<AttrInstance>) attrs.value1;
                    ArrayList<AttrInstance> newList = new ArrayList<>(list.size());
                    // important to create new AttrInstance objects, but the
                    // underlying data does not need to be deep copied
                    for (int i = 0; i < list.size(); i++) {
                        AttrInstance a = list.get(i);
                        newList.add(new AttrInstance(a.name, a.value));
                    }
                    this.value1 = newList;
                } else {
                    this.name1 = attrs.name1;
                    this.value1 = attrs.value1;
                }
            }
        }

        private static AttrInstance find(ArrayList<AttrInstance> list, String name) {
            for (int i = 0; i < list.size(); i++) {
                AttrInstance a = list.get(i);
                if (a.name.equals(name)) {
                    return a;
                }
            }
            return null;
        }

        @Override
        public void put(String name, Object value) {
            if (value1 == null) {
                name1 = name;
                value1 = value;
            } else if (value1 instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<AttrInstance> list = (ArrayList<AttrInstance>) value1;
                AttrInstance attr = find(list, name);
                if (attr != null) {
                    attr.value = value;
                    return;
                }
                list.add(new AttrInstance(name, value));
            } else {
                if (name1.equals(name)) {
                    value1 = value;
                } else {
                    ArrayList<AttrInstance> list = new ArrayList<>(2);
                    list.add(new AttrInstance(name1, value1));
                    list.add(new AttrInstance(name, value));
                    value1 = list;
                }
            }
        }

        /**
         * For 1 attribute, the {@code name}.
         */
        private String name1;
        /**
         * For 1 attribute the {@code value}, else an instance of {@code ArrayList<AttrInstance>}. A
         * value of {@code null} means no attributes.
         */
        private Object value1;

        @Override
        @SuppressWarnings("unchecked")
        public int size() {
            if (value1 == null) {
                return 0;
            } else if (value1 instanceof ArrayList) {
                return ((ArrayList<AttrInstance>) value1).size();
            } else {
                return 1;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isEmpty() {
            if (value1 == null) {
                return true;
            } else if (value1 instanceof ArrayList) {
                return ((ArrayList<AttrInstance>) value1).isEmpty();
            } else {
                return false;
            }
        }

        @Override
        public void remove(String name) {
            if (value1 != null) {
                if (value1 instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<AttrInstance> list = (ArrayList<AttrInstance>) value1;
                    for (int i = 0; i < list.size(); i++) {
                        AttrInstance a = list.get(i);
                        if (a.name.equals(name)) {
                            list.remove(i);
                            return;
                        }
                    }
                } else {
                    if (name1.equals(name)) {
                        value1 = null;
                    }
                }
            }
        }

        @Override
        public Object get(String name) {
            if (value1 != null) {
                if (value1 instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<AttrInstance> list = (ArrayList<AttrInstance>) value1;
                    AttrInstance attr = find(list, name);
                    if (attr != null) {
                        return attr.value;
                    }
                } else {
                    if (name1.equals(name)) {
                        return value1;
                    }
                }
            }
            return null;
        }

        @Override
        public void clear() {
            value1 = null;
        }

        @Override
        public RAttributes copy() {
            return new RAttributesImpl(this);
        }

        /**
         * An iterator for the attributes, specified in terms of {@code Map.Entry<String, Object> }
         * to avoid copying in the normal case.
         */
        @Override
        public Iterator<RAttribute> iterator() {
            return new Iter();
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer().append('{');
            if (value1 != null) {
                if (value1 instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<AttrInstance> list = (ArrayList<AttrInstance>) value1;
                    for (int i = 0; i < list.size(); i++) {
                        AttrInstance a = list.get(i);
                        if (i != 0) {
                            sb.append(", ");
                        }
                        sb.append(a.toString());
                    }
                } else {
                    sb.append(name1);
                    sb.append(": ");
                    sb.append(value1.toString());
                }
            }
            sb.append('}');
            return sb.toString();
        }

        private class Iter implements Iterator<RAttribute> {
            ListIterator<AttrInstance> iter;
            boolean readSingleton;

            Iter() {
                if (value1 == null) {
                    readSingleton = true;
                } else if (value1 instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<AttrInstance> list = (ArrayList<AttrInstance>) value1;
                    iter = list.listIterator();
                } else {
                    // singleton to read
                }

            }

            @Override
            public boolean hasNext() {
                if (iter != null) {
                    return iter.hasNext();
                } else {
                    return !readSingleton;
                }
            }

            @Override
            public RAttribute next() {
                if (iter != null) {
                    return iter.next();
                } else {
                    if (readSingleton) {
                        throw new NoSuchElementException();
                    } else {
                        readSingleton = true;
                        return RAttributesImpl.this;
                    }
                }
            }

        }

        public String getName() {
            return name1;
        }

        public Object getValue() {
            return value1;
        }
    }

    // Performance analysis

    /**
     * Only used when gathering performance statistics.
     */
    private static class RAttributesStatsImpl extends RAttributesImpl {
        private static ArrayList<RAttributesStatsImpl> instances = new ArrayList<>();
        private static final int OVERFLOW = 3;
        private static int[] hist = new int[OVERFLOW + 1];

        private int maxSize;

        RAttributesStatsImpl() {
            super();
            instances.add(this);
        }

        @Override
        public void put(String name, Object value) {
            super.put(name, value);
            int s = size();
            if (s > maxSize) {
                maxSize = s;
            }
        }

        static void report() {
            // Checkstyle: stop system print check
            int globalMaxSize = 0;
            for (RAttributesStatsImpl instance : instances) {
                if (instance.maxSize > globalMaxSize) {
                    globalMaxSize = instance.maxSize;
                }
                if (instance.maxSize > OVERFLOW) {
                    hist[OVERFLOW]++;
                } else {
                    hist[instance.maxSize]++;
                }
            }
            System.out.println("RAttributes statistics");
            System.out.printf("size 0: %d, 1: %d, 2: %d, > 2: %d, max %d%n", hist[0], hist[1], hist[2], hist[3], globalMaxSize);

        }
    }

    private static boolean stats;

    public static class PerfHandler implements RPerfAnalysis.Handler {
        public void initialize() {
            stats = true;
        }

        public String getName() {
            return "attributes";

        }

        public void report() {
            RAttributesStatsImpl.report();
        }

    }

}
