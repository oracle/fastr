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

import java.util.HashSet;

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.PredefMappers;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RNull;

public final class PredefMappersSamplers implements PredefMappers {

    public ValuePredicateArgumentMapperSampler<Byte, Boolean> toBoolean() {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RRuntime.fromLogical(x), x -> RRuntime.asLogical(x), Boolean.class);
    }

    public ValuePredicateArgumentMapperSampler<String, Integer> charAt0(int defaultValue) {
        final ConditionProfile profile = ConditionProfile.createBinaryProfile();
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> profile.profile(x == null || x.isEmpty()) ? defaultValue : (int) x.charAt(0),
                        x -> x == null ? "" + (char) defaultValue : "" + (char) x.intValue(), Integer.class);
    }

    public ValuePredicateArgumentMapperSampler<String, String> constant(String s) {
        return ValuePredicateArgumentMapperSampler.<String, String> fromLambda((String x) -> s, (String x) -> null, samples(s), CastUtils.<String> samples(), String.class);
    }

    public ValuePredicateArgumentMapperSampler<Integer, Integer> constant(int i) {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> i, x -> null, samples(i), CastUtils.<Integer> samples(), Integer.class);
    }

    public ValuePredicateArgumentMapperSampler<Double, Double> constant(double d) {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> d, x -> null, samples(d), CastUtils.<Double> samples(), Double.class);
    }

    public ValuePredicateArgumentMapperSampler<Byte, Byte> constant(byte l) {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> l, x -> null, samples(l), CastUtils.<Byte> samples(), Byte.class);
    }

    public <T> ArgumentMapperSampler<T, T> defaultValue(T defVal) {

        assert (defVal != null);

        final TypeExpr defType = TypeExpr.atom(defVal.getClass()).or(TypeExpr.atom(RNull.class).not());

        return new ArgumentMapperSampler<T, T>() {

            final ConditionProfile profile = ConditionProfile.createBinaryProfile();

            public T map(T arg) {
                if (profile.profile(arg == RNull.instance || arg == null)) {
                    return defVal;
                } else {
                    return arg;
                }
            }

            public TypeExpr resultTypes() {
                return defType;
            }

            public Samples<T> collectSamples(Samples<T> downStreamSamples) {
                HashSet<T> posSamples = new HashSet<>(downStreamSamples.positiveSamples());
                posSamples.add(defVal);
                return new Samples<>(posSamples, downStreamSamples.negativeSamples());
            }
        };
    }
}
