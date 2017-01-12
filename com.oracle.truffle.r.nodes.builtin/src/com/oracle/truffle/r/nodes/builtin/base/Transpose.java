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
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
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
public abstract class Transpose extends RBuiltinNode {

    private final BranchProfile hasDimNamesProfile = BranchProfile.create();
    private final ConditionProfile isMatrixProfile = ConditionProfile.createBinaryProfile();

    private final VectorLengthProfile lengthProfile = VectorLengthProfile.create();
    private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();

    @Child private CopyOfRegAttributesNode copyRegAttributes = CopyOfRegAttributesNodeGen.create();
    @Child private InitAttributesNode initAttributes = InitAttributesNode.create();
    @Child private SetFixedAttributeNode putDimensions = SetFixedAttributeNode.createDim();
    @Child private SetFixedAttributeNode putDimNames = SetFixedAttributeNode.createDimNames();
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();
    @Child private GetDimAttributeNode getDimNode;

    public abstract Object execute(RAbstractVector o);

    @FunctionalInterface
    private interface WriteArray<T extends RAbstractVector, A> {
        void apply(A array, T vector, int i, int j);
    }

    protected <T extends RAbstractVector, A> RVector<?> transposeInternal(T vector, Function<Integer, A> createArray, WriteArray<T, A> writeArray, BiFunction<A, Boolean, RVector<?>> createResult) {
        int length = lengthProfile.profile(vector.getLength());
        int firstDim;
        int secondDim;
        if (isMatrixProfile.profile(vector.isMatrix())) {
            if (getDimNode == null) {
                CompilerDirectives.transferToInterpreter();
                getDimNode = insert(GetDimAttributeNode.create());
            }
            int[] dims = getDimNode.getDimensions(vector);
            firstDim = dims[0];
            secondDim = dims[1];
        } else {
            firstDim = length;
            secondDim = 1;
        }
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
        putDimensions.execute(initAttributes.execute(r), RDataFactory.createIntVector(newDim, RDataFactory.COMPLETE_VECTOR));
        // set new dim names
        RList dimNames = getDimNamesNode.getDimNames(vector);
        if (dimNames != null) {
            hasDimNamesProfile.enter();
            assert dimNames.getLength() == 2;
            RList newDimNames = RDataFactory.createList(new Object[]{dimNames.getDataAt(1), dimNames.getDataAt(0)});
            putDimNames.execute(r.getAttributes(), newDimNames);
        }
        return r;
    }

    @Specialization
    protected RVector<?> transpose(RAbstractIntVector x) {
        return transposeInternal(x, l -> new int[l], (a, v, i, j) -> a[i] = v.getDataAt(j), RDataFactory::createIntVector);
    }

    @Specialization
    protected RVector<?> transpose(RAbstractLogicalVector x) {
        return transposeInternal(x, l -> new byte[l], (a, v, i, j) -> a[i] = v.getDataAt(j), RDataFactory::createLogicalVector);
    }

    @Specialization
    protected RVector<?> transpose(RAbstractDoubleVector x) {
        return transposeInternal(x, l -> new double[l], (a, v, i, j) -> a[i] = v.getDataAt(j), RDataFactory::createDoubleVector);
    }

    @Specialization
    protected RVector<?> transpose(RAbstractComplexVector x) {
        return transposeInternal(x, l -> new double[l * 2], (a, v, i, j) -> {
            RComplex d = v.getDataAt(j);
            a[i * 2] = d.getRealPart();
            a[i * 2 + 1] = d.getImaginaryPart();
        }, RDataFactory::createComplexVector);
    }

    @Specialization
    protected RVector<?> transpose(RAbstractStringVector x) {
        return transposeInternal(x, l -> new String[l], (a, v, i, j) -> a[i] = v.getDataAt(j), RDataFactory::createStringVector);
    }

    @Specialization
    protected RVector<?> transpose(RAbstractListVector x) {
        return transposeInternal(x, l -> new Object[l], (a, v, i, j) -> a[i] = v.getDataAt(j), (a, c) -> RDataFactory.createList(a));
    }

    @Specialization
    protected RVector<?> transpose(RAbstractRawVector x) {
        return transposeInternal(x, l -> new byte[l], (a, v, i, j) -> a[i] = v.getRawDataAt(j), (a, c) -> RDataFactory.createRawVector(a));
    }

    @Fallback
    protected RVector<?> transpose(@SuppressWarnings("unused") Object x) {
        throw RError.error(RError.SHOW_CALLER, Message.ARGUMENT_NOT_MATRIX);
    }
}
