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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromComplexAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromComplexAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.AccessIterator;

class RToComplexVectorClosure extends RToVectorClosure implements RAbstractComplexVector {

    protected RToComplexVectorClosure(RAbstractVector vector, boolean keepAttributes) {
        super(vector, keepAttributes);
    }

    @Override
    public final RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createComplexVector(new double[newLength << 1], newIsComplete);
    }

    @Override
    public final RComplexVector materialize() {
        int length = getLength();
        double[] result = new double[length << 1];
        for (int i = 0; i < length; i++) {
            RComplex data = getDataAt(i);
            int index = i << 1;
            result[index] = data.getRealPart();
            result[index + 1] = data.getImaginaryPart();
        }
        RComplexVector materialized = RDataFactory.createComplexVector(result, getVector().isComplete());
        copyAttributes(materialized);
        return materialized;
    }

    @TruffleBoundary
    private void copyAttributes(RComplexVector materialized) {
        if (keepAttributes) {
            materialized.initAttributes(getVector().getAttributes());
        }
    }

    @Override
    public RComplex getDataAt(int index) {
        RAbstractVector v = getVector();
        VectorAccess spa = v.slowPathAccess();
        return spa.getComplex(spa.randomAccess(v), index);
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this, getVector().access());
    }

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }

    private static final SlowPathFromComplexAccess SLOW_PATH_ACCESS = new SlowPathFromComplexAccess() {

        @Override
        protected RComplex getComplexImpl(AccessIterator accessIter, int index) {
            RToComplexVectorClosure vector = (RToComplexVectorClosure) accessIter.getStore();
            return vector.getDataAt(index);
        }

        @Override
        protected double getComplexRImpl(AccessIterator accessIter, int index) {
            RToComplexVectorClosure vector = (RToComplexVectorClosure) accessIter.getStore();
            return vector.getDataAt(index).getRealPart();
        }

        @Override
        protected double getComplexIImpl(AccessIterator accessIter, int index) {
            RToComplexVectorClosure vector = (RToComplexVectorClosure) accessIter.getStore();
            return vector.getDataAt(index).getImaginaryPart();
        }

        @Override
        protected void setComplexImpl(AccessIterator accessIter, int index, double real, double imaginary) {
            RToComplexVectorClosure vector = (RToComplexVectorClosure) accessIter.getStore();
            vector.setDataAt(vector.getInternalStore(), index, RComplex.valueOf(real, imaginary));
        }
    };

    private static final class FastPathAccess extends FastPathFromComplexAccess {

        @Node.Child private VectorAccess delegate;

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
            return super.getStore(((RToComplexVectorClosure) vector).getVector());
        }

        @Override
        public RComplex getComplex(RandomIterator iter, int index) {
            return delegate.getComplex(iter, index);
        }

        @Override
        public double getComplexR(RandomIterator iter, int index) {
            return delegate.getComplexR(iter, index);
        }

        @Override
        public double getComplexI(RandomIterator iter, int index) {
            return delegate.getComplexI(iter, index);
        }

        @Override
        public RComplex getComplex(SequentialIterator iter) {
            return delegate.getComplex(iter);
        }

        @Override
        public double getComplexR(SequentialIterator iter) {
            return delegate.getComplexR(iter);
        }

        @Override
        public double getComplexI(SequentialIterator iter) {
            return delegate.getComplexI(iter);
        }

        @Override
        protected RComplex getComplexImpl(AccessIterator accessIterator, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected double getComplexRImpl(AccessIterator accessIterator, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected double getComplexIImpl(AccessIterator accessIterator, int index) {
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        protected void setComplexImpl(AccessIterator accessIterator, int index, double real, double imaginary) {
            throw RInternalError.shouldNotReachHere();
        }
    }
}
