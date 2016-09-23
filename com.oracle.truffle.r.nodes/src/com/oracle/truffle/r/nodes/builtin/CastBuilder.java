/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.DoubleFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MatrixFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.BoxPrimitiveStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep.TargetType;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CustomNodeStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultErrorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultWarningStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentFilterFactory;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentFilterFactoryImpl;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentMapperFactory;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentMapperFactoryImpl;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public final class CastBuilder {

    private static final PipelineBuilder[] EMPTY_BUILDERS = new PipelineBuilder[0];

    private final RBuiltinNode builtinNode;
    private final String[] argumentNames;
    private PipelineBuilder[] argumentBuilders;
    private CastNode[] castsCache = null;

    public CastBuilder(RBuiltinNode builtinNode) {
        assert builtinNode != null : "builtinNode is null";
        // Note: if we have the builtin metadata, we pre-allocate the arrays, builtinNode != null is
        // used to determine, if the arrays are pre-allocated or if they can grow
        RBuiltin builtin = builtinNode.getRBuiltin();
        if (builtin == null) {
            this.builtinNode = null;
            argumentNames = null;
            argumentBuilders = EMPTY_BUILDERS;
        } else {
            this.builtinNode = builtinNode;
            argumentNames = builtin.parameterNames();
            argumentBuilders = new PipelineBuilder[builtin.parameterNames().length];
        }
    }

    public CastBuilder(int argumentsCount) {
        assert argumentsCount >= 0 : "argumentsCount must be non-negative";
        builtinNode = null;
        argumentNames = null;
        argumentBuilders = new PipelineBuilder[argumentsCount];
    }

    public CastBuilder() {
        builtinNode = null;
        argumentNames = null;
        argumentBuilders = EMPTY_BUILDERS;
    }

    /**
     * Returns the first case node in the chain for each argument, if argument does not require any
     * casting, returns {@code null} as its cast node.
     */
    public CastNode[] getCasts() {
        if (castsCache == null) {
            castsCache = new CastNode[argumentBuilders.length];
            for (int i = 0; i < argumentBuilders.length; i++) {
                PipelineBuilder arg = argumentBuilders[i];
                if (arg != null) {
                    PipelineStep<?, ?> firstStep = arg.chainBuilder != null ? arg.chainBuilder.firstStep : null;
                    castsCache[i] = PipelineToCastNode.convert(arg.pcb, firstStep);
                }
            }
        }
        return castsCache;
    }

    // ---------------
    // Deprecated API:
    // Converted to the new API preserving original semantics as much as possible

    private PipelineBuilder getBuilderForDeprecated(int index) {
        PipelineBuilder builder = getBuilder(index, "");
        builder.getPipelineConfig().allowNull();
        return builder;
    }

    public CastBuilder toVector(int index) {
        PipelineBuilder builder = getBuilderForDeprecated(index);
        // TODO: asVector with preserveNonVector transforms RNull, this is not compatible with the
        // semantics of RNull being forwarded by all cast nodes. Because of this we need to map null
        // for legacy API calls
        builder.getPipelineConfig().setWasLegacyAsVectorCall();
        builder.getPipelineConfig().mapNull(new MapToValue<>(RDataFactory.createList()));
        builder.appendAsVector();
        return this;
    }

    public CastBuilder toInteger(int index) {
        PipelineBuilder builder = getBuilderForDeprecated(index);
        if (builder.getPipelineConfig().wasLegacyAsVectorCall()) {
            mapNullAndMissing(builder, RDataFactory.createIntVector(0));
        } else {
            builder.getPipelineConfig().mapNull(new MapToValue<>(RNull.instance));
        }
        builder.appendAsIntegerVector(false, false, false);
        return this;
    }

    public CastBuilder toDouble(int index) {
        PipelineBuilder builder = getBuilderForDeprecated(index);
        if (builder.getPipelineConfig().wasLegacyAsVectorCall()) {
            builder.getPipelineConfig().mapNull(new MapToValue<>(RDataFactory.createDoubleVector(0)));
            // CastDoubleBaseNode does not handle RMissing it seems...
        } else {
            allowNullAndMissing(builder);
        }
        builder.appendAsDoubleVector(false, false, false);
        return this;
    }

    public CastBuilder toLogical(int index) {
        PipelineBuilder builder = getBuilderForDeprecated(index);
        if (builder.getPipelineConfig().wasLegacyAsVectorCall()) {
            mapNullAndMissing(builder, RDataFactory.createLogicalVector(0));
        } else {
            allowNullAndMissing(builder);
        }
        builder.appendAsLogicalVector(false, false, false);
        return this;
    }

    public CastBuilder boxPrimitive(int index) {
        PipelineBuilder builder = getBuilderForDeprecated(index);
        allowNullAndMissing(builder);
        builder.append(new BoxPrimitiveStep<>());
        return this;
    }

    public CastBuilder firstIntegerWithError(int index, RError.Message error, String name) {
        // TODO: check if we can remove FirstIntNode when this is removed...
        getBuilderForDeprecated(index).appendAsIntegerVector(false, false, false);
        getBuilderForDeprecated(index).appendFindFirst(null, Integer.class, SHOW_CALLER, error, new Object[]{name});
        return this;
    }

    public CastBuilder firstBoolean(int index) {
        PipelineBuilder builder = getBuilderForDeprecated(index);
        builder.appendAsLogicalVector(false, false, false);
        builder.appendFindFirst(null, Byte.class, SHOW_CALLER, RError.Message.ARGUMENT_NOT_INTERPRETABLE_LOGICAL, new Object[0]);
        builder.appendMap(MapByteToBoolean.INSTANCE);
        return this;
    }

    public CastBuilder firstBoolean(int index, String invalidValueName) {
        if (invalidValueName == null) {
            return firstBoolean(index);
        }
        PipelineBuilder builder = getBuilderForDeprecated(index);
        builder.appendAsLogicalVector(false, false, false);
        builder.appendFindFirst(null, Byte.class, SHOW_CALLER, RError.Message.INVALID_VALUE, new Object[]{invalidValueName});
        builder.appendMap(MapByteToBoolean.INSTANCE);
        return this;
    }

    private void allowNullAndMissing(PipelineBuilder builder) {
        builder.getPipelineConfig().mapNull(new MapToValue<>(RNull.instance));
        builder.getPipelineConfig().mapMissing(new MapToValue<>(RMissing.instance));
    }

    private void mapNullAndMissing(PipelineBuilder builder, Object value) {
        builder.getPipelineConfig().mapNull(new MapToValue<>(value));
        builder.getPipelineConfig().mapMissing(new MapToValue<>(value));
    }

    // end of deprecated API:
    // ---------------------
    // The cast-pipelines API starts here

    public PreinitialPhaseBuilder<Object> arg(int argumentIndex, String argumentName) {
        assert builtinNode != null : "arg(int, String) is only supported for builtins cast pipelines";
        assert argumentIndex >= 0 && argumentIndex < argumentBuilders.length : "argument index out of range";
        assert argumentNames[argumentIndex].equals(argumentName) : "wrong argument name " + argumentName;
        return new PreinitialPhaseBuilder<>(getBuilder(argumentIndex, argumentName));
    }

    public PreinitialPhaseBuilder<Object> arg(int argumentIndex) {
        boolean existingIndex = argumentNames != null && argumentIndex >= 0 && argumentIndex < argumentNames.length;
        String name = existingIndex ? argumentNames[argumentIndex] : null;
        return new PreinitialPhaseBuilder<>(getBuilder(argumentIndex, name));
    }

    public PreinitialPhaseBuilder<Object> arg(String argumentName) {
        assert builtinNode != null : "arg(String) is only supported for builtins cast pipelines";
        return new PreinitialPhaseBuilder<>(getBuilder(getArgumentIndex(argumentName), argumentName));
    }

    private PipelineBuilder getBuilder(int argumentIndex, String argumentName) {
        if (builtinNode == null && argumentIndex >= argumentBuilders.length) {
            // in the case that we have a builtin, the arguments size is known and fixed, otherwise
            // we grow the array accordingly
            argumentBuilders = Arrays.copyOf(argumentBuilders, argumentIndex + 1);
        }
        if (argumentBuilders[argumentIndex] == null) {
            argumentBuilders[argumentIndex] = new PipelineBuilder(new PipelineConfigBuilder(argumentName));
        }
        return argumentBuilders[argumentIndex];
    }

    private int getArgumentIndex(String argumentName) {
        if (builtinNode == null) {
            throw new IllegalArgumentException("No builtin node associated with cast builder");
        }
        for (int i = 0; i < argumentNames.length; i++) {
            if (argumentName.equals(argumentNames[i])) {
                return i;
            }
        }
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(String.format("Argument %s not found in builtin %s", argumentName, builtinNode.getRBuiltin().name()));
    }

    @SuppressWarnings({"unchecked"})
    public static Object[] substituteArgPlaceholder(Object arg, Object[] messageArgs) {
        Object[] newMsgArgs = Arrays.copyOf(messageArgs, messageArgs.length);

        for (int i = 0; i < messageArgs.length; i++) {
            final Object msgArg = messageArgs[i];
            if (msgArg instanceof Function) {
                newMsgArgs[i] = ((Function<Object, Object>) msgArg).apply(arg);
            }
        }

        return newMsgArgs;
    }

    public static final class Predef {

        @SuppressWarnings("unchecked")
        public static <T> NotFilter<T> not(Filter<? super T, ? extends T> filter) {
            NotFilter<? super T> n = filter.not();
            return (NotFilter<T>) n;
        }

        public static <T> AndFilter<T, T> and(Filter<T, T> filter1, Filter<T, T> filter2) {
            return filter1.and(filter2);
        }

        public static <T> OrFilter<T> or(Filter<T, T> filter1, Filter<T, T> filter2) {
            return filter1.or(filter2);
        }

        public static <T, R extends T> PipelineStep<T, R> mustBe(Filter<T, R> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            return new FilterStep<>(argFilter, new MessageData(callObj, message, messageArgs), false);
        }

        public static <T, R extends T> PipelineStep<T, R> mustBe(Filter<T, R> argFilter) {
            return mustBe(argFilter, null, null);
        }

        public static <T> PipelineStep<T, T> shouldBe(Filter<T, ? extends T> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            return new FilterStep<>(argFilter, new MessageData(callObj, message, messageArgs), true);
        }

        public static <T> PipelineStep<T, T> shouldBe(Filter<T, ? extends T> argFilter) {
            return shouldBe(argFilter, null, null);
        }

        public static <T, R> PipelineStep<T, R> map(Mapper<T, R> mapper) {
            return new MapStep<>(mapper);
        }

        public static <T, S extends T, R> PipelineStep<T, R> mapIf(Filter<? super T, S> filter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
            return new MapIfStep<>(filter, trueBranch, falseBranch);
        }

        public static <T, S extends T, R> PipelineStep<T, R> mapIf(Filter<? super T, S> filter, PipelineStep<?, ?> trueBranch) {
            return mapIf(filter, trueBranch, null);
        }

        public static <T> ChainBuilder<T> chain(PipelineStep<T, ?> firstStep) {
            return new ChainBuilder<>(firstStep);
        }

        public static <T> PipelineStep<T, Integer> asInteger() {
            return new CoercionStep<>(TargetType.Integer, false);
        }

        public static <T> PipelineStep<T, RAbstractIntVector> asIntegerVector() {
            return new CoercionStep<>(TargetType.Integer, true);
        }

        public static <T> PipelineStep<T, RAbstractIntVector> asIntegerVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            return new CoercionStep<>(TargetType.Integer, true, preserveNames, preserveDimensions, preserveAttributes);
        }

        public static <T> PipelineStep<T, Double> asDouble() {
            return new CoercionStep<>(TargetType.Double, false);
        }

        public static <T> PipelineStep<T, RAbstractDoubleVector> asDoubleVector() {
            return new CoercionStep<>(TargetType.Double, true);
        }

        public static <T> PipelineStep<T, RAbstractDoubleVector> asDoubleVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            return new CoercionStep<>(TargetType.Double, true, preserveNames, preserveDimensions, preserveAttributes);
        }

        public static <T> PipelineStep<T, String> asString() {
            return new CoercionStep<>(TargetType.Character, false);
        }

        public static <T> PipelineStep<T, RAbstractStringVector> asStringVector() {
            return new CoercionStep<>(TargetType.Character, true);
        }

        public static <T> PipelineStep<T, RAbstractStringVector> asStringVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            return new CoercionStep<>(TargetType.Character, true, preserveNames, preserveDimensions, preserveAttributes);
        }

        public static <T> PipelineStep<T, RAbstractComplexVector> asComplexVector() {
            return new CoercionStep<>(TargetType.Complex, true);
        }

        public static <T> PipelineStep<T, RAbstractRawVector> asRawVector() {
            return new CoercionStep<>(TargetType.Raw, true);
        }

        public static <T> PipelineStep<T, Byte> asLogical() {
            return new CoercionStep<>(TargetType.Logical, false);
        }

        public static <T> PipelineStep<T, RAbstractLogicalVector> asLogicalVector() {
            return new CoercionStep<>(TargetType.Logical, true);
        }

        public static <T> PipelineStep<T, RAbstractLogicalVector> asLogicalVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            return new CoercionStep<>(TargetType.Logical, true, preserveNames, preserveDimensions, preserveAttributes, false);
        }

        public static PipelineStep<Byte, Boolean> asBoolean() {
            return map(toBoolean());
        }

        public static <T> PipelineStep<T, RAbstractVector> asVector() {
            return new CoercionStep<>(TargetType.Any, true);
        }

        public static <T> PipelineStep<T, RAbstractVector> asVector(boolean preserveNonVector) {
            return new CoercionStep<>(TargetType.Any, true, false, false, false, preserveNonVector);
        }

        /**
         * Version of {@code findFirst} step that can be used in {@code chain}, must be followed by
         * call for {@code xyzElement()}.
         */
        public static <V extends RAbstractVector> FindFirstNodeBuilder findFirst(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            return new FindFirstNodeBuilder(callObj, message, messageArgs);
        }

        /**
         * Version of {@code findFirst} step that can be used in {@code chain}, must be followed by
         * call for {@code xyzElement()}.
         */
        public static <V extends RAbstractVector> FindFirstNodeBuilder findFirst(RError.Message message, Object... messageArgs) {
            return new FindFirstNodeBuilder(null, message, messageArgs);
        }

        /**
         * Version of {@code findFirst} step that can be used in {@code chain}, must be followed by
         * call for {@code xyzElement()}.
         */
        public static <V extends RAbstractVector> FindFirstNodeBuilder findFirst() {
            return new FindFirstNodeBuilder(null, null, null);
        }

        public static <T> PipelineStep<T, T> notNA(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            return notNA(null, callObj, message, messageArgs);
        }

        public static <T> PipelineStep<T, T> notNA(RError.Message message, Object... messageArgs) {
            return notNA(null, null, message, messageArgs);
        }

        public static <T> PipelineStep<T, T> notNA(T naReplacement, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            return new NotNAStep<>(naReplacement, new MessageData(callObj, message, messageArgs));
        }

        public static <T> PipelineStep<T, T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            return notNA(naReplacement, null, message, messageArgs);
        }

        public static <T> PipelineStep<T, T> notNA(T naReplacement) {
            return new NotNAStep<>(naReplacement, null);
        }

        public static <T> PipelineStep<T, T> notNA() {
            return new NotNAStep<>(null, null);
        }

        public static <T> CompareFilter<T> sameAs(T x) {
            return new CompareFilter<>(CompareFilter.SAME, new CompareFilter.ScalarValue(x, RType.Any));
        }

        public static <T> CompareFilter<T> equalTo(T x) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ScalarValue(x, RType.Any));
        }

        public static <T extends RAbstractVector> CompareFilter<T> notEmpty() {
            return new CompareFilter<>(CompareFilter.GT, new CompareFilter.VectorSize(0));
        }

        public static <T extends RAbstractVector> CompareFilter<T> singleElement() {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.VectorSize(1));
        }

        public static <T extends RAbstractVector> CompareFilter<T> size(int s) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.VectorSize(s));
        }

        public static <T extends RAbstractStringVector> CompareFilter<T> elementAt(int index, String value) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ElementAt(index, value, RType.Character));
        }

        public static <T extends RAbstractIntVector> CompareFilter<T> elementAt(int index, int value) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ElementAt(index, value, RType.Integer));
        }

        public static <T extends RAbstractDoubleVector> CompareFilter<T> elementAt(int index, double value) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ElementAt(index, value, RType.Double));
        }

        public static <T extends RAbstractComplexVector> CompareFilter<T> elementAt(int index, RComplex value) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ElementAt(index, value, RType.Complex));
        }

        public static <T extends RAbstractLogicalVector> CompareFilter<T> elementAt(int index, byte value) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ElementAt(index, value, RType.Logical));
        }

        public static <T extends RAbstractVector> MatrixFilter<T> matrix() {
            return MatrixFilter.isMatrixFilter();
        }

        public static <T extends RAbstractVector> MatrixFilter<T> squareMatrix() {
            return MatrixFilter.isSquareMatrixFilter();
        }

        public static <T extends RAbstractVector> CompareFilter<T> dimEq(int dim, int x) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.Dim(dim, x));
        }

        public static <T extends RAbstractVector> CompareFilter<T> dimGt(int dim, int x) {
            return new CompareFilter<>(CompareFilter.GT, new CompareFilter.Dim(dim, x));
        }

        public static CompareFilter<Byte> logicalTrue() {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ScalarValue(RRuntime.LOGICAL_TRUE, RType.Logical));
        }

        public static CompareFilter<Byte> logicalFalse() {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.ScalarValue(RRuntime.LOGICAL_FALSE, RType.Logical));
        }

        public static CompareFilter<Integer> intNA() {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.NATest(RType.Integer));
        }

        public static NotFilter<Integer> notIntNA() {
            return new NotFilter<>(intNA());
        }

        public static CompareFilter<Byte> logicalNA() {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.NATest(RType.Logical));
        }

        public static NotFilter<Byte> notLogicalNA() {
            return new NotFilter<>(logicalNA());
        }

        public static CompareFilter<Double> doubleNA() {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.NATest(RType.Double));
        }

        public static NotFilter<Double> notDoubleNA() {
            return new NotFilter<>(doubleNA());
        }

        public static CompareFilter<String> stringNA() {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.NATest(RType.Character));
        }

        public static NotFilter<String> notStringNA() {
            return new NotFilter<>(stringNA());
        }

        public static CompareFilter<RComplex> complexNA() {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.NATest(RType.Complex));
        }

        public static NotFilter<RComplex> notComplexNA() {
            return new NotFilter<>(complexNA());
        }

        public static DoubleFilter isFractional() {
            return DoubleFilter.IS_FRACTIONAL;
        }

        public static DoubleFilter isFinite() {
            return DoubleFilter.IS_FINITE;
        }

        public static CompareFilter<Integer> eq(int x) {
            return new CompareFilter<>(CompareFilter.SAME, new CompareFilter.ScalarValue(x, RType.Integer));
        }

        public static CompareFilter<Double> eq(double x) {
            return new CompareFilter<>(CompareFilter.SAME, new CompareFilter.ScalarValue(x, RType.Double));
        }

        public static CompareFilter<Byte> eq(byte x) {
            return new CompareFilter<>(CompareFilter.SAME, new CompareFilter.ScalarValue(x, RType.Logical));
        }

        public static CompareFilter<String> eq(String x) {
            return new CompareFilter<>(CompareFilter.SAME, new CompareFilter.ScalarValue(x, RType.Character));
        }

        public static NotFilter<Integer> neq(int x) {
            return new NotFilter<>(eq(x));
        }

        public static NotFilter<Double> neq(double x) {
            return new NotFilter<>(eq(x));
        }

        public static CompareFilter<Integer> gt(int x) {
            return new CompareFilter<>(CompareFilter.GT, new CompareFilter.ScalarValue(x, RType.Integer));
        }

        public static CompareFilter<Double> gt(double x) {
            return new CompareFilter<>(CompareFilter.GT, new CompareFilter.ScalarValue(x, RType.Double));
        }

        public static CompareFilter<Integer> gte(int x) {
            return new CompareFilter<>(CompareFilter.GE, new CompareFilter.ScalarValue(x, RType.Integer));
        }

        public static CompareFilter<Double> gte(double x) {
            return new CompareFilter<>(CompareFilter.GE, new CompareFilter.ScalarValue(x, RType.Double));
        }

        public static CompareFilter<Integer> lt(int x) {
            return new CompareFilter<>(CompareFilter.LT, new CompareFilter.ScalarValue(x, RType.Integer));
        }

        public static CompareFilter<Double> lt(double x) {
            return new CompareFilter<>(CompareFilter.LT, new CompareFilter.ScalarValue(x, RType.Double));
        }

        public static CompareFilter<Integer> lte(int x) {
            return new CompareFilter<>(CompareFilter.LE, new CompareFilter.ScalarValue(x, RType.Integer));
        }

        public static CompareFilter<Double> lte(double x) {
            return new CompareFilter<>(CompareFilter.LE, new CompareFilter.ScalarValue(x, RType.Double));
        }

        public static CompareFilter<String> length(int l) {
            return new CompareFilter<>(CompareFilter.EQ, new CompareFilter.StringLength(l));
        }

        public static CompareFilter<String> isEmpty() {
            return length(0);
        }

        public static CompareFilter<String> lengthGt(int l) {
            return new CompareFilter<>(CompareFilter.GT, new CompareFilter.StringLength(l));
        }

        public static CompareFilter<String> lengthGte(int l) {
            return new CompareFilter<>(CompareFilter.GE, new CompareFilter.StringLength(l));
        }

        public static CompareFilter<String> lengthLt(int l) {
            return new CompareFilter<>(CompareFilter.LT, new CompareFilter.StringLength(l));
        }

        public static CompareFilter<String> lengthLte(int l) {
            return new CompareFilter<>(CompareFilter.LE, new CompareFilter.StringLength(l));
        }

        public static CompareFilter<Integer> gt0() {
            return gt(0);
        }

        public static CompareFilter<Integer> gte0() {
            return gte(0);
        }

        public static CompareFilter<Integer> gt1() {
            return gt(1);
        }

        public static CompareFilter<Integer> gte1() {
            return gte(1);
        }

        public static <R> TypeFilter<Object, R> instanceOf(Class<R> cls) {
            return new TypeFilter<>(x -> cls.isInstance(x), cls);
        }

        public static <R extends RAbstractIntVector> Filter<Object, R> integerValue() {
            return new RTypeFilter<>(RType.Integer);
        }

        public static <R extends RAbstractStringVector> Filter<Object, R> stringValue() {
            return new RTypeFilter<>(RType.Character);
        }

        public static <R extends RAbstractDoubleVector> Filter<Object, R> doubleValue() {
            return new RTypeFilter<>(RType.Double);
        }

        public static <R extends RAbstractLogicalVector> Filter<Object, R> logicalValue() {
            return new RTypeFilter<>(RType.Logical);
        }

        public static <R extends RAbstractComplexVector> Filter<Object, R> complexValue() {
            return new RTypeFilter<>(RType.Complex);
        }

        public static <R extends RAbstractRawVector> Filter<Object, R> rawValue() {
            return new RTypeFilter<>(RType.Raw);
        }

        public static <R> TypeFilter<Object, R> anyValue() {
            return new TypeFilter<>(x -> true, Object.class);
        }

        @SuppressWarnings({"rawtypes", "unchecked", "cast"})
        public static Filter<Object, RAbstractVector> numericValue() {
            Filter f = integerValue().or(doubleValue()).or(logicalValue());
            return (Filter<Object, RAbstractVector>) f;
        }

        /**
         * Checks that the argument is a list or vector/scalar of type numeric, string, complex or
         * raw.
         */
        @SuppressWarnings({"rawtypes", "unchecked", "cast"})
        public static Filter<Object, RAbstractVector> abstractVectorValue() {
            Filter f = numericValue().or(stringValue()).or(complexValue()).or(rawValue()).or(instanceOf(RAbstractListVector.class));
            return (Filter<Object, RAbstractVector>) f;
        }

        /**
         * @deprecated tests for scalar types are dangerous
         */
        @Deprecated
        public static Filter<Object, Integer> scalarIntegerValue() {
            return new TypeFilter<>(x -> x instanceof String, String.class);
        }

        public static Filter<Object, Integer> atomicIntegerValue() {
            return new TypeFilter<>(x -> x instanceof String, String.class);
        }

        /**
         * @deprecated tests for scalar types are dangerous
         */
        @Deprecated
        public static Filter<Object, Double> scalarDoubleValue() {
            return new TypeFilter<>(x -> x instanceof Double, Double.class);
        }

        /**
         * @deprecated tests for scalar types are dangerous
         */
        @Deprecated
        public static Filter<Object, Byte> scalarLogicalValue() {
            return new TypeFilter<>(x -> x instanceof Byte, Byte.class);
        }

        public static Filter<Object, Byte> atomicLogicalValue() {
            return new TypeFilter<>(x -> x instanceof Byte, Byte.class);
        }

        /**
         * @deprecated tests for scalar types are dangerous
         */
        @Deprecated
        public static Filter<Object, RComplex> scalarComplexValue() {
            return new TypeFilter<>(x -> x instanceof RComplex, RComplex.class);
        }

        public static MapByteToBoolean toBoolean() {
            return MapByteToBoolean.INSTANCE;
        }

        public static MapDoubleToInt doubleToInt() {
            return MapDoubleToInt.INSTANCE;
        }

        public static MapToCharAt charAt0(int defaultValue) {
            return new MapToCharAt(0, defaultValue);
        }

        public static <T> MapToValue<T, RNull> nullConstant() {
            return new MapToValue<>(RNull.instance);
        }

        public static <T> MapToValue<T, RMissing> missingConstant() {
            return new MapToValue<>(RMissing.instance);
        }

        public static <T> MapToValue<T, String> constant(String s) {
            return new MapToValue<>(s);
        }

        public static <T> MapToValue<T, Integer> constant(int i) {
            return new MapToValue<>(i);
        }

        public static <T> MapToValue<T, Double> constant(double d) {
            return new MapToValue<>(d);
        }

        public static <T> MapToValue<T, Byte> constant(byte l) {
            return new MapToValue<>(l);
        }

        public static <T> MapToValue<T, RIntVector> emptyIntegerVector() {
            return new MapToValue<>(RDataFactory.createEmptyIntVector());
        }

        public static <T> MapToValue<T, RDoubleVector> emptyDoubleVector() {
            return new MapToValue<>(RDataFactory.createEmptyDoubleVector());
        }

        public static <T> MapToValue<T, RLogicalVector> emptyLogicalVector() {
            return new MapToValue<>(RDataFactory.createEmptyLogicalVector());
        }

        public static <T> MapToValue<T, RComplexVector> emptyComplexVector() {
            return new MapToValue<>(RDataFactory.createEmptyComplexVector());
        }

        public static <T> MapToValue<T, RStringVector> emptyStringVector() {
            return new MapToValue<>(RDataFactory.createEmptyStringVector());
        }

        public static <T> MapToValue<T, RList> emptyList() {
            return new MapToValue<>(RDataFactory.createList());
        }

    }

    @SuppressWarnings("unchecked")
    public static class ArgCastBuilder<T, THIS> {

        private final PipelineBuilder builder;

        public ArgCastBuilder(PipelineBuilder builder) {
            this.builder = builder;
        }

        public PipelineBuilder pipelineBuilder() {
            return builder;
        }

        public THIS defaultError(RBaseNode callObj, RError.Message message, Object... args) {
            pipelineBuilder().appendDefaultErrorStep(callObj, message, args);
            return (THIS) this;
        }

        public THIS defaultError(RError.Message message, Object... args) {
            defaultError(null, message, args);
            return (THIS) this;
        }

        public THIS defaultWarning(RBaseNode callObj, RError.Message message, Object... args) {
            pipelineBuilder().appendDefaultWarningStep(callObj, message, args);
            return (THIS) this;
        }

        public THIS shouldBe(Filter<? super T, ?> argFilter, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendShouldBeStep(argFilter, message, messageArgs);
            return (THIS) this;
        }

        public THIS shouldBe(Filter<? super T, ?> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendShouldBeStep(argFilter, callObj, message, messageArgs);
            return (THIS) this;
        }

        public THIS shouldBe(Filter<? super T, ?> argFilter) {
            pipelineBuilder().appendShouldBeStep(argFilter, null, null, null);
            return (THIS) this;
        }

        /**
         * Custom nodes in cast pipeline block optimisations and analysis, use them sparsely.
         */
        public THIS customNode(Supplier<CastNode> castNodeFactory) {
            pipelineBuilder().append(new CustomNodeStep<>(castNodeFactory));
            return (THIS) this;
        }

        public <R, THAT extends ArgCastBuilder<R, THAT>> THAT alias(Function<THIS, THAT> aliaser) {
            return aliaser.apply((THIS) this);
        }

    }

    public static class InitialPhaseBuilder<T> extends ArgCastBuilder<T, InitialPhaseBuilder<T>> {

        public InitialPhaseBuilder(PipelineBuilder builder) {
            super(builder);
        }

        public <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendMustBeStep(argFilter, callObj, message, messageArgs);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
            return mustBe(argFilter, null, message, messageArgs);
        }

        public <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter) {
            return mustBe(argFilter, null, null, (Object[]) null);
        }

        public <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            mustBe(Predef.instanceOf(cls), callObj, message, messageArgs);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            mustBe(Predef.instanceOf(cls), message, messageArgs);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls) {
            mustBe(Predef.instanceOf(cls));
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            shouldBe(Predef.instanceOf(cls), callObj, message, messageArgs);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            shouldBe(Predef.instanceOf(cls), message, messageArgs);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls) {
            shouldBe(Predef.instanceOf(cls));
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S> InitialPhaseBuilder<S> map(Mapper<T, S> mapFn) {
            pipelineBuilder().appendMap(mapFn);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
            pipelineBuilder().appendMapIf(argFilter, trueBranchMapper);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
            pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch) {
            pipelineBuilder().appendMapIf(argFilter, trueBranch);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
            pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch);
            return new InitialPhaseBuilder<>(pipelineBuilder());
        }

        public InitialPhaseBuilder<T> notNA(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendNotNA(null, callObj, message, messageArgs);
            return this;
        }

        public InitialPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendNotNA(null, null, message, messageArgs);
            return this;
        }

        public InitialPhaseBuilder<T> notNA(T naReplacement, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendNotNA(naReplacement, callObj, message, messageArgs);
            return this;
        }

        public InitialPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendNotNA(naReplacement, null, message, messageArgs);
            return this;
        }

        public InitialPhaseBuilder<T> notNA(T naReplacement) {
            pipelineBuilder().appendNotNA(naReplacement, null, null, null);
            return this;
        }

        /**
         * This method should be used as a step in pipeline, not as an argument to {@code mustBe}.
         * Example: {@code casts.arg("x").notNA()}.
         */
        public InitialPhaseBuilder<T> notNA() {
            pipelineBuilder().appendNotNA(null, null, null, null);
            return this;
        }

        public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            pipelineBuilder().appendAsIntegerVector(preserveNames, dimensionsPreservation, attrPreservation);
            return new CoercedPhaseBuilder<>(pipelineBuilder(), Integer.class);
        }

        public CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector() {
            return asIntegerVector(false, false, false);
        }

        public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            pipelineBuilder().appendAsDoubleVector(preserveNames, dimensionsPreservation, attrPreservation);
            return new CoercedPhaseBuilder<>(pipelineBuilder(), Double.class);
        }

        public CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector() {
            return asDoubleVector(false, false, false);
        }

        public CoercedPhaseBuilder<RAbstractDoubleVector, Byte> asLogicalVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            pipelineBuilder().appendAsLogicalVector(preserveNames, dimensionsPreservation, attrPreservation);
            return new CoercedPhaseBuilder<>(pipelineBuilder(), Byte.class);
        }

        public CoercedPhaseBuilder<RAbstractDoubleVector, Byte> asLogicalVector() {
            return asLogicalVector(false, false, false);
        }

        public CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            pipelineBuilder().appendAsStringVector(preserveNames, dimensionsPreservation, attrPreservation);
            return new CoercedPhaseBuilder<>(pipelineBuilder(), String.class);
        }

        public CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector() {
            pipelineBuilder().appendAsStringVector();
            return new CoercedPhaseBuilder<>(pipelineBuilder(), String.class);
        }

        public CoercedPhaseBuilder<RAbstractComplexVector, RComplex> asComplexVector() {
            pipelineBuilder().appendAsComplexVector();
            return new CoercedPhaseBuilder<>(pipelineBuilder(), RComplex.class);
        }

        public CoercedPhaseBuilder<RAbstractRawVector, RRaw> asRawVector() {
            pipelineBuilder().appendAsRawVector();
            return new CoercedPhaseBuilder<>(pipelineBuilder(), RRaw.class);
        }

        public CoercedPhaseBuilder<RAbstractVector, Object> asVector() {
            // TODO: asVector() sets preserveNonVector to false, which is against intended semantics
            // of cast nodes that always forward RNull, we need to revise all calls to this method
            // and remove the preserveNonVector option of CastToVectorNode
            pipelineBuilder().appendAsVector();
            return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
        }

        public CoercedPhaseBuilder<RAbstractVector, Object> asVector(boolean preserveNonVector) {
            pipelineBuilder().appendAsVector(false, false, false, preserveNonVector);
            return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
        }

        public CoercedPhaseBuilder<RAbstractVector, Object> asVectorPreserveAttrs(boolean preserveNonVector) {
            pipelineBuilder().appendAsVector(false, false, true, false);
            return new CoercedPhaseBuilder<>(pipelineBuilder(), Object.class);
        }

        public HeadPhaseBuilder<RAttributable> asAttributable(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            pipelineBuilder().appendAsAttributable(preserveNames, dimensionsPreservation, attrPreservation);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

    }

    public static final class PreinitialPhaseBuilder<T> extends InitialPhaseBuilder<T> {

        public PreinitialPhaseBuilder(PipelineBuilder pipelineBuilder) {
            super(pipelineBuilder);
        }

        public PreinitialPhaseBuilder<T> conf(Consumer<PipelineConfigBuilder> cfgLambda) {
            cfgLambda.accept(pipelineBuilder().pcb);
            return this;
        }

        public InitialPhaseBuilder<T> allowNull() {
            return conf(c -> c.allowNull());
        }

        public InitialPhaseBuilder<T> mustNotBeNull() {
            return conf(c -> c.mustNotBeNull(null, null, (Object[]) null));
        }

        public InitialPhaseBuilder<T> mustNotBeNull(RError.Message errorMsg, Object... msgArgs) {
            return conf(c -> c.mustNotBeNull(null, errorMsg, msgArgs));
        }

        public InitialPhaseBuilder<T> mustNotBeNull(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
            return conf(c -> c.mustNotBeNull(callObj, errorMsg, msgArgs));
        }

        public InitialPhaseBuilder<T> mapNull(Mapper<? super RNull, ?> mapper) {
            return conf(c -> c.mapNull(mapper));
        }

        public InitialPhaseBuilder<T> allowMissing() {
            return conf(c -> c.allowMissing());
        }

        public InitialPhaseBuilder<T> mustNotBeMissing() {
            return conf(c -> c.mustNotBeMissing(null, null, (Object[]) null));
        }

        public InitialPhaseBuilder<T> mustNotBeMissing(RError.Message errorMsg, Object... msgArgs) {
            return conf(c -> c.mustNotBeMissing(null, errorMsg, msgArgs));
        }

        public InitialPhaseBuilder<T> mustNotBeMissing(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
            return conf(c -> c.mustNotBeMissing(callObj, errorMsg, msgArgs));
        }

        public InitialPhaseBuilder<T> mapMissing(Mapper<? super RMissing, ?> mapper) {
            return conf(c -> c.mapMissing(mapper));
        }

        public InitialPhaseBuilder<T> allowNullAndMissing() {
            return conf(c -> c.allowMissing().allowNull());
        }

        @Override
        public PreinitialPhaseBuilder<T> defaultError(RBaseNode callObj, RError.Message message, Object... args) {
            pipelineBuilder().getPipelineConfig().setDefaultError(new MessageData(callObj, message, args));
            pipelineBuilder().appendDefaultErrorStep(callObj, message, args);
            return this;
        }

        @Override
        public PreinitialPhaseBuilder<T> defaultError(Message message, Object... args) {
            defaultError(null, message, args);
            return this;
        }

        @Override
        public PreinitialPhaseBuilder<T> defaultWarning(RBaseNode callObj, RError.Message message, Object... args) {
            pipelineBuilder().getPipelineConfig().setDefaultWarning(new MessageData(callObj, message, args));
            pipelineBuilder().appendDefaultWarningStep(callObj, message, args);
            return this;
        }
    }

    public static final class CoercedPhaseBuilder<T extends RAbstractVector, S> extends ArgCastBuilder<T, CoercedPhaseBuilder<T, S>> {

        private final Class<?> elementClass;

        public CoercedPhaseBuilder(PipelineBuilder builder, Class<?> elementClass) {
            super(builder);
            this.elementClass = elementClass;
        }

        /**
         * The inserted cast node returns the default value if the input vector is empty. It also
         * reports the warning message.
         */
        public HeadPhaseBuilder<S> findFirst(S defaultValue, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendFindFirst(defaultValue, elementClass, null, message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public HeadPhaseBuilder<S> findFirst(S defaultValue, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendFindFirst(defaultValue, elementClass, callObj, message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        /**
         * The inserted cast node raises an error if the input vector is empty.
         */
        public HeadPhaseBuilder<S> findFirst(RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendFindFirst(null, elementClass, null, message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public HeadPhaseBuilder<S> findFirst(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendFindFirst(null, elementClass, callObj, message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        /**
         * The inserted cast node raises the public error, if defined, or RError.Message.LENGTH_ZERO
         * error if the input vector is empty.
         */
        public HeadPhaseBuilder<S> findFirst() {
            pipelineBuilder().appendFindFirst(null, elementClass, null, null, null);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        /**
         * The inserted cast node returns the default value if the input vector is empty. It reports
         * no warning message.
         */
        public HeadPhaseBuilder<S> findFirst(S defaultValue) {
            assert defaultValue != null : "defaultValue cannot be null";
            pipelineBuilder().appendFindFirst(defaultValue, elementClass, null, null, null);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendMustBeStep(argFilter, callObj, message, messageArgs);
            return this;
        }

        public CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendMustBeStep(argFilter, null, message, messageArgs);
            return this;
        }

        public CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter) {
            return mustBe(argFilter, null, null, (Object[]) null);
        }
    }

    public static final class HeadPhaseBuilder<T> extends ArgCastBuilder<T, HeadPhaseBuilder<T>> {

        public HeadPhaseBuilder(PipelineBuilder builder) {
            super(builder);
        }

        public <S> HeadPhaseBuilder<S> map(Mapper<T, S> mapFn) {
            // state().castBuilder().insert(state().index(), () -> MapNode.create(mapFn));
            pipelineBuilder().appendMap(mapFn);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
            pipelineBuilder().appendMapIf(argFilter, trueBranchMapper);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
            pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<S, ?> trueBranch) {
            pipelineBuilder().appendMapIf(argFilter, trueBranch);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<S, R> trueBranch, PipelineStep<T, ?> falseBranch) {
            pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendMustBeStep(argFilter, callObj, message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendMustBeStep(argFilter, null, message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter) {
            return mustBe(argFilter, null, null, (Object[]) null);
        }

        public <S extends T> HeadPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            mustBe(Predef.instanceOf(cls), message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S extends T> HeadPhaseBuilder<S> mustBe(Class<S> cls) {
            mustBe(Predef.instanceOf(cls));
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            shouldBe(Predef.instanceOf(cls), callObj, message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            shouldBe(Predef.instanceOf(cls), message, messageArgs);
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls) {
            shouldBe(Predef.instanceOf(cls));
            return new HeadPhaseBuilder<>(pipelineBuilder());
        }

        public HeadPhaseBuilder<T> notNA(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendNotNA(null, callObj, message, messageArgs);
            return this;
        }

        public HeadPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendNotNA(null, null, message, messageArgs);
            return this;
        }

        public HeadPhaseBuilder<T> notNA(T naReplacement, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendNotNA(naReplacement, callObj, message, messageArgs);
            return this;
        }

        public HeadPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendNotNA(naReplacement, null, message, messageArgs);
            return this;
        }

        public HeadPhaseBuilder<T> notNA() {
            pipelineBuilder().appendNotNA(null, null, null, null);
            return this;
        }

        public HeadPhaseBuilder<T> notNA(T naReplacement) {
            pipelineBuilder().appendNotNA(naReplacement, null, null, null);
            return this;
        }

    }

    public static final class ChainBuilder<T> {
        private final PipelineStep<?, ?> firstStep;
        private PipelineStep<?, ?> lastStep;

        private ChainBuilder(PipelineStep<?, ?> firstStep) {
            assert firstStep != null : "firstStep must not be null";
            this.firstStep = firstStep;
            lastStep = firstStep;
        }

        private void addStep(PipelineStep<?, ?> nextStep) {
            lastStep.setNext(nextStep);
            lastStep = nextStep;
        }

        @SuppressWarnings("overloads")
        public ChainBuilder<T> with(PipelineStep<?, ?> nextStep) {
            addStep(nextStep);
            return this;
        }

        @SuppressWarnings("overloads")
        public ChainBuilder<T> with(Mapper<?, ?> mapper) {
            addStep(new MapStep<>(mapper));
            return this;
        }

        public PipelineStep<?, ?> end() {
            return firstStep;
        }

    }

    /**
     * Allows to convert find first into a valid step when used in {@code chain}, for example
     * {@code chain(findFirst().stringElement())}.
     */
    public static final class FindFirstNodeBuilder {
        private final RBaseNode callObj;
        private final Message message;
        private final Object[] messageArgs;

        private FindFirstNodeBuilder(RBaseNode callObj, Message message, Object[] messageArgs) {
            this.callObj = callObj;
            this.message = message;
            this.messageArgs = messageArgs;
        }

        private <V, E> PipelineStep<V, E> create(Class<?> elementClass, Object defaultValue) {
            return new FindFirstStep<>(defaultValue, elementClass, new MessageData(callObj, message, messageArgs));
        }

        public PipelineStep<RAbstractLogicalVector, Byte> logicalElement() {
            return create(Byte.class, null);
        }

        public PipelineStep<RAbstractLogicalVector, Byte> logicalElement(byte defaultValue) {
            return create(Byte.class, defaultValue);
        }

        public PipelineStep<RAbstractDoubleVector, Double> doubleElement() {
            return create(Double.class, null);
        }

        public PipelineStep<RAbstractDoubleVector, Double> doubleElement(double defaultValue) {
            return create(Double.class, defaultValue);
        }

        public PipelineStep<RAbstractIntVector, Integer> integerElement() {
            return create(Integer.class, null);
        }

        public PipelineStep<RAbstractIntVector, Integer> integerElement(int defaultValue) {
            return create(Integer.class, defaultValue);
        }

        public PipelineStep<RAbstractStringVector, String> stringElement() {
            return create(String.class, null);
        }

        public PipelineStep<RAbstractStringVector, String> stringElement(String defaultValue) {
            return create(String.class, defaultValue);
        }

        public PipelineStep<RAbstractComplexVector, RComplex> complexElement() {
            return create(String.class, null);
        }

        public PipelineStep<RAbstractComplexVector, RComplex> complexElement(RComplex defaultValue) {
            return create(String.class, defaultValue);
        }

        public PipelineStep<RAbstractVector, Object> objectElement() {
            return create(Object.class, null);
        }

        public PipelineStep<RAbstractVector, Object> objectElement(Object defaultValue) {
            return create(Object.class, defaultValue);
        }

    }

    public static final class PipelineConfigBuilder {

        // TODO: move the "internal" methods into a child class

        private static ArgumentFilterFactory filterFactory = ArgumentFilterFactoryImpl.INSTANCE;
        private static ArgumentMapperFactory mapperFactory = ArgumentMapperFactoryImpl.INSTANCE;

        private final String argumentName;
        private MessageData defaultError;
        private MessageData defaultWarning;

        private Mapper<? super RMissing, ?> missingMapper = null;
        private Mapper<? super RNull, ?> nullMapper = null;
        private MessageData missingMsg;
        private MessageData nullMsg;

        // Note: to be removed with legacy API
        private boolean wasLegacyAsVectorCall = false;

        public PipelineConfigBuilder(String argumentName) {
            defaultError = new MessageData(null, RError.Message.INVALID_ARGUMENT, argumentName);
            defaultWarning = defaultError;
            this.argumentName = argumentName;
        }

        public void setWasLegacyAsVectorCall() {
            wasLegacyAsVectorCall = true;
        }

        public boolean wasLegacyAsVectorCall() {
            return wasLegacyAsVectorCall;
        }

        public String getArgumentName() {
            return argumentName;
        }

        public void setDefaultError(MessageData defaultError) {
            this.defaultError = defaultError;
        }

        /**
         * The preinitialization phase where one can configure RNull and RMissing handling may also
         * define default error, in such case it is remembered the {@link PipelineConfigBuilder} and
         * also set default error step is added to the pipeline.The default error saved here is used
         * for RNull and RMissing handling. It may be null, if no default error was set explicitly.
         */
        public MessageData getDefaultError() {
            return defaultError;
        }

        public void setDefaultWarning(MessageData defaultWarning) {
            this.defaultWarning = defaultWarning;
        }

        /**
         * The same as in {@link #getDefaultError()} applies for default warning.
         */
        public MessageData getDefaultWarning() {
            return defaultWarning;
        }

        /**
         * Default message that should be used when no explicit default error/warning was set. For
         * the time being this is not configurable.
         */
        public MessageData getDefaultDefaultMessage() {
            return new MessageData(null, RError.Message.INVALID_ARGUMENT, argumentName);
        }

        public Mapper<? super RMissing, ?> getMissingMapper() {
            return missingMapper;
        }

        public Mapper<? super RNull, ?> getNullMapper() {
            return nullMapper;
        }

        public MessageData getMissingMessage() {
            return missingMsg;
        }

        public MessageData getNullMessage() {
            return nullMsg;
        }

        public PipelineConfigBuilder mustNotBeMissing(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
            missingMapper = null;
            missingMsg = new MessageData(callObj, errorMsg, msgArgs);
            return this;
        }

        public PipelineConfigBuilder mapMissing(Mapper<? super RMissing, ?> mapper) {
            missingMapper = mapper;
            missingMsg = null;
            return this;
        }

        public PipelineConfigBuilder mapMissing(Mapper<? super RMissing, ?> mapper, RBaseNode callObj, RError.Message warningMsg, Object... msgArgs) {
            missingMapper = mapper;
            missingMsg = new MessageData(callObj, warningMsg, msgArgs);
            return this;
        }

        public PipelineConfigBuilder allowMissing() {
            return mapMissing(Predef.missingConstant());
        }

        public PipelineConfigBuilder allowMissing(RBaseNode callObj, RError.Message warningMsg, Object... msgArgs) {
            return mapMissing(Predef.missingConstant(), callObj, warningMsg, msgArgs);
        }

        public PipelineConfigBuilder mustNotBeNull(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
            nullMapper = null;
            nullMsg = new MessageData(callObj, errorMsg, msgArgs);
            return this;
        }

        public PipelineConfigBuilder mapNull(Mapper<? super RNull, ?> mapper) {
            nullMapper = mapper;
            nullMsg = null;
            return this;
        }

        public PipelineConfigBuilder mapNull(Mapper<? super RNull, ?> mapper, RBaseNode callObj, RError.Message warningMsg, Object... msgArgs) {
            nullMapper = mapper;
            nullMsg = new MessageData(callObj, warningMsg, msgArgs);
            return this;
        }

        public PipelineConfigBuilder allowNull() {
            return mapNull(Predef.nullConstant());
        }

        public PipelineConfigBuilder allowNull(RBaseNode callObj, RError.Message warningMsg, Object... msgArgs) {
            return mapNull(Predef.nullConstant(), callObj, warningMsg, msgArgs);
        }

        public static ArgumentFilterFactory getFilterFactory() {
            return filterFactory;
        }

        public static ArgumentMapperFactory getMapperFactory() {
            return mapperFactory;
        }

        public static void setFilterFactory(ArgumentFilterFactory ff) {
            filterFactory = ff;
        }

        public static void setMapperFactory(ArgumentMapperFactory mf) {
            mapperFactory = mf;
        }
    }

    /**
     * Class that holds the data for a pipeline for a single parameter. It holds the cast pipeline
     * steps created so far and some data related to the selected argument for which we are creating
     * the pipeline.
     */
    public static final class PipelineBuilder {

        private final PipelineConfigBuilder pcb;
        private ChainBuilder<?> chainBuilder;

        PipelineBuilder(PipelineConfigBuilder pcb) {
            this.pcb = pcb;
        }

        private void append(PipelineStep<?, ?> step) {
            if (chainBuilder == null) {
                chainBuilder = new ChainBuilder<>(step);
            } else {
                chainBuilder.addStep(step);
            }
        }

        public PipelineConfigBuilder getPipelineConfig() {
            return pcb;
        }

        public void appendFindFirst(Object defaultValue, Class<?> elementClass, RBaseNode callObj, Message message, Object[] messageArgs) {
            append(new FindFirstStep<>(defaultValue, elementClass, createMessage(callObj, message, messageArgs)));
        }

        public void appendAsAttributable(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            append(new CoercionStep<>(TargetType.Attributable, false, preserveNames, dimensionsPreservation, attrPreservation));
        }

        public void appendAsVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean preserveNonVector) {
            append(new CoercionStep<>(TargetType.Any, true, preserveNames, preserveDimensions, preserveAttributes, preserveNonVector));
        }

        public void appendAsVector() {
            appendAsVector(false, false, false, false);
        }

        public void appendAsRawVector() {
            append(new CoercionStep<>(TargetType.Raw, true, false, false, false));
        }

        public void appendAsComplexVector() {
            append(new CoercionStep<>(TargetType.Complex, true, false, false, false));
        }

        public void appendAsStringVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            append(new CoercionStep<>(TargetType.Character, true, preserveNames, dimensionsPreservation, attrPreservation));
        }

        public void appendAsStringVector() {
            append(new CoercionStep<>(TargetType.Character, true, false, false, false));
        }

        public void appendAsLogicalVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            append(new CoercionStep<>(TargetType.Logical, true, preserveNames, dimensionsPreservation, attrPreservation));
        }

        public void appendAsDoubleVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            append(new CoercionStep<>(TargetType.Double, true, preserveNames, dimensionsPreservation, attrPreservation));
        }

        public void appendAsIntegerVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            append(new CoercionStep<>(TargetType.Integer, true, preserveNames, dimensionsPreservation, attrPreservation));
        }

        public void appendNotNA(Object naReplacement, RBaseNode callObj, Message message, Object[] messageArgs) {
            append(new NotNAStep<>(naReplacement, createMessage(callObj, message, messageArgs)));
        }

        public void appendMapIf(Filter<?, ?> argFilter, Mapper<?, ?> trueBranchMapper) {
            appendMapIf(argFilter, trueBranchMapper, null);
        }

        public void appendMapIf(Filter<?, ?> argFilter, Mapper<?, ?> trueBranchMapper, Mapper<?, ?> falseBranchMapper) {
            appendMapIf(argFilter, new MapStep<>(trueBranchMapper), falseBranchMapper == null ? null : new MapStep<>(falseBranchMapper));
        }

        public void appendMapIf(Filter<?, ?> argFilter, PipelineStep<?, ?> trueBranch) {
            appendMapIf(argFilter, trueBranch, null);
        }

        public void appendMapIf(Filter<?, ?> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
            append(new MapIfStep<>(argFilter, trueBranch, falseBranch));
        }

        public void appendMap(Mapper<?, ?> mapFn) {
            append(new MapStep<>(mapFn));
        }

        public void appendMustBeStep(Filter<?, ?> argFilter, RBaseNode callObj, Message message, Object[] messageArgs) {
            append(new FilterStep<>(argFilter, createMessage(callObj, message, messageArgs), false));
        }

        public void appendShouldBeStep(Filter<?, ?> argFilter, Message message, Object[] messageArgs) {
            appendShouldBeStep(argFilter, null, message, messageArgs);
        }

        public void appendShouldBeStep(Filter<?, ?> argFilter, RBaseNode callObj, Message message, Object[] messageArgs) {
            append(new FilterStep<>(argFilter, createMessage(callObj, message, messageArgs), true));
        }

        public void appendDefaultWarningStep(RBaseNode callObj, Message message, Object[] args) {
            append(new DefaultWarningStep<>(createMessage(callObj, message, args)));
        }

        public void appendDefaultErrorStep(RBaseNode callObj, Message message, Object[] args) {
            append(new DefaultErrorStep<>(createMessage(callObj, message, args)));
        }

        private MessageData createMessage(RBaseNode callObj, Message message, Object[] messageArgs) {
            return message == null ? null : new MessageData(callObj, message, messageArgs);
        }
    }
}
