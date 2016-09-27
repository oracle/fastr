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
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.ChainBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.FindFirstNodeBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.InitialPhaseBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineConfigBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PreinitialPhaseBuilder;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
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
                    castsCache[i] = PipelineToCastNode.convert(arg.getPipelineConfig().build(), arg.getFirstStep());
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

    /**
     * Returns a builder of a cast pipeline for given parameter.
     * <p>
     * In the pre-initial phase, the pipeline can be configured using
     * {@link PreinitialPhaseBuilder#conf(Consumer)} or any other method of the
     * {@link PreinitialPhaseBuilder} class, e.g. {@link PreinitialPhaseBuilder#allowNull()}. The
     * default configuration is that {@code RNull} and {@code RMissing} values are not allowed and
     * will cause default error (which can be configured). However, if there is a 'find first' step
     * in the pipeline with a default value, then {@code RNull} and {@code RMissing} are passed to
     * that step, which maps them to the default value and executes any other following steps. If
     * {@code RNull} or {@code RMissing} are explicitly allowed, they will bypass the whole
     * pipeline. In such case, they can be also mapped to some other value (which will also bypass
     * the whole pipeline). In either case, {@code RNull} and {@code RMissing} can be ignored when
     * constructing the rest of the pipeline.
     * </p>
     * <p>
     * In the initial phase, the pipeline can filter or cast the argument, which can narrow down the
     * type of the argument and the API will be restricted to adding only pipeline steps that match
     * the type, this is coerced phase.
     * </p>
     * <p>
     * The coerced phase can be followed by head phase once a {@code findFirst} step is used. In
     * this phase the argument type is narrowed down to a scalar value.
     * </p>
     * <p>
     * During any phase, one can add filter and mapper steps. The methods creating such steps, e.g.
     * {@link InitialPhaseBuilder#mustBe(Filter)}, usually take {@link Filter} or {@link Mapper}
     * instance. Use convenient static methods in the {@link Predef} class to construct these
     * instances.
     * </p>
     * <p>
     * Notable is the {@code mapIf} step, which allows to split the pipeline into two eventualities
     * depending on the filter condition. The second and third argument, namely
     * {@code trueBranchMapper} and {@code falseBranchMapper}, can be simple mappers, e.g.
     * {@link Predef#toBoolean()} or one can construct more complex mapping using
     * {@link Predef#chain(PipelineStep)} invocation followed by {@code with(step)} calls and
     * finished by {@code end()} invocation. The steps can be constructed using convenient methods
     * in the {@link Predef} class. For technical reasons, when using 'find first' step by means of
     * {@link Predef#findFirst()} in this situation, it must be followed by call to
     * {@link FindFirstNodeBuilder#integerElement()} or other similar method corresponding to the
     * expected element type.
     * </p>
     */
    public PreinitialPhaseBuilder<Object> arg(String argumentName) {
        assert builtinNode != null : "arg(String) is only supported for builtins cast pipelines";
        return new PreinitialPhaseBuilder<>(getBuilder(getArgumentIndex(argumentName), argumentName));
    }

    /**
     * @see #arg(String)
     */
    public PreinitialPhaseBuilder<Object> arg(int argumentIndex, String argumentName) {
        assert builtinNode != null : "arg(int, String) is only supported for builtins cast pipelines";
        assert argumentIndex >= 0 && argumentIndex < argumentBuilders.length : "argument index out of range";
        assert argumentNames[argumentIndex].equals(argumentName) : "wrong argument name " + argumentName;
        return new PreinitialPhaseBuilder<>(getBuilder(argumentIndex, argumentName));
    }

    /**
     * @see #arg(String)
     */
    public PreinitialPhaseBuilder<Object> arg(int argumentIndex) {
        boolean existingIndex = argumentNames != null && argumentIndex >= 0 && argumentIndex < argumentNames.length;
        String name = existingIndex ? argumentNames[argumentIndex] : null;
        return new PreinitialPhaseBuilder<>(getBuilder(argumentIndex, name));
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
            return new CoercionStep<>(RType.Integer, false);
        }

        public static <T> PipelineStep<T, RAbstractIntVector> asIntegerVector() {
            return new CoercionStep<>(RType.Integer, true);
        }

        public static <T> PipelineStep<T, RAbstractIntVector> asIntegerVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            return new CoercionStep<>(RType.Integer, true, preserveNames, preserveDimensions, preserveAttributes);
        }

        public static <T> PipelineStep<T, Double> asDouble() {
            return new CoercionStep<>(RType.Double, false);
        }

        public static <T> PipelineStep<T, RAbstractDoubleVector> asDoubleVector() {
            return new CoercionStep<>(RType.Double, true);
        }

        public static <T> PipelineStep<T, RAbstractDoubleVector> asDoubleVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            return new CoercionStep<>(RType.Double, true, preserveNames, preserveDimensions, preserveAttributes);
        }

        public static <T> PipelineStep<T, String> asString() {
            return new CoercionStep<>(RType.Character, false);
        }

        public static <T> PipelineStep<T, RAbstractStringVector> asStringVector() {
            return new CoercionStep<>(RType.Character, true);
        }

        public static <T> PipelineStep<T, RAbstractStringVector> asStringVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            return new CoercionStep<>(RType.Character, true, preserveNames, preserveDimensions, preserveAttributes);
        }

        public static <T> PipelineStep<T, RAbstractComplexVector> asComplexVector() {
            return new CoercionStep<>(RType.Complex, true);
        }

        public static <T> PipelineStep<T, RAbstractRawVector> asRawVector() {
            return new CoercionStep<>(RType.Raw, true);
        }

        public static <T> PipelineStep<T, Byte> asLogical() {
            return new CoercionStep<>(RType.Logical, false);
        }

        public static <T> PipelineStep<T, RAbstractLogicalVector> asLogicalVector() {
            return new CoercionStep<>(RType.Logical, true);
        }

        public static <T> PipelineStep<T, RAbstractLogicalVector> asLogicalVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            return new CoercionStep<>(RType.Logical, true, preserveNames, preserveDimensions, preserveAttributes, false);
        }

        public static PipelineStep<Byte, Boolean> asBoolean() {
            return map(toBoolean());
        }

        public static <T> PipelineStep<T, RAbstractVector> asVector() {
            return new CoercionStep<>(RType.Any, true);
        }

        public static <T> PipelineStep<T, RAbstractVector> asVector(boolean preserveNonVector) {
            return new CoercionStep<>(RType.Any, true, false, false, false, preserveNonVector);
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
}
