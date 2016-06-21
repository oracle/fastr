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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Samples<T> {

    private static final Samples<?> EMPTY = new Samples<>(Collections.emptySet(), Collections.emptySet());

    @SuppressWarnings("unchecked")
    public static <T> Samples<T> empty() {
        return (Samples<T>) EMPTY;
    }

    private final Set<? extends T> posSamples;
    private final Set<?> negSamples;

    public Samples(Set<? extends T> positiveSamples, Set<?> negativeSamples) {
        this.posSamples = positiveSamples;
        this.negSamples = negativeSamples;
    }

    public Set<? extends T> positiveSamples() {
        return posSamples;
    }

    public Set<?> negativeSamples() {
        return negSamples;
    }

    public <R> Samples<R> map(Function<T, R> posMapper, Function<Object, Object> negMapper) {
        Set<R> mappedPositive = positiveSamples().stream().map(posMapper).collect(Collectors.toSet());
        Set<Object> mappedNegative = negativeSamples().stream().map(negMapper).collect(Collectors.toSet());
        return new Samples<>(mappedPositive, mappedNegative);
    }

    public Samples<T> filter(Predicate<T> posCondition) {
        Set<T> newPositive = positiveSamples().stream().filter(posCondition).collect(Collectors.toSet());
        Set<T> newNegativeFromPositive = positiveSamples().stream().filter(x -> !posCondition.test(x)).collect(Collectors.toSet());
        Set<Object> newNegative = new HashSet<>(negativeSamples());
        newNegative.addAll(newNegativeFromPositive);
        return new Samples<>(newPositive, newNegative);
    }

    public Samples<T> and(Samples<? extends T> other) {
        Set<Object> negativeUnion = new HashSet<>(other.negativeSamples());
        negativeUnion.addAll(negativeSamples());
        Set<T> positiveUnion = new HashSet<>(other.positiveSamples());
        positiveUnion.addAll(positiveSamples());
        positiveUnion.removeAll(negativeUnion);

        return new Samples<>(positiveUnion, negativeUnion);
    }

    public Samples<T> or(Samples<? extends T> other) {
        Set<T> positiveUnion = new HashSet<>(other.positiveSamples());
        positiveUnion.addAll(positiveSamples());

        Set<Object> negativeUnion = new HashSet<>(other.negativeSamples());
        negativeUnion.addAll(negativeSamples());
        negativeUnion.removeAll(positiveUnion);

        return new Samples<>(positiveUnion, negativeUnion);
    }

    public Samples<Object> swap() {
        return new Samples<>(negSamples, posSamples);
    }

    @Override
    public String toString() {
        return posSamples.toString() + ":" + negSamples.toString();
    }
}
