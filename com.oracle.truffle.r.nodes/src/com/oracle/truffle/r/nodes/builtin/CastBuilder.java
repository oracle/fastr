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

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastFunctionsFactory;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalScalarNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastNode.Samples;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.ConvertIntNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstBooleanNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstIntNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class CastBuilder {

    private static final CastNode[] EMPTY_CASTS_ARRAY = new CastNode[0];

    /**
     * This object is used as a placeholder for the cast argument in error/warning messages.
     *
     * @see CastBuilder#substituteArgPlaceholder(Object, Object...)
     */
    public static final Object ARG = new Object();

    private final RBuiltinNode builtinNode;

    private CastNode[] casts = EMPTY_CASTS_ARRAY;

    private PrintWriter out;

    public CastBuilder(RBuiltinNode builtinNode) {
        this.builtinNode = builtinNode;
    }

    public CastBuilder() {
        this(null);
    }

    private CastBuilder insert(int index, CastNode cast) {
        if (index >= casts.length) {
            casts = Arrays.copyOf(casts, index + 1);
        }
        if (casts[index] == null) {
            casts[index] = cast;
        } else {
            casts[index] = new ChainedCastNode(casts[index], cast);
        }
        return this;
    }

    public CastNode[] getCasts() {
        return casts;
    }

    public CastBuilder toAttributable(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, CastToAttributableNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toVector(int index) {
        return insert(index, CastToVectorNodeGen.create(false));
    }

    public CastBuilder toInteger(int index) {
        return toInteger(index, false, false, false);
    }

    public CastBuilder toInteger(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, CastIntegerNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toDouble(int index) {
        return toDouble(index, false, false, false);
    }

    public CastBuilder toDouble(int index, boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        return insert(index, CastDoubleNodeGen.create(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public CastBuilder toLogical(int index) {
        return insert(index, CastLogicalNodeGen.create(false, false, false));
    }

    public CastBuilder toCharacter(int index) {
        return insert(index, CastStringNodeGen.create(false, false, false, false));
    }

    public CastBuilder boxPrimitive(int index) {
        return insert(index, BoxPrimitiveNodeGen.create());
    }

    public CastBuilder custom(int index, CastNode cast) {
        return insert(index, cast);
    }

    public CastBuilder firstIntegerWithWarning(int index, int intNa, String name) {
        insert(index, CastIntegerNodeGen.create(false, false, false));
        return insert(index, FirstIntNode.createWithWarning(RError.Message.FIRST_ELEMENT_USED, name, intNa));
    }

    public CastBuilder convertToInteger(int index) {
        return insert(index, ConvertIntNodeGen.create());
    }

    public CastBuilder firstIntegerWithError(int index, RError.Message error, String name) {
        insert(index, CastIntegerNodeGen.create(false, false, false));
        return insert(index, FirstIntNode.createWithError(error, name));
    }

    public CastBuilder firstStringWithError(int index, RError.Message error, String name) {
        return insert(index, FirstStringNode.createWithError(error, name));
    }

    public CastBuilder firstBoolean(int index) {
        return insert(index, FirstBooleanNodeGen.create(null));
    }

    public CastBuilder firstBoolean(int index, String invalidValueName) {
        return insert(index, FirstBooleanNodeGen.create(invalidValueName));
    }

    public CastBuilder firstLogical(int index) {
        return insert(index, CastLogicalScalarNode.create());
    }

    public InitialPhaseBuilder<Object> arg(int argumentIndex, String argumentName) {
        return new ArgCastBuilderFactoryImpl(argumentIndex, argumentName).newInitialPhaseBuilder();
    }

    public InitialPhaseBuilder<Object> arg(int argumentIndex) {
        return arg(argumentIndex, builtinNode == null ? null : builtinNode.getBuiltin().getSignature().getName(argumentIndex));
    }

    public InitialPhaseBuilder<Object> arg(String argumentName) {
        return arg(getArgumentIndex(argumentName), argumentName);
    }

    private int getArgumentIndex(String argumentName) {
        if (builtinNode == null) {
            throw new IllegalArgumentException("No builtin node associated with cast builder");
        }
        ArgumentsSignature signature = builtinNode.getBuiltin().getSignature();
        for (int i = 0; i < signature.getLength(); i++) {
            if (argumentName.equals(signature.getName(i))) {
                return i;
            }
        }

        throw RInternalError.shouldNotReachHere(String.format("Argument %s not found in builtin %s", argumentName, builtinNode.getRBuiltin().name()));
    }

    /**
     * Overrides the default output for warnings. Used in tests only.
     *
     * @param o the overriding output writer for warnings
     * @return this builder
     */
    public CastBuilder output(Writer o) {
        out = new PrintWriter(o);
        return this;
    }

    public static Object[] substituteArgPlaceholder(Object arg, Object[] messageArgs) {
        int argPlaceholderIndex = -1;
        for (int i = 0; i < messageArgs.length; i++) {
            if (messageArgs[i] == ARG) {
                argPlaceholderIndex = i;
                break;
            }
        }

        Object[] newMsgArgs;
        if (argPlaceholderIndex >= 0) {
            newMsgArgs = Arrays.copyOf(messageArgs, messageArgs.length);
            newMsgArgs[argPlaceholderIndex] = arg;
        } else {
            newMsgArgs = messageArgs;
        }

        return newMsgArgs;
    }

    @SafeVarargs
    public static <T> Set<? extends T> samples(T samplesHead, T... samplesTail) {
        HashSet<T> sampleSet = new HashSet<>(Arrays.asList(samplesTail));
        sampleSet.add(samplesHead);
        return sampleSet;
    }

    public static <T> Set<? extends T> samples(T s) {
        return Collections.singleton(s);
    }

    public static <T> Set<? extends T> samples() {
        return Collections.emptySet();
    }

    public static final class Predef {

        public static <T, R extends T> ValuePredicateArgumentFilter<T, R> sameAs(R x) {
            return ValuePredicateArgumentFilter.fromLambda(arg -> arg == x, samples(x), CastBuilder.<R> samples());
        }

        public static <T, R extends T> ValuePredicateArgumentFilter<T, R> equalTo(R x) {
            return ValuePredicateArgumentFilter.fromLambda(arg -> Objects.equals(arg, x), samples(x), CastBuilder.<R> samples());
        }

        public static <T, R extends T> ValuePredicateArgumentFilter<T, R> nullValue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x == RNull.instance || x == null, CastBuilder.<R> samples(null), CastBuilder.<R> samples());
        }

        public static <T, R extends T> ValuePredicateArgumentFilter<T, R> notNull() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x != RNull.instance && x != null, CastBuilder.<R> samples(), CastBuilder.<R> samples(null));
        }

        public static <T extends RAbstractVector, R extends T> VectorPredicateArgumentFilter<T, R> notEmpty() {
            return new VectorPredicateArgumentFilter<>(x -> x.getLength() > 0, false);
        }

        public static <T extends RAbstractVector, R extends T> VectorPredicateArgumentFilter<T, R> singleElement() {
            return new VectorPredicateArgumentFilter<>(x -> x.getLength() == 1, false);
        }

        public static final ValuePredicateArgumentFilter<Boolean, Boolean> trueValue = ValuePredicateArgumentFilter.fromLambda(x -> x, CastBuilder.<Boolean> samples(), samples(Boolean.FALSE));
        public static final ValuePredicateArgumentFilter<Boolean, Boolean> falseValue = ValuePredicateArgumentFilter.fromLambda(x -> x, CastBuilder.<Boolean> samples(), samples(Boolean.FALSE));
        public static final ValuePredicateArgumentFilter<Integer, Integer> intNA = ValuePredicateArgumentFilter.fromLambda((Integer x) -> RRuntime.isNA(x), samples(RRuntime.INT_NA), samples(0));
        public static final ValuePredicateArgumentFilter<Integer, Integer> notIntNA = ValuePredicateArgumentFilter.fromLambda((Integer x) -> !RRuntime.isNA(x), CastBuilder.<Integer> samples(),
                        samples(RRuntime.INT_NA));
        public static final ValuePredicateArgumentFilter<Byte, Byte> logicalNA = ValuePredicateArgumentFilter.fromLambda((Byte x) -> RRuntime.isNA(x), samples(RRuntime.LOGICAL_NA),
                        samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE));
        public static final ValuePredicateArgumentFilter<Byte, Byte> notLogicalNA = ValuePredicateArgumentFilter.fromLambda((Byte x) -> !RRuntime.isNA(x), CastBuilder.<Byte> samples(),
                        samples(RRuntime.LOGICAL_NA));
        public static final ValuePredicateArgumentFilter<Double, Double> doubleNA = ValuePredicateArgumentFilter.fromLambda((Double x) -> RRuntime.isNA(x), samples(RRuntime.DOUBLE_NA), samples(0.0));
        public static final ValuePredicateArgumentFilter<Double, Double> notDoubleNA = ValuePredicateArgumentFilter.fromLambda((Double x) -> !RRuntime.isNA(x), CastBuilder.<Double> samples(),
                        samples(RRuntime.DOUBLE_NA));
        public static final ValuePredicateArgumentFilter<String, String> stringNA = ValuePredicateArgumentFilter.fromLambda((String x) -> RRuntime.isNA(x), samples(RRuntime.STRING_NA), samples(""));
        public static final ValuePredicateArgumentFilter<String, String> notStringNA = ValuePredicateArgumentFilter.fromLambda((String x) -> !RRuntime.isNA(x), CastBuilder.<String> samples(),
                        samples(RRuntime.STRING_NA));

        public static ValuePredicateArgumentFilter<Integer, Integer> gt(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg > x, samples(x + 1), samples(x));
        }

        public static ValuePredicateArgumentFilter<Double, Double> gt(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg > x, samples(x + 0.00001), samples(x));
        }

        public static ValuePredicateArgumentFilter<Integer, Integer> gte(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg >= x, samples(x), samples(x - 1));
        }

        public static ValuePredicateArgumentFilter<Double, Double> gte(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg >= x, samples(x), samples(x - 0.00001));
        }

        public static ValuePredicateArgumentFilter<Integer, Integer> lt(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg < x, samples(x - 1), samples(x));
        }

        public static ValuePredicateArgumentFilter<Double, Double> lt(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg < x, samples(x - 0.00001), samples(x));
        }

        public static ValuePredicateArgumentFilter<Integer, Integer> lte(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg <= x, samples(x), samples(x + 1));
        }

        public static ValuePredicateArgumentFilter<Double, Double> lte(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg <= x, samples(x), samples(x + 0.00001));
        }

        public static ValuePredicateArgumentFilter<String, String> length(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() == l);
        }

        public static ValuePredicateArgumentFilter<String, String> isEmpty() {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.isEmpty());
        }

        public static ValuePredicateArgumentFilter<String, String> lengthGt(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() > l);
        }

        public static ValuePredicateArgumentFilter<String, String> lengthGte(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() >= l);
        }

        public static ValuePredicateArgumentFilter<String, String> lengthLt(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() < l);
        }

        public static ValuePredicateArgumentFilter<String, String> lengthLte(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() <= l);
        }

        public static final ValuePredicateArgumentFilter<Integer, Integer> gt0 = ValuePredicateArgumentFilter.fromLambda((Integer x) -> x > 0, CastBuilder.<Integer> samples(), samples(-1, 0),
                        Integer.class);
        public static final ValuePredicateArgumentFilter<Integer, Integer> gte0 = ValuePredicateArgumentFilter.fromLambda((Integer x) -> x >= 0, CastBuilder.<Integer> samples(), samples(-1),
                        Integer.class);
        public static final ValuePredicateArgumentFilter<Integer, Integer> gt1 = ValuePredicateArgumentFilter.fromLambda((Integer x) -> x > 1, CastBuilder.<Integer> samples(), samples(-1, 0, 1),
                        Integer.class);
        public static final ValuePredicateArgumentFilter<Integer, Integer> gte1 = ValuePredicateArgumentFilter.fromLambda((Integer x) -> x >= 1, CastBuilder.<Integer> samples(), samples(-1, 0),
                        Integer.class);

        public static <R extends RAbstractIntVector> ValuePredicateArgumentFilter<Object, R> integerValue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x instanceof Integer || x instanceof RAbstractIntVector, RAbstractIntVector.class);
        }

        public static <R extends RAbstractStringVector> ValuePredicateArgumentFilter<Object, R> stringValue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x instanceof String ||
                            x instanceof RAbstractStringVector, RAbstractStringVector.class);
        }

        public static <R extends RAbstractDoubleVector> ValuePredicateArgumentFilter<Object, R> doubleValue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x instanceof Double ||
                            x instanceof RAbstractDoubleVector, RAbstractDoubleVector.class);
        }

        public static <R extends RAbstractLogicalVector> ValuePredicateArgumentFilter<Object, R> logicalValue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x instanceof Byte ||
                            x instanceof RAbstractLogicalVector, RAbstractLogicalVector.class);
        }

        public static <R extends RAbstractComplexVector> ValuePredicateArgumentFilter<Object, R> complexValue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x instanceof RComplex ||
                            x instanceof RAbstractComplexVector, RAbstractComplexVector.class);
        }

        public static final ArgumentFilter<Object, ?> numericValue = integerValue().union(doubleValue()).union(complexValue()).union(logicalValue());

        public static final ValuePredicateArgumentFilter<Object, String> scalarStringValue = ValuePredicateArgumentFilter.fromLambda(x -> x instanceof String, String.class);

        public static final ValuePredicateArgumentFilter<Object, Integer> scalarIntegerValue = ValuePredicateArgumentFilter.fromLambda(x -> x instanceof Integer, Integer.class);

        public static final ValuePredicateArgumentFilter<Object, Double> scalarDoubleValue = ValuePredicateArgumentFilter.fromLambda(x -> x instanceof Double, Double.class);

        public static final ValuePredicateArgumentFilter<Object, Byte> scalarLogicalValue = ValuePredicateArgumentFilter.fromLambda(x -> x instanceof Byte, Byte.class);

        public static final ValuePredicateArgumentFilter<Object, RComplex> scalarComplexValue = ValuePredicateArgumentFilter.fromLambda(x -> x instanceof RComplex, RComplex.class);

        public static final ValuePredicateArgumentFilter<Object, RMissing> missingValue = ValuePredicateArgumentFilter.fromLambda(x -> RMissing.instance == x, RMissing.class);

        public static final ValuePredicateArgumentMapper<Byte, Boolean> toBoolean = ValuePredicateArgumentMapper.fromLambda(x -> RRuntime.fromLogical(x), x -> RRuntime.asLogical(x), Boolean.class);

        public static ValuePredicateArgumentMapper<String, Integer> charAt0(int defaultValue) {
            return ValuePredicateArgumentMapper.fromLambda(x -> x == null || x.isEmpty() ? defaultValue : (int) x.charAt(0),
                            x -> x == null ? "" + (char) defaultValue : "" + (char) x.intValue(), Integer.class);
        }

        public static ValuePredicateArgumentMapper<String, String> constant(String s) {
            return ValuePredicateArgumentMapper.<String, String> fromLambda((String x) -> s, (String x) -> (String) null, samples(s), CastBuilder.<String> samples(), String.class);
        }

        public static ValuePredicateArgumentMapper<Integer, Integer> constant(int i) {
            return ValuePredicateArgumentMapper.fromLambda(x -> i, x -> null, samples(i), CastBuilder.<Integer> samples(), Integer.class);
        }

        public static ValuePredicateArgumentMapper<Double, Double> constant(double d) {
            return ValuePredicateArgumentMapper.fromLambda(x -> d, x -> null, samples(d), CastBuilder.<Double> samples(), Double.class);
        }

        public static ValuePredicateArgumentMapper<Byte, Byte> constant(byte l) {
            return ValuePredicateArgumentMapper.fromLambda(x -> l, x -> null, samples(l), CastBuilder.<Byte> samples(), Byte.class);
        }

        public static <T> ArgumentMapper<T, T> defaultValue(T defVal) {

            assert (defVal != null);

            final Set<Class<?>> defCls = Collections.singleton(defVal.getClass());

            return new ArgumentMapper<T, T>() {

                public T map(T arg) {
                    return arg == RNull.instance || arg == null ? defVal : arg;
                }

                public Set<Class<?>> resultTypes() {
                    return defCls;
                }

                public Samples<T> collectSamples(Samples<T> downStreamSamples) {
                    HashSet<T> posSamples = new HashSet<>(downStreamSamples.positiveSamples());
                    posSamples.add(defVal);
                    return new Samples<>(posSamples, downStreamSamples.negativeSamples());
                }
            };
        }

    }

    @SuppressWarnings("unchecked")
    interface ArgCastBuilder<T, THIS> {

        ArgCastBuilderState state();

        default THIS defaultError(RError.Message message, Object... args) {
            state().setDefaultError(message, args);
            return (THIS) this;
        }

        default THIS defaultWarning(RError.Message message, Object... args) {
            state().setDefaultWarning(message, args);
            return (THIS) this;
        }

        default THIS shouldBe(ArgumentFilter<? super T, ? extends T> argFilter, RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.ArgumentValueConditionNodeGen.create(argFilter, true, message, messageArgs, state().boxPrimitives, state().cb.out));
            return (THIS) this;
        }

        default THIS shouldBe(ArgumentFilter<? super T, ? extends T> argFilter) {
            return shouldBe(argFilter, state().defaultWarning().message, state().defaultWarning().args);
        }

    }

    interface ArgCastBuilderFactory {

        InitialPhaseBuilder<Object> newInitialPhaseBuilder();

        <T> InitialPhaseBuilder<T> newInitialPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder);

        <T extends RAbstractVector, S> CoercedPhaseBuilder<T, S> newCoercedPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder, Class<?> elementClass);

        <T> HeadPhaseBuilder<T> newHeadPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder);

    }

    static class DefaultError {
        final RError.Message message;
        final Object[] args;

        DefaultError(RError.Message message, Object... args) {
            this.message = message;
            this.args = args;
        }

    }

    static class ArgCastBuilderState {
        private final DefaultError defaultDefaultError;

        private final int argumentIndex;
        private final String argumentName;
        final ArgCastBuilderFactory factory;
        private final CastBuilder cb;
        final boolean boxPrimitives;
        private DefaultError defError;
        private DefaultError defWarning;

        ArgCastBuilderState(int argumentIndex, String argumentName, ArgCastBuilderFactory fact, CastBuilder cb, boolean boxPrimitives) {
            this.argumentIndex = argumentIndex;
            this.argumentName = argumentName;
            this.factory = fact;
            this.cb = cb;
            this.boxPrimitives = boxPrimitives;
            this.defaultDefaultError = new DefaultError(RError.Message.INVALID_ARGUMENT, argumentName);
        }

        ArgCastBuilderState(ArgCastBuilderState prevState, boolean boxPrimitives) {
            this.argumentIndex = prevState.argumentIndex;
            this.argumentName = prevState.argumentName;
            this.factory = prevState.factory;
            this.cb = prevState.cb;
            this.boxPrimitives = boxPrimitives;
            this.defError = prevState.defError;
            this.defWarning = prevState.defWarning;
            this.defaultDefaultError = new DefaultError(RError.Message.INVALID_ARGUMENT, argumentName);
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

        boolean isDefaultErrorDefined() {
            return defError != null;
        }

        boolean isDefaultWarningDefined() {
            return defWarning != null;
        }

        void setDefaultError(RError.Message message, Object... args) {
            defError = new DefaultError(message, args);
        }

        void setDefaultWarning(RError.Message message, Object... args) {
            defWarning = new DefaultError(message, args);
        }

        DefaultError defaultError() {
            return defError == null ? defaultDefaultError : defError;
        }

        DefaultError defaultError(RError.Message defaultDefaultMessage, Object... defaultDefaultArgs) {
            return defError == null ? new DefaultError(defaultDefaultMessage, defaultDefaultArgs) : defError;
        }

        DefaultError defaultWarning() {
            return defWarning == null ? defaultDefaultError : defWarning;
        }

        DefaultError defaultWarning(RError.Message defaultDefaultMessage, Object... defaultDefaultArgs) {
            return defWarning == null ? new DefaultError(defaultDefaultMessage, defaultDefaultArgs) : defWarning;
        }

        void mustBe(ArgumentFilter<?, ?> argFilter, RError.Message message, Object... messageArgs) {
            castBuilder().insert(index(), CastFunctionsFactory.ArgumentValueConditionNodeGen.create(argFilter, false, message, messageArgs, boxPrimitives, cb.out));
        }

        void mustBe(ArgumentFilter<?, ?> argFilter) {
            mustBe(argFilter, defaultError().message, defaultError().args);
        }

        void shouldBe(ArgumentFilter<?, ?> argFilter, RError.Message message, Object... messageArgs) {
            castBuilder().insert(index(), CastFunctionsFactory.ArgumentValueConditionNodeGen.create(argFilter, true, message, messageArgs, boxPrimitives, cb.out));
        }

        void shouldBe(ArgumentFilter<?, ?> argFilter) {
            shouldBe(argFilter, defaultWarning().message, defaultWarning().args);
        }

    }

    abstract class ArgCastBuilderBase<T, THIS> implements ArgCastBuilder<T, THIS> {

        private final ArgCastBuilderState st;

        ArgCastBuilderBase(ArgCastBuilderState state) {
            this.st = state;
        }

        public ArgCastBuilderState state() {
            return st;
        }
    }

    public interface InitialPhaseBuilder<T> extends ArgCastBuilder<T, InitialPhaseBuilder<T>> {

        default <S extends T> InitialPhaseBuilder<S> mustBe(ArgumentFilter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T> InitialPhaseBuilder<S> mustBe(ArgumentFilter<? super T, S> argFilter) {
            return mustBe(argFilter, state().defaultError().message, state().defaultError().args);
        }

        default <S> InitialPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            mustBe(ValuePredicateArgumentFilter.fromLambda(x -> cls.isInstance(x), cls), message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> mustBe(Class<S> cls) {
            mustBe(ValuePredicateArgumentFilter.fromLambda(x -> cls.isInstance(x), cls));
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> map(ArgumentMapper<T, S> mapFn) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.MapNodeGen.create(mapFn));
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S extends T, R> InitialPhaseBuilder<S> mapIf(ArgumentFilter<? super T, ? extends S> argFilter, ArgumentMapper<S, R> mapFn) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.ConditionalMapNodeGen.create(argFilter, mapFn));
            return state().factory.newInitialPhaseBuilder(this);
        }

        default InitialPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create(message, messageArgs, state().cb.out, null));
            return this;
        }

        default InitialPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create(message, messageArgs, state().cb.out, naReplacement));
            return this;
        }

        default InitialPhaseBuilder<T> notNA() {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create(state().defaultError().message, state().defaultError().args, state().cb.out, RNull.instance));
            return this;
        }

        default InitialPhaseBuilder<T> notNA(T naReplacement) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create(naReplacement));
            return this;
        }

        default CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().castBuilder().toInteger(state().index(), preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newCoercedPhaseBuilder(this, Integer.class);
        }

        default CoercedPhaseBuilder<RAbstractIntVector, Integer> asIntegerVector() {
            return asIntegerVector(false, false, false);
        }

        default CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().castBuilder().toDouble(state().index(), preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newCoercedPhaseBuilder(this, Double.class);
        }

        default CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDoubleVector() {
            return asDoubleVector(false, false, false);
        }

        default CoercedPhaseBuilder<RAbstractLogicalVector, Byte> asLogicalVector() {
            state().castBuilder().toLogical(state().index());
            return state().factory.newCoercedPhaseBuilder(this, Byte.class);
        }

        default CoercedPhaseBuilder<RAbstractStringVector, String> asStringVector() {
            state().castBuilder().toCharacter(state().index());
            return state().factory.newCoercedPhaseBuilder(this, String.class);
        }

        default CoercedPhaseBuilder<RAbstractVector, Object> asVector() {
            state().castBuilder().toVector(state().index());
            return state().factory.newCoercedPhaseBuilder(this, Object.class);
        }

        default HeadPhaseBuilder<RAttributable> asAttributable(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().castBuilder().toAttributable(state().index(), preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newHeadPhaseBuilder(this);
        }

    }

    public interface CoercedPhaseBuilder<T extends RAbstractVector, S> extends ArgCastBuilder<T, CoercedPhaseBuilder<T, S>> {

        /**
         * The inserted cast node returns the default value if the input vector is empty. It also
         * reports the warning message.
         */
        default HeadPhaseBuilder<S> findFirst(S defaultValue, RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.FindFirstNodeGen.create(elementClass(), message, messageArgs, state().cb.out, defaultValue));
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node raises an error if the input vector is empty.
         */
        default HeadPhaseBuilder<S> findFirst(RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.FindFirstNodeGen.create(elementClass(), message, messageArgs, state().cb.out, null));
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node raises the default error, if defined, or
         * RError.Message.LENGTH_ZERO error if the input vector is empty.
         */
        default HeadPhaseBuilder<S> findFirst() {
            DefaultError err = state().isDefaultErrorDefined() ? state().defaultError() : new DefaultError(RError.Message.LENGTH_ZERO);
            state().castBuilder().insert(state().index(),
                            CastFunctionsFactory.FindFirstNodeGen.create(elementClass(), err.message, err.args, state().cb.out, null));
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node returns the default value if the input vector is empty. It reports
         * no warning message.
         */
        default HeadPhaseBuilder<S> findFirst(S defaultValue) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.FindFirstNodeGen.create(elementClass(), defaultValue));
            return state().factory.newHeadPhaseBuilder(this);
        }

        Class<?> elementClass();

        default CoercedPhaseBuilder<T, S> mustBe(ArgumentFilter<? super T, ? extends T> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, message, messageArgs);
            return state().factory.newCoercedPhaseBuilder(this, elementClass());
        }

        default CoercedPhaseBuilder<T, S> mustBe(ArgumentFilter<? super T, ? extends T> argFilter) {
            return mustBe(argFilter, state().defaultError().message, state().defaultError().args);
        }

    }

    public interface HeadPhaseBuilder<T> extends ArgCastBuilder<T, HeadPhaseBuilder<T>> {

        default <S> HeadPhaseBuilder<S> map(ArgumentMapper<T, S> mapFn) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.MapNodeGen.create(mapFn));
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T, R> HeadPhaseBuilder<S> mapIf(ArgumentFilter<? super T, ? extends S> argFilter, ArgumentMapper<S, R> mapFn) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.ConditionalMapNodeGen.create(argFilter, mapFn));

            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T> HeadPhaseBuilder<S> mustBe(ArgumentFilter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S extends T> HeadPhaseBuilder<S> mustBe(ArgumentFilter<? super T, S> argFilter) {
            return mustBe(argFilter, state().defaultError().message, state().defaultError().args);
        }

        default <S> InitialPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            mustBe(ValuePredicateArgumentFilter.fromLambda(x -> cls.isInstance(x), cls), message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> mustBe(Class<S> cls) {
            mustBe(ValuePredicateArgumentFilter.fromLambda(x -> cls.isInstance(x), cls));
            return state().factory.newInitialPhaseBuilder(this);
        }

        default HeadPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create(message, messageArgs, state().cb.out, null));
            return this;
        }

        default HeadPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create(message, messageArgs, state().cb.out, naReplacement));
            return this;
        }

        default HeadPhaseBuilder<T> notNA() {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create(state().defaultError().message, state().defaultError().args, state().cb.out, RNull.instance));
            return this;
        }

        default HeadPhaseBuilder<T> notNA(T naReplacement) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create(naReplacement));
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

        public InitialPhaseBuilderImpl<Object> newInitialPhaseBuilder() {
            return new InitialPhaseBuilderImpl<>();
        }

        public <T> InitialPhaseBuilderImpl<T> newInitialPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder) {
            return new InitialPhaseBuilderImpl<>(currentBuilder.state());
        }

        public <T extends RAbstractVector, S> CoercedPhaseBuilderImpl<T, S> newCoercedPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder, Class<?> elementClass) {
            return new CoercedPhaseBuilderImpl<>(currentBuilder.state(), elementClass);
        }

        public <T> HeadPhaseBuilderImpl<T> newHeadPhaseBuilder(ArgCastBuilder<?, ?> currentBuilder) {
            return new HeadPhaseBuilderImpl<>(currentBuilder.state());
        }

        public final class InitialPhaseBuilderImpl<T> extends ArgCastBuilderBase<T, InitialPhaseBuilder<T>> implements InitialPhaseBuilder<T> {
            InitialPhaseBuilderImpl(ArgCastBuilderState state) {
                super(new ArgCastBuilderState(state, true));
            }

            InitialPhaseBuilderImpl() {
                super(new ArgCastBuilderState(argumentIndex, argumentName, ArgCastBuilderFactoryImpl.this, CastBuilder.this, false));
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

}
