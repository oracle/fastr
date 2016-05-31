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

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.CastUtils;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.casts.TypeExpr;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class FindFirstNodeGenSampler extends CastNodeSampler<FindFirstNodeGen> {

    private final Object defaultValue;
    private final Class<?> elementClass;

    public FindFirstNodeGenSampler(FindFirstNodeGen castNode) {
        super(castNode);
        this.elementClass = castNode.getElementClass();
        this.defaultValue = castNode.getDefaultValue();
    }

    @Override
    public Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
        Samples<Object> defaultSamples = defaultSamples();

        // convert scalar samples to vector ones
        Samples<Object> vectorizedSamples = downStreamSamples.map(x -> CastUtils.singletonVector(x), x -> CastUtils.singletonVector(x));
        return defaultSamples.and(vectorizedSamples);
    }

    private Samples<Object> defaultSamples() {
        Set<Object> defaultNegativeSamples = new HashSet<>();
        Set<Object> defaultPositiveSamples = new HashSet<>();

        if (defaultValue == null) {
            defaultNegativeSamples.add(RNull.instance);
            Object emptyVec = CastUtils.emptyVector(elementClass);
            if (emptyVec != null) {
                defaultNegativeSamples.add(emptyVec);
            }
            defaultNegativeSamples.add(RNull.instance);
        } else {
            defaultPositiveSamples.add(CastUtils.singletonVector(defaultValue));
        }

        return new Samples<>(defaultPositiveSamples, defaultNegativeSamples);
    }

    @Override
    public TypeExpr resultTypes(TypeExpr inputType) {
        if (elementClass == null || elementClass == Object.class) {
            if (inputType.isAnything()) {
                return TypeExpr.atom(RAbstractVector.class).not();
            } else {
                Set<Type> resTypes = inputType.classify().stream().
                                map(c -> CastUtils.elementType(c)).
                                collect(Collectors.toSet());
                return TypeExpr.union(resTypes);
            }
        } else {
            return TypeExpr.atom(elementClass).or(TypeExpr.atom(RNull.class)).or(TypeExpr.atom(RMissing.class));
        }
    }

}
