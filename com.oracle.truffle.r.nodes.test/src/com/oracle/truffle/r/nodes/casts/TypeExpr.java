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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.casts.CastUtils.Cast;

public final class TypeExpr {

    public static final TypeExpr ANYTHING = TypeExpr.atom(Object.class);
    public static final TypeExpr NOTHING = new TypeExpr(Collections.emptySet());

    private final Set<Set<? extends Type>> disjNormForm;

    private TypeExpr(Set<Set<? extends Type>> disjNormForm) {
        this.disjNormForm = disjNormForm;
    }

    public boolean isNothing() {
        return disjNormForm.isEmpty();
    }

    public boolean isAnything() {
        if (disjNormForm.size() != 1) {
            return false;
        } else {
            Set<? extends Type> conj = disjNormForm.iterator().next();
            return conj.size() == 1 && conj.iterator().next() == Object.class;
        }
    }

    public static TypeExpr atom(Type t) {
        return new TypeExpr(Collections.singleton(Collections.singleton(t)));
    }

    public TypeExpr map(Function<Type, Type> typeMapper) {
        Set<Set<? extends Type>> newDisjNormForm = disjNormForm.stream().map(conj -> conj.stream().map(typeMapper).collect(Collectors.toSet())).collect(Collectors.toSet());
        return new TypeExpr(newDisjNormForm);
    }

    public TypeExpr filter(Predicate<Type> filterPred) {
        Set<Set<? extends Type>> newDisjNormForm = disjNormForm.stream().map(conjSet -> {
            return conjSet.stream().filter(filterPred).collect(Collectors.toSet());
        }).filter(newConjSet -> !newConjSet.isEmpty()).collect(Collectors.toSet());
        return new TypeExpr(newDisjNormForm);
    }

    public boolean contains(Type tp) {
        return disjNormForm.stream().flatMap(conjSet -> conjSet.stream().filter(t -> t.equals(tp))).findFirst().isPresent();
    }

    public TypeExpr or(TypeExpr te) {
        Set<Set<? extends Type>> newDisjNormForm = new HashSet<>(this.disjNormForm);
        newDisjNormForm.addAll(te.disjNormForm);
        return new TypeExpr(newDisjNormForm);
    }

    public TypeExpr and(TypeExpr te) {
        Set<Set<? extends Type>> newDisjNormForm = new HashSet<>();

        for (Set<? extends Type> conj1 : this.disjNormForm) {
            for (Set<? extends Type> conj2 : te.disjNormForm) {
                Set<Type> newConj = new HashSet<>(conj1);
                newConj.addAll(conj2);
                newDisjNormForm.add(newConj);
            }
        }

        return new TypeExpr(newDisjNormForm);
    }

    public static TypeExpr union(Set<? extends Type> types) {
        Set<Set<? extends Type>> disjNormForm = types.stream().map(t -> Collections.singleton(t)).collect(Collectors.toSet());
        return new TypeExpr(disjNormForm);
    }

    public static TypeExpr union(Class<?>... types) {
        return union(new HashSet<Type>(Arrays.asList(types)));
    }

    public TypeExpr not() {
        if (isNothing()) {
            return ANYTHING;
        }
        if (isAnything()) {
            return NOTHING;
        }
        // !(A.B | C.D) = !(A.B).!(C.D) = (!A | !B).(!C | !D) = !A.!C | !A.!D | !B.!C | !B.!D
        // !(!A.!B | !C.!D) = !(!A.!B).!(!C.!D) = (A | B).(C | D) = A.C | A.D | B.C | B.D
        Set<TypeExpr> asUnions = disjNormForm.stream().map(conj -> union(conj)).collect(Collectors.toSet());
        TypeExpr conj = asUnions.stream().reduce((res, te) -> res.and(te)).orElse(NOTHING);
        return conj.map(t -> Not.negateType(t));
    }

    public Set<Type> normalize() {
        return disjNormForm.stream().map(conj -> normalize(conj)).filter(t -> !t.equals(Not.NOTHING)).collect(Collectors.toSet());
    }

    public Set<Class<?>> toClasses() {
        return normalize().stream().filter(t -> t instanceof Class).map(t -> (Class<?>) t).collect(Collectors.toSet());
    }

    private static Type normalize(Set<? extends Type> conj) {
        Type[] conjArray = conj.toArray(new Type[conj.size()]);
        Set<Type> lessSpecific = new HashSet<>();

        for (int i = 0; i < conj.size(); i++) {
            for (int j = i + 1; j < conj.size(); j++) {

                switch (CastUtils.Casts.isConvertible(conjArray[i], conjArray[j], true)) {
                    case none:
                        // a contradiction found
                        return Not.NOTHING;
                    case potential:
                        break;
                    case partial:
                        if (CastUtils.Casts.isConvertible(conjArray[j], conjArray[i], true) == Cast.Coverage.full) {
                            lessSpecific.add(conjArray[i]);
                        }
                        break;
                    case full:
                        lessSpecific.add(conjArray[j]);
                        break;
                }
            }
        }

        HashSet<Type> moreSpecific = new HashSet<>(conj);
        moreSpecific.removeAll(lessSpecific);

        if (moreSpecific.size() == 1) {
            Type t = moreSpecific.iterator().next();
            return t;
        } else {
            assert !moreSpecific.isEmpty();
            return new TypeConjunction(moreSpecific);
        }
    }

    public Set<Class<?>> classify() {
        return normalize().stream().map(t -> classify(t)).filter(ot -> ot.isPresent()).map(ot -> ot.get()).collect(Collectors.toSet());
    }

    private static Optional<Class<?>> classify(Type t) {
        if (t instanceof Class<?>) {
            return Optional.of((Class<?>) t);
        } else {
            assert t instanceof TypeAndInstanceCheck;
            return ((TypeAndInstanceCheck) t).classify();
        }
    }

    public boolean isInstance(Object x) {
        return normalize().stream().filter(t -> {
            if (t instanceof TypeAndInstanceCheck) {
                return ((TypeAndInstanceCheck) t).isInstance(x);
            } else {
                assert t instanceof Class;
                return ((Class<?>) t).isInstance(x);
            }
        }).findAny().isPresent();
    }

    public Cast.Coverage isConvertibleFrom(Type from, boolean includeImplicits) {
        return normalize().stream().map(t -> CastUtils.Casts.isConvertible(from, t, includeImplicits)).reduce((res, cvg) -> res.or(cvg)).orElse(Cast.Coverage.none);
    }

    @Override
    public String toString() {
        return disjNormForm.toString();
    }

}
