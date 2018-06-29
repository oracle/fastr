/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromIntAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

class RToIntVectorClosure extends RToVectorClosure implements RAbstractIntVector {

    protected RToIntVectorClosure(RAbstractVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public final RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createIntVector(new int[newLength], newIsComplete);
    }

    @Override
    public final RIntVector materialize() {
        int length = getLength();
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            int data = getDataAt(i);
            result[i] = data;
        }
        RIntVector materialized = RDataFactory.createIntVector(result, getVector().isComplete());
        copyAttributes(materialized);
        return materialized;
    }

    @TruffleBoundary
    private void copyAttributes(RIntVector materialized) {
        if (keepAttributes) {
            materialized.copyAttributesFrom(getVector());
        }
    }

    @Override
    public int getDataAt(int index) {
        RAbstractVector v = getVector();
        VectorAccess spa = v.slowPathAccess();
        return spa.getInt(spa.randomAccess(v), index);
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this, getVector().access());
    }

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    private static final SlowPathFromIntAccess SLOW_PATH_ACCESS = new SlowPathFromIntAccess() {
        @Override
        protected int getInt(Object store, int index) {
            RToIntVectorClosure vector = (RToIntVectorClosure) store;
            return vector.getDataAt(index);
        }

        @Override
        protected void setInt(Object store, int index, int value) {
            RToIntVectorClosure vector = (RToIntVectorClosure) store;
            vector.setDataAt(vector.getInternalStore(), index, value);
        }
    };

    private static final class FastPathAccess extends FastPathFromIntAccess {

        @Child private VectorAccess delegate;

        FastPathAccess(RAbstractContainer value, VectorAccess delegate) {
            super(value);
            this.delegate = delegate;
        }

        @Override
        public boolean supports(Object value) {
            return delegate.supports(value);
        }

        @Override
        protected Object getStore(RAbstractContainer vector) {
            return super.getStore(((RToIntVectorClosure) vector).getVector());
        }

        @Override
        public int getInt(RandomIterator iter, int index) {
            return delegate.getInt(iter, index);
        }

        @Override
        public int getInt(SequentialIterator iter) {
            return delegate.getInt(iter);
        }

        @Override
        protected int getInt(Object store, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected void setInt(Object store, int index, int value) {
            throw RInternalError.shouldNotReachHere();
        }
    }
}
