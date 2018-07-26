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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromDoubleAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromDoubleAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.AccessIterator;

class RToDoubleVectorClosure extends RToVectorClosure implements RAbstractDoubleVector {

    protected RToDoubleVectorClosure(RAbstractVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public final RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createDoubleVector(new double[newLength], newIsComplete);
    }

    @Override
    public final RDoubleVector materialize() {
        int length = getLength();
        double[] result = new double[length];
        for (int i = 0; i < result.length; i++) {
            double data = getDataAt(i);
            result[i] = data;
        }
        RDoubleVector materialized = RDataFactory.createDoubleVector(result, getVector().isComplete());
        copyAttributes(materialized);
        return materialized;
    }

    @TruffleBoundary
    private void copyAttributes(RDoubleVector materialized) {
        if (keepAttributes) {
            materialized.initAttributes(getVector().getAttributes());
        }
    }

    @Override
    public double getDataAt(int index) {
        RAbstractVector v = getVector();
        VectorAccess spa = v.slowPathAccess();
        return spa.getDouble(spa.randomAccess(v), index);
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this, getVector().access());
    }

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    private static final SlowPathFromDoubleAccess SLOW_PATH_ACCESS = new SlowPathFromDoubleAccess() {
        @Override
        protected double getDoubleImpl(AccessIterator accessIter, int index) {
            RToDoubleVectorClosure vector = (RToDoubleVectorClosure) accessIter.getStore();
            return vector.getDataAt(index);
        }

        @Override
        protected void setDoubleImpl(AccessIterator accessIter, int index, double value) {
            RToDoubleVectorClosure vector = (RToDoubleVectorClosure) accessIter.getStore();
            vector.setDataAt(vector.getInternalStore(), index, value);
        }
    };

    private static final class FastPathAccess extends FastPathFromDoubleAccess {

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
            return super.getStore(((RToDoubleVectorClosure) vector).getVector());
        }

        @Override
        public double getDouble(RandomIterator iter, int index) {
            return delegate.getDouble(iter, index);
        }

        @Override
        public double getDouble(SequentialIterator iter) {
            return delegate.getDouble(iter);
        }

        @Override
        protected double getDoubleImpl(AccessIterator accessIterator, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected void setDoubleImpl(AccessIterator accessIterator, int index, double value) {
            throw RInternalError.shouldNotReachHere();
        }
    }
}
