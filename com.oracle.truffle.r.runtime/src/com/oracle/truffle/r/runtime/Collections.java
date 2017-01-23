/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.Arrays;

import com.oracle.truffle.r.runtime.data.RComplex;

public final class Collections {

    private Collections() {
        // private
    }

    // simple implementations of non-recursive hash-maps to enable compilation
    // TODO: consider replacing with a more efficient library implementation

    public static class NonRecursiveHashSet<KEY> {

        private Object[] keys;

        public NonRecursiveHashSet(int approxCapacity) {
            keys = new Object[Math.max(approxCapacity, 1)];
        }

        public boolean add(KEY key) {
            int ind = Math.abs(key.hashCode()) % keys.length;
            int firstInd = ind;
            while (true) {
                if (keys[ind] == null) {
                    keys[ind] = key;
                    return false;
                } else if (key.equals(keys[ind])) {
                    return true;
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd) {
                        // resize
                        Object[] newKeys = new Object[keys.length + keys.length / 2];
                        for (int i = 0; i < keys.length; i++) {
                            if (keys[i] != null) {
                                int tmpInd = Math.abs(keys[i].hashCode()) % newKeys.length;
                                while (true) {
                                    if (newKeys[tmpInd] == null) {
                                        newKeys[tmpInd] = keys[i];
                                        break;
                                    } else {
                                        assert !keys[i].equals(newKeys[tmpInd]);
                                        tmpInd = (tmpInd + 1) % newKeys.length;
                                    }
                                }
                            }
                        }

                        keys = newKeys;

                        // start hashing from the beginning
                        ind = Math.abs(key.hashCode()) % keys.length;
                    }
                }
            }
        }
    }

    public static class NonRecursiveHashSetDouble {

        private double[] keys;

        public NonRecursiveHashSetDouble(int approxCapacity) {
            keys = new double[Math.max(approxCapacity, 1)];
            Arrays.fill(keys, RRuntime.DOUBLE_NA);
        }

        public boolean add(double key) {
            int ind = Math.abs(Double.hashCode(key)) % keys.length;
            int firstInd = ind;
            while (true) {
                if (RRuntime.isNAorNaN(keys[ind])) {
                    keys[ind] = key;
                    return false;
                } else if (key == keys[ind]) {
                    return true;
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd) {
                        // resize
                        double[] newKeys = new double[keys.length + keys.length / 2];
                        Arrays.fill(newKeys, RRuntime.DOUBLE_NA);
                        for (int i = 0; i < keys.length; i++) {
                            if (!RRuntime.isNAorNaN(keys[i])) {
                                int tmpInd = Math.abs(Double.hashCode(keys[i])) % newKeys.length;
                                while (true) {
                                    if (RRuntime.isNAorNaN(newKeys[tmpInd])) {
                                        newKeys[tmpInd] = keys[i];
                                        break;
                                    } else {
                                        assert keys[i] != newKeys[tmpInd];
                                        tmpInd = (tmpInd + 1) % newKeys.length;
                                    }
                                }
                            }
                        }

                        keys = newKeys;

                        // start hashing from the beginning
                        ind = Math.abs(Double.hashCode(key)) % keys.length;
                    }
                }
            }
        }
    }

    public static class NonRecursiveHashSetInt {

        private int[] keys;

        public NonRecursiveHashSetInt(int approxCapacity) {
            keys = new int[Math.max(approxCapacity, 1)];
            Arrays.fill(keys, RRuntime.INT_NA);
        }

        public boolean add(int key) {
            int ind = Math.abs(Integer.hashCode(key)) % keys.length;
            int firstInd = ind;
            while (true) {
                if (RRuntime.isNA(keys[ind])) {
                    keys[ind] = key;
                    return false;
                } else if (key == keys[ind]) {
                    return true;
                } else {
                    ind = (ind + 1) % keys.length;
                    if (ind == firstInd) {
                        // resize
                        int[] newKeys = new int[keys.length + keys.length / 2];
                        Arrays.fill(newKeys, RRuntime.INT_NA);
                        for (int i = 0; i < keys.length; i++) {
                            if (!RRuntime.isNA(keys[i])) {
                                int tmpInd = Math.abs(Integer.hashCode(keys[i])) % newKeys.length;
                                while (true) {
                                    if (RRuntime.isNA(newKeys[tmpInd])) {
                                        newKeys[tmpInd] = keys[i];
                                        break;
                                    } else {
                                        assert keys[i] != newKeys[tmpInd];
                                        tmpInd = (tmpInd + 1) % newKeys.length;
                                    }
                                }
                            }
                        }

                        keys = newKeys;

                        // start hashing from the beginning
                        ind = Math.abs(Integer.hashCode(key)) % keys.length;
                    }
                }
            }
        }
    }

    private abstract static class NonRecursiveHashMap {

        protected final int[] values;
        protected int naValue;

        protected NonRecursiveHashMap(int entryCount) {
            int capacity = Math.max(entryCount * 3 / 2, 1);
            values = new int[Integer.highestOneBit(capacity) << 1];
        }

        protected int index(int hash) {
            // Multiply by -127
            return ((hash << 1) - (hash << 8)) & (values.length - 1);
        }
    }

    public static final class NonRecursiveHashMapCharacter extends NonRecursiveHashMap {

        private final String[] keys;

        public NonRecursiveHashMapCharacter(int approxCapacity) {
            super(approxCapacity);
            keys = new String[values.length];
        }

        public boolean put(String key, int value) {
            assert value >= 0;
            if (RRuntime.isNA(key)) {
                boolean ret = naValue == 0;
                naValue = value + 1;
                return ret;
            } else {
                int keyHash = key.hashCode();
                int ind = index(keyHash);
                while (true) {
                    if (values[ind] == 0) {
                        keys[ind] = key;
                        values[ind] = value + 1;
                        return false;
                    } else if (keys[ind].hashCode() == keyHash && key.equals(keys[ind])) {
                        values[ind] = value + 1;
                        return true;
                    } else {
                        ind++;
                        if (ind == values.length) {
                            ind = 0;
                        }
                    }
                }
            }
        }

        public int get(String key) {
            if (RRuntime.isNA(key)) {
                return naValue - 1;
            } else {
                int ind = index(key.hashCode());
                int firstInd = ind;
                while (true) {
                    if (key.equals(keys[ind])) {
                        return values[ind] - 1;
                    } else {
                        ind++;
                        if (ind == values.length) {
                            ind = 0;
                        }
                        if (ind == firstInd || values[ind] == 0) {
                            return -1;
                        }
                    }
                }
            }
        }
    }

    public static final class NonRecursiveHashMapComplex extends NonRecursiveHashMap {

        private final RComplex[] keys;

        public NonRecursiveHashMapComplex(int approxCapacity) {
            super(approxCapacity);
            keys = new RComplex[values.length];
        }

        public boolean put(RComplex key, int value) {
            assert value >= 0;
            if (RRuntime.isNA(key)) {
                boolean ret = naValue == 0;
                naValue = value + 1;
                return ret;
            } else {
                int ind = index(key.hashCode());
                while (true) {
                    if (values[ind] == 0) {
                        keys[ind] = key;
                        values[ind] = value + 1;
                        return false;
                    } else if (key.equals(keys[ind])) {
                        values[ind] = value + 1;
                        return true;
                    } else {
                        ind++;
                        if (ind == values.length) {
                            ind = 0;
                        }
                    }
                }
            }
        }

        public int get(RComplex key) {
            if (RRuntime.isNA(key)) {
                return naValue - 1;
            } else {
                int ind = index(key.hashCode());
                int firstInd = ind;
                while (true) {
                    if (key.equals(keys[ind])) {
                        return values[ind] - 1;
                    } else {
                        ind++;
                        if (ind == values.length) {
                            ind = 0;
                        }
                        if (ind == firstInd || values[ind] == 0) {
                            return -1;
                        }
                    }
                }
            }
        }
    }

    public static final class NonRecursiveHashMapDouble extends NonRecursiveHashMap {

        private final double[] keys;
        private int nanValue;

        public NonRecursiveHashMapDouble(int approxCapacity) {
            super(approxCapacity);
            keys = new double[values.length];
            Arrays.fill(keys, RRuntime.DOUBLE_NA);
        }

        public boolean put(double key, int value) {
            assert value >= 0;
            if (RRuntime.isNA(key)) {
                boolean ret = naValue == 0;
                naValue = value + 1;
                return ret;
            } else if (Double.isNaN(key)) {
                boolean ret = nanValue == 0;
                nanValue = value + 1;
                return ret;
            } else {
                int ind = index(Double.hashCode(key));
                while (true) {
                    if (values[ind] == 0) {
                        keys[ind] = key;
                        values[ind] = value + 1;
                        return false;
                    } else if (key == keys[ind]) {
                        values[ind] = value + 1;
                        return true;
                    } else {
                        ind++;
                        if (ind == values.length) {
                            ind = 0;
                        }
                    }
                }
            }
        }

        public int get(double key) {
            if (RRuntime.isNA(key)) {
                return naValue - 1;
            } else if (Double.isNaN(key)) {
                return nanValue - 1;
            } else {
                int ind = index(Double.hashCode(key));
                int firstInd = ind;
                while (true) {
                    if (key == keys[ind]) {
                        return values[ind] - 1;
                    } else {
                        ind++;
                        if (ind == values.length) {
                            ind = 0;
                        }
                        if (ind == firstInd || values[ind] == 0) {
                            return -1;
                        }
                    }
                }
            }
        }
    }

    public static final class NonRecursiveHashMapInt extends NonRecursiveHashMap {

        private final int[] keys;

        public NonRecursiveHashMapInt(int approxCapacity) {
            super(approxCapacity);
            keys = new int[values.length];
            Arrays.fill(keys, RRuntime.INT_NA);
        }

        public boolean put(int key, int value) {
            assert value >= 0;
            if (RRuntime.isNA(key)) {
                boolean ret = naValue == 0;
                naValue = value + 1;
                return ret;
            } else {
                int ind = index(Integer.hashCode(key));
                while (true) {
                    if (values[ind] == 0) {
                        keys[ind] = key;
                        values[ind] = value + 1;
                        return false;
                    } else if (key == keys[ind]) {
                        values[ind] = value + 1;
                        return true;
                    } else {
                        ind++;
                        if (ind == values.length) {
                            ind = 0;
                        }
                    }
                }
            }
        }

        public int get(int key) {
            if (RRuntime.isNA(key)) {
                return naValue - 1;
            } else {
                int ind = index(Integer.hashCode(key));
                int firstInd = ind;
                while (true) {
                    if (key == keys[ind]) {
                        return values[ind] - 1;
                    } else {
                        ind++;
                        if (ind == values.length) {
                            ind = 0;
                        }
                        if (ind == firstInd || values[ind] == 0) {
                            return -1;
                        }
                    }
                }
            }
        }
    }
}
