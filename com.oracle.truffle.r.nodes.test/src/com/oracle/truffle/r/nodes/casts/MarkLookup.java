/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.DoubleFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.FilterVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MatrixFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MissingFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NullFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapperVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.AttributableCoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.BoxPrimitiveStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.PipelineStepVisitor;

public final class MarkLookup implements PipelineStepVisitor<Map<String, Object>>, FilterVisitor<Map<String, Object>>, MapperVisitor<Map<String, Object>> {
    private final Map<Object, String> marx = new HashMap<>();

    public static final MarkLookup INSTANCE = new MarkLookup();

    private MarkLookup() {
    }

    public static void clear() {
        INSTANCE.marx.clear();
    }

    public static <T, S extends T, F extends Filter<T, S>> F mark(F filter, String m) {
        INSTANCE.marx.put(filter, m);
        return filter;
    }

    public static <T, S, M extends Mapper<T, S>> M mark(M mapper, String m) {
        INSTANCE.marx.put(mapper, m);
        return mapper;
    }

    public static Map<String, Object> lookup(PipelineStep<?, ?> firstStep, String... filterNames) {
        return lookup(firstStep, Arrays.stream(filterNames).collect(Collectors.toSet()));
    }

    public static Map<String, Object> lookup(PipelineStep<?, ?> firstStep, Set<String> filterNames) {
        Map<String, Object> foundMarks = new HashMap<>();
        for (String key : filterNames) {
            foundMarks.put(key, null);
        }
        return lookup(firstStep, foundMarks);
    }

    public static Map<String, Object> lookup(PipelineStep<?, ?> firstStep, Map<String, Object> foundMarks) {
        return firstStep.acceptPipeline(INSTANCE, foundMarks);
    }

    @Override
    public Map<String, Object> visit(FindFirstStep<?, ?> step, Map<String, Object> foundMarks) {
        return foundMarks;
    }

    @Override
    public Map<String, Object> visit(CoercionStep<?, ?> step, Map<String, Object> foundMarks) {
        return foundMarks;
    }

    @Override
    public Map<String, Object> visit(MapStep<?, ?> step, Map<String, Object> foundMarks) {
        return step.getMapper().accept(this, foundMarks);
    }

    @Override
    public Map<String, Object> visit(MapIfStep<?, ?> step, Map<String, Object> foundMarks) {
        Map<String, Object> found = foundMarks;
        found = step.getFilter().accept(this, found);
        if (step.getTrueBranch() != null) {
            found = step.getTrueBranch().accept(this, found);
        }
        if (step.getFalseBranch() != null) {
            found = step.getFalseBranch().accept(this, found);
        }
        return found;
    }

    @Override
    public Map<String, Object> visit(FilterStep<?, ?> step, Map<String, Object> foundMarks) {
        return step.getFilter().accept(this, foundMarks);
    }

    @Override
    public Map<String, Object> visit(NotNAStep<?> step, Map<String, Object> foundMarks) {
        return foundMarks;
    }

    @Override
    public Map<String, Object> visit(BoxPrimitiveStep<?> step, Map<String, Object> foundMarks) {
        return foundMarks;
    }

    @Override
    public Map<String, Object> visit(AttributableCoercionStep<?> step, Map<String, Object> foundMarks) {
        return foundMarks;
    }

    // Filter visitor

    public Map<String, Object> visitFilter(Filter<?, ?> filter, Map<String, Object> foundMarks) {
        String mark = marx.get(filter);
        if (mark != null && foundMarks.containsKey(mark)) {
            foundMarks.put(mark, filter);
        }
        return foundMarks;
    }

    @Override
    public Map<String, Object> visit(TypeFilter<?, ?> filter, Map<String, Object> foundMarks) {
        return visitFilter(filter, foundMarks);
    }

    @Override
    public Map<String, Object> visit(RTypeFilter<?> filter, Map<String, Object> foundMarks) {
        return visitFilter(filter, foundMarks);
    }

    @Override
    public Map<String, Object> visit(CompareFilter<?> filter, Map<String, Object> foundMarks) {
        return visitFilter(filter, foundMarks);
    }

    @Override
    public Map<String, Object> visit(AndFilter<?, ?> filter, Map<String, Object> foundMarks) {
        Map<String, Object> found = filter.getLeft().accept(this, visitFilter(filter, foundMarks));
        return filter.getRight().accept(this, filter.getLeft().accept(this, found));
    }

    @Override
    public Map<String, Object> visit(OrFilter<?> filter, Map<String, Object> foundMarks) {
        Map<String, Object> found = filter.getLeft().accept(this, visitFilter(filter, foundMarks));
        return filter.getRight().accept(this, filter.getLeft().accept(this, found));
    }

    @Override
    public Map<String, Object> visit(NotFilter<?> filter, Map<String, Object> foundMarks) {
        return filter.getFilter().accept(this, visitFilter(filter, foundMarks));
    }

    @Override
    public Map<String, Object> visit(MatrixFilter<?> filter, Map<String, Object> foundMarks) {
        return visitFilter(filter, foundMarks);
    }

    @Override
    public Map<String, Object> visit(DoubleFilter filter, Map<String, Object> foundMarks) {
        return visitFilter(filter, foundMarks);
    }

    @Override
    public Map<String, Object> visit(NullFilter filter, Map<String, Object> foundMarks) {
        return visitFilter(filter, foundMarks);
    }

    @Override
    public Map<String, Object> visit(MissingFilter filter, Map<String, Object> foundMarks) {
        return visitFilter(filter, foundMarks);
    }

    // Mapper visitor

    public Map<String, Object> visitMapper(Mapper<?, ?> mapper, Map<String, Object> foundMarks) {
        String mark = marx.get(mapper);
        if (mark != null && foundMarks.containsKey(mark)) {
            foundMarks.put(mark, mapper);
        }
        return foundMarks;
    }

    @Override
    public Map<String, Object> visit(MapToValue<?, ?> mapper, Map<String, Object> foundMarks) {
        return visitMapper(mapper, foundMarks);
    }

    @Override
    public Map<String, Object> visit(MapByteToBoolean mapper, Map<String, Object> foundMarks) {
        return visitMapper(mapper, foundMarks);
    }

    @Override
    public Map<String, Object> visit(MapDoubleToInt mapper, Map<String, Object> foundMarks) {
        return visitMapper(mapper, foundMarks);
    }

    @Override
    public Map<String, Object> visit(MapToCharAt mapper, Map<String, Object> foundMarks) {
        return visitMapper(mapper, foundMarks);
    }

}
