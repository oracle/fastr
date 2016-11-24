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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.builtin.ValuePredicateArgumentFilter;
import com.oracle.truffle.r.nodes.casts.ArgumentFilterSampler.ArgumentValueFilterSampler;

public class ValuePredicateArgumentFilterSampler<T> extends ValuePredicateArgumentFilter<T> implements ArgumentValueFilterSampler<T> {

    private final TypeExpr trueBranchTypes;
    private final Samples<T> samples;
    private final String desc;

    @SuppressWarnings("unchecked")
    public ValuePredicateArgumentFilterSampler(String desc, Predicate<? super T> valuePredicate, Set<? extends T> positiveSamples, Set<? extends T> negativeSamples, Set<Class<?>> allowedTypeSet) {
        super(valuePredicate);

        this.trueBranchTypes = allowedTypeSet.isEmpty() ? TypeExpr.ANYTHING : TypeExpr.union(allowedTypeSet);
        Predicate<Object> posMembership = x -> trueBranchTypes.isInstance(x) && test((T) x);
        this.samples = new Samples<>(desc, positiveSamples, negativeSamples, posMembership);

        assert positiveSamples.stream().allMatch(x -> valuePredicate.test(x));

        this.desc = desc;
    }

    @Override
    public TypeExpr trueBranchType() {
        return trueBranchTypes;
    }

    @Override
    public String toString() {
        return desc;
    }

    @Override
    public Samples<T> collectSamples(TypeExpr inputType) {
        return samples;
    }

    public static <T> ValuePredicateArgumentFilterSampler<T> fromLambdaWithSamples(Predicate<? super T> predicate, Set<? extends T> positiveSamples, Set<? extends T> negativeSamples,
                    Class<?> resultClass) {
        return new ValuePredicateArgumentFilterSampler<>(CastUtils.getPredefStepDesc(), predicate, positiveSamples, negativeSamples, Collections.singleton(resultClass));
    }

    public static <T> ValuePredicateArgumentFilterSampler<T> fromLambdaWithResTypes(Predicate<T> predicate, Class<?>... resultClass) {
        return new ValuePredicateArgumentFilterSampler<>(CastUtils.getPredefStepDesc(), predicate, Collections.emptySet(), Collections.emptySet(),
                        Arrays.asList(resultClass).stream().collect(Collectors.toSet()));
    }

    public static <T> ValuePredicateArgumentFilterSampler<T> fromLambdaWithSamples(Predicate<T> predicate, Set<? extends T> positiveSamples, Set<? extends T> negativeSamples) {
        return new ValuePredicateArgumentFilterSampler<>(CastUtils.getPredefStepDesc(), predicate, positiveSamples, negativeSamples, Collections.emptySet());
    }

    public static <T> ValuePredicateArgumentFilterSampler<T> fromLambdaWithSamples(Predicate<T> predicate, Set<T> negativeSamples,
                    @SuppressWarnings("unused") Class<T> commonAncestorClass, Set<Class<?>> resultClasses) {
        return new ValuePredicateArgumentFilterSampler<>(CastUtils.getPredefStepDesc(), predicate, Collections.emptySet(), negativeSamples, resultClasses);
    }

}
