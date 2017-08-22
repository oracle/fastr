/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.nodes.attributes.CopyOfRegAttributesNodeGen;
import com.oracle.truffle.r.nodes.attributes.InitAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ReuseNonSharedNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "t.default", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
public abstract class Transpose extends RBuiltinNode.Arg1 {

    private final BranchProfile hasDimNamesProfile = BranchProfile.create();
    private final ConditionProfile isMatrixProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile isNonSharedProfile = BranchProfile.create();

    private final VectorLengthProfile lengthProfile = VectorLengthProfile.create();
    private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();

    @Child private CopyOfRegAttributesNode copyRegAttributes = CopyOfRegAttributesNodeGen.create();
    @Child private InitAttributesNode initAttributes = InitAttributesNode.create();
    @Child private SetFixedAttributeNode putDimensions = SetFixedAttributeNode.createDim();
    @Child private SetFixedAttributeNode putDimNames = SetFixedAttributeNode.createDimNames();
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();
    @Child private GetNamesAttributeNode getAxisNamesNode = GetNamesAttributeNode.create();
    @Child private GetDimAttributeNode getDimNode;
    @Child private ReuseNonSharedNode reuseNonShared = ReuseNonSharedNode.create();

    static {
        Casts.noCasts(Transpose.class);
    }

    public abstract Object execute(RAbstractVector o);

    @FunctionalInterface
    private interface WriteArray<T extends RAbstractVector, A> {
        void apply(A array, T vector, int i, int j);
    }

    @FunctionalInterface
    private interface Swap {
        /** Swap element at (i, j) with element at (j, i). */
        void swap(int i, int j);
    }

    protected <T extends RAbstractVector, A> RVector<?> transposeInternal(T vector, Function<Integer, A> createArray, WriteArray<T, A> writeArray, BiFunction<A, Boolean, RVector<?>> createResult) {
        int length = lengthProfile.profile(vector.getLength());
        int firstDim;
        int secondDim;
        assert vector.isMatrix();
        int[] dims = getDimensions(vector);
        firstDim = dims[0];
        secondDim = dims[1];
        RBaseNode.reportWork(this, length);

        A array = createArray.apply(length);
        int j = 0;
        loopProfile.profileCounted(length);
        for (int i = 0; loopProfile.inject(i < length); i++, j += firstDim) {
            if (j > (length - 1)) {
                j -= (length - 1);
            }
            writeArray.apply(array, vector, i, j);
        }
        RVector<?> r = createResult.apply(array, vector.isComplete());
        // copy attributes
        copyRegAttributes.execute(vector, r);
        // set new dimensions
        int[] newDim = new int[]{secondDim, firstDim};
        putNewDimensions(vector, r, newDim);
        return r;
    }

    protected RVector<?> transposeSquareMatrixInPlace(RVector<?> vector, Swap swapper) {
        int length = lengthProfile.profile(vector.getLength());
        assert vector.isMatrix();
        int[] dims = getDimensions(vector);
        assert dims.length == 2;
        assert dims[0] == dims[1];
        int dim = dims[0];
        RBaseNode.reportWork(this, length);

        loopProfile.profileCounted(length);
        for (int i = 0; loopProfile.inject(i < dim); i++) {
            for (int j = 0; j < i; j++) {
                swapper.swap(i * dim + j, j * dim + i);
            }
        }
        // don't need to set new dimensions; it is a square matrix
        putNewDimNames(vector, vector);
        return vector;
    }

    protected RDoubleVector transposeInPlace(RDoubleVector v) {
        int[] dimensions = getDimensions(v);
        int w = dimensions[0];
        int h = dimensions[1];

        for (int start = 0; start <= w * h - 1; ++start) {
            int next = start;
            int i = 0;
            do {
                ++i;
                next = (next % h) * w + next / h;
            } while (next > start);

            if (next >= start && i != 1) {
                double tmp = v.getDataAt(start);
                next = start;
                do {
                    i = (next % h) * w + next / h;
                    v.setElement(next, (i == start) ? tmp : v.getDataAt(i));
                    next = i;
                } while (next > start);
            }
        }
        int[] newDim = new int[]{h, w};
        putNewDimensions(v, v, newDim);
        return v;
    }

    private int[] getDimensions(RAbstractVector vector) {
        assert vector.isMatrix();
        if (getDimNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getDimNode = insert(GetDimAttributeNode.create());
        }
        return getDimNode.getDimensions(vector);
    }

    protected boolean isSquare(RAbstractVector vector) {
        if (vector.isMatrix()) {
            int[] dims = getDimensions(vector);
            assert dims.length >= 2;
            return dims[0] == dims[1];
        }
        return false;
    }

    @Specialization(guards = "isSquare(x)")
    protected RVector<?> transposeSquare(RAbstractIntVector x) {
        RIntVector execute = (RIntVector) reuseNonShared.execute(x).materialize();
        return transposeSquareMatrixInPlace(execute, (i, j) -> {
            int tmp = execute.getDataAt(i);
            execute.setElement(i, execute.getDataAt(j));
            execute.setElement(j, tmp);
        });
    }

    @Specialization(guards = "isSquare(x)")
    protected RVector<?> transposeSquare(RAbstractLogicalVector x) {
        RLogicalVector execute = (RLogicalVector) reuseNonShared.execute(x).materialize();
        return transposeSquareMatrixInPlace(execute, (i, j) -> {
            byte tmp = execute.getDataAt(i);
            execute.setElement(i, execute.getDataAt(j));
            execute.setElement(j, tmp);
        });
    }

    @Specialization(guards = "isSquare(x)")
    protected RVector<?> transposeSquare(RAbstractDoubleVector x) {
        RDoubleVector execute = (RDoubleVector) reuseNonShared.execute(x).materialize();
        return transposeSquareMatrixInPlace(execute, (i, j) -> {
            double tmp = execute.getDataAt(i);
            execute.setElement(i, execute.getDataAt(j));
            execute.setElement(j, tmp);
        });
    }

    @Specialization(guards = "isSquare(x)")
    protected RVector<?> transposeSquare(RAbstractComplexVector x) {
        RComplexVector execute = (RComplexVector) reuseNonShared.execute(x).materialize();
        return transposeSquareMatrixInPlace(execute, (i, j) -> {
            RComplex tmp = execute.getDataAt(i);
            execute.setElement(i, execute.getDataAt(j));
            execute.setElement(j, tmp);
        });
    }

    @Specialization(guards = "isSquare(x)")
    protected RVector<?> transposeSquare(RAbstractStringVector x) {
        RStringVector execute = (RStringVector) reuseNonShared.execute(x).materialize();
        return transposeSquareMatrixInPlace(execute, (i, j) -> {
            String tmp = execute.getDataAt(i);
            execute.setElement(i, execute.getDataAt(j));
            execute.setElement(j, tmp);
        });
    }

    @Specialization(guards = "isSquare(x)")
    protected RVector<?> transposeSquare(RAbstractListVector x) {
        RList execute = (RList) reuseNonShared.execute(x).materialize();
        return transposeSquareMatrixInPlace(execute, (i, j) -> {
            Object tmp = execute.getDataAt(i);
            execute.setElement(i, execute.getDataAt(j));
            execute.setElement(j, tmp);
        });
    }

    @Specialization(guards = "isSquare(x)")
    protected RVector<?> transposeSquare(RAbstractRawVector x) {
        RRawVector execute = (RRawVector) reuseNonShared.execute(x).materialize();
        return transposeSquareMatrixInPlace(execute, (i, j) -> {
            RRaw tmp = execute.getDataAt(i);
            execute.setElement(i, execute.getDataAt(j));
            execute.setElement(j, tmp);
        });
    }

    @Specialization(guards = "x.isMatrix()")
    protected RVector<?> transpose(RAbstractIntVector x) {
        return transposeInternal(x, l -> new int[l], (a, v, i, j) -> a[i] = v.getDataAt(j), RDataFactory::createIntVector);
    }

    @Specialization(guards = "x.isMatrix()")
    protected RVector<?> transpose(RAbstractLogicalVector x) {
        return transposeInternal(x, l -> new byte[l], (a, v, i, j) -> a[i] = v.getDataAt(j), RDataFactory::createLogicalVector);
    }

    @Specialization(guards = "x.isMatrix()")
    protected RVector<?> transpose(RAbstractDoubleVector x) {
// RDoubleVector v = (RDoubleVector) reuseNonShared.execute(x).materialize();
// return transposeInPlace(v);
        return transposeInternal(x, l -> new double[l], (a, v, i, j) -> a[i] = v.getDataAt(j),
                        RDataFactory::createDoubleVector);
    }

    @Specialization(guards = "x.isMatrix()")
    protected RVector<?> transpose(RAbstractComplexVector x) {
        return transposeInternal(x, l -> new double[l * 2], (a, v, i, j) -> {
            RComplex d = v.getDataAt(j);
            a[i * 2] = d.getRealPart();
            a[i * 2 + 1] = d.getImaginaryPart();
        }, RDataFactory::createComplexVector);
    }

    @Specialization(guards = "x.isMatrix()")
    protected RVector<?> transpose(RAbstractStringVector x) {
        return transposeInternal(x, l -> new String[l], (a, v, i, j) -> a[i] = v.getDataAt(j), RDataFactory::createStringVector);
    }

    @Specialization(guards = "x.isMatrix()")
    protected RVector<?> transpose(RAbstractListVector x) {
        return transposeInternal(x, l -> new Object[l], (a, v, i, j) -> a[i] = v.getDataAt(j), (a, c) -> RDataFactory.createList(a));
    }

    @Specialization(guards = "x.isMatrix()")
    protected RVector<?> transpose(RAbstractRawVector x) {
        return transposeInternal(x, l -> new byte[l], (a, v, i, j) -> a[i] = v.getRawDataAt(j), (a, c) -> RDataFactory.createRawVector(a));
    }

    @Specialization(guards = "!x.isMatrix()")
    protected RVector<?> transpose(RAbstractVector x) {
        RVector<?> reused = reuseNonShared.execute(x);
        putNewDimensions(reused, reused, new int[]{1, x.getLength()});
        return reused;

    }

    private void putNewDimensions(RAbstractVector source, RVector<?> dest, int[] newDim) {
        putDimensions.execute(initAttributes.execute(dest), RDataFactory.createIntVector(newDim, RDataFactory.COMPLETE_VECTOR));
        putNewDimNames(source, dest);
    }

    private void putNewDimNames(RAbstractVector source, RVector<?> dest) {
        // set new dim names
        RList dimNames = getDimNamesNode.getDimNames(source);
        if (dimNames != null) {
            hasDimNamesProfile.enter();
            assert dimNames.getLength() == 2;
            RStringVector axisNames = getAxisNamesNode.getNames(dimNames);
            RStringVector transAxisNames = axisNames == null ? null : RDataFactory.createStringVector(new String[]{axisNames.getDataAt(1), axisNames.getDataAt(0)}, true);
            RList newDimNames = RDataFactory.createList(new Object[]{dimNames.getDataAt(1),
                            dimNames.getDataAt(0)}, transAxisNames);
            putDimNames.execute(dest.getAttributes(), newDimNames);
        }
    }

    @Fallback
    protected RVector<?> transpose(@SuppressWarnings("unused") Object x) {
        throw error(Message.ARGUMENT_NOT_MATRIX);
    }
}
