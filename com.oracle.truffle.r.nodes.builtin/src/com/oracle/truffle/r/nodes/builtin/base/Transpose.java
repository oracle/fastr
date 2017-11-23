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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "t.default", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
public abstract class Transpose extends RBuiltinNode.Arg1 {

    private final BranchProfile hasDimNamesProfile = BranchProfile.create();

    private final VectorLengthProfile lengthProfile = VectorLengthProfile.create();
    private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();

    @Child private CopyOfRegAttributesNode copyRegAttributes = CopyOfRegAttributesNodeGen.create();
    @Child private InitAttributesNode initAttributes = InitAttributesNode.create();
    @Child private SetFixedAttributeNode putDimensions = SetFixedAttributeNode.createDim();
    @Child private SetFixedAttributeNode putDimNames = SetFixedAttributeNode.createDimNames();
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();
    @Child private GetNamesAttributeNode getAxisNamesNode = GetNamesAttributeNode.create();
    @Child private GetDimAttributeNode getDimNode = GetDimAttributeNode.create();
    @Child private ReuseNonSharedNode reuseNonShared = ReuseNonSharedNode.create();

    static {
        Casts.noCasts(Transpose.class);
    }

    public abstract Object execute(RAbstractVector o);

    protected boolean isSquare(RAbstractVector vector) {
        int[] dims = getDimNode.getDimensions(vector);
        if (GetDimAttributeNode.isMatrix(dims)) {
            assert dims.length >= 2;
            return dims[0] == dims[1];
        }
        return false;
    }

    protected boolean isMatrix(RAbstractVector vector) {
        return GetDimAttributeNode.isMatrix(getDimNode.getDimensions(vector));
    }

    private void transposeSquareMatrixInPlace(RAbstractVector vector, RandomIterator iter, VectorAccess access) {
        int length = lengthProfile.profile(vector.getLength());
        assert isMatrix(vector);
        int[] dims = getDimNode.getDimensions(vector);
        assert dims.length == 2;
        assert dims[0] == dims[1];
        int dim = dims[0];
        RBaseNode.reportWork(this, length);

        RType type = access.getType();
        loopProfile.profileCounted(length);
        for (int i = 0; loopProfile.inject(i < dim); i++) {
            for (int j = 0; j < i; j++) {
                int swapi = i * dim + j;
                int swapj = j * dim + i;
                switch (type) {
                    case Character: {
                        String tmp = access.getString(iter, swapi);
                        access.setString(iter, swapi, access.getString(iter, swapj));
                        access.setString(iter, swapj, tmp);
                        break;
                    }
                    case Complex: {
                        double tmpReal = access.getComplexR(iter, swapi);
                        double tmpImaginary = access.getComplexI(iter, swapi);
                        access.setComplex(iter, swapi, access.getComplexR(iter, swapj), access.getComplexI(iter, swapj));
                        access.setComplex(iter, swapj, tmpReal, tmpImaginary);
                        break;
                    }
                    case Double: {
                        double tmp = access.getDouble(iter, swapi);
                        access.setDouble(iter, swapi, access.getDouble(iter, swapj));
                        access.setDouble(iter, swapj, tmp);
                        break;
                    }
                    case Integer: {
                        int tmp = access.getInt(iter, swapi);
                        access.setInt(iter, swapi, access.getInt(iter, swapj));
                        access.setInt(iter, swapj, tmp);
                        break;
                    }
                    case List: {
                        Object tmp = access.getListElement(iter, swapi);
                        access.setListElement(iter, swapi, access.getListElement(iter, swapj));
                        access.setListElement(iter, swapj, tmp);
                        break;
                    }
                    case Logical: {
                        byte tmp = access.getLogical(iter, swapi);
                        access.setLogical(iter, swapi, access.getLogical(iter, swapj));
                        access.setLogical(iter, swapj, tmp);
                        break;
                    }
                    case Raw: {
                        byte tmp = access.getRaw(iter, swapi);
                        access.setRaw(iter, swapi, access.getRaw(iter, swapj));
                        access.setRaw(iter, swapj, tmp);
                        break;
                    }
                    default:
                        throw RInternalError.shouldNotReachHere();
                }
            }
        }
        // don't need to set new dimensions; it is a square matrix
        putNewDimNames(vector, vector);
    }

    @Specialization(guards = {"isSquare(x)", "!isRExpression(x)", "xReuse.supports(x)"})
    protected RAbstractVector transposeSquare(RAbstractVector x,
                    @Cached("createNonShared(x)") VectorReuse xReuse) {
        RAbstractVector result = xReuse.getResult(x);
        VectorAccess resultAccess = xReuse.access(result);
        try (RandomIterator resultIter = resultAccess.randomAccess(result)) {
            transposeSquareMatrixInPlace(result, resultIter, resultAccess);
        }
        return result;
    }

    @Specialization(replaces = "transposeSquare", guards = {"isSquare(x)", "!isRExpression(x)"})
    protected RAbstractVector transposeSquareGeneric(RAbstractVector x,
                    @Cached("createNonSharedGeneric()") VectorReuse xReuse) {
        return transposeSquare(x, xReuse);
    }

    @Specialization(guards = {"isMatrix(x)", "!isSquare(x)", "!isRExpression(x)", "xAccess.supports(x)"})
    protected RAbstractVector transpose(RAbstractVector x,
                    @Cached("create()") VectorFactory factory,
                    @Cached("x.access()") VectorAccess xAccess,
                    @Cached("createNew(xAccess.getType())") VectorAccess resultAccess) {
        try (RandomIterator xIter = xAccess.randomAccess(x)) {
            RAbstractVector result = factory.createVector(xAccess.getType(), xAccess.getLength(xIter), false);
            try (RandomIterator resultIter = resultAccess.randomAccess(result)) {
                int length = lengthProfile.profile(xAccess.getLength(xIter));
                assert isMatrix(x);
                int[] dims = getDimNode.getDimensions(x);
                int firstDim = dims[0];
                int secondDim = dims[1];
                RBaseNode.reportWork(this, length);

                int j = 0;
                loopProfile.profileCounted(length);
                for (int i = 0; loopProfile.inject(i < length); i++, j += firstDim) {
                    if (j > (length - 1)) {
                        j -= (length - 1);
                    }
                    resultAccess.setFromSameType(resultIter, i, xAccess, xIter, j);
                }
                // copy attributes
                copyRegAttributes.execute(x, result);
                // set new dimensions
                putNewDimensions(x, result, new int[]{secondDim, firstDim});
            }
            result.setComplete(x.isComplete());
            return result;
        }
    }

    @Specialization(replaces = "transpose", guards = {"isMatrix(x)", "!isSquare(x)", "!isRExpression(x)"})
    protected RAbstractVector transposeGeneric(RAbstractVector x,
                    @Cached("create()") VectorFactory factory) {
        return transpose(x, factory, x.slowPathAccess(), VectorAccess.createSlowPathNew(x.getRType()));
    }

    @Specialization(guards = {"!isMatrix(x)", "!isRExpression(x)"})
    protected RVector<?> transposeNonMatrix(RAbstractVector x) {
        RVector<?> reused = reuseNonShared.execute(x);
        putNewDimensions(reused, reused, new int[]{1, x.getLength()});
        return reused;

    }

    private void putNewDimensions(RAbstractVector source, RAbstractVector dest, int[] newDim) {
        putDimensions.execute(initAttributes.execute(dest), RDataFactory.createIntVector(newDim, RDataFactory.COMPLETE_VECTOR));
        putNewDimNames(source, dest);
    }

    private void putNewDimNames(RAbstractVector source, RAbstractVector dest) {
        // set new dim names
        RList dimNames = getDimNamesNode.getDimNames(source);
        if (dimNames != null) {
            hasDimNamesProfile.enter();
            assert dimNames.getLength() == 2;
            RStringVector axisNames = getAxisNamesNode.getNames(dimNames);
            RStringVector transAxisNames = axisNames == null ? null : RDataFactory.createStringVector(new String[]{axisNames.getDataAt(1), axisNames.getDataAt(0)}, true);
            RList newDimNames = RDataFactory.createList(new Object[]{dimNames.getDataAt(1), dimNames.getDataAt(0)}, transAxisNames);
            putDimNames.execute(dest.getAttributes(), newDimNames);
        }
    }

    @Fallback
    protected RVector<?> transpose(@SuppressWarnings("unused") Object x) {
        throw error(Message.ARGUMENT_NOT_MATRIX);
    }
}
