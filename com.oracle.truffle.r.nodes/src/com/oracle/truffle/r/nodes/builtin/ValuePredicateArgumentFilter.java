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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.unary.CastNode.Samples;
import com.oracle.truffle.r.runtime.data.RNull;

public class ValuePredicateArgumentFilter<T, R extends T> implements ArgumentFilter<T, R> {

    private final Predicate<? super T> valuePredicate;
    private final Set<? extends T> positiveSamples;
    private final Set<? extends T> negativeSamples;
    private final Set<Class<?>> allowedTypeSet;
    private final boolean isNullable;

    public ValuePredicateArgumentFilter(Predicate<? super T> valuePredicate, Set<? extends T> positiveSamples, Set<? extends T> negativeSamples, Set<Class<?>> allowedTypeSet, boolean isNullable) {
        this.valuePredicate = valuePredicate;
        this.allowedTypeSet = allowedTypeSet;
        this.positiveSamples = positiveSamples;
        this.negativeSamples = negativeSamples;
        this.isNullable = isNullable;

        assert positiveSamples.stream().allMatch(x -> valuePredicate.test(x));
        assert negativeSamples.stream().allMatch(x -> !valuePredicate.test(x));
    }

    @Override
    public boolean test(T arg) {
        if (!isNullable && (arg == RNull.instance || arg == null)) {
            return false;
        } else {
            return valuePredicate.test(arg);
        }
    }

    @Override
    public Set<Class<?>> allowedTypes() {
        return allowedTypeSet;
    }

    public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
        Set<T> allowedPositiveValues = downStreamSamples.positiveSamples().stream().filter(x -> test(x)).collect(Collectors.toSet());
        Set<T> forbiddenPositiveValues = downStreamSamples.positiveSamples().stream().filter(x -> !test(x)).collect(Collectors.toSet());

        Set<T> negativeAndForbiddenSamples = new HashSet<>(forbiddenPositiveValues);
        negativeAndForbiddenSamples.addAll(downStreamSamples.negativeSamples());

        return new Samples<>(allowedPositiveValues, negativeAndForbiddenSamples).and(new Samples<>(positiveSamples, negativeSamples));
    }

    public static <T, R extends T> ValuePredicateArgumentFilter<T, R> fromLambda(Predicate<? super T> predicate, Set<? extends T> positiveSamples, Set<? extends T> negativeSamples,
                    Class<R> resultClass) {
        return new ValuePredicateArgumentFilter<>(predicate, positiveSamples, negativeSamples, Collections.singleton(resultClass), false);
    }

    public static <T, R extends T> ValuePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate, Class<?> resultClass) {
        return new ValuePredicateArgumentFilter<>(predicate, Collections.emptySet(), Collections.emptySet(), Collections.singleton(resultClass), false);
    }

    public static <T, R extends T> ValuePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate, Set<? extends T> positiveSamples, Set<? extends T> negativeSamples) {
        return new ValuePredicateArgumentFilter<>(predicate, positiveSamples, negativeSamples, Collections.emptySet(), false);
    }

    public static <T, R extends T> ValuePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate) {
        return new ValuePredicateArgumentFilter<>(predicate, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false);
    }

    public static <T, R extends T> ValuePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate, Set<T> negativeSamples,
                    @SuppressWarnings("unused") Class<R> commonAncestorClass, Set<Class<?>> resultClasses) {
        return new ValuePredicateArgumentFilter<>(predicate, Collections.emptySet(), negativeSamples, resultClasses, false);
    }

    public static <T, R extends T> ValuePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate, @SuppressWarnings("unused") Class<R> commonAncestorClass, Set<Class<?>> resultClasses) {
        return new ValuePredicateArgumentFilter<>(predicate, Collections.emptySet(), Collections.emptySet(), resultClasses, false);
    }
}
