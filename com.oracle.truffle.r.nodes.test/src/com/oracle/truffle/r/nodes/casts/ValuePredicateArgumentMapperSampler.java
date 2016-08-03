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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.oracle.truffle.r.nodes.builtin.ValuePredicateArgumentMapper;

public class ValuePredicateArgumentMapperSampler<T, R> extends ValuePredicateArgumentMapper<T, R> implements ArgumentMapperSampler<T, R> {

    private final Function<R, T> unmapper;
    private final TypeExpr inputTypes;
    private final TypeExpr resTypes;
    private final String desc;
    private final Samples<T> samples;

    public ValuePredicateArgumentMapperSampler(String desc, Function<T, R> mapper, Function<R, T> unmapper, Set<? extends T> positiveSamples, Set<?> negativeSamples, Set<Class<?>> inputTypeSet,
                    Set<Class<?>> resultTypeSet) {
        super(mapper);
        this.unmapper = unmapper;
        this.inputTypes = inputTypeSet.isEmpty() ? TypeExpr.ANYTHING : TypeExpr.union(inputTypeSet);
        this.resTypes = resultTypeSet.isEmpty() ? TypeExpr.ANYTHING : TypeExpr.union(resultTypeSet);
        this.desc = desc;
        Predicate<Object> posMembership = x -> inputTypes.isInstance(x) && !negativeSamples.contains(x);
        this.samples = new Samples<>(desc, positiveSamples, negativeSamples, posMembership);
    }

    @Override
    public TypeExpr resultTypes(TypeExpr it) {
        return resTypes;
    }

    @Override
    public String toString() {
        return desc;
    }

    public static <T, R> ValuePredicateArgumentMapperSampler<T, R> fromLambda(Function<T, R> mapper, Function<R, T> unmapper, Class<T> inputClass, Class<R> resultClass) {
        return new ValuePredicateArgumentMapperSampler<>(CastUtils.getPredefStepDesc(), mapper, unmapper, Collections.emptySet(), Collections.emptySet(),
                        inputClass == null ? Collections.emptySet() : Collections.singleton(inputClass),
                        resultClass == null ? Collections.emptySet() : Collections.singleton(resultClass));
    }

    public static <T, R> ValuePredicateArgumentMapperSampler<T, R> fromLambda(Function<T, R> mapper, Function<R, T> unmapper, Set<? extends T> positiveSamples, Set<? extends T> negativeSamples,
                    Class<T> inputClass, Class<R> resultClass) {
        return new ValuePredicateArgumentMapperSampler<>(CastUtils.getPredefStepDesc(), mapper, unmapper, positiveSamples, negativeSamples, Collections.singleton(inputClass),
                        Collections.singleton(resultClass));
    }

    @Override
    public Samples<T> collectSamples(Samples<R> downStreamSamples) {
        if (unmapper == null) {
            return samples;
        } else {
            Samples<R> filtered = downStreamSamples.filter(x -> resTypes.isInstance(x), x -> resTypes.isInstance(x));
            @SuppressWarnings("unchecked")
            Samples<T> unmappedSamples = filtered.map(x -> unmapper.apply(x), x -> unmapper.apply((R) x),
                            x -> inputTypes.isInstance(x) ? Optional.of(mapper.apply((T) x)) : Optional.empty(),
                            x -> inputTypes.isInstance(x) ? Optional.of(mapper.apply((T) x)) : Optional.empty());

            Samples<T> combined = samples.and(unmappedSamples);
            return combined;
        }
    }

}
