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

import static com.oracle.truffle.r.nodes.builtin.TypePredicates.is;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.TypePredicates.ReflectiveFunction;
import com.oracle.truffle.r.nodes.builtin.TypePredicates.ReflectivePredicate;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastFunctionsFactory;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalScalarNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.ConvertIntNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstBooleanNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstIntNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.MessagePredicate;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RComplex;
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

        throw RError.error(builtinNode, RError.Message.GENERIC, String.format("No %s argument found in builtin %s", argumentName, builtinNode.getBuiltin().getName()));
    }

    @FunctionalInterface
    public static interface CastFunction0<R> {
        R apply(CastNode node);
    }

    @FunctionalInterface
    public static interface CastFunction<T, R> {
        R apply(CastNode node, T arg);
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

    private static Object[] substituteArgPlaceholder(Object arg, Object... messageArgs) {
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

    @SuppressWarnings("unchecked")
    interface ArgCastBuilder<THIS> {

        ArgCastBuilderState state();

        default THIS defaultError(RError.Message message, Object... args) {
            state().setDefaultError(message, args);
            return (THIS) this;
        }

        default THIS defaultWarning(RError.Message message, Object... args) {
            state().setDefaultWarning(message, args);
            return (THIS) this;
        }

    }

    interface ArgCastBuilderFactory {

        InitialPhaseBuilder<Object> newInitialPhaseBuilder();

        <T> InitialPhaseBuilder<T> newInitialPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes);

        <T extends RAbstractVector, S> CoercedPhaseBuilder<T, S> newCoercedPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes);

        LogicalCoercedPhaseBuilder newLogicalCoercedPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes);

        <T> HeadPhaseBuilder<T> newHeadPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes);

        <T> FinalPhaseBuilder<T> newFinalPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes);

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
        private final Set<Class<?>> possibleTypes;

        ArgCastBuilderState(int argumentIndex, String argumentName, ArgCastBuilderFactory fact, CastBuilder cb, Set<Class<?>> possibleTypes, boolean boxPrimitives) {
            this.argumentIndex = argumentIndex;
            this.argumentName = argumentName;
            this.factory = fact;
            this.cb = cb;
            this.boxPrimitives = boxPrimitives;
            this.possibleTypes = Collections.unmodifiableSet(new HashSet<>(possibleTypes));
            this.defaultDefaultError = new DefaultError(RError.Message.INVALID_ARGUMENT, argumentName);
        }

        ArgCastBuilderState(ArgCastBuilderState prevState, boolean boxPrimitives, Set<Class<?>> possibleTypes) {
            this.argumentIndex = prevState.argumentIndex;
            this.argumentName = prevState.argumentName;
            this.factory = prevState.factory;
            this.cb = prevState.cb;
            this.boxPrimitives = boxPrimitives;
            this.defError = prevState.defError;
            this.defWarning = prevState.defWarning;
            this.possibleTypes = Collections.unmodifiableSet(new HashSet<>(possibleTypes));
            this.defaultDefaultError = new DefaultError(RError.Message.INVALID_ARGUMENT, argumentName);
        }

        public int index() {
            return argumentIndex;
        }

        public String name() {
            return argumentName;
        }

        public Set<Class<?>> contextTypes() {
            return possibleTypes;
        }

        public CastBuilder castBuilder() {
            return cb;
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
    }

    abstract class ArgCastBuilderBase<THIS> implements ArgCastBuilder<THIS> {

        private final ArgCastBuilderState st;

        ArgCastBuilderBase(ArgCastBuilderState state) {
            this.st = state;
        }

        public ArgCastBuilderState state() {
            return st;
        }
    }

    @SuppressWarnings("unchecked")
    public interface ValidationBuilder<T, THIS> extends ArgCastBuilder<THIS> {

        default THIS require(CastFunction<? super T, Boolean> condition, CastFunction<? super T, Void> success, CastFunction<? super T, Void> failure) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.ArgumentConditionNodeGen.create(condition, success, failure, state().boxPrimitives));
            return (THIS) this;
        }

        default THIS error(Predicate<? super T> validator, RError.Message message, Object... messageArgs) {
            require((n, x) -> validator.test(x),
                            (n, x) -> null,
                            (n, x) -> {
                                if (RContext.getRRuntimeASTAccess() == null) {
                                    throw new IllegalArgumentException(String.format(message.message, substituteArgPlaceholder(x, messageArgs)));
                                } else {
                                    throw RError.error(n, message, substituteArgPlaceholder(x, messageArgs));
                                }
                            });
            return (THIS) this;
        }

        default THIS error(MessagePredicate messagePred, Object... messageArgs) {
            return error((Predicate<T>) messagePred.getPredicate(), messagePred.getMessage(), messageArgs);
        }

        default THIS error(Predicate<? super T> validator) {
            DefaultError defaultError = state().defaultError();
            return error(validator, defaultError.message, defaultError.args);
        }

        default THIS warning(Predicate<? super T> validator, RError.Message message, Object... messageArgs) {
            require((n, x) -> validator.test(x),
                            (n, x) -> null,
                            (n, x) -> {
                                if (state().castBuilder().out != null) {
                                    state().castBuilder().out.printf(message.message, substituteArgPlaceholder(x, messageArgs));
                                } else if (RContext.getRRuntimeASTAccess() == null) {
                                    System.err.println(String.format(message.message, substituteArgPlaceholder(x, messageArgs)));
                                } else {
                                    RError.warning(n, message, substituteArgPlaceholder(x, messageArgs));
                                }
                                return null;
                            });
            return (THIS) this;
        }

        default THIS warning(MessagePredicate messagePred, Object... messageArgs) {
            return warning((Predicate<T>) messagePred.getPredicate(), messagePred.getMessage(), messageArgs);
        }

        default THIS warning(Predicate<? super T> validator) {
            DefaultError defaultWarning = state().defaultWarning();
            return error(validator, defaultWarning.message, defaultWarning.args);
        }
    }

    interface CommonValidationBuilderUtils<T, THIS> extends ValidationBuilder<T, THIS> {

        default THIS notNull(RError.Message message, Object... messageArgs) {
            return error(x -> x != RNull.instance && x != null, message, messageArgs);
        }

        default THIS notNull() {
            return error(x -> x != RNull.instance && x != null);
        }

        default THIS instanceOf(Class<?> cls, RError.Message message, Object... messageArgs) {
            return error(x -> cls.isInstance(x), message, messageArgs);
        }

        default THIS instanceOf(Class<?> cls) {
            return error(x -> cls.isInstance(x));
        }
    }

    public static final ReflectivePredicate<Integer> IS_SCALAR_INTEGER = x -> is(x instanceof Integer);
    public static final ReflectivePredicate<RAbstractIntVector> IS_INTEGER = x -> is(x instanceof Integer || x instanceof RAbstractIntVector);
    public static final ReflectivePredicate<String> IS_SCALAR_STRING = x -> is(x instanceof String);
    public static final ReflectivePredicate<RAbstractStringVector> IS_STRING = x -> is(x instanceof String || x instanceof RAbstractStringVector);
    public static final ReflectivePredicate<Double> IS_SCALAR_DOUBLE = x -> is(x instanceof Double);
    public static final ReflectivePredicate<RAbstractDoubleVector> IS_DOUBLE = x -> is(x instanceof Double || x instanceof RAbstractDoubleVector);
    public static final ReflectivePredicate<Byte> IS_SCALAR_LOGICAL = x -> is(x instanceof Byte);
    public static final ReflectivePredicate<Byte> IS_SCALAR_BOOLEAN = x -> is(x instanceof Boolean);
    public static final ReflectivePredicate<RAbstractLogicalVector> IS_LOGICAL = x -> is(x instanceof Byte || x instanceof RAbstractLogicalVector);
    public static final ReflectivePredicate<RComplex> IS_SCALAR_COMPLEX = x -> is(x instanceof RComplex);
    public static final ReflectivePredicate<RAbstractComplexVector> IS_COMPLEX = x -> is(x instanceof RComplex || x instanceof RAbstractComplexVector);
    public static final ReflectivePredicate<? extends RAbstractVector> IS_NUMERIC = IS_INTEGER.or(IS_DOUBLE).or(IS_COMPLEX).or(IS_LOGICAL);

    public interface InitialValidationBuilderUtils<T, THIS> extends CommonValidationBuilderUtils<T, THIS> {

        default <S> InitialPhaseBuilder<S> is(ReflectivePredicate<S> typePredicate, RError.Message message, Object... messageArgs) {
            error(typePredicate.predicate(), message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this, typePredicate.returnTypes());
        }

        default <S> InitialPhaseBuilder<S> is(ReflectivePredicate<S> typePredicate) {
            error(typePredicate.predicate());
            return state().factory.newInitialPhaseBuilder(this, typePredicate.returnTypes());
        }

        default InitialPhaseBuilder<RAbstractIntVector> isInteger(RError.Message message, Object... messageArgs) {
            return is(IS_INTEGER, message, messageArgs);
        }

        default InitialPhaseBuilder<RAbstractIntVector> isInteger() {
            return is(IS_INTEGER);
        }

        default InitialPhaseBuilder<RAbstractStringVector> isString(RError.Message message, Object... messageArgs) {
            return is(IS_STRING, message, messageArgs);
        }

        default InitialPhaseBuilder<RAbstractStringVector> isString() {
            return is(IS_STRING);
        }

        default InitialPhaseBuilder<RAbstractDoubleVector> isDouble(RError.Message message, Object... messageArgs) {
            return is(IS_DOUBLE, message, messageArgs);
        }

        default InitialPhaseBuilder<RAbstractDoubleVector> isDouble() {
            return is(IS_DOUBLE);
        }

        default InitialPhaseBuilder<RAbstractLogicalVector> isLogical(RError.Message message, Object... messageArgs) {
            return is(IS_LOGICAL, message, messageArgs);
        }

        default InitialPhaseBuilder<RAbstractLogicalVector> isLogical() {
            return is(IS_LOGICAL);
        }

        default InitialPhaseBuilder<RAbstractComplexVector> isComplex(RError.Message message, Object... messageArgs) {
            return is(IS_COMPLEX, message, messageArgs);
        }

        default InitialPhaseBuilder<RAbstractComplexVector> isComplex() {
            return is(IS_COMPLEX);
        }

        default InitialPhaseBuilder<? extends RAbstractVector> isNumeric(RError.Message message, Object... messageArgs) {
            return is(IS_NUMERIC, message, messageArgs);
        }

        default InitialPhaseBuilder<? extends RAbstractVector> isNumeric() {
            return is(IS_NUMERIC);
        }
    }

    public interface VectorValidationBuilderUtils<T extends RAbstractVector, THIS> extends CommonValidationBuilderUtils<T, THIS> {

        default THIS emptyError(RError.Message message, Object... messageArgs) {
            return error(x -> x.getLength() > 0, message, messageArgs);
        }

        default THIS emptyError() {
            DefaultError defaultError = state().defaultError(RError.Message.LENGTH_ZERO);
            return emptyError(defaultError.message, defaultError.args);
        }

        default THIS emptyWarning(RError.Message message, Object... messageArgs) {
            return warning(x -> x.getLength() > 0, message, messageArgs);
        }

        default THIS emptyWarning() {
            DefaultError defaultError = state().defaultError(RError.Message.LENGTH_ZERO);
            return emptyWarning(defaultError.message, defaultError.args);
        }

        default THIS sizeError(RError.Message message, Object... messageArgs) {
            return error(x -> x.getLength() <= 1, message, messageArgs);
        }

        default THIS sizeError() {
            DefaultError defaultError = state().defaultError(RError.Message.LENGTH_GT_1);
            return sizeError(defaultError.message, defaultError.args);
        }

        default THIS sizeWarning(RError.Message message, Object... messageArgs) {
            return warning(x -> x.getLength() <= 1, message, messageArgs);
        }

        default THIS sizeWarning() {
            DefaultError defaultWarning = state().defaultWarning(RError.Message.LENGTH_GT_1);
            return sizeWarning(defaultWarning.message, defaultWarning.args);
        }
    }

    public interface Coercions<THIS> extends ArgCastBuilder<THIS> {

        default CoercedPhaseBuilder<RAbstractIntVector, Integer> asInteger(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().castBuilder().toInteger(state().index(), preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newCoercedPhaseBuilder(this, Collections.singleton(RAbstractIntVector.class));
        }

        default CoercedPhaseBuilder<RAbstractIntVector, Integer> asInteger() {
            return asInteger(false, false, false);
        }

        default CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDouble(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().castBuilder().toDouble(state().index(), preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newCoercedPhaseBuilder(this, Collections.singleton(RAbstractDoubleVector.class));
        }

        default CoercedPhaseBuilder<RAbstractDoubleVector, Double> asDouble() {
            return asDouble(false, false, false);
        }

        default LogicalCoercedPhaseBuilder asLogical() {
            state().castBuilder().toLogical(state().index());
            return state().factory.newLogicalCoercedPhaseBuilder(this, Collections.singleton(RAbstractLogicalVector.class));
        }

        default CoercedPhaseBuilder<RAbstractStringVector, String> asString() {
            state().castBuilder().toCharacter(state().index());
            return state().factory.newCoercedPhaseBuilder(this, Collections.singleton(RAbstractStringVector.class));
        }

        default CoercedPhaseBuilder<RAbstractVector, Object> asVector() {
            state().castBuilder().toVector(state().index());
            HashSet<Class<?>> newTypes = state().possibleTypes.stream().filter(x -> RAbstractVector.class.isAssignableFrom(x)).collect(Collectors.toCollection(HashSet::new));
            if (newTypes.isEmpty()) {
                return state().factory.newCoercedPhaseBuilder(this, Collections.singleton(RAbstractVector.class));
            } else {
                return state().factory.newCoercedPhaseBuilder(this, newTypes);
            }
        }

        default HeadPhaseBuilder<RAttributable> asAttributable(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
            state().castBuilder().toAttributable(state().index(), preserveNames, dimensionsPreservation, attrPreservation);
            return state().factory.newHeadPhaseBuilder(this, Collections.singleton(RAttributable.class));
        }
    }

    public interface InitialPhaseBuilder<T> extends Coercions<InitialPhaseBuilder<T>>, ValidationBuilder<Object, InitialPhaseBuilder<T>>, InitialValidationBuilderUtils<Object, InitialPhaseBuilder<T>> {

        default <S> InitialPhaseBuilder<S> map(ReflectiveFunction<T, S> mapFn) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.MapNodeGen.create(mapFn));
            return state().factory.newInitialPhaseBuilder(this, Collections.singleton(mapFn.returnType()));
        }

    }

    public interface FindFirstBuilder<T extends RAbstractVector, S, THIS> extends ArgCastBuilder<THIS> {

        default HeadPhaseBuilder<S> findFirst() {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.FindFirstNodeGen.create());
            HashSet<Class<?>> elementTypes = state().possibleTypes.stream().map(x -> elementType(x)).collect(Collectors.toCollection(HashSet::new));
            return state().factory.newHeadPhaseBuilder(this, elementTypes);
        }
    }

    public interface CoercedPhaseBuilder<T extends RAbstractVector, S> extends FindFirstBuilder<T, S, CoercedPhaseBuilder<T, S>>,
                    ValidationBuilder<T, CoercedPhaseBuilder<T, S>>, VectorValidationBuilderUtils<T, CoercedPhaseBuilder<T, S>> {

    }

    public interface LogicalCoercedPhaseBuilder extends FindFirstBuilder<RAbstractLogicalVector, Byte, LogicalCoercedPhaseBuilder>,
                    ValidationBuilder<RAbstractLogicalVector, LogicalCoercedPhaseBuilder>, VectorValidationBuilderUtils<RAbstractLogicalVector, LogicalCoercedPhaseBuilder> {

        default HeadPhaseBuilder<Boolean> findFirstBoolean() {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.FindFirstBooleanNodeGen.create());
            HashSet<Class<?>> elementTypes = state().possibleTypes.stream().map(x -> elementType(x)).collect(Collectors.toCollection(HashSet::new));
            return state().factory.newHeadPhaseBuilder(this, elementTypes);
        }

    }

    @SuppressWarnings("unchecked")
    public interface HeadOperations<T, THIS> extends ArgCastBuilder<THIS> {

        default FinalPhaseBuilder<T> isPresent(CastFunction<T, T> present, CastFunction0<T> missing) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.OptionalElementNodeGen.create(present, missing));
            return state().factory.newFinalPhaseBuilder(this, state().possibleTypes);
        }

        default FinalPhaseBuilder<T> orElse(T other) {
            return isPresent((n, x) -> x, (n) -> other);
        }

        default FinalPhaseBuilder<T> orElseThrow(RError.Message message, Object... messageArgs) {
            return isPresent((n, x) -> x,
                            (n) -> {
                                if (RContext.getRRuntimeASTAccess() == null) {
                                    throw new IllegalArgumentException(String.format(message.message, messageArgs));
                                } else {
                                    throw RError.error(n, message, messageArgs);
                                }
                            });
        }

        default FinalPhaseBuilder<T> orElseThrow() {
            DefaultError defaultError = state().defaultError();
            return orElseThrow(defaultError.message, defaultError.args);
        }

        default THIS noNA() {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.NonNANodeGen.create());
            return (THIS) this;
        }
    }

    public interface HeadPhaseBuilder<T> extends HeadOperations<T, HeadPhaseBuilder<T>>, ValidationBuilder<T, HeadPhaseBuilder<T>> {

        default <S> HeadPhaseBuilder<S> map(ReflectiveFunction<T, S> mapFn) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.MapNodeGen.create(mapFn));
            return state().factory.newHeadPhaseBuilder(this, Collections.singleton(mapFn.returnType()));
        }

    }

    public interface FinalMapper<T> extends ArgCastBuilder<FinalPhaseBuilder<T>> {

        default <S, R> FinalPhaseBuilder<R> mapIf(ReflectivePredicate<S> typePredicate, Function<S, R> mapFn) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.ConditionalMapNodeGen.create(typePredicate.predicate(), mapFn));

            Set<Class<?>> typesToRemove = typePredicate.returnTypes();
            Set<Class<?>> newTypes = new HashSet<>(state().possibleTypes);
            newTypes.removeIf(x -> typesToRemove.contains(x));

            return state().factory.newFinalPhaseBuilder(this, newTypes);
        }

        default <S> FinalPhaseBuilder<S> map(ReflectiveFunction<T, S> mapFn) {
            state().castBuilder().insert(state().index(), CastFunctionsFactory.MapNodeGen.create(mapFn));
            return state().factory.newFinalPhaseBuilder(this, Collections.singleton(mapFn.returnType()));
        }

    }

    public interface FinalPhaseBuilder<T> extends ArgCastBuilder<FinalPhaseBuilder<T>>, FinalMapper<T>, ValidationBuilder<T, FinalPhaseBuilder<T>> {
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

        public <T> InitialPhaseBuilderImpl<T> newInitialPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes) {
            return new InitialPhaseBuilderImpl<>(currentBuilder.state(), possibleTypes);
        }

        public <T extends RAbstractVector, S> CoercedPhaseBuilderImpl<T, S> newCoercedPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes) {
            return new CoercedPhaseBuilderImpl<>(currentBuilder.state(), possibleTypes);
        }

        public LogicalCoercedPhaseBuilderImpl newLogicalCoercedPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes) {
            return new LogicalCoercedPhaseBuilderImpl(currentBuilder.state(), possibleTypes);
        }

        public <T> HeadPhaseBuilderImpl<T> newHeadPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes) {
            return new HeadPhaseBuilderImpl<>(currentBuilder.state(), possibleTypes);
        }

        public <T> FinalPhaseBuilderImpl<T> newFinalPhaseBuilder(ArgCastBuilder<?> currentBuilder, Set<Class<?>> possibleTypes) {
            return new FinalPhaseBuilderImpl<>(currentBuilder.state(), possibleTypes);
        }

        public final class InitialPhaseBuilderImpl<T> extends ArgCastBuilderBase<InitialPhaseBuilder<T>> implements InitialPhaseBuilder<T> {
            InitialPhaseBuilderImpl(ArgCastBuilderState state, Set<Class<?>> possibleTypes) {
                super(new ArgCastBuilderState(state, true, possibleTypes));
            }

            InitialPhaseBuilderImpl() {
                super(new ArgCastBuilderState(argumentIndex, argumentName, ArgCastBuilderFactoryImpl.this, CastBuilder.this, Collections.singleton(Object.class), false));
            }
        }

        public final class CoercedPhaseBuilderImpl<T extends RAbstractVector, S> extends ArgCastBuilderBase<CoercedPhaseBuilder<T, S>> implements CoercedPhaseBuilder<T, S> {

            CoercedPhaseBuilderImpl(ArgCastBuilderState state, Set<Class<?>> possibleTypes) {
                super(new ArgCastBuilderState(state, true, possibleTypes));
            }

        }

        public final class LogicalCoercedPhaseBuilderImpl extends ArgCastBuilderBase<LogicalCoercedPhaseBuilder> implements LogicalCoercedPhaseBuilder {
            LogicalCoercedPhaseBuilderImpl(ArgCastBuilderState state, Set<Class<?>> possibleTypes) {
                super(new ArgCastBuilderState(state, true, possibleTypes));
            }
        }

        public final class HeadPhaseBuilderImpl<T> extends ArgCastBuilderBase<HeadPhaseBuilder<T>> implements HeadPhaseBuilder<T> {
            HeadPhaseBuilderImpl(ArgCastBuilderState state, Set<Class<?>> possibleTypes) {
                super(new ArgCastBuilderState(state, false, possibleTypes));
            }
        }

        public final class FinalPhaseBuilderImpl<T> extends ArgCastBuilderBase<FinalPhaseBuilder<T>> implements FinalPhaseBuilder<T> {
            FinalPhaseBuilderImpl(ArgCastBuilderState state, Set<Class<?>> possibleTypes) {
                super(new ArgCastBuilderState(state, false, possibleTypes));
            }
        }

    }

    private static Class<?> elementType(Class<?> vectorType) {
        if (RAbstractIntVector.class.isAssignableFrom(vectorType)) {
            return Integer.class;
        }
        if (RAbstractDoubleVector.class.isAssignableFrom(vectorType)) {
            return Double.class;
        }
        if (RAbstractLogicalVector.class.isAssignableFrom(vectorType)) {
            return Byte.class;
        }
        if (RAbstractStringVector.class.isAssignableFrom(vectorType)) {
            return String.class;
        }
        if (RAbstractComplexVector.class.isAssignableFrom(vectorType)) {
            return RComplex.class;
        }
        throw new IllegalArgumentException("Unsupported vector type " + vectorType);
    }

}
