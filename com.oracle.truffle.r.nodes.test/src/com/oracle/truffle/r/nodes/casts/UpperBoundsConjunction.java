/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.Stream;

import com.oracle.truffle.r.nodes.casts.CastUtils.Cast.Coverage;
import com.oracle.truffle.r.runtime.RInternalError;

public final class UpperBoundsConjunction implements WildcardType, TypeAndInstanceCheck {
    private static final Type[] EMPTY_TYPES = new Type[0];

    private final Set<Type> upperBounds;
    private final boolean useAsWildcard;
    /**
     * This typeRepr object represents the wildcard type. Two wildcard types are equivalent if their
     * type representations are the same.
     */
    private final Object typeRepr;

    UpperBoundsConjunction(Set<Type> upperBounds, boolean useAsWildcard, Object typeRepr) {
        this.upperBounds = TypeExpr.normalizeConjunctionSet(upperBounds);
        this.useAsWildcard = useAsWildcard;
        this.typeRepr = typeRepr == null && useAsWildcard ? new Object() : typeRepr;
    }

    public UpperBoundsConjunction asWildcard(Object typeRepres) {
        return new UpperBoundsConjunction(upperBounds, true, typeRepres);
    }

    private UpperBoundsConjunction removeWildcardAndNormalize() {
        return new UpperBoundsConjunction(fromType(TypeExpr.normalizeConjunction(flatten())).upperBounds, false, null);
    }

    public Set<Type> flatten() {
        Set<Type> flattened = new HashSet<>();
        for (Type t : upperBounds) {
            if (t instanceof UpperBoundsConjunction) {
                flattened.addAll(((UpperBoundsConjunction) t).flatten());
            } else {
                flattened.add(t);
            }
        }
        return flattened;
    }

    public static UpperBoundsConjunction create(Type... types) {
        Stream<Type> typeStream = Arrays.asList(types).stream();
        return create(typeStream);
    }

    public static UpperBoundsConjunction create(Stream<? extends Type> typeStream) {
        Set<Type> upperBounds = typeStream.flatMap(t -> ((t instanceof UpperBoundsConjunction && !((UpperBoundsConjunction) t).isWildcard()) ? ((UpperBoundsConjunction) t).upperBounds
                        : Collections.singleton(t)).stream()).collect(Collectors.toSet());
        return new UpperBoundsConjunction(upperBounds, false, null);
    }

    public static UpperBoundsConjunction fromType(Type t) {
        if (t instanceof UpperBoundsConjunction) {
            return (UpperBoundsConjunction) t;
        } else {
            return new UpperBoundsConjunction(Collections.singleton(t), false, null);
        }
    }

    @Override
    public Type[] getUpperBounds() {
        return upperBounds.toArray(new Type[upperBounds.size()]);
    }

    @Override
    public Type[] getLowerBounds() {
        return useAsWildcard ? EMPTY_TYPES : getUpperBounds();
    }

    public boolean isWildcard() {
        return useAsWildcard;
    }

    @Override
    public boolean isInstance(Object x) {
        return upperBounds.stream().filter(t -> ((TypeAndInstanceCheck) t).isInstance(x)).findAny().isPresent();
    }

    public Coverage coverageFrom(UpperBoundsConjunction from, boolean includeImplicits) {
        return adjustCoverageFrom(upperBounds.stream().map(ubt -> {
            return from.coverageTo(ubt, includeImplicits);
        }).reduce((res, cvg) -> cvg.and(res)).orElse(Coverage.none), from);
    }

    public Coverage coverageTo(Type to, boolean includeImplicits) {
        assert to instanceof Not || to instanceof Class;
        return adjustCoverageTo(upperBounds.stream().map(ubt -> {
            return TypeAndInstanceCheck.coverage(ubt, to, includeImplicits);
        }).reduce((res, cvg) -> res == Coverage.none || cvg == Coverage.none ? Coverage.none : cvg.or(res)).orElse(Coverage.none));
    }

    private Coverage adjustCoverageTo(Coverage cvg) {
        if (isWildcard()) {
            switch (cvg) {
                case full:
                    return Coverage.full;
                case partial:
                    return Coverage.potential;
                case potential:
                    return Coverage.potential;
                case none:
                    return Coverage.none;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        } else {
            return cvg;
        }
    }

    private Coverage adjustCoverageFrom(Coverage cvg, UpperBoundsConjunction from) {
        if (isWildcard()) {
            switch (cvg) {
                case full:
                    UpperBoundsConjunction fromNoWildCard = from.removeWildcardAndNormalize();
                    UpperBoundsConjunction toNoWildCard = this.removeWildcardAndNormalize();

                    boolean bijection = fromNoWildCard.upperBounds.equals(toNoWildCard.upperBounds);
                    return bijection ? Coverage.partial : Coverage.potential;
                case partial:
                    return Coverage.partial;
                case potential:
                    return Coverage.potential;
                case none:
                    return Coverage.none;
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        } else {
            return cvg;
        }
    }

    @Override
    public Type normalize() {
        Set<Type> normUpperBounds = fromType(TypeExpr.normalizeConjunction(upperBounds)).upperBounds;
        if (!isWildcard() && normUpperBounds.size() == 1) {
            return normUpperBounds.iterator().next();
        } else {
            return new UpperBoundsConjunction(normUpperBounds, useAsWildcard, typeRepr);
        }
    }

    @Override
    public Optional<Class<?>> classify() {
        Set<Class<?>> classes = upperBounds.stream().filter(t -> t instanceof Class<?>).map(t -> (Class<?>) t).collect(Collectors.toSet());
        return classes.size() == 1 ? Optional.of(classes.iterator().next()) : Optional.empty();
    }

    @Override
    public String toString() {
        return isWildcard() ? "(" + System.identityHashCode(typeRepr) + " extends " + upperBounds + ")" : "(" + upperBounds + ")";
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
        UpperBoundsConjunction other = (UpperBoundsConjunction) obj;
        return useAsWildcard == other.useAsWildcard && upperBounds.equals(other.upperBounds) && (!useAsWildcard || typeRepr.equals(other.typeRepr));
    }
}
