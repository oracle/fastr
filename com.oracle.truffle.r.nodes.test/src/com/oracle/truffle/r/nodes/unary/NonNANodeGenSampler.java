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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.CastUtils;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.casts.TypeExpr;

public class NonNANodeGenSampler extends CastNodeSampler<NonNANodeGen> {

    private final Object naReplacement;

    public NonNANodeGenSampler(NonNANodeGen castNode) {
        super(castNode);
        naReplacement = castNode.getNAReplacement();
    }

    @Override
    public TypeExpr resultTypes(TypeExpr inputType) {
        return inputType;
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> unmap(T x) {
        if (naReplacement == null) {
            return CastUtils.isNaValue(x) ? Optional.empty() : Optional.of(x);
        } else {
            return CastUtils.isNaValue(x) ? Optional.of((T) naReplacement) : Optional.of(x);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Samples<?> collectSamples(TypeExpr inputTypes, Samples<?> downStreamSamples) {
        Set<Object> defaultPositiveSamples;
        Set<Object> defaultNegativeSamples;

        Samples<Object> mappedSamples = ((Samples<Object>) downStreamSamples).map(x -> x, x -> x, this::unmap, this::unmap);

        Set<Object> naSamples = inputTypes.normalize().stream().filter(t -> t instanceof Class).map(t -> CastUtils.naValue((Class<?>) t)).filter(x -> x != null).collect(Collectors.toSet());
        if (naReplacement != null) {
            defaultNegativeSamples = Collections.emptySet();
            defaultPositiveSamples = new HashSet<>();
            defaultPositiveSamples.add(naReplacement);
            defaultPositiveSamples.addAll(naSamples);
        } else {
            defaultNegativeSamples = naSamples;
            defaultPositiveSamples = Collections.emptySet();
        }

        Predicate<Object> posMembership = x -> naReplacement != null || !CastUtils.isNaValue(x);
        Samples<Object> defaultSamples = new Samples<>("nonNA-" + naReplacement == null ? "noDef" : "withDef", defaultPositiveSamples, defaultNegativeSamples, posMembership);

        Samples<Object> combined = defaultSamples.and(mappedSamples);
        return combined;
    }

}
