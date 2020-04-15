/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.FastPathVectorAccess.FastPathFromComplexAccess;
import com.oracle.truffle.r.runtime.data.nodes.SlowPathVectorAccess.SlowPathFromComplexAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;

@ValueType
public final class RComplex extends RAbstractComplexVector implements RScalarVector {

    private final double realPart;
    private final double imaginaryPart;

    private RComplex(double realPart, double imaginaryPart) {
        super(!RRuntime.isComplexNA(realPart, imaginaryPart));
        this.realPart = realPart;
        this.imaginaryPart = imaginaryPart;
    }

    public static RComplex createNA() {
        return RComplex.valueOf(RRuntime.COMPLEX_NA_REAL_PART, RRuntime.COMPLEX_NA_IMAGINARY_PART);
    }

    public static RComplex valueOf(double real, double imaginary) {
        return new RComplex(real, imaginary);
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public RAbstractVector copy() {
        return this;
    }

    @Override
    public RComplexVector materialize() {
        RComplexVector result = RDataFactory.createComplexVector(new double[]{realPart, imaginaryPart}, isComplete());
        MemoryCopyTracer.reportCopying(this, result);
        return result;
    }

    @Override
    public double[] getDataCopy() {
        return new double[]{realPart, imaginaryPart};
    }

    @Override
    public RAbstractVector castSafe(RType type, ConditionProfile isNAProfile, boolean keepAttributes) {
        switch (type) {
            case Complex:
                return this;
            case Character:
                return RString.valueOf(RContext.getRRuntimeASTAccess().encodeComplex(this));
            case List:
                return RDataFactory.createListFromScalar(this);
            default:
                return null;
        }
    }

    @Override
    public RComplex getDataAt(int index) {
        assert index == 0;
        return this;
    }

    @Override
    public RType getRType() {
        return RType.Complex;
    }

    public double getRealPart() {
        return realPart;
    }

    public double getImaginaryPart() {
        return imaginaryPart;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return toString(Double.toString(realPart), Double.toString(imaginaryPart));
    }

    public String toString(String realPartString, String imaginaryPartString) {
        CompilerAsserts.neverPartOfCompilation();
        return isNA() ? "NA" : realPartString + (imaginaryPart < 0.0 ? "" : "+") + imaginaryPartString + "i";
    }

    @Override
    public boolean isNA() {
        return RRuntime.isNA(this);
    }

    public boolean isZero() {
        return realPart == 0.0 && imaginaryPart == 0.0;
    }

    public boolean isRealOne() {
        return realPart == 1.0 && imaginaryPart == 0.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RComplex)) {
            return false;
        }
        RComplex other = (RComplex) obj;
        return isNA() && other.isNA() || (((Double) realPart).equals(other.getRealPart())) && (((Double) imaginaryPart).equals(other.getImaginaryPart()));
    }

    @Override
    public int hashCode() {
        return Double.hashCode(realPart) ^ Double.hashCode(imaginaryPart);
    }

    public static double abs(double re, double im) {
        if (!RRuntime.isFinite(re) || !RRuntime.isFinite(im)) {
            if (Double.isInfinite(re) || Double.isInfinite(im)) {
                return Double.POSITIVE_INFINITY;
            } else if (Double.isNaN(im)) {
                return im;
            } else {
                return re;
            }
        } else {
            return Math.sqrt(re * re + im * im);
        }
    }

    private static final class FastPathAccess extends FastPathFromComplexAccess {

        FastPathAccess(RAbstractContainer value) {
            super(value);
        }

        @Override
        protected RComplex getComplexImpl(AccessIterator accessIter, int index) {
            assert index == 0;
            return (RComplex) accessIter.getStore();
        }

        @Override
        protected double getComplexRImpl(AccessIterator accessIter, int index) {
            assert index == 0;
            return ((RComplex) accessIter.getStore()).realPart;
        }

        @Override
        protected double getComplexIImpl(AccessIterator accessIter, int index) {
            assert index == 0;
            return ((RComplex) accessIter.getStore()).imaginaryPart;
        }
    }

    @Override
    public VectorAccess access() {
        return new FastPathAccess(this);
    }

    private static final SlowPathFromComplexAccess SLOW_PATH_ACCESS = new SlowPathFromComplexAccess() {
        @Override
        protected RComplex getComplexImpl(AccessIterator accessIter, int index) {
            assert index == 0;
            return (RComplex) accessIter.getStore();
        }

        @Override
        protected double getComplexRImpl(AccessIterator accessIter, int index) {
            assert index == 0;
            return ((RComplex) accessIter.getStore()).realPart;
        }

        @Override
        protected double getComplexIImpl(AccessIterator accessIter, int index) {
            assert index == 0;
            return ((RComplex) accessIter.getStore()).imaginaryPart;
        }
    };

    @Override
    public VectorAccess slowPathAccess() {
        return SLOW_PATH_ACCESS;
    }
}
