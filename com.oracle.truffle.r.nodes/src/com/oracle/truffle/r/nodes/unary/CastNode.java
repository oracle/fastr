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
package com.oracle.truffle.r.nodes.unary;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.builtin.CastUtils;

/**
 * Cast nodes behave like unary nodes, but in many cases it is useful to have a specific type for
 * casts.
 */
public abstract class CastNode extends UnaryNode {

    public final Set<Class<?>> resultTypes() {
        return resultTypes(Collections.emptySet());
    }

    protected Set<Class<?>> resultTypes(Set<Class<?>> inputTypes) {
        return inputTypes;
    }

    public final Samples<?> collectSamples() {
        Set<Class<?>> resTypes = resultTypes().stream().filter(cls -> cls != Object.class).collect(Collectors.toSet());

        Set<?> defaultPositiveSamples;
        if (resTypes.isEmpty()) {
            defaultPositiveSamples = CastUtils.sampleValuesForClass(Object.class);
        } else {
            defaultPositiveSamples = resTypes.stream().flatMap(cls -> CastUtils.sampleValuesForClass(cls).stream()).collect(Collectors.toSet());
        }

        return collectSamples(new Samples<>(defaultPositiveSamples, Collections.emptySet()));
    }

    @SuppressWarnings("unused")
    protected Samples<?> collectSamples(Samples<?> downStreamSamples) {
        return Samples.EMPTY;
    }

    public static final class Samples<T> {

        private static final Samples<?> EMPTY = new Samples<>(Collections.emptySet(), Collections.emptySet());

        @SuppressWarnings("unchecked")
        public static <T> Samples<T> empty() {
            return (Samples<T>) EMPTY;
        }

        private final Set<? extends T> posSamples;
        private final Set<? extends T> negSamples;

        public Samples(Set<? extends T> positiveSamples, Set<? extends T> negativeSamples) {
            this.posSamples = positiveSamples;
            this.negSamples = negativeSamples;
        }

        public Set<? extends T> positiveSamples() {
            return posSamples;
        }

        public Set<? extends T> negativeSamples() {
            return negSamples;
        }

        public <R> Samples<R> map(Function<T, R> mapper) {
            Set<R> mappedPositive = positiveSamples().stream().map(mapper).collect(Collectors.toSet());
            Set<R> mappedNegative = negativeSamples().stream().map(mapper).collect(Collectors.toSet());
            return new Samples<>(mappedPositive, mappedNegative);
        }

        public Samples<T> filter(Predicate<T> condition) {
            Set<T> mappedPositive = positiveSamples().stream().filter(condition).collect(Collectors.toSet());
            Set<T> mappedNegative = negativeSamples().stream().filter(condition).collect(Collectors.toSet());
            return new Samples<>(mappedPositive, mappedNegative);
        }

        public Samples<T> and(Samples<T> other) {
            Set<T> negativeUnion = new HashSet<>(other.negativeSamples());
            negativeUnion.addAll(negativeSamples());
            Set<T> positiveUnion = new HashSet<>(other.positiveSamples());
            positiveUnion.addAll(positiveSamples());
            positiveUnion.removeAll(negativeUnion);

            return new Samples<>(positiveUnion, negativeUnion);
        }

        public Samples<T> or(Samples<T> other) {
            Set<T> positiveUnion = new HashSet<>(other.positiveSamples());
            positiveUnion.addAll(positiveSamples());

            Set<T> negativeUnion = new HashSet<>(other.negativeSamples());
            negativeUnion.addAll(negativeSamples());
            negativeUnion.removeAll(positiveUnion);

            return new Samples<>(positiveUnion, negativeUnion);
        }

        public Samples<T> swap() {
            return new Samples<>(negSamples, posSamples);
        }
    }

}
