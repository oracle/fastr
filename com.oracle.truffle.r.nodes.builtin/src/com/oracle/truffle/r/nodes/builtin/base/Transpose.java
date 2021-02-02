/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.profile.VectorLengthProfile;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;
import com.oracle.truffle.r.runtime.data.nodes.VectorReuse;
import com.oracle.truffle.r.runtime.data.nodes.attributes.CopyOfRegAttributesNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.CopyOfRegAttributesNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.RemoveAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.ExtractNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = "t.default", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
@ImportStatic({RRuntime.class, ConvertForeignObjectNode.class})
public abstract class Transpose extends RBuiltinNode.Arg1 {

    private final BranchProfile hasDimNamesProfile = BranchProfile.create();

    private final VectorLengthProfile lengthProfile = VectorLengthProfile.create();
    private final LoopConditionProfile loopProfile = LoopConditionProfile.createCountingProfile();

    @Child private CopyOfRegAttributesNode copyRegAttributes = CopyOfRegAttributesNodeGen.create();
    @Child private SetFixedAttributeNode putDimensions = SetFixedAttributeNode.createDim();
    @Child private SetFixedAttributeNode putDimNames = SetFixedAttributeNode.createDimNames();
    @Child private GetDimNamesAttributeNode getDimNamesNode = GetDimNamesAttributeNode.create();
    @Child private ExtractNamesAttributeNode extractAxisNamesNode = ExtractNamesAttributeNode.create();
    @Child private GetDimAttributeNode getDimNode = GetDimAttributeNode.create();
    @Child private GetNamesAttributeNode getNamesNode;
    @Child private RemoveAttributeNode removeAttributeNode;

    static {
        Casts.noCasts(Transpose.class);
    }

    protected static Transpose create() {
        return TransposeNodeGen.create();
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
        convertDimNames(vector, vector);
    }

    @Specialization(guards = {"isSquare(x)", "!isRExpression(x)", "xReuse.supports(x)"})
    protected RAbstractVector transposeSquare(RAbstractVector x,
                    @Cached("createTemporary(x)") VectorReuse xReuse) {
        RAbstractVector result = xReuse.getResult(x);
        VectorAccess resultAccess = xReuse.access(result);
        try (RandomIterator resultIter = resultAccess.randomAccess(result)) {
            transposeSquareMatrixInPlace(result, resultIter, resultAccess);
        }
        return result;
    }

    @Specialization(replaces = "transposeSquare", guards = {"isSquare(x)", "!isRExpression(x)"})
    protected RAbstractVector transposeSquareGeneric(RAbstractVector x,
                    @Cached("createTemporaryGeneric()") VectorReuse xReuse) {
        return transposeSquare(x, xReuse);
    }

    @Specialization(guards = {"isMatrix(x)", "!isSquare(x)", "!isRExpression(x)"}, limit = "getGenericDataLibraryCacheSize()")
    protected RAbstractVector transpose(RAbstractVector x,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @Cached("create()") VectorFactory factory,
                    @CachedLibrary(limit = "getTypedVectorDataLibraryCacheSize()") VectorDataLibrary resultDataLib) {
        Object xData = x.getData();
        int xLen = xDataLib.getLength(xData);
        RandomAccessIterator xIter = xDataLib.randomAccessIterator(xData);
        RAbstractVector result = factory.createVector(xDataLib.getType(xData), xLen, false);
        Object resultData = result.getData();
        RandomAccessWriteIterator resultIter = resultDataLib.randomAccessWriteIterator(resultData);
        try {
            int length = lengthProfile.profile(xLen);
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
                Object value = xDataLib.getElement(xData, xIter, j);
                resultDataLib.setElement(resultData, resultIter, i, value);
            }
            // copy attributes
            copyRegAttributes.execute(x, result);
            // set new dimensions
            putNewDimsFromDimnames(x, result, new int[]{secondDim, firstDim});
        } finally {
            resultDataLib.commitRandomAccessWriteIterator(resultData, resultIter, xDataLib.getNACheck(xData).neverSeenNA());
        }
        return result;
    }

    @Specialization(replaces = "transpose", guards = {"isMatrix(x)", "!isSquare(x)", "!isRExpression(x)"})
    protected RAbstractVector transposeGeneric(RAbstractVector x,
                    @Cached("create()") VectorFactory factory,
                    @CachedLibrary(limit = "getGenericDataLibraryCacheSize()") VectorDataLibrary xDataLib,
                    @CachedLibrary(limit = "getGenericDataLibraryCacheSize()") VectorDataLibrary resultDataLib) {
        return transpose(x, xDataLib, factory, resultDataLib);
    }

    @Specialization(guards = {"!isMatrix(x)", "!isRExpression(x)", "reuseNonSharedNode.supports(x)"}, limit = "getVectorAccessCacheSize()")
    protected RAbstractVector transposeNonMatrix(RAbstractVector x,
                    @Cached("createTemporary(x)") VectorReuse reuseNonSharedNode) {
        RAbstractVector reused = reuseNonSharedNode.getMaterializedResult(x);
        putNewDimsFromNames(reused, reused, new int[]{1, x.getLength()});
        return reused;
    }

    @Specialization(replaces = "transposeNonMatrix", guards = {"!isMatrix(x)", "!isRExpression(x)"})
    protected RAbstractVector transposeNonMatrixGeneric(RAbstractVector x,
                    @Cached("createTemporaryGeneric()") VectorReuse reuseNonSharedNode) {
        return transposeNonMatrix(x, reuseNonSharedNode);
    }

    private void putNewDimsFromDimnames(RAbstractVector source, RAbstractVector dest, int[] newDim) {
        putDimensions.setAttr(dest, RDataFactory.createIntVector(newDim, RDataFactory.COMPLETE_VECTOR));
        convertDimNames(source, dest);
    }

    private void putNewDimsFromNames(RAbstractVector source, RAbstractVector dest, int[] newDim) {
        putDimensions.setAttr(dest, RDataFactory.createIntVector(newDim, RDataFactory.COMPLETE_VECTOR));
        convertNamesToDimnames(source, dest);
    }

    private void convertDimNames(RAbstractVector source, RAbstractVector dest) {
        // set new dim names
        RList dimNames = getDimNamesNode.getDimNames(source);
        if (dimNames != null) {
            hasDimNamesProfile.enter();
            assert dimNames.getLength() == 2;
            RStringVector axisNames = extractAxisNamesNode.execute(dimNames);
            RStringVector transAxisNames = axisNames == null ? null : RDataFactory.createStringVector(new String[]{axisNames.getDataAt(1), axisNames.getDataAt(0)}, true);
            RList newDimNames = RDataFactory.createList(new Object[]{dimNames.getDataAt(1), dimNames.getDataAt(0)}, transAxisNames);
            putDimNames.setAttr(dest, newDimNames);
        }
    }

    private void convertNamesToDimnames(RAbstractVector source, RAbstractVector dest) {
        if (getNamesNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNamesNode = insert(GetNamesAttributeNode.create());
        }
        RStringVector names = (RStringVector) getNamesNode.execute(source);
        if (names != null) {
            RList newDimNames = RDataFactory.createList(new Object[]{RNull.instance, names});
            putDimNames.setAttr(dest, newDimNames);
            if (removeAttributeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                removeAttributeNode = insert(RemoveAttributeNode.create());
            }
            removeAttributeNode.execute(dest, "names");
        }
    }

    @Specialization(guards = {"isForeignArray(x, interop)"}, limit = "getInteropLibraryCacheSize()")
    protected Object transposeForeign(TruffleObject x,
                    @Cached("create()") ConvertForeignObjectNode convertForeign,
                    @Cached("create()") Transpose recursive,
                    @SuppressWarnings("unused") @CachedLibrary("x") InteropLibrary interop) {
        RAbstractVector vec = (RAbstractVector) convertForeign.convert(x);
        return recursive.execute(vec);
    }

    @Specialization(guards = {"isForeignObject(x)", "!isForeignArray(x, interop)"}, limit = "getInteropLibraryCacheSize()")
    protected Object transposeForeign(@SuppressWarnings("unused") TruffleObject x,
                    @SuppressWarnings("unused") @CachedLibrary("x") InteropLibrary interop) {
        throw error(Message.ARGUMENT_NOT_MATRIX);
    }

    @Fallback
    protected RAbstractVector transposeOthers(@SuppressWarnings("unused") Object x) {
        throw error(Message.ARGUMENT_NOT_MATRIX);
    }
}
