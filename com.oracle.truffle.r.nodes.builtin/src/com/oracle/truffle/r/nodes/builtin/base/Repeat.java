/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.typeName;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.RepeatNodeGen.FastRInternalRepeatNodeGen;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineBuilder;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.call.PrepareMatchInternalArguments;
import com.oracle.truffle.r.nodes.function.call.PrepareMatchInternalArgumentsNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.CopyResizedToPreallocated;
import com.oracle.truffle.r.runtime.data.nodes.CopyResizedToPreallocatedNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;

/**
 * The {@code rep} builtin works as follows.
 * <ol>
 * <li>If {@code each} is greater than one, all elements of {@code x} are first replicated
 * {@code each} times.
 * <li>If {@code length.out} is given, the result of the first step is truncated or extended as
 * required. In this case, {@code times} is ignored.
 * <li>If {@code length.out} is not given, {@code times} is regarded:
 * <ul>
 * <li>If {@code times} is a one-element vector, the result of the first step is replicated
 * {@code times} times.
 * <li>If {@code times} is a vector longer than one, and {@code each} is greater than one, an error
 * is issued.
 * <li>If {@code times} is a vector longer than one, and {@code each} is one, and {@code times} is
 * as long as {@code x}, each element of {@code x} is given the number of times indicated by the
 * value at the same index of {@code times}. If {@code times} has a different length, an error is
 * issued.
 * </ul>
 * </ol>
 */
@RBuiltin(name = "rep", kind = PRIMITIVE, parameterNames = {"x", "..."}, dispatch = INTERNAL_GENERIC, behavior = PURE)
@SuppressWarnings("this-escape")
public abstract class Repeat extends RBuiltinNode.Arg2 {

    private static final PipelineBuilder PB_TIMES;
    private static final PipelineBuilder PB_LENGTH_OUT;
    private static final PipelineBuilder PB_EACH;
    private static final FormalArguments FORMALS;
    private static final int ARG_IDX_TIMES;
    private static final int ARG_IDX_LENGHT_OUT;
    private static final int ARG_IDX_EACH;

    static {
        Casts casts = new Casts(Repeat.class);
        casts.arg("x").allowNull().mustBe(abstractVectorValue(), RError.Message.ATTEMPT_TO_REPLICATE, typeName());

        // prepare cast pipeline nodes for vararg matching
        PB_TIMES = new PipelineBuilder("times");
        PB_TIMES.fluent().defaultError(RError.Message.INVALID_ARGUMENT, "times").mustNotBeNull().asIntegerVector();

        PB_LENGTH_OUT = new PipelineBuilder("length.out");
        PB_LENGTH_OUT.fluent().asIntegerVector().shouldBe(size(1).or(size(0)),
                        RError.Message.FIRST_ELEMENT_USED, "length.out").findFirst(RRuntime.INT_NA,
                                        RError.Message.FIRST_ELEMENT_USED, "length.out").mustBe(intNA().or(gte(0)));

        PB_EACH = new PipelineBuilder("each");
        PB_EACH.fluent().asIntegerVector().shouldBe(size(1).or(size(0)),
                        RError.Message.FIRST_ELEMENT_USED, "each").findFirst(1, RError.Message.FIRST_ELEMENT_USED,
                                        "each").replaceNA(1).mustBe(gte(0));

        // "..." in signature ensures that the matcher will not report additional arguments which
        // are also ignored by GNUR
        ArgumentsSignature signature = ArgumentsSignature.get("times", "length.out", "each", "...");
        ARG_IDX_TIMES = 0;
        ARG_IDX_LENGHT_OUT = 1;
        ARG_IDX_EACH = 2;
        FORMALS = FormalArguments.createForBuiltin(new Object[]{1, RRuntime.INT_NA, 1}, signature);

        // Note: repeat is happy with empty arguments in varagrs
    }

    @Child private FastRInternalRepeat internalNode = FastRInternalRepeatNodeGen.create();
    @Child private CastNode castTimes = PB_TIMES.buildNode();
    @Child private CastNode castLengthOut = PB_LENGTH_OUT.buildNode();
    @Child private CastNode castEach = PB_EACH.buildNode();
    @Child private PrepareMatchInternalArguments prepareArgs = PrepareMatchInternalArgumentsNodeGen.create(FORMALS, this);

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY};
    }

    @Specialization
    protected RNull repeatNull(@SuppressWarnings("unused") RNull x, @SuppressWarnings("unused") RArgsValuesAndNames args) {
        return RNull.instance;
    }

    @Specialization
    protected Object repeat(VirtualFrame frame, RAbstractVector x, RArgsValuesAndNames args) {
        RArgsValuesAndNames margs = prepareArgs.execute(args, null);

        // cast arguments
        Object times = castTimes.doCast(margs.getArgument(ARG_IDX_TIMES));
        Object lengthOut = castLengthOut.doCast(margs.getArgument(ARG_IDX_LENGHT_OUT));
        Object each = castEach.doCast(margs.getArgument(ARG_IDX_EACH));

        return internalNode.execute(frame, x, times, lengthOut, each);
    }

    @TypeSystemReference(RTypes.class)
    @ImportStatic(DSLConfig.class)
    abstract static class FastRInternalRepeat extends RBaseNode {

        private final ConditionProfile lengthOutOrTimes = ConditionProfile.createBinaryProfile();
        private final ConditionProfile oneTimeGiven = ConditionProfile.createBinaryProfile();

        @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();
        @Child private VectorDataLibrary resultDataLib;
        @Child private VectorDataLibrary namesDataLib;
        @Child private VectorDataLibrary eachResultDataLib;

        @Child CopyResizedToPreallocated copyResizedNode;

        @CompilationFinal private boolean trySimple = true;

        public abstract RAbstractVector execute(VirtualFrame frame, Object... args);

        @Specialization(limit = "getGenericVectorAccessCacheSize()")
        protected RAbstractVector rep(RAbstractVector xIn, RIntVector times, int lengthOutIn, int eachIn,
                        @Cached("createClassProfile()") ValueProfile xProfile,
                        @CachedLibrary("xIn.getData()") VectorDataLibrary xDataLib,
                        @CachedLibrary("times.getData()") VectorDataLibrary timesDataLib,
                        @Cached("createEqualityProfile()") PrimitiveValueProfile lengthOutProfile,
                        @Cached("createEqualityProfile()") PrimitiveValueProfile eachProfile,
                        @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile) {
            RAbstractVector x = xProfile.profile(xIn);
            Object timesData = times.getData();
            Object xData = x.getData();
            int lengthOut = lengthOutProfile.profile(lengthOutIn);
            int each = eachProfile.profile(eachIn);

            // fast path for very simple case of filling with a single double values:
            if (trySimple) {
                if (x instanceof RDoubleVector && xDataLib.getLength(xData) == 1 && timesDataLib.getLength(timesData) == 1 && each == 1 && getNames.getNames(x) == null) {
                    int t = timesDataLib.getIntAt(timesData, 0);
                    if (t < 0) {
                        throw error(RError.Message.INVALID_ARGUMENT, "times");
                    }
                    int length = lengthOutOrTimes.profile(!RRuntime.isNA(lengthOut)) ? lengthOut : t;
                    double[] data = new double[length];
                    double value = xDataLib.getDoubleAt(xData, 0);
                    Arrays.fill(data, value);
                    return RDataFactory.createDoubleVector(data, !RRuntime.isNA(value));
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    trySimple = false;
                }
            }
            return preprocessEach(xDataLib, xData, x, timesDataLib, timesData, lengthOut, each, hasNamesProfile);
        }

        private RAbstractVector preprocessEach(VectorDataLibrary xDataLib, Object xData, RAbstractVector x, VectorDataLibrary timesDataLib, Object timesData, int lengthOut, int each,
                        ConditionProfile hasNamesProfile) {
            if (each != 1) {
                if (each <= 0) {
                    throw error(RError.Message.INVALID_ARGUMENT, "times");
                }
                if (timesDataLib.getLength(timesData) > 1) {
                    throw error(RError.Message.INVALID_ARGUMENT, "times");
                }
                initEachResultDataLib();
                RAbstractVector input = handleEach(xDataLib, xData, x, each, eachResultDataLib);
                return repInternal(eachResultDataLib, input.getData(), input, timesDataLib, timesData, lengthOut, each, hasNamesProfile);
            } else {
                return repInternal(xDataLib, xData, x, timesDataLib, timesData, lengthOut, each, hasNamesProfile);
            }
        }

        private RAbstractVector repInternal(VectorDataLibrary xDataLib, Object xData, RAbstractVector x, VectorDataLibrary timesDataLib, Object timesData, int lengthOut, int each,
                        ConditionProfile hasNamesProfile) {
            RAbstractVector r;
            if (lengthOutOrTimes.profile(RRuntime.isNA(lengthOut))) {
                r = handleTimes(xDataLib, xData, x, timesDataLib, timesData);
            } else {
                r = handleLengthOut(xDataLib, xData, x, lengthOut);
            }
            if (hasNamesProfile != null) {
                RStringVector names = getNames.getNames(x);
                if (hasNamesProfile.profile(names != null)) {
                    // handle names - passing null as hasNamesProfile stops the recursion
                    names = (RStringVector) preprocessEach(getNamesDataLib(), names.getData(), names, timesDataLib, timesData, lengthOut, each, null);
                    r.initAttributes(RAttributesLayout.createNames(names));
                }
            }
            return r;
        }

        /**
         * Prepare the input vector by replicating its elements.
         */
        private static RAbstractVector handleEach(VectorDataLibrary xDataLib, Object xData, RAbstractVector x, int each, VectorDataLibrary eachResultDataLib) {
            int xLen = xDataLib.getLength(xData);
            // Note: the complete flag will be updated in the commitRandomAccessWriteIterator
            RAbstractVector r = x.createEmptySameType(xLen * each, true);
            Object rData = r.getData();
            RandomAccessWriteIterator rIt = eachResultDataLib.randomAccessWriteIterator(rData);
            boolean neverSeenNA = false;
            try {
                RandomAccessIterator xIt = xDataLib.randomAccessIterator(xData);
                for (int i = 0; i < xLen; i++) {
                    for (int j = i * each; j < (i + 1) * each; j++) {
                        eachResultDataLib.transfer(rData, rIt, j, xDataLib, xIt, xData, i);
                    }
                }
                neverSeenNA = xDataLib.isComplete(xData) || xDataLib.getNACheck(xData).neverSeenNA();
            } finally {
                eachResultDataLib.commitRandomAccessWriteIterator(rData, rIt, neverSeenNA);
            }
            return r;
        }

        /**
         * Extend or truncate the vector to a specified length.
         */
        private RAbstractVector handleLengthOut(VectorDataLibrary xDataLib, Object xData, RAbstractVector x, int lengthOut) {
            return copyResized(xDataLib, xData, x, lengthOut);
        }

        private RAbstractVector copyResized(VectorDataLibrary xDataLib, Object xData, RAbstractVector x, int length) {
            if (copyResizedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                copyResizedNode = insert(CopyResizedToPreallocatedNodeGen.create());
            }
            boolean fillWithNA = xDataLib.getLength(xData) == 0;
            RAbstractVector result = x.createEmptySameType(length, xDataLib.isComplete(xData) && !fillWithNA);
            copyResizedNode.execute(xDataLib, xData, result.getData(), fillWithNA);
            return result;
        }

        /**
         * Replicate the vector a given number of times.
         */
        private RAbstractVector handleTimes(VectorDataLibrary xDataLib, Object xData, RAbstractVector x, VectorDataLibrary timesDataLib, Object timesData) {
            if (oneTimeGiven.profile(timesDataLib.getLength(timesData) == 1)) {
                // only one times value is given
                int howManyTimes = timesDataLib.getIntAt(timesData, 0);
                if (howManyTimes < 0) {
                    throw error(RError.Message.INVALID_ARGUMENT, "times");
                }
                int resultLength = xDataLib.getLength(xData) * howManyTimes;
                return copyResized(xDataLib, xData, x, resultLength);
            } else {
                // times is a vector with several elements
                if (xDataLib.getLength(xData) != timesDataLib.getLength(timesData)) {
                    throw error(RError.Message.INVALID_ARGUMENT, "times");
                }
                // iterate once over the times vector to determine result vector size
                int resultLength = 0;
                SeqIterator it = timesDataLib.iterator(timesData);
                while (timesDataLib.nextLoopCondition(timesData, it)) {
                    int t = timesDataLib.getNextInt(timesData, it);
                    if (t < 0) {
                        throw error(RError.Message.INVALID_ARGUMENT, "times");
                    }
                    resultLength += t;
                }
                // create and populate result vector, the complete flag will be updated in the
                // iterator commit
                RAbstractVector r = x.createEmptySameType(resultLength, true);
                Object rData = r.getData();
                VectorDataLibrary rDataLib = getResultDataLib();
                RandomAccessWriteIterator rIt = rDataLib.randomAccessWriteIterator(rData);
                boolean neverSeenNA = false;
                try {
                    RandomAccessIterator xIt = xDataLib.randomAccessIterator(xData);
                    int xLen = xDataLib.getLength(xData);
                    int wp = 0; // write pointer
                    for (int i = 0; i < xLen; i++) {
                        for (int j = 0; j < timesDataLib.getIntAt(timesData, i); ++j, ++wp) {
                            rDataLib.transfer(rData, rIt, wp, xDataLib, xIt, xData, i);
                        }
                    }
                    neverSeenNA = xDataLib.isComplete(xData) || xDataLib.getNACheck(xData).neverSeenNA();
                } finally {
                    rDataLib.commitRandomAccessWriteIterator(rData, rIt, neverSeenNA);
                }
                return r;
            }
        }

        public void initEachResultDataLib() {
            if (eachResultDataLib == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                eachResultDataLib = insert(VectorDataLibrary.getFactory().createDispatched(DSLConfig.getGenericVectorAccessCacheSize()));
            }
        }

        public VectorDataLibrary getResultDataLib() {
            if (resultDataLib == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                resultDataLib = insert(VectorDataLibrary.getFactory().createDispatched(DSLConfig.getGenericVectorAccessCacheSize()));
            }
            return resultDataLib;
        }

        public VectorDataLibrary getNamesDataLib() {
            if (namesDataLib == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                namesDataLib = insert(VectorDataLibrary.getFactory().createDispatched(DSLConfig.getGenericVectorAccessCacheSize()));
            }
            return namesDataLib;
        }
    }
}
