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

import java.util.Collections;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.PredefMappers;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RNull;

public final class PredefMappersSamplers implements PredefMappers {

    @Override
    public ValuePredicateArgumentMapperSampler<Byte, Boolean> toBoolean() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RRuntime.fromLogical(x), x -> RRuntime.asLogical(x), samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA),
                        CastUtils.<Byte> samples(), Byte.class, Boolean.class);
    }

    @Override
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

    @Override
    public <T> ValuePredicateArgumentMapperSampler<T, RNull> nullConstant() {
        return ValuePredicateArgumentMapperSampler.<T, RNull> fromLambda((T x) -> RNull.instance, null, null, RNull.class);
    }

    @Override
    public ValuePredicateArgumentMapperSampler<String, String> constant(String s) {
        return ValuePredicateArgumentMapperSampler.<String, String> fromLambda((String x) -> s, (String x) -> s, CastUtils.<String> samples(), CastUtils.<String> samples(), String.class,
                        String.class);
    }

    @Override
    public ValuePredicateArgumentMapperSampler<Integer, Integer> constant(int i) {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> i, x -> i, CastUtils.<Integer> samples(), CastUtils.<Integer> samples(), Integer.class, Integer.class);
    }

    @Override
    public ValuePredicateArgumentMapperSampler<Double, Double> constant(double d) {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> d, x -> d, CastUtils.<Double> samples(), CastUtils.<Double> samples(), Double.class, Double.class);
    }

    @Override
    public ValuePredicateArgumentMapperSampler<Byte, Byte> constant(byte l) {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> l, x -> l, CastUtils.<Byte> samples(), CastUtils.<Byte> samples(), Byte.class, Byte.class);
    }

    @Override
    public <T> ArgumentMapperSampler<T, T> defaultValue(T defVal) {

        assert (defVal != null);

        return new ArgumentMapperSampler<T, T>() {

            final ConditionProfile profile = ConditionProfile.createBinaryProfile();

            @Override
            public T map(T arg) {
                if (profile.profile(arg == RNull.instance)) {
                    return defVal;
                } else {
                    return arg;
                }
            }

            @Override
            public TypeExpr resultTypes(TypeExpr inputTypes) {
                return inputTypes.and(TypeExpr.atom(RNull.class).not());
            }

            @SuppressWarnings("unchecked")
            @Override
            public Samples<T> collectSamples(Samples<T> downStreamSamples) {
                Samples<Object> nullOnly = new Samples<>("RNullOnly", Collections.singleton(RNull.instance), Collections.emptySet(), x -> x == RNull.instance);
                return (Samples<T>) nullOnly.or(Samples.anything(defVal).and(downStreamSamples));
            }
        };
    }
}
