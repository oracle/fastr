/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.nodes.builtin.ArgumentMapper;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapperVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentMapperFactory;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public final class MapperSamplerFactory implements ArgumentMapperFactory, MapperVisitor<ValuePredicateArgumentMapperSampler<?, ?>> {

    public static final MapperSamplerFactory INSTANCE = new MapperSamplerFactory();

    private MapperSamplerFactory() {
        // singleton
    }

    @Override
    public ArgumentMapper<?, ?> createMapper(Mapper<?, ?> mapper) {
        return mapper.accept(this);
    }

    @Override
    public ValuePredicateArgumentMapperSampler<Object, Object> visit(MapToValue<?, ?> mapper) {
        final Object value = mapper.getValue();
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> value, x -> null, CastUtils.samples(), CastUtils.samples(), null, value.getClass());
    }

    @Override
    public ValuePredicateArgumentMapperSampler<?, ?> visit(MapByteToBoolean mapper) {
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> RRuntime.fromLogical(x), (Boolean x) -> RRuntime.asLogical(x),
                        samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA),
                        CastUtils.<Byte> samples(), Byte.class, Boolean.class);
    }

    @Override
    public ValuePredicateArgumentMapperSampler<?, ?> visit(MapDoubleToInt mapper) {
        final NACheck naCheck = NACheck.create();
        return ValuePredicateArgumentMapperSampler.fromLambda(x -> {
            naCheck.enable(x);
            return naCheck.convertDoubleToInt(x);
        }, x -> x == null ? null : (double) x, Double.class, Integer.class);
    }

    @Override
    public ValuePredicateArgumentMapperSampler<?, ?> visit(MapToCharAt mapper) {
        final int defaultValue = mapper.getDefaultValue();

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
                return x.equals(RRuntime.INT_NA) ? RRuntime.STRING_NA : x.toString();
            }
        }, samples(defaultValue == RRuntime.INT_NA ? RRuntime.STRING_NA : "" + (char) defaultValue), CastUtils.<String> samples(), String.class, Integer.class);
    }
}
