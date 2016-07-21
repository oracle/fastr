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

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.casts.CastUtils.Cast.Coverage;

public final class TypeConjunction implements WildcardType, TypeAndInstanceCheck {

    private final Set<Type> upperBounds;

    TypeConjunction(Set<Type> upperBounds) {
        this.upperBounds = new HashSet<>(upperBounds);
    }

    public static TypeConjunction create(Type... types) {
        Set<Type> upperBounds = Arrays.asList(types).stream().flatMap(t -> (t instanceof TypeConjunction ? ((TypeConjunction) t).upperBounds : Collections.singleton(t)).stream()).collect(
                        Collectors.toSet());
        return new TypeConjunction(upperBounds);
    }

    public static TypeConjunction fromType(Type t) {
        if (t instanceof TypeConjunction) {
            return (TypeConjunction) t;
        } else {
            return new TypeConjunction(Collections.singleton(t));
        }
    }

    @Override
    public Type[] getUpperBounds() {
        return upperBounds.toArray(new Type[upperBounds.size()]);
    }

    @Override
    public Type[] getLowerBounds() {
        return new Type[0];
    }

    @Override
    public boolean isInstance(Object x) {
        return upperBounds.stream().filter(t -> ((TypeAndInstanceCheck) t).isInstance(x)).findAny().isPresent();
    }

    public Coverage coverageFrom(TypeConjunction from, boolean includeImplicits) {
        return upperBounds.stream().map(ubt -> from.coverageTo(ubt, includeImplicits)).reduce((res, cvg) -> cvg.and(res)).orElse(Coverage.none);
    }

    public Coverage coverageTo(Type to, boolean includeImplicits) {
        assert to instanceof Not || to instanceof Class;
        return upperBounds.stream().map(ubt -> TypeAndInstanceCheck.coverage(ubt, to, includeImplicits)).reduce(
                        (res, cvg) -> res == Coverage.none || cvg == Coverage.none ? Coverage.none : cvg.or(res)).orElse(Coverage.none);
    }

    @Override
    public Optional<Class<?>> classify() {
        Set<Class<?>> classes = upperBounds.stream().filter(t -> t instanceof Class<?>).map(t -> (Class<?>) t).collect(Collectors.toSet());
        return classes.size() == 1 ? Optional.of(classes.iterator().next()) : Optional.empty();
    }

    @Override
    public String toString() {
        return "(? extends " + upperBounds + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((upperBounds == null) ? 0 : upperBounds.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TypeConjunction other = (TypeConjunction) obj;
        return upperBounds.equals(other.upperBounds);
    }

}
