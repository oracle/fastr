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
package com.oracle.truffle.r.nodes.casts;

import static com.oracle.truffle.r.nodes.casts.CastUtils.samples;

import java.util.Objects;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.PredefFilters;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class PredefFiltersSamplers implements PredefFilters {

    @Override
    public <T> ValuePredicateArgumentFilterSampler<T> sameAs(T x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(arg -> arg == x, samples(x), CastUtils.<T> samples());
    }

    @Override
    public <T> ValuePredicateArgumentFilterSampler<T> equalTo(T x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(arg -> Objects.equals(arg, x), samples(x), CastUtils.<T> samples());
    }

    @Override
    public <T, R extends T> TypePredicateArgumentFilterSampler<T, R> nullValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x == RNull.instance || x == null, CastUtils.<R> samples(null), CastUtils.<R> samples(), RNull.class);
    }

    @Override
    public <T extends RAbstractVector> VectorPredicateArgumentFilterSampler<T> notEmpty() {
        return new VectorPredicateArgumentFilterSampler<>(x -> x.getLength() > 0, false);
    }

    @Override
    public <T extends RAbstractVector> VectorPredicateArgumentFilterSampler<T> singleElement() {
        return new VectorPredicateArgumentFilterSampler<>(x -> x.getLength() == 1, false);
    }

    @Override
    public <T extends RAbstractVector, R extends T> VectorPredicateArgumentFilterSampler<T> size(int s) {
        return new VectorPredicateArgumentFilterSampler<>(x -> x.getLength() == s, false);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Boolean> trueValue() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(x -> x, CastUtils.<Boolean> samples(), samples(Boolean.FALSE));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Boolean> falseValue() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(x -> x, CastUtils.<Boolean> samples(), samples(Boolean.FALSE));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Byte> logicalTrue() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(x -> RRuntime.LOGICAL_TRUE == x, CastUtils.<Byte> samples(),
                        samples(RRuntime.LOGICAL_FALSE));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Byte> logicalFalse() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(x -> RRuntime.LOGICAL_FALSE == x, CastUtils.<Byte> samples(),
                        samples(RRuntime.LOGICAL_TRUE));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> intNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer x) -> RRuntime.isNA(x), samples(RRuntime.INT_NA), samples(0));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> notIntNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer x) -> !RRuntime.isNA(x), CastUtils.<Integer> samples(),
                        samples(RRuntime.INT_NA));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Byte> logicalNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Byte x) -> RRuntime.isNA(x), samples(RRuntime.LOGICAL_NA),
                        samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Byte> notLogicalNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Byte x) -> !RRuntime.isNA(x), CastUtils.<Byte> samples(),
                        samples(RRuntime.LOGICAL_NA));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> doubleNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double x) -> RRuntime.isNA(x), samples(RRuntime.DOUBLE_NA), samples(0.0));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> notDoubleNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double x) -> !RRuntime.isNA(x), CastUtils.<Double> samples(),
                        samples(RRuntime.DOUBLE_NA));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> stringNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String x) -> RRuntime.isNA(x), samples(RRuntime.STRING_NA), samples(""));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> notStringNA() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String x) -> !RRuntime.isNA(x), CastUtils.<String> samples(),
                        samples(RRuntime.STRING_NA));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> eq(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg != null && arg.intValue() == x, samples(x), samples(x + 1));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> eq(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg != null && arg.doubleValue() == x, samples(x), samples(x + 0.00001));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> neq(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg == null || arg.intValue() != x, samples(x + 1), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> neq(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg == null || arg.doubleValue() != x, samples(x + 0.00001), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> gt(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg > x, samples(x + 1), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> gt(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg > x, samples(x + 0.00001), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> gte(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg >= x, samples(x), samples(x - 1));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> gte(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg >= x, samples(x), samples(x - 0.00001));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> lt(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg < x, samples(x - 1), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> lt(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg < x, samples(x - 0.00001), samples(x));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> lte(int x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg <= x, samples(x), samples(x + 1));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Double> lte(double x) {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg <= x, samples(x), samples(x + 0.00001));
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> length(int l) {
        return ValuePredicateArgumentFilterSampler.omLambdaWithResTypes((String arg) -> arg != null && arg.length() == l);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> isEmpty() {
        return ValuePredicateArgumentFilterSampler.omLambdaWithResTypes((String arg) -> arg != null && arg.isEmpty());
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> lengthGt(int l) {
        return ValuePredicateArgumentFilterSampler.omLambdaWithResTypes((String arg) -> arg != null && arg.length() > l);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> lengthGte(int l) {
        return ValuePredicateArgumentFilterSampler.omLambdaWithResTypes((String arg) -> arg != null && arg.length() >= l);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> lengthLt(int l) {
        return ValuePredicateArgumentFilterSampler.omLambdaWithResTypes((String arg) -> arg != null && arg.length() < l);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<String> lengthLte(int l) {
        return ValuePredicateArgumentFilterSampler.omLambdaWithResTypes((String arg) -> arg != null && arg.length() <= l);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> gt0() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer x) -> x > 0, CastUtils.<Integer> samples(), samples(-1, 0),
                        Integer.class);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> gte0() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer x) -> x >= 0, CastUtils.<Integer> samples(), samples(-1),
                        Integer.class);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> gt1() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer x) -> x > 1, CastUtils.<Integer> samples(), samples(-1, 0, 1),
                        Integer.class);
    }

    @Override
    public ValuePredicateArgumentFilterSampler<Integer> gte1() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer x) -> x >= 1, CastUtils.<Integer> samples(), samples(-1, 0),
                        Integer.class);
    }

    @Override
    public <R> TypePredicateArgumentFilterSampler<Object, R> instanceOf(Class<R> cls) {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> cls.isInstance(x), cls);
    }

    @Override
    public <R extends RAbstractIntVector> TypePredicateArgumentFilterSampler<Object, R> integerValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Integer || x instanceof RAbstractIntVector, RAbstractIntVector.class, Integer.class);
    }

    @Override
    public <R extends RAbstractStringVector> TypePredicateArgumentFilterSampler<Object, R> stringValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof String ||
                        x instanceof RAbstractStringVector, RAbstractStringVector.class, String.class);
    }

    @Override
    public <R extends RAbstractDoubleVector> TypePredicateArgumentFilterSampler<Object, R> doubleValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Double ||
                        x instanceof RAbstractDoubleVector, RAbstractDoubleVector.class, Double.class);
    }

    @Override
    public <R extends RAbstractLogicalVector> TypePredicateArgumentFilterSampler<Object, R> logicalValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Byte ||
                        x instanceof RAbstractLogicalVector, RAbstractLogicalVector.class, Byte.class);
    }

    @Override
    public <R extends RAbstractComplexVector> TypePredicateArgumentFilterSampler<Object, R> complexValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof RComplex ||
                        x instanceof RAbstractComplexVector, RAbstractComplexVector.class, RComplex.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, String> scalarStringValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof String, String.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, Integer> scalarIntegerValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Integer, Integer.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, Double> scalarDoubleValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Double, Double.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, Byte> scalarLogicalValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof Byte, Byte.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, RComplex> scalarComplexValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> x instanceof RComplex, RComplex.class);
    }

    @Override
    public TypePredicateArgumentFilterSampler<Object, RMissing> missingValue() {
        return TypePredicateArgumentFilterSampler.fromLambda(x -> RMissing.instance == x, RMissing.class);
    }

}
