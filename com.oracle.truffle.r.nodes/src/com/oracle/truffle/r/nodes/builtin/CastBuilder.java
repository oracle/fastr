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

import java.util.Arrays;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.DoubleFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MatrixFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep.TargetType;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultErrorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultWarningStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.FirstBooleanNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstIntNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
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

    private static final CastNodeFactory[] EMPTY_CAST_FACT_ARRAY = new CastNodeFactory[0];

    private final RBuiltinNode builtinNode;

    private CastNodeFactory[] castFactories = EMPTY_CAST_FACT_ARRAY;
    private CastNode[] castsWrapped = null;

    public CastBuilder(RBuiltinNode builtinNode) {
        this.builtinNode = builtinNode;
    }

    public CastBuilder() {
        this(null);
    }

    @FunctionalInterface
    public interface CastNodeFactory {
        CastNode create();
    }

    private CastBuilder insert(int index, final CastNodeFactory castNodeFactory) {
        if (index >= castFactories.length) {
            castFactories = Arrays.copyOf(castFactories, index + 1);
        }
        final CastNodeFactory cnf = castFactories[index];
        if (cnf == null) {
            castFactories[index] = castNodeFactory;
        } else {
            castFactories[index] = () -> new ChainedCastNode(cnf, castNodeFactory);
        }
        return this;
    }

    public CastNode[] getCasts() {
        if (castsWrapped == null) {
            castsWrapped = new CastNode[castFactories.length];
            for (int i = 0; i < castFactories.length; i++) {
                CastNodeFactory cnf = i < castFactories.length ? castFactories[i] : null;
                CastNode cn = cnf == null ? null : cnf.create();
                castsWrapped[i] = cn;
            }
        }

        return castsWrapped;
    }

    public CastBuilder toAttributable(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, () -> CastToAttributableNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toVector(int index) {
        return insert(index, () -> CastToVectorNodeGen.create(false));
    }

    public CastBuilder toVector(int index, boolean preserveNonVector) {
        return insert(index, () -> CastToVectorNodeGen.create(preserveNonVector));
    }

    public CastBuilder toInteger(int index) {
        return toInteger(index, false, false, false);
    }

    public CastBuilder toInteger(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, () -> CastIntegerNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toDouble(int index) {
        return toDouble(index, false, false, false);
    }

    public CastBuilder toDouble(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, () -> CastDoubleNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toLogical(int index) {
        return insert(index, () -> CastLogicalNodeGen.create(false, false, false));
    }

    public CastBuilder toCharacter(int index) {
        return insert(index, () -> CastStringNodeGen.create(false, false, false));
    }

    public CastBuilder toCharacter(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, () -> CastStringNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toComplex(int index) {
        return insert(index, () -> CastComplexNodeGen.create(false, false, false));
    }

    public CastBuilder toRaw(int index) {
        return insert(index, () -> CastRawNodeGen.create(false, false, false));
    }

    public CastBuilder boxPrimitive(int index) {
        return insert(index, () -> BoxPrimitiveNodeGen.create());
    }

    public CastBuilder custom(int index, CastNodeFactory castNodeFactory) {
        return insert(index, castNodeFactory);
    }

    public CastBuilder firstIntegerWithWarning(int index, int intNa, String name) {
        insert(index, () -> CastIntegerNodeGen.create(false, false, false));
        return insert(index, () -> FirstIntNode.createWithWarning(RError.Message.FIRST_ELEMENT_USED, name, intNa));
    }

    public CastBuilder firstIntegerWithError(int index, RError.Message error, String name) {
        insert(index, () -> CastIntegerNodeGen.create(false, false, false));
        return insert(index, () -> FirstIntNode.createWithError(error, name));
    }

    public CastBuilder firstStringWithError(int index, RError.Message error, String name) {
        return insert(index, () -> FirstStringNode.createWithError(error, name));
    }

    public CastBuilder firstBoolean(int index) {
        return insert(index, () -> FirstBooleanNodeGen.create(null));
    }

    public CastBuilder firstBoolean(int index, String invalidValueName) {
        return insert(index, () -> FirstBooleanNodeGen.create(invalidValueName));
    }

    public CastBuilder firstLogical(int index) {
        arg(index).asLogicalVector().findFirst(RRuntime.LOGICAL_NA);
        return this;
    }

    public PreinitialPhaseBuilder<Object> arg(int argumentIndex, String argumentName) {
        return new ArgCastBuilderFactoryImpl(argumentIndex, argumentName).newPreinitialPhaseBuilder();
    }

    public PreinitialPhaseBuilder<Object> arg(int argumentIndex) {
        return arg(argumentIndex, builtinNode == null ? null : builtinNode.getRBuiltin().parameterNames()[argumentIndex]);
    }

    public PreinitialPhaseBuilder<Object> arg(String argumentName) {
        return arg(getArgumentIndex(argumentName), argumentName);
    }

    private int getArgumentIndex(String argumentName) {
        if (builtinNode == null) {
            throw new IllegalArgumentException("No builtin node associated with cast builder");
        }
        String[] parameterNames = builtinNode.getRBuiltin().parameterNames();
        for (int i = 0; i < parameterNames.length; i++) {
            if (argumentName.equals(parameterNames[i])) {
                return i;
            }
        }
        CompilerDirectives.transferToInterpreter();
        throw RInternalError.shouldNotReachHere(String.format("Argument %s not found in builtin %s", argumentName, builtinNode.getRBuiltin().name()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object[] substituteArgPlaceholder(Object arg, Object[] messageArgs) {
        Object[] newMsgArgs = Arrays.copyOf(messageArgs, messageArgs.length);

        for (int i = 0; i < messageArgs.length; i++) {
            final Object msgArg = messageArgs[i];
            if (msgArg instanceof Function) {
                newMsgArgs[i] = ((Function) msgArg).apply(arg);
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

        public static <V extends RAbstractVector> FindFirstNodeBuilder<V> findFirst(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            return new FindFirstNodeBuilder<>(callObj, message, messageArgs);
        }

        public static <V extends RAbstractVector> FindFirstNodeBuilder<V> findFirst(RError.Message message, Object... messageArgs) {
            return new FindFirstNodeBuilder<>(null, message, messageArgs);
        }

        public static <V extends RAbstractVector> FindFirstNodeBuilder<V> findFirst() {
            return new FindFirstNodeBuilder<>(null, null, null);
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

        public static <R extends RAbstractIntVector> TypeFilter<Object, R> integerValue() {
            return new TypeFilter<>(x -> x instanceof Integer || x instanceof RAbstractIntVector, Integer.class, RAbstractIntVector.class);
        }

        public static <R extends RAbstractStringVector> TypeFilter<Object, R> stringValue() {
            return new TypeFilter<>(x -> x instanceof String || x instanceof RAbstractStringVector, String.class, RAbstractStringVector.class);
        }

        public static <R extends RAbstractDoubleVector> TypeFilter<Object, R> doubleValue() {
            return new TypeFilter<>(x -> x instanceof Double || x instanceof RAbstractDoubleVector, Double.class, RAbstractDoubleVector.class);
        }

        public static <R extends RAbstractLogicalVector> TypeFilter<Object, R> logicalValue() {
            return new TypeFilter<>(x -> x instanceof Byte || x instanceof RAbstractLogicalVector, Byte.class, RAbstractLogicalVector.class);
        }

        public static <R extends RAbstractComplexVector> TypeFilter<Object, R> complexValue() {
            return new TypeFilter<>(x -> x instanceof RAbstractComplexVector, RAbstractComplexVector.class);
        }

        public static <R extends RAbstractRawVector> Filter<Object, R> rawValue() {
            return new TypeFilter<>(x -> x instanceof RAbstractRawVector, RAbstractRawVector.class);
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
    public interface ArgCastBuilder<T, THIS> {

        ArgCastBuilderState state();

        default CastBuilder builder() {
            return state().castBuilder();
        }

        default THIS defaultError(RBaseNode callObj, RError.Message message, Object... args) {
            state().setDefaultError(callObj, message, args);
            state().pipelineBuilder().appendDefaultErrorStep(callObj, message, args);
            return (THIS) this;
        }

        default THIS defaultError(RError.Message message, Object... args) {
            state().setDefaultError(message, args);
            state().pipelineBuilder().appendDefaultErrorStep(message, args);
            return (THIS) this;
        }

        default THIS defaultWarning(RBaseNode callObj, RError.Message message, Object... args) {
            state().setDefaultWarning(callObj, message, args);
            state().pipelineBuilder().appendDefaultWarningStep(callObj, message, args);
            return (THIS) this;
        }

        default THIS defaultWarning(RError.Message message, Object... args) {
            state().setDefaultWarning(message, args);
            state().pipelineBuilder().appendDefaultWarningStep(message, args);
            return (THIS) this;
        }

        default THIS shouldBe(Filter<? super T, ?> argFilter, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendShouldBeStep(argFilter, message, messageArgs);
            return (THIS) this;
        }

        default THIS shouldBe(Filter<? super T, ?> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendShouldBeStep(argFilter, callObj, message, messageArgs);
            return (THIS) this;
        }

        default THIS shouldBe(Filter<? super T, ?> argFilter) {
            return shouldBe(argFilter, state().defaultWarning().getCallObj(), state().defaultWarning().getMessage(), state().defaultWarning().getMessageArgs());
        }

        default <R, THAT extends ArgCastBuilder<R, THAT>> THAT alias(Function<THIS, THAT> aliaser) {
            return aliaser.apply((THIS) this);
        }

    }

    interface ArgCastBuilderFactory {

        PreinitialPhaseBuilder<Object> newPreinitialPhaseBuilder();

        <T> InitialPhaseBuilder<T> newInitialPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder);

        <T extends RAbstractVector, S> CoercedPhaseBuilder<T, S> newCoercedPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder, Class<?> elementClass);

        <T> HeadPhaseBuilder<T> newHeadPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder);

    }

// public static class DefaultError {
// public final RBaseNode callObj;
// public final RError.Message message;
// public final Object[] args;
//
// DefaultError(RBaseNode callObj, RError.Message message, Object... args) {
// this.callObj = callObj;
// this.message = message;
// this.args = args;
// }
//
// public DefaultError fixCallObj(RBaseNode callObjFix) {
// if (callObj == null) {
// return new DefaultError(callObjFix, message, args);
// } else {
// return this;
// }
// }
//
// }

    public static class ArgCastBuilderState {
        private final MessageData defaultDefaultError;

        private final int argumentIndex;
        private final String argumentName;
        final ArgCastBuilderFactory factory;
        private final CastBuilder cb;
        private final PipelineConfigBuilder pcb;
        private final PipelineBuilder pb;

        final boolean boxPrimitives;
        private MessageData defError;
        private MessageData defWarning;

        public ArgCastBuilderState(int argumentIndex, String argumentName, ArgCastBuilderFactory fact, CastBuilder cb, boolean boxPrimitives) {
            this.argumentIndex = argumentIndex;
            this.argumentName = argumentName;
            this.factory = fact;
            this.cb = cb;
            this.boxPrimitives = boxPrimitives;
            this.defaultDefaultError = new MessageData(null, RError.Message.INVALID_ARGUMENT, argumentName);
            this.pcb = new PipelineConfigBuilder(this);
            this.pb = new PipelineBuilder(this.pcb);
        }

        ArgCastBuilderState(ArgCastBuilderState prevState, boolean boxPrimitives) {
            this.argumentIndex = prevState.argumentIndex;
            this.argumentName = prevState.argumentName;
            this.factory = prevState.factory;
            this.cb = prevState.cb;
            this.boxPrimitives = boxPrimitives;
            this.defError = prevState.defError;
            this.defWarning = prevState.defWarning;
            this.defaultDefaultError = new MessageData(null, RError.Message.INVALID_ARGUMENT, argumentName);
            this.pcb = prevState.pcb;
            this.pb = prevState.pb;
        }

        public int index() {
            return argumentIndex;
        }

        public String name() {
            return argumentName;
        }

        public CastBuilder castBuilder() {
            return cb;
        }

        public PipelineBuilder pipelineBuilder() {
            return pb;
        }

        boolean isDefaultErrorDefined() {
            return defError != null;
        }

        boolean isDefaultWarningDefined() {
            return defWarning != null;
        }

        void setDefaultError(RBaseNode callObj, RError.Message message, Object... args) {
            defError = new MessageData(callObj, message, args);
        }

        void setDefaultError(RError.Message message, Object... args) {
            defError = new MessageData(null, message, args);
        }

        void setDefaultWarning(RBaseNode callObj, RError.Message message, Object... args) {
            defWarning = new MessageData(callObj, message, args);
        }

        void setDefaultWarning(RError.Message message, Object... args) {
            defWarning = new MessageData(null, message, args);
        }

        MessageData defaultError() {
            return defError == null ? defaultDefaultError : defError;
        }

        MessageData defaultError(RBaseNode callObj, RError.Message defaultDefaultMessage, Object... defaultDefaultArgs) {
            return defError == null ? new MessageData(callObj, defaultDefaultMessage, defaultDefaultArgs) : defError;
        }

        MessageData defaultError(RError.Message defaultDefaultMessage, Object... defaultDefaultArgs) {
            return defError == null ? new MessageData(null, defaultDefaultMessage, defaultDefaultArgs) : defError;
        }

        MessageData defaultWarning() {
            return defWarning == null ? defaultDefaultError : defWarning;
        }

        MessageData defaultWarning(RBaseNode callObj, RError.Message defaultDefaultMessage, Object... defaultDefaultArgs) {
            return defWarning == null ? new MessageData(callObj, defaultDefaultMessage, defaultDefaultArgs) : defWarning;
        }

        MessageData defaultWarning(RError.Message defaultDefaultMessage, Object... defaultDefaultArgs) {
            return defWarning == null ? new MessageData(null, defaultDefaultMessage, defaultDefaultArgs) : defWarning;
        }

        void mustBe(Filter<?, ?> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendMustBeStep(argFilter, callObj, message, messageArgs);
        }

        void mustBe(Filter<?, ?> argFilter) {
            mustBe(argFilter, defaultError().getCallObj(), defaultError().getMessage(), defaultError().getMessageArgs());
        }

        void shouldBe(Filter<?, ?> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            pipelineBuilder().appendShouldBeStep(argFilter, callObj, message, messageArgs);
        }

        void shouldBe(Filter<?, ?> argFilter) {
            shouldBe(argFilter, defaultWarning().getCallObj(), defaultWarning().getMessage(), defaultWarning().getMessageArgs());
        }

    }

    abstract class ArgCastBuilderBase<T, THIS> implements ArgCastBuilder<T, THIS> {

        private final ArgCastBuilderState st;

        ArgCastBuilderBase(ArgCastBuilderState state) {
            this.st = state;
        }

        @Override
        public ArgCastBuilderState state() {
            return st;
        }
    }

    public interface InitialPhaseBuilder<T> extends ArgCastBuilder<T, InitialPhaseBuilder<T>> {

        default <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, callObj, message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, null, message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T> InitialPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter) {
            return mustBe(argFilter, state().defaultError().getCallObj(), state().defaultError().getMessage(), state().defaultError().getMessageArgs());
        }

        default <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            mustBe(Predef.instanceOf(cls), callObj, message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            mustBe(Predef.instanceOf(cls), message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T> InitialPhaseBuilder<S> mustBe(Class<S> cls) {
            mustBe(Predef.instanceOf(cls));
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            shouldBe(Predef.instanceOf(cls), callObj, message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            shouldBe(Predef.instanceOf(cls), message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> shouldBe(Class<S> cls) {
            shouldBe(Predef.instanceOf(cls));
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> map(Mapper<T, S> mapFn) {
            state().pipelineBuilder().appendMap(mapFn);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
            state().pipelineBuilder().appendMapIf(argFilter, trueBranchMapper);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
            state().pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch) {
            state().pipelineBuilder().appendMapIf(argFilter, trueBranch);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T, R> InitialPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
            state().pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default InitialPhaseBuilder<T> notNA(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendNotNA(null, callObj, message, messageArgs);
            return this;
        }

        default InitialPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendNotNA(null, null, message, messageArgs);
            return this;
        }

        default InitialPhaseBuilder<T> notNA(T naReplacement, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendNotNA(naReplacement, callObj, message, messageArgs);
            return this;
        }

        default InitialPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendNotNA(naReplacement, null, message, messageArgs);
            return this;
        }

        default InitialPhaseBuilder<T> notNA(T naReplacement) {
            state().pipelineBuilder().appendNotNA(naReplacement, null, null, null);
            return this;
        }

        /**
         * This method should be used as a step in pipeline, not as an argument to {@code mustBe}.
         * Example: {@code casts.arg("x").notNA()}.
         */
        default InitialPhaseBuilder<T> notNA() {
            state().pipelineBuilder().appendNotNA(null, null, null, null);
            return this;
        }

        default CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().pipelineBuilder().appendAsIntegerVector(preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newCoercedPhaseBuilder(this, Integer.class);
        }

        default CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector() {
            return asIntegerVector(false, false, false);
        }

        default CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().pipelineBuilder().appendAsDoubleVector(preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newCoercedPhaseBuilder(this, Double.class);
        }

        default CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector() {
            return asDoubleVector(false, false, false);
        }

        default CoercedPhaseBuilder<RAbstractDoubleVector, Byte> asLogicalVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().pipelineBuilder().appendAsLogicalVector(preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newCoercedPhaseBuilder(this, Byte.class);
        }

        default CoercedPhaseBuilder<RAbstractDoubleVector, Byte> asLogicalVector() {
            return asLogicalVector(false, false, false);
        }

        default CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().pipelineBuilder().appendAsStringVector(preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newCoercedPhaseBuilder(this, String.class);
        }

        default CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector() {
            state().pipelineBuilder().appendAsStringVector();
            return state().factory.newCoercedPhaseBuilder(this, String.class);
        }

        default CoercedPhaseBuilder<RAbstractComplexVector, RComplex> asComplexVector() {
            state().pipelineBuilder().appendAsComplexVector();
            return state().factory.newCoercedPhaseBuilder(this, RComplex.class);
        }

        default CoercedPhaseBuilder<RAbstractRawVector, RRaw> asRawVector() {
            state().pipelineBuilder().appendAsRawVector();
            return state().factory.newCoercedPhaseBuilder(this, RRaw.class);
        }

        default CoercedPhaseBuilder<RAbstractVector, Object> asVector() {
            state().pipelineBuilder().appendAsVector();
            return state().factory.newCoercedPhaseBuilder(this, Object.class);
        }

        default CoercedPhaseBuilder<RAbstractVector, Object> asVector(boolean preserveNonVector) {
            state().pipelineBuilder().appendAsVector(preserveNonVector);
            return state().factory.newCoercedPhaseBuilder(this, Object.class);
        }

        default HeadPhaseBuilder<RAttributable> asAttributable(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().pipelineBuilder().appendAsAttributable(preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newHeadPhaseBuilder(this);
        }

    }

    public interface PreinitialPhaseBuilder<T> extends InitialPhaseBuilder<T> {

        default InitialPhaseBuilder<T> conf(Function<PipelineConfigBuilder, PipelineConfigBuilder> cfgLambda) {
            cfgLambda.apply(getPipelineConfigBuilder());
            return this;
        }

        default InitialPhaseBuilder<T> allowNull() {
            return conf(c -> c.allowNull());
        }

        default InitialPhaseBuilder<T> mustNotBeNull() {
            return conf(c -> c.mustNotBeNull(state().defaultError().getCallObj(), state().defaultError().getMessage(), state().defaultError().getMessageArgs()));
        }

        default InitialPhaseBuilder<T> mustNotBeNull(RError.Message errorMsg, Object... msgArgs) {
            return conf(c -> c.mustNotBeNull(null, errorMsg, msgArgs));
        }

        default InitialPhaseBuilder<T> mustNotBeNull(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
            return conf(c -> c.mustNotBeNull(callObj, errorMsg, msgArgs));
        }

        default InitialPhaseBuilder<T> mapNull(Mapper<? super RNull, ?> mapper) {
            return conf(c -> c.mapNull(mapper));
        }

        default InitialPhaseBuilder<T> allowMissing() {
            return conf(c -> c.allowMissing());
        }

        default InitialPhaseBuilder<T> mustNotBeMissing() {
            return conf(c -> c.mustNotBeMissing(state().defaultError().getCallObj(), state().defaultError().getMessage(), state().defaultError().getMessageArgs()));
        }

        default InitialPhaseBuilder<T> mustNotBeMissing(RError.Message errorMsg, Object... msgArgs) {
            return conf(c -> c.mustNotBeMissing(null, errorMsg, msgArgs));
        }

        default InitialPhaseBuilder<T> mustNotBeMissing(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
            return conf(c -> c.mustNotBeMissing(callObj, errorMsg, msgArgs));
        }

        default InitialPhaseBuilder<T> mapMissing(Mapper<? super RMissing, ?> mapper) {
            return conf(c -> c.mapMissing(mapper));
        }

        default InitialPhaseBuilder<T> allowNullAndMissing() {
            return conf(c -> c.allowMissing().allowNull());
        }

        @Override
        default PreinitialPhaseBuilder<T> defaultError(RBaseNode callObj, RError.Message message, Object... args) {
            state().setDefaultError(callObj, message, args);
            state().pipelineBuilder().appendDefaultErrorStep(callObj, message, args);
            getPipelineConfigBuilder().updateDefaultError();
            return this;
        }

        @Override
        default PreinitialPhaseBuilder<T> defaultError(RError.Message message, Object... args) {
            return defaultError(null, message, args);
        }

        @Override
        default PreinitialPhaseBuilder<T> defaultWarning(RBaseNode callObj, RError.Message message, Object... args) {
            state().setDefaultWarning(callObj, message, args);
            state().pipelineBuilder().appendDefaultWarningStep(callObj, message, args);
            return this;
        }

        @Override
        default PreinitialPhaseBuilder<T> defaultWarning(RError.Message message, Object... args) {
            return defaultWarning(null, message, args);
        }

        PipelineConfigBuilder getPipelineConfigBuilder();
    }

    public interface CoercedPhaseBuilder<T extends RAbstractVector, S> extends ArgCastBuilder<T, CoercedPhaseBuilder<T, S>> {

        /**
         * The inserted cast node returns the default value if the input vector is empty. It also
         * reports the warning message.
         */
        default HeadPhaseBuilder<S> findFirst(S defaultValue, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendFindFirst(defaultValue, elementClass(), null, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default HeadPhaseBuilder<S> findFirst(S defaultValue, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendFindFirst(defaultValue, elementClass(), callObj, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node raises an error if the input vector is empty.
         */
        default HeadPhaseBuilder<S> findFirst(RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendFindFirst(null, elementClass(), null, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default HeadPhaseBuilder<S> findFirst(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendFindFirst(null, elementClass(), callObj, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node raises the default error, if defined, or
         * RError.Message.LENGTH_ZERO error if the input vector is empty.
         */
        default HeadPhaseBuilder<S> findFirst() {
            MessageData err = state().isDefaultErrorDefined() ? state().defaultError() : new MessageData(null, RError.Message.LENGTH_ZERO);
            state().pipelineBuilder().appendFindFirst(null, elementClass(), err.getCallObj(), err.getMessage(), err.getMessageArgs());
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node returns the default value if the input vector is empty. It reports
         * no warning message.
         */
        default HeadPhaseBuilder<S> findFirst(S defaultValue) {
            assert defaultValue != null : "defaultValue cannot be null";
            state().pipelineBuilder().appendFindFirst(defaultValue, elementClass(), null, null, null);
            return state().factory.newHeadPhaseBuilder(this);
        }

        Class<?> elementClass();

        default CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, callObj, message, messageArgs);
            return this;
        }

        default CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, null, message, messageArgs);
            return this;
        }

        default CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter) {
            return mustBe(argFilter, state().defaultError().getCallObj(), state().defaultError().getMessage(), state().defaultError().getMessageArgs());
        }

    }

    public interface HeadPhaseBuilder<T> extends ArgCastBuilder<T, HeadPhaseBuilder<T>> {

        default <S> HeadPhaseBuilder<S> map(Mapper<T, S> mapFn) {
            // state().castBuilder().insert(state().index(), () -> MapNode.create(mapFn));
            state().pipelineBuilder().appendMap(mapFn);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
            state().pipelineBuilder().appendMapIf(argFilter, trueBranchMapper);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
            state().pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<S, ?> trueBranch) {
            state().pipelineBuilder().appendMapIf(argFilter, trueBranch);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<S, R> trueBranch, PipelineStep<T, ?> falseBranch) {
            state().pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, callObj, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, null, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter) {
            return mustBe(argFilter, state().defaultError().getCallObj(), state().defaultError().getMessage(), state().defaultError().getMessageArgs());
        }

        default <S extends T> HeadPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            mustBe(Predef.instanceOf(cls), message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T> HeadPhaseBuilder<S> mustBe(Class<S> cls) {
            mustBe(Predef.instanceOf(cls));
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            shouldBe(Predef.instanceOf(cls), callObj, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            shouldBe(Predef.instanceOf(cls), message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls) {
            shouldBe(Predef.instanceOf(cls));
            return state().factory.newHeadPhaseBuilder(this);
        }

        default HeadPhaseBuilder<T> notNA(RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendNotNA(null, callObj, message, messageArgs);
            return this;
        }

        default HeadPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendNotNA(null, null, message, messageArgs);
            return this;
        }

        default HeadPhaseBuilder<T> notNA(T naReplacement, RBaseNode callObj, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendNotNA(naReplacement, callObj, message, messageArgs);
            return this;
        }

        default HeadPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            state().pipelineBuilder().appendNotNA(naReplacement, null, message, messageArgs);
            return this;
        }

        default HeadPhaseBuilder<T> notNA() {
            state().pipelineBuilder().appendNotNA(null, null, null, null);
            return this;
        }

        default HeadPhaseBuilder<T> notNA(T naReplacement) {
            state().pipelineBuilder().appendNotNA(naReplacement, null, null, null);
            return this;
        }

    }

    final class ArgCastBuilderFactoryImpl implements ArgCastBuilderFactory {

        private final int argumentIndex;
        private final String argumentName;

        ArgCastBuilderFactoryImpl(int argumentIndex, String argumentName) {
            this.argumentIndex = argumentIndex;
            this.argumentName = argumentName;
        }

        @Override
        public PreinitialPhaseBuilderImpl<Object> newPreinitialPhaseBuilder() {
            return new PreinitialPhaseBuilderImpl<>();
        }

        @Override
        public <T> InitialPhaseBuilderImpl<T> newInitialPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder) {
            return new InitialPhaseBuilderImpl<>(currentBuilder.state());
        }

        @Override
        public <T extends RAbstractVector, S> CoercedPhaseBuilderImpl<T, S> newCoercedPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder, Class<?> elementClass) {
            return new CoercedPhaseBuilderImpl<>(currentBuilder.state(), elementClass);
        }

        @Override
        public <T> HeadPhaseBuilderImpl<T> newHeadPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder) {
            return new HeadPhaseBuilderImpl<>(currentBuilder.state());
        }

        public final class InitialPhaseBuilderImpl<T> extends ArgCastBuilderBase<T, InitialPhaseBuilder<T>> implements InitialPhaseBuilder<T> {
            InitialPhaseBuilderImpl(ArgCastBuilderState state) {
                super(new ArgCastBuilderState(state, false));
            }

        }

        public final class PreinitialPhaseBuilderImpl<T> extends ArgCastBuilderBase<T, InitialPhaseBuilder<T>> implements PreinitialPhaseBuilder<T> {

            PreinitialPhaseBuilderImpl() {
                super(new ArgCastBuilderState(argumentIndex, argumentName, ArgCastBuilderFactoryImpl.this, CastBuilder.this, false));
                insert(argumentIndex, state().pipelineBuilder());
            }

            @Override
            public PipelineConfigBuilder getPipelineConfigBuilder() {
                return state().pcb;
            }

        }

        public final class CoercedPhaseBuilderImpl<T extends RAbstractVector, S> extends ArgCastBuilderBase<T, CoercedPhaseBuilder<T, S>> implements CoercedPhaseBuilder<T, S> {

            private final Class<?> elementClass;

            CoercedPhaseBuilderImpl(ArgCastBuilderState state, Class<?> elementClass) {
                super(new ArgCastBuilderState(state, true));
                this.elementClass = elementClass;
            }

            @Override
            public Class<?> elementClass() {
                return elementClass;
            }
        }

        public final class HeadPhaseBuilderImpl<T> extends ArgCastBuilderBase<T, HeadPhaseBuilder<T>> implements HeadPhaseBuilder<T> {
            HeadPhaseBuilderImpl(ArgCastBuilderState state) {
                super(new ArgCastBuilderState(state, false));
            }
        }

    }

    public static final class ChainBuilder<T> {
        private final PipelineStep<?, ?> firstStep;
        private PipelineStep<?, ?> lastStep;

        private ChainBuilder(PipelineStep<?, ?> firstStep) {
            this.firstStep = firstStep;
            this.lastStep = firstStep;
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

    public static final class FindFirstNodeBuilder<W> {
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

        private final ArgCastBuilderState state;

        private Mapper<? super RMissing, ?> missingMapper = null;
        private Mapper<? super RNull, ?> nullMapper = null;
        private MessageData missingMsg;
        private MessageData nullMsg;

        public PipelineConfigBuilder(ArgCastBuilderState state) {
            this.state = state;
        }

        public MessageData getDefaultDefaultMessage() {
            return state.defaultDefaultError;
        }

        public String getArgName() {
            return state.argumentName;
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

        void updateDefaultError() {
            missingMsg = state.defaultError();
            nullMsg = state.defaultError();
        }

    }

    private static final class PipelineBuilder implements CastNodeFactory {

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

        public void appendFindFirst(Object defaultValue, Class<?> elementClass, RBaseNode callObj, Message message, Object[] messageArgs) {
            append(new FindFirstStep<>(defaultValue, elementClass, message == null ? null : new MessageData(callObj, message, messageArgs)));
        }

        public void appendAsAttributable(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            append(new CoercionStep<>(TargetType.Attributable, false, preserveNames, dimensionsPreservation, attrPreservation));
        }

        public void appendAsVector(boolean preserveNonVector) {
            append(new CoercionStep<>(TargetType.Any, true, false, false, false, preserveNonVector));
        }

        public void appendAsVector() {
            appendAsVector(false);
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
            append(new NotNAStep<>(naReplacement, message == null ? null : new MessageData(callObj, message, messageArgs)));
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
            append(new FilterStep<>(argFilter, new MessageData(callObj, message, messageArgs), false));
        }

        public void appendShouldBeStep(Filter<?, ?> argFilter, Message message, Object[] messageArgs) {
            appendShouldBeStep(argFilter, null, message, messageArgs);
        }

        public void appendShouldBeStep(Filter<?, ?> argFilter, RBaseNode callObj, Message message, Object[] messageArgs) {
            append(new FilterStep<>(argFilter, new MessageData(callObj, message, messageArgs), true));
        }

        public void appendDefaultWarningStep(Message message, Object[] args) {
            appendDefaultWarningStep(null, message, args);
        }

        public void appendDefaultWarningStep(RBaseNode callObj, Message message, Object[] args) {
            append(new DefaultWarningStep<>(new MessageData(callObj, message, args)));
        }

        public void appendDefaultErrorStep(Message message, Object[] args) {
            appendDefaultErrorStep(null, message, args);
        }

        public void appendDefaultErrorStep(RBaseNode callObj, Message message, Object[] args) {
            append(new DefaultErrorStep<>(new MessageData(callObj, message, args)));
        }

        @Override
        public CastNode create() {
            return PipelineToCastNode.convert(pcb, chainBuilder == null ? null : chainBuilder.firstStep);
        }

    }
}
