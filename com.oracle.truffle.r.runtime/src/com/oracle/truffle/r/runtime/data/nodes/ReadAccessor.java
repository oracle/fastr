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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Convenience classes that wraps {@link VectorReadAccess} node, the vector and its store so that
 * those data can be passed around as a single paramter.
 */
public abstract class ReadAccessor {
    private ReadAccessor() {
    }

    public static final class Int extends ReadAccessor {
        private final RAbstractIntVector vector;
        private final VectorReadAccess.Int readAccess;
        private final Object store;

        public Int(RAbstractIntVector vector, VectorReadAccess.Int readAccess) {
            this.vector = vector;
            this.readAccess = readAccess;
            store = readAccess.getDataStore(vector);
        }

        public int getDataAt(int index) {
            return readAccess.getDataAt(vector, store, index);
        }

        public Object getStore() {
            return store;
        }

        public RAbstractIntVector getVector() {
            return vector;
        }
    }

    public static final class Double extends ReadAccessor {
        private final RAbstractDoubleVector vector;
        private final VectorReadAccess.Double readAccess;
        private final Object store;

        public Double(RAbstractDoubleVector vector, VectorReadAccess.Double readAccess) {
            this.vector = vector;
            this.readAccess = readAccess;
            store = readAccess.getDataStore(vector);
        }

        public double getDataAt(int index) {
            return readAccess.getDataAt(vector, store, index);
        }

        public Object getStore() {
            return store;
        }

        public RAbstractDoubleVector getVector() {
            return vector;
        }
    }

    public static final class Logical extends ReadAccessor {
        private final RAbstractLogicalVector vector;
        private final VectorReadAccess.Logical readAccess;
        private final Object store;

        public Logical(RAbstractLogicalVector vector, VectorReadAccess.Logical readAccess) {
            this.vector = vector;
            this.readAccess = readAccess;
            store = readAccess.getDataStore(vector);
        }

        public byte getDataAt(int index) {
            return readAccess.getDataAt(vector, store, index);
        }

        public Object getStore() {
            return store;
        }

        public RAbstractLogicalVector getVector() {
            return vector;
        }
    }

    public static final class Raw extends ReadAccessor {
        private final RAbstractRawVector vector;
        private final VectorReadAccess.Raw readAccess;
        private final Object store;

        public Raw(RAbstractRawVector vector, VectorReadAccess.Raw readAccess) {
            this.vector = vector;
            this.readAccess = readAccess;
            store = readAccess.getDataStore(vector);
        }

        public byte getDataAt(int index) {
            return readAccess.getDataAt(vector, store, index);
        }

        public Object getStore() {
            return store;
        }

        public RAbstractRawVector getVector() {
            return vector;
        }
    }

    public static final class Complex extends ReadAccessor {
        private final RAbstractComplexVector vector;
        private final VectorReadAccess.Complex readAccess;
        private final Object store;

        public Complex(RAbstractComplexVector vector, VectorReadAccess.Complex readAccess) {
            this.vector = vector;
            this.readAccess = readAccess;
            store = readAccess.getDataStore(vector);
        }

        public RComplex getDataAt(int index) {
            return readAccess.getDataAt(vector, store, index);
        }

        public Object getStore() {
            return store;
        }

        public RAbstractComplexVector getVector() {
            return vector;
        }
    }

    public static final class String extends ReadAccessor {
        private final RAbstractStringVector vector;
        private final VectorReadAccess.String readAccess;
        private final Object store;

        public String(RAbstractStringVector vector, VectorReadAccess.String readAccess) {
            this.vector = vector;
            this.readAccess = readAccess;
            store = readAccess.getDataStore(vector);
        }

        public java.lang.String getDataAt(int index) {
            return readAccess.getDataAt(vector, store, index);
        }

        public Object getStore() {
            return store;
        }

        public RAbstractStringVector getVector() {
            return vector;
        }
    }
}
