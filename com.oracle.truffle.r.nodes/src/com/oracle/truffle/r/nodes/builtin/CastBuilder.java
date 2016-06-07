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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Objects;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter.ArgumentTypeFilter;
import com.oracle.truffle.r.nodes.unary.CastDoubleBaseNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleBaseNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerBaseNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerBaseNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalBaseNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalBaseNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalScalarNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastStringBaseNode;
import com.oracle.truffle.r.nodes.unary.CastStringBaseNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.ConditionalMapNodeGen;
import com.oracle.truffle.r.nodes.unary.ConvertIntNodeGen;
import com.oracle.truffle.r.nodes.unary.FilterNodeGen;
import com.oracle.truffle.r.nodes.unary.FindFirstNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstBooleanNodeGen;
import com.oracle.truffle.r.nodes.unary.FirstIntNode;
import com.oracle.truffle.r.nodes.unary.FirstStringNode;
import com.oracle.truffle.r.nodes.unary.MapNode;
import com.oracle.truffle.r.nodes.unary.MapNodeGen;
import com.oracle.truffle.r.nodes.unary.NonNANodeGen;
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
        return arg(argumentIndex, builtinNode == null ? null : builtinNode.getRBuiltin().parameterNames()[argumentIndex]);
    }

    public InitialPhaseBuilder<Object> arg(String argumentName) {
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

    public CastBuilder output(OutputStream o) {
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

    public interface PredefFilters {

        <T> ValuePredicateArgumentFilter<T> sameAs(T x);

        <T> ValuePredicateArgumentFilter<T> equalTo(T x);

        <T, R extends T> TypePredicateArgumentFilter<T, R> nullValue();

        <T extends RAbstractVector> VectorPredicateArgumentFilter<T> notEmpty();

        <T extends RAbstractVector> VectorPredicateArgumentFilter<T> singleElement();

        <T extends RAbstractVector, R extends T> VectorPredicateArgumentFilter<T> size(int s);

        ValuePredicateArgumentFilter<Boolean> trueValue();

        ValuePredicateArgumentFilter<Boolean> falseValue();

        ValuePredicateArgumentFilter<Byte> logicalTrue();

        ValuePredicateArgumentFilter<Byte> logicalFalse();

        ValuePredicateArgumentFilter<Integer> intNA();

        ValuePredicateArgumentFilter<Integer> notIntNA();

        ValuePredicateArgumentFilter<Byte> logicalNA();

        ValuePredicateArgumentFilter<Byte> notLogicalNA();

        ValuePredicateArgumentFilter<Double> doubleNA();

        ValuePredicateArgumentFilter<Double> notDoubleNA();

        ValuePredicateArgumentFilter<String> stringNA();

        ValuePredicateArgumentFilter<String> notStringNA();

        ValuePredicateArgumentFilter<Integer> eq(int x);

        ValuePredicateArgumentFilter<Double> eq(double x);

        ValuePredicateArgumentFilter<Integer> neq(int x);

        ValuePredicateArgumentFilter<Double> neq(double x);

        ValuePredicateArgumentFilter<Integer> gt(int x);

        ValuePredicateArgumentFilter<Double> gt(double x);

        ValuePredicateArgumentFilter<Integer> gte(int x);

        ValuePredicateArgumentFilter<Double> gte(double x);

        ValuePredicateArgumentFilter<Integer> lt(int x);

        ValuePredicateArgumentFilter<Double> lt(double x);

        ValuePredicateArgumentFilter<Integer> lte(int x);

        ValuePredicateArgumentFilter<Double> lte(double x);

        ValuePredicateArgumentFilter<String> length(int l);

        ValuePredicateArgumentFilter<String> isEmpty();

        ValuePredicateArgumentFilter<String> lengthGt(int l);

        ValuePredicateArgumentFilter<String> lengthGte(int l);

        ValuePredicateArgumentFilter<String> lengthLt(int l);

        ValuePredicateArgumentFilter<String> lengthLte(int l);

        ValuePredicateArgumentFilter<Integer> gt0();

        ValuePredicateArgumentFilter<Integer> gte0();

        ValuePredicateArgumentFilter<Integer> gt1();

        ValuePredicateArgumentFilter<Integer> gte1();

        <R> TypePredicateArgumentFilter<Object, R> instanceOf(Class<R> cls);

        <R extends RAbstractIntVector> TypePredicateArgumentFilter<Object, R> integerValue();

        <R extends RAbstractStringVector> TypePredicateArgumentFilter<Object, R> stringValue();

        <R extends RAbstractDoubleVector> TypePredicateArgumentFilter<Object, R> doubleValue();

        <R extends RAbstractLogicalVector> TypePredicateArgumentFilter<Object, R> logicalValue();

        <R extends RAbstractComplexVector> TypePredicateArgumentFilter<Object, R> complexValue();

        TypePredicateArgumentFilter<Object, String> scalarStringValue();

        TypePredicateArgumentFilter<Object, Integer> scalarIntegerValue();

        TypePredicateArgumentFilter<Object, Double> scalarDoubleValue();

        TypePredicateArgumentFilter<Object, Byte> scalarLogicalValue();

        TypePredicateArgumentFilter<Object, RComplex> scalarComplexValue();

        TypePredicateArgumentFilter<Object, RMissing> missingValue();

    }

    public interface PredefMappers {
        ValuePredicateArgumentMapper<Byte, Boolean> toBoolean();

        ValuePredicateArgumentMapper<String, Integer> charAt0(int defaultValue);

        ValuePredicateArgumentMapper<String, String> constant(String s);

        ValuePredicateArgumentMapper<Integer, Integer> constant(int i);

        ValuePredicateArgumentMapper<Double, Double> constant(double d);

        ValuePredicateArgumentMapper<Byte, Byte> constant(byte l);

        <T> ArgumentMapper<T, T> defaultValue(T defVal);

    }

    public static final class DefaultPredefFilters implements PredefFilters {

        public <T> ValuePredicateArgumentFilter<T> sameAs(T x) {
            return ValuePredicateArgumentFilter.fromLambda(arg -> arg == x);
        }

        public <T> ValuePredicateArgumentFilter<T> equalTo(T x) {
            return ValuePredicateArgumentFilter.fromLambda(arg -> Objects.equals(arg, x));
        }

        public <T, R extends T> TypePredicateArgumentFilter<T, R> nullValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x == RNull.instance || x == null);
        }

        public <T extends RAbstractVector> VectorPredicateArgumentFilter<T> notEmpty() {
            return new VectorPredicateArgumentFilter<>(x -> x.getLength() > 0, false);
        }

        public <T extends RAbstractVector> VectorPredicateArgumentFilter<T> singleElement() {
            return new VectorPredicateArgumentFilter<>(x -> x.getLength() == 1, false);
        }

        public <T extends RAbstractVector, R extends T> VectorPredicateArgumentFilter<T> size(int s) {
            return new VectorPredicateArgumentFilter<>(x -> x.getLength() == s, false);
        }

        public ValuePredicateArgumentFilter<Boolean> trueValue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x);
        }

        public ValuePredicateArgumentFilter<Boolean> falseValue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> x);
        }

        public ValuePredicateArgumentFilter<Byte> logicalTrue() {
            return ValuePredicateArgumentFilter.fromLambda(x -> RRuntime.LOGICAL_TRUE == x);
        }

        public ValuePredicateArgumentFilter<Byte> logicalFalse() {
            return ValuePredicateArgumentFilter.fromLambda(x -> RRuntime.LOGICAL_FALSE == x);
        }

        public ValuePredicateArgumentFilter<Integer> intNA() {
            return ValuePredicateArgumentFilter.fromLambda((Integer x) -> RRuntime.isNA(x));
        }

        public ValuePredicateArgumentFilter<Integer> notIntNA() {
            return ValuePredicateArgumentFilter.fromLambda((Integer x) -> !RRuntime.isNA(x));
        }

        public ValuePredicateArgumentFilter<Byte> logicalNA() {
            return ValuePredicateArgumentFilter.fromLambda((Byte x) -> RRuntime.isNA(x));
        }

        public ValuePredicateArgumentFilter<Byte> notLogicalNA() {
            return ValuePredicateArgumentFilter.fromLambda((Byte x) -> !RRuntime.isNA(x));
        }

        public ValuePredicateArgumentFilter<Double> doubleNA() {
            return ValuePredicateArgumentFilter.fromLambda((Double x) -> RRuntime.isNA(x));
        }

        public ValuePredicateArgumentFilter<Double> notDoubleNA() {
            return ValuePredicateArgumentFilter.fromLambda((Double x) -> !RRuntime.isNA(x));
        }

        public ValuePredicateArgumentFilter<String> stringNA() {
            return ValuePredicateArgumentFilter.fromLambda((String x) -> RRuntime.isNA(x));
        }

        public ValuePredicateArgumentFilter<String> notStringNA() {
            return ValuePredicateArgumentFilter.fromLambda((String x) -> !RRuntime.isNA(x));
        }

        public ValuePredicateArgumentFilter<Integer> eq(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg != null && arg.intValue() == x);
        }

        public ValuePredicateArgumentFilter<Double> eq(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg != null && arg.doubleValue() == x);
        }

        public ValuePredicateArgumentFilter<Integer> neq(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg == null || arg.intValue() != x);
        }

        public ValuePredicateArgumentFilter<Double> neq(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg == null || arg.doubleValue() != x);
        }

        public ValuePredicateArgumentFilter<Integer> gt(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg > x);
        }

        public ValuePredicateArgumentFilter<Double> gt(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg > x);
        }

        public ValuePredicateArgumentFilter<Integer> gte(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg >= x);
        }

        public ValuePredicateArgumentFilter<Double> gte(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg >= x);
        }

        public ValuePredicateArgumentFilter<Integer> lt(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg < x);
        }

        public ValuePredicateArgumentFilter<Double> lt(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg < x);
        }

        public ValuePredicateArgumentFilter<Integer> lte(int x) {
            return ValuePredicateArgumentFilter.fromLambda((Integer arg) -> arg <= x);
        }

        public ValuePredicateArgumentFilter<Double> lte(double x) {
            return ValuePredicateArgumentFilter.fromLambda((Double arg) -> arg <= x);
        }

        public ValuePredicateArgumentFilter<String> length(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() == l);
        }

        public ValuePredicateArgumentFilter<String> isEmpty() {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.isEmpty());
        }

        public ValuePredicateArgumentFilter<String> lengthGt(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() > l);
        }

        public ValuePredicateArgumentFilter<String> lengthGte(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() >= l);
        }

        public ValuePredicateArgumentFilter<String> lengthLt(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() < l);
        }

        public ValuePredicateArgumentFilter<String> lengthLte(int l) {
            return ValuePredicateArgumentFilter.fromLambda((String arg) -> arg != null && arg.length() <= l);
        }

        public ValuePredicateArgumentFilter<Integer> gt0() {
            return ValuePredicateArgumentFilter.fromLambda((Integer x) -> x > 0);
        }

        public ValuePredicateArgumentFilter<Integer> gte0() {
            return ValuePredicateArgumentFilter.fromLambda((Integer x) -> x >= 0);
        }

        public ValuePredicateArgumentFilter<Integer> gt1() {
            return ValuePredicateArgumentFilter.fromLambda((Integer x) -> x > 1);
        }

        public ValuePredicateArgumentFilter<Integer> gte1() {
            return ValuePredicateArgumentFilter.fromLambda((Integer x) -> x >= 1);
        }

        public <R> TypePredicateArgumentFilter<Object, R> instanceOf(Class<R> cls) {
            return TypePredicateArgumentFilter.fromLambda(x -> cls.isInstance(x));
        }

        public <R extends RAbstractIntVector> TypePredicateArgumentFilter<Object, R> integerValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof Integer || x instanceof RAbstractIntVector);
        }

        public <R extends RAbstractStringVector> TypePredicateArgumentFilter<Object, R> stringValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof String || x instanceof RAbstractStringVector);
        }

        public <R extends RAbstractDoubleVector> TypePredicateArgumentFilter<Object, R> doubleValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof Double || x instanceof RAbstractDoubleVector);
        }

        public <R extends RAbstractLogicalVector> TypePredicateArgumentFilter<Object, R> logicalValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof Byte || x instanceof RAbstractLogicalVector);
        }

        public <R extends RAbstractComplexVector> TypePredicateArgumentFilter<Object, R> complexValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof RComplex || x instanceof RAbstractComplexVector);
        }

        public TypePredicateArgumentFilter<Object, String> scalarStringValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof String);
        }

        public TypePredicateArgumentFilter<Object, Integer> scalarIntegerValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof Integer);
        }

        public TypePredicateArgumentFilter<Object, Double> scalarDoubleValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof Double);
        }

        public TypePredicateArgumentFilter<Object, Byte> scalarLogicalValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof Byte);
        }

        public TypePredicateArgumentFilter<Object, RComplex> scalarComplexValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> x instanceof RComplex);
        }

        public TypePredicateArgumentFilter<Object, RMissing> missingValue() {
            return TypePredicateArgumentFilter.fromLambda(x -> RMissing.instance == x);
        }

    }

    public static final class DefaultPredefMappers implements PredefMappers {
        public ValuePredicateArgumentMapper<Byte, Boolean> toBoolean() {
            return ValuePredicateArgumentMapper.fromLambda(x -> RRuntime.fromLogical(x));
        }

        public ValuePredicateArgumentMapper<String, Integer> charAt0(int defaultValue) {
            final ConditionProfile profile = ConditionProfile.createBinaryProfile();
            return ValuePredicateArgumentMapper.fromLambda(x -> profile.profile(x == null || x.isEmpty()) ? defaultValue : (int) x.charAt(0));
        }

        public ValuePredicateArgumentMapper<String, String> constant(String s) {
            return ValuePredicateArgumentMapper.<String, String> fromLambda((String x) -> s);
        }

        public ValuePredicateArgumentMapper<Integer, Integer> constant(int i) {
            return ValuePredicateArgumentMapper.fromLambda(x -> i);
        }

        public ValuePredicateArgumentMapper<Double, Double> constant(double d) {
            return ValuePredicateArgumentMapper.fromLambda(x -> d);
        }

        public ValuePredicateArgumentMapper<Byte, Byte> constant(byte l) {
            return ValuePredicateArgumentMapper.fromLambda(x -> l);
        }

        public <T> ArgumentMapper<T, T> defaultValue(T defVal) {

            assert (defVal != null);

            return new ArgumentMapper<T, T>() {

                final ConditionProfile profile = ConditionProfile.createBinaryProfile();

                public T map(T arg) {
                    if (profile.profile(arg == RNull.instance || arg == null)) {
                        return defVal;
                    } else {
                        return arg;
                    }
                }

            };
        }
    }

    public static final class Predef {

        private static PredefFilters predefFilters = new DefaultPredefFilters();
        private static PredefMappers predefMappers = new DefaultPredefMappers();

        /**
         * Invoked from tests only.
         *
         * @param pf
         */
        public static void setPredefFilters(PredefFilters pf) {
            predefFilters = pf;
        }

        /**
         * Invoked from tests only.
         *
         * @param pm
         */
        public static void setPredefMappers(PredefMappers pm) {
            predefMappers = pm;
        }

        private static PredefFilters predefFilters() {
            return predefFilters;
        }

        private static PredefMappers predefMappers() {
            return predefMappers;
        }

        public static <T, R> MapNode mapNode(ArgumentMapper<T, R> mapper) {
            return MapNodeGen.create(mapper);
        }

        public static CastIntegerBaseNode asInteger() {
            return CastIntegerBaseNodeGen.create(false, false, false);
        }

        public static CastIntegerNode asIntegerVector() {
            return CastIntegerNodeGen.create(false, false, false);
        }

        public static CastDoubleBaseNode asDouble() {
            return CastDoubleBaseNodeGen.create(false, false, false);
        }

        public static CastDoubleNode asDoubleVector() {
            return CastDoubleNodeGen.create(false, false, false);
        }

        public static CastStringBaseNode asString() {
            return CastStringBaseNodeGen.create(false, false, false);
        }

        public static CastStringNode asStringVector() {
            return CastStringNodeGen.create(false, false, false, false);
        }

        public static CastLogicalBaseNode asLogical() {
            return CastLogicalBaseNodeGen.create(false, false, false);
        }

        public static CastLogicalNode asLogicalVector() {
            return CastLogicalNodeGen.create(false, false, false);
        }

        public static MapNode asBoolean() {
            return mapNode(toBoolean());
        }

        public static <T> ValuePredicateArgumentFilter<T> sameAs(T x) {
            return predefFilters().sameAs(x);
        }

        public static <T> ValuePredicateArgumentFilter<T> equalTo(T x) {
            return predefFilters().equalTo(x);
        }

        public static <T, R extends T> TypePredicateArgumentFilter<T, R> nullValue() {
            return predefFilters().nullValue();
        }

        public static <T extends RAbstractVector> VectorPredicateArgumentFilter<T> notEmpty() {
            return predefFilters().notEmpty();
        }

        public static <T extends RAbstractVector> VectorPredicateArgumentFilter<T> singleElement() {
            return predefFilters().singleElement();
        }

        public static <T extends RAbstractVector, R extends T> VectorPredicateArgumentFilter<T> size(int s) {
            return predefFilters().size(s);
        }

        public static ValuePredicateArgumentFilter<Boolean> trueValue() {
            return predefFilters().trueValue();
        }

        public static ValuePredicateArgumentFilter<Boolean> falseValue() {
            return predefFilters().falseValue();
        }

        public static ValuePredicateArgumentFilter<Byte> logicalTrue() {
            return predefFilters().logicalTrue();
        }

        public static ValuePredicateArgumentFilter<Byte> logicalFalse() {
            return predefFilters().logicalFalse();
        }

        public static ValuePredicateArgumentFilter<Integer> intNA() {
            return predefFilters().intNA();
        }

        public static ValuePredicateArgumentFilter<Integer> notIntNA() {
            return predefFilters().notIntNA();
        }

        public static ValuePredicateArgumentFilter<Byte> logicalNA() {
            return predefFilters().logicalNA();
        }

        public static ValuePredicateArgumentFilter<Byte> notLogicalNA() {
            return predefFilters().notLogicalNA();
        }

        public static ValuePredicateArgumentFilter<Double> doubleNA() {
            return predefFilters().doubleNA();
        }

        public static ValuePredicateArgumentFilter<Double> notDoubleNA() {
            return predefFilters().notDoubleNA();
        }

        public static ValuePredicateArgumentFilter<String> stringNA() {
            return predefFilters().stringNA();
        }

        public static ValuePredicateArgumentFilter<String> notStringNA() {
            return predefFilters().notStringNA();
        }

        public static ValuePredicateArgumentFilter<Integer> eq(int x) {
            return predefFilters().eq(x);
        }

        public static ValuePredicateArgumentFilter<Double> eq(double x) {
            return predefFilters().eq(x);
        }

        public static ValuePredicateArgumentFilter<Integer> neq(int x) {
            return predefFilters().neq(x);
        }

        public static ValuePredicateArgumentFilter<Double> neq(double x) {
            return predefFilters().neq(x);
        }

        public static ValuePredicateArgumentFilter<Integer> gt(int x) {
            return predefFilters().gt(x);
        }

        public static ValuePredicateArgumentFilter<Double> gt(double x) {
            return predefFilters().gt(x);
        }

        public static ValuePredicateArgumentFilter<Integer> gte(int x) {
            return predefFilters().gte(x);
        }

        public static ValuePredicateArgumentFilter<Double> gte(double x) {
            return predefFilters().gte(x);
        }

        public static ValuePredicateArgumentFilter<Integer> lt(int x) {
            return predefFilters().lt(x);
        }

        public static ValuePredicateArgumentFilter<Double> lt(double x) {
            return predefFilters().lt(x);
        }

        public static ValuePredicateArgumentFilter<Integer> lte(int x) {
            return predefFilters().lte(x);
        }

        public static ValuePredicateArgumentFilter<Double> lte(double x) {
            return predefFilters().lte(x);
        }

        public static ValuePredicateArgumentFilter<String> length(int l) {
            return predefFilters().length(l);
        }

        public static ValuePredicateArgumentFilter<String> isEmpty() {
            return predefFilters().isEmpty();
        }

        public static ValuePredicateArgumentFilter<String> lengthGt(int l) {
            return predefFilters().lengthGt(l);
        }

        public static ValuePredicateArgumentFilter<String> lengthGte(int l) {
            return predefFilters().lengthGte(l);
        }

        public static ValuePredicateArgumentFilter<String> lengthLt(int l) {
            return predefFilters().lengthLt(l);
        }

        public static ValuePredicateArgumentFilter<String> lengthLte(int l) {
            return predefFilters().lengthLte(l);
        }

        public static ValuePredicateArgumentFilter<Integer> gt0() {
            return predefFilters().gt0();
        }

        public static ValuePredicateArgumentFilter<Integer> gte0() {
            return predefFilters().gte0();
        }

        public static ValuePredicateArgumentFilter<Integer> gt1() {
            return predefFilters().gt1();
        }

        public static ValuePredicateArgumentFilter<Integer> gte1() {
            return predefFilters().gte1();
        }

        public static <R> TypePredicateArgumentFilter<Object, R> instanceOf(Class<R> cls) {
            return predefFilters().instanceOf(cls);
        }

        public static <R extends RAbstractIntVector> TypePredicateArgumentFilter<Object, R> integerValue() {
            return predefFilters().integerValue();
        }

        public static <R extends RAbstractStringVector> TypePredicateArgumentFilter<Object, R> stringValue() {
            return predefFilters().stringValue();
        }

        public static <R extends RAbstractDoubleVector> TypePredicateArgumentFilter<Object, R> doubleValue() {
            return predefFilters().doubleValue();
        }

        public static <R extends RAbstractLogicalVector> TypePredicateArgumentFilter<Object, R> logicalValue() {
            return predefFilters().logicalValue();
        }

        public static <R extends RAbstractComplexVector> TypePredicateArgumentFilter<Object, R> complexValue() {
            return predefFilters().complexValue();
        }

        public static ArgumentTypeFilter<Object, Object> numericValue() {
            return integerValue().or(doubleValue()).or(logicalValue());
        }

        public static TypePredicateArgumentFilter<Object, String> scalarStringValue() {
            return predefFilters().scalarStringValue();
        }

        public static TypePredicateArgumentFilter<Object, Integer> scalarIntegerValue() {
            return predefFilters().scalarIntegerValue();
        }

        public static TypePredicateArgumentFilter<Object, Double> scalarDoubleValue() {
            return predefFilters().scalarDoubleValue();
        }

        public static TypePredicateArgumentFilter<Object, Byte> scalarLogicalValue() {
            return predefFilters().scalarLogicalValue();
        }

        public static TypePredicateArgumentFilter<Object, RComplex> scalarComplexValue() {
            return predefFilters().scalarComplexValue();
        }

        public static TypePredicateArgumentFilter<Object, RMissing> missingValue() {
            return predefFilters().missingValue();
        }

        public static ValuePredicateArgumentMapper<Byte, Boolean> toBoolean() {
            return predefMappers().toBoolean();
        }

        public static ValuePredicateArgumentMapper<String, Integer> charAt0(int defaultValue) {
            return predefMappers().charAt0(defaultValue);
        }

        public static ValuePredicateArgumentMapper<String, String> constant(String s) {
            return predefMappers().constant(s);
        }

        public static ValuePredicateArgumentMapper<Integer, Integer> constant(int i) {
            return predefMappers().constant(i);
        }

        public static ValuePredicateArgumentMapper<Double, Double> constant(double d) {
            return predefMappers().constant(d);
        }

        public static ValuePredicateArgumentMapper<Byte, Byte> constant(byte l) {
            return predefMappers().constant(l);
        }

        public static <T> ArgumentMapper<T, T> defaultValue(T defVal) {
            return predefMappers().defaultValue(defVal);
        }

    }

    @SuppressWarnings("unchecked")
    interface ArgCastBuilder<T, THIS> {

        ArgCastBuilderState state();

        default CastBuilder builder() {
            return state().castBuilder();
        }

        default THIS defaultError(RError.Message message, Object... args) {
            state().setDefaultError(message, args);
            return (THIS) this;
        }

        default THIS defaultWarning(RError.Message message, Object... args) {
            state().setDefaultWarning(message, args);
            return (THIS) this;
        }

        default THIS shouldBe(ArgumentFilter<? super T, ?> argFilter, RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), FilterNodeGen.create(argFilter, true, message, messageArgs, state().boxPrimitives, state().cb.out));
            return (THIS) this;
        }

        default THIS shouldBe(ArgumentFilter<? super T, ?> argFilter) {
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
            castBuilder().insert(index(), FilterNodeGen.create(argFilter, false, message, messageArgs, boxPrimitives, cb.out));
        }

        void mustBe(ArgumentFilter<?, ?> argFilter) {
            mustBe(argFilter, defaultError().message, defaultError().args);
        }

        void shouldBe(ArgumentFilter<?, ?> argFilter, RError.Message message, Object... messageArgs) {
            castBuilder().insert(index(), FilterNodeGen.create(argFilter, true, message, messageArgs, boxPrimitives, cb.out));
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

        default <S> InitialPhaseBuilder<S> mustBe(ArgumentFilter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> mustBe(ArgumentFilter<? super T, S> argFilter) {
            return mustBe(argFilter, state().defaultError().message, state().defaultError().args);
        }

        default <S> InitialPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            mustBe(Predef.instanceOf(cls), message, messageArgs);
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> mustBe(Class<S> cls) {
            mustBe(Predef.instanceOf(cls));
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S> InitialPhaseBuilder<S> map(ArgumentMapper<T, S> mapFn) {
            state().castBuilder().insert(state().index(), MapNodeGen.create(mapFn));
            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S, R> InitialPhaseBuilder<S> mapIf(ArgumentFilter<? super T, S> argFilter, ArgumentMapper<S, R> trueBranchMapper) {
            state().castBuilder().insert(state().index(), ConditionalMapNodeGen.create(argFilter, MapNodeGen.create(trueBranchMapper), null));

            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S, R> InitialPhaseBuilder<S> mapIf(ArgumentFilter<? super T, S> argFilter, CastNode trueBranchNode) {
            state().castBuilder().insert(state().index(), ConditionalMapNodeGen.create(argFilter, trueBranchNode, null));

            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S, R> InitialPhaseBuilder<T> mapIf(ArgumentFilter<? super T, S> argFilter, ArgumentMapper<S, R> trueBranchMapper, ArgumentMapper<T, T> falseBranchMapper) {
            state().castBuilder().insert(
                            state().index(),
                            ConditionalMapNodeGen.create(argFilter, MapNodeGen.create(trueBranchMapper),
                                            MapNodeGen.create(falseBranchMapper)));

            return state().factory.newInitialPhaseBuilder(this);
        }

        default <S, R> InitialPhaseBuilder<T> mapIf(ArgumentFilter<? super T, S> argFilter, CastNode trueBranchNode, CastNode falseBranchNode) {
            state().castBuilder().insert(state().index(), ConditionalMapNodeGen.create(argFilter, trueBranchNode, falseBranchNode));

            return this;
        }

        default InitialPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), NonNANodeGen.create(message, messageArgs, state().cb.out, null));
            return this;
        }

        default InitialPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), NonNANodeGen.create(message, messageArgs, state().cb.out, naReplacement));
            return this;
        }

        default InitialPhaseBuilder<T> notNA(T naReplacement) {
            state().castBuilder().insert(state().index(), NonNANodeGen.create(naReplacement));
            return this;
        }

        default InitialPhaseBuilder<T> notNA() {
            state().castBuilder().insert(state().index(), NonNANodeGen.create(state().defaultError().message, state().defaultError().args, state().cb.out, null));
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
            state().castBuilder().insert(state().index(), FindFirstNodeGen.create(elementClass(), message, messageArgs, state().cb.out, defaultValue));
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node raises an error if the input vector is empty.
         */
        default HeadPhaseBuilder<S> findFirst(RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), FindFirstNodeGen.create(elementClass(), message, messageArgs, state().cb.out, null));
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node raises the default error, if defined, or
         * RError.Message.LENGTH_ZERO error if the input vector is empty.
         */
        default HeadPhaseBuilder<S> findFirst() {
            DefaultError err = state().isDefaultErrorDefined() ? state().defaultError() : new DefaultError(RError.Message.LENGTH_ZERO);
            state().castBuilder().insert(state().index(),
                            FindFirstNodeGen.create(elementClass(), err.message, err.args, state().cb.out, null));
            return state().factory.newHeadPhaseBuilder(this);
        }

        /**
         * The inserted cast node returns the default value if the input vector is empty. It reports
         * no warning message.
         */
        default HeadPhaseBuilder<S> findFirst(S defaultValue) {
            state().castBuilder().insert(state().index(), FindFirstNodeGen.create(elementClass(), defaultValue));
            return state().factory.newHeadPhaseBuilder(this);
        }

        Class<?> elementClass();

        default CoercedPhaseBuilder<T, S> mustBe(ArgumentFilter<? super T, ? extends T> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, message, messageArgs);
            return this;
        }

        default CoercedPhaseBuilder<T, S> mustBe(ArgumentFilter<? super T, ? extends T> argFilter) {
            return mustBe(argFilter, state().defaultError().message, state().defaultError().args);
        }

    }

    public interface HeadPhaseBuilder<T> extends ArgCastBuilder<T, HeadPhaseBuilder<T>> {

        default <S> HeadPhaseBuilder<S> map(ArgumentMapper<T, S> mapFn) {
            state().castBuilder().insert(state().index(), MapNodeGen.create(mapFn));
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S, R> HeadPhaseBuilder<S> mapIf(ArgumentFilter<? super T, S> argFilter, ArgumentMapper<S, R> trueBranchMapper) {
            state().castBuilder().insert(state().index(), ConditionalMapNodeGen.create(argFilter, MapNodeGen.create(trueBranchMapper), null));

            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S, R> HeadPhaseBuilder<S> mapIf(ArgumentFilter<? super T, S> argFilter, CastNode trueBranchNode) {
            state().castBuilder().insert(state().index(), ConditionalMapNodeGen.create(argFilter, trueBranchNode, null));

            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S, R> HeadPhaseBuilder<T> mapIf(ArgumentFilter<? super T, S> argFilter, ArgumentMapper<S, R> trueBranchMapper, ArgumentMapper<T, T> falseBranchMapper) {
            state().castBuilder().insert(
                            state().index(),
                            ConditionalMapNodeGen.create(argFilter, MapNodeGen.create(trueBranchMapper),
                                            MapNodeGen.create(falseBranchMapper)));

            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S, R> HeadPhaseBuilder<T> mapIf(ArgumentFilter<? super T, S> argFilter, CastNode trueBranchNode, CastNode falseBranchNode) {
            state().castBuilder().insert(state().index(), ConditionalMapNodeGen.create(argFilter, trueBranchNode, falseBranchNode));

            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S> HeadPhaseBuilder<S> mustBe(ArgumentFilter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
            state().mustBe(argFilter, message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S> HeadPhaseBuilder<S> mustBe(ArgumentFilter<? super T, S> argFilter) {
            return mustBe(argFilter, state().defaultError().message, state().defaultError().args);
        }

        default <S> HeadPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
            mustBe(Predef.instanceOf(cls), message, messageArgs);
            return state().factory.newHeadPhaseBuilder(this);
        }

        default <S> HeadPhaseBuilder<S> mustBe(Class<S> cls) {
            mustBe(Predef.instanceOf(cls));
            return state().factory.newHeadPhaseBuilder(this);
        }

        default HeadPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), NonNANodeGen.create(message, messageArgs, state().cb.out, null));
            return this;
        }

        default HeadPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
            state().castBuilder().insert(state().index(), NonNANodeGen.create(message, messageArgs, state().cb.out, naReplacement));
            return this;
        }

        default HeadPhaseBuilder<T> notNA() {
            state().castBuilder().insert(state().index(), NonNANodeGen.create(state().defaultError().message, state().defaultError().args, state().cb.out, null));
            return this;
        }

        default HeadPhaseBuilder<T> notNA(T naReplacement) {
            state().castBuilder().insert(state().index(), NonNANodeGen.create(naReplacement));
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
                super(new ArgCastBuilderState(state, false));
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
