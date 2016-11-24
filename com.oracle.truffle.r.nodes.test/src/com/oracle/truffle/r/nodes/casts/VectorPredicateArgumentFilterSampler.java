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
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.builtin.VectorPredicateArgumentFilter;
import com.oracle.truffle.r.nodes.casts.ArgumentFilterSampler.ArgumentValueFilterSampler;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class VectorPredicateArgumentFilterSampler<T extends RAbstractVector> extends VectorPredicateArgumentFilter<T> implements ArgumentValueFilterSampler<T> {

    private final String desc;
    private final List<Integer> invalidVectorSize;

    public VectorPredicateArgumentFilterSampler(String desc, Predicate<T> valuePredicate, Integer... invalidVectorSize) {
        super(valuePredicate);
        this.desc = desc;
        this.invalidVectorSize = invalidVectorSize == null ? Collections.emptyList() : Arrays.asList(invalidVectorSize);
    }

    @SuppressWarnings("unchecked")
    private boolean testVector(Object x) {
        if (x instanceof RAbstractVector) {
            return test((T) x);
        } else {
            Object v = CastUtils.singletonVector(x);
            if (v == RNull.instance || v == RMissing.instance) {
                // these values are ignored, they should never appear in a filter or a mapper
                return true;
            }
            return test((T) v);
        }
    }

    @Override
    public String toString() {
        return this.desc;
    }

    @Override
    public Samples<T> collectSamples(TypeExpr inputType) {
        Set<RAbstractVector> negSamples = inputType.toClasses().stream().flatMap(vt -> invalidVectorSize.stream().map(sz -> CastUtils.vectorOfSize(vt, sz))).collect(Collectors.toSet());

        Predicate<Object> posMembership = this::testVector;
        final Samples<T> samples = new Samples<>(desc, Collections.emptySet(), negSamples, posMembership);

        return samples;
    }

    @Override
    public TypeExpr trueBranchType() {
        return TypeExpr.ANYTHING;
    }

}
