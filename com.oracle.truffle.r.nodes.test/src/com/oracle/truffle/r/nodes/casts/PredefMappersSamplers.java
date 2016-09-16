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

import com.oracle.truffle.r.nodes.builtin.ValuePredicateArgumentMapper;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class PredefMappersSamplers {

    public ValuePredicateArgumentMapperSampler<Byte, Boolean> toBoolean() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RRuntime.fromLogical(x), x -> RRuntime.asLogical(x), samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA),
                        CastUtils.<Byte> samples(), Byte.class, Boolean.class);
    }

    public ValuePredicateArgumentMapperSampler<Double, Integer> doubleToInt() {
        final NACheck naCheck = NACheck.create();
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> {
            naCheck.enable(x);
            return naCheck.convertDoubleToInt(x);
        }, x -> x == null ? null : (double) x, Double.class, Integer.class);
    }

    public ValuePredicateArgumentMapperSampler<String, Integer> charAt0(int defaultValue) {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> {
            if (x == null || x.isEmpty()) {
                return defaultValue;
            } else {
                if (x == RRuntime.STRING_NA) {
                    return RRuntime.INT_NA;
                } else {
                    return (int) x.charAt(0);
                }
            }
        }, x -> {
            if (x == null) {
                return defaultValue == RRuntime.INT_NA ? RRuntime.STRING_NA : "" + (char) defaultValue;
            } else {
                return x == RRuntime.INT_NA ? RRuntime.STRING_NA : "" + (char) x.intValue();
            }
        }, samples(defaultValue == RRuntime.INT_NA ? RRuntime.STRING_NA : "" + (char) defaultValue), CastUtils.<String> samples(), String.class, Integer.class);
    }

    public <T> ValuePredicateArgumentMapperSampler<T, RNull> nullConstant() {
        return ValuePredicateArgumentMapperSampler.fromLambda((T x) -> RNull.instance, null, null, RNull.class);
    }

    public <T> ValuePredicateArgumentMapperSampler<T, RMissing> missingConstant() {
        return ValuePredicateArgumentMapperSampler.fromLambda((T x) -> RMissing.instance, null, null, RMissing.class);
    }

    public <T> ValuePredicateArgumentMapperSampler<T, String> constant(String s) {
        return ValuePredicateArgumentMapperSampler.fromLambda((T x) -> s, (String x) -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null,
                        String.class);
    }

    public <T> ValuePredicateArgumentMapperSampler<T, Integer> constant(int i) {
        return ValuePredicateArgumentMapperSampler.fromLambda((T x) -> i, (Integer x) -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null,
                        Integer.class);
    }

    public <T> ValuePredicateArgumentMapperSampler<T, Double> constant(double d) {
        return ValuePredicateArgumentMapperSampler.fromLambda((T x) -> d, (Double x) -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null, Double.class);
    }

    public <T> ValuePredicateArgumentMapperSampler<T, Byte> constant(byte l) {
        return ValuePredicateArgumentMapperSampler.fromLambda((T x) -> l, x -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null, Byte.class);
    }

    public <T> ValuePredicateArgumentMapperSampler<T, RIntVector> emptyIntegerVector() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RDataFactory.createEmptyIntVector(), x -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null, RIntVector.class);
    }

    public <T> ValuePredicateArgumentMapper<T, RDoubleVector> emptyDoubleVector() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RDataFactory.createEmptyDoubleVector(), x -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null, RDoubleVector.class);
    }

    public <T> ValuePredicateArgumentMapper<T, RLogicalVector> emptyLogicalVector() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RDataFactory.createEmptyLogicalVector(), x -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null, RLogicalVector.class);
    }

    public <T> ValuePredicateArgumentMapper<T, RComplexVector> emptyComplexVector() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RDataFactory.createEmptyComplexVector(), x -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null, RComplexVector.class);
    }

    public <T> ValuePredicateArgumentMapper<T, RStringVector> emptyStringVector() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RDataFactory.createEmptyStringVector(), x -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null, RStringVector.class);
    }

    public <T> ValuePredicateArgumentMapper<T, RList> emptyList() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RDataFactory.createList(), x -> null, CastUtils.<T> samples(), CastUtils.<T> samples(), null, RList.class);
    }

}
