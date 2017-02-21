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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.casts.CastUtils.Cast;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public final class TypeExpr {

    public static final TypeExpr ANYTHING = TypeExpr.atom(Object.class);
    public static final TypeExpr NOTHING = new TypeExpr(Collections.emptySet(), Samples.nothing());

    private final Set<Set<Type>> disjNormForm;

    private final Samples<?> samples;

    private static final Set<Set<Class<?>>> mutuallyExclusiveInterfaces = new HashSet<>();

    static {
        registerMutuallyExclusiveInterfaces(RAbstractIntVector.class, RAbstractDoubleVector.class, RAbstractLogicalVector.class, RAbstractComplexVector.class, RAbstractRawVector.class,
                        RAbstractStringVector.class, RAbstractListVector.class);
    }

    private TypeExpr(Set<Set<Type>> disjNormForm, Samples<?> samples) {
        // remove contradictions
        // Set<Set<Type>> noContra = disjNormForm.stream().filter(conj ->
        // !isContradiction(conj)).collect(Collectors.toSet());
        Set<Set<Type>> noContra = disjNormForm.stream().map(conj -> normalizeConjunctionSet(conj)).filter(conj -> !conj.isEmpty()).collect(Collectors.toSet());
        this.disjNormForm = noContra;
        this.samples = samples;
    }

    public static void registerMutuallyExclusiveInterfaces(Class<?>... types) {
        mutuallyExclusiveInterfaces.add(Arrays.stream(types).collect(Collectors.toSet()));
    }

    public static Optional<Class<?>> findInMutuallyExclusiveInterfaces(Type t, Set<Class<?>> exclusiveGroup) {
        if (t instanceof Class) {
            return exclusiveGroup.stream().filter(x -> x.isAssignableFrom((Class<?>) t)).findFirst();
        } else {
            return Optional.empty();
        }
    }

    public static boolean areMutuallyExclusiveInterfaces(Type t1, Type t2, Set<Class<?>> exclusiveGroup) {
        Optional<Class<?>> x1 = findInMutuallyExclusiveInterfaces(t1, exclusiveGroup);
        if (!x1.isPresent()) {
            return false;
        }
        Optional<Class<?>> x2 = findInMutuallyExclusiveInterfaces(t2, exclusiveGroup);
        if (!x2.isPresent()) {
            return false;
        }

        return x1.get() != x2.get();
    }

    public static boolean areMutuallyExclusive(Type t1, Type t2) {
        return mutuallyExclusiveInterfaces.stream().filter(exclusiveTypesSet -> areMutuallyExclusiveInterfaces(t1, t2, exclusiveTypesSet)).findFirst().isPresent();
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
        Samples<?> samples = new Samples<>(t.getTypeName(), Collections.emptySet(), Collections.emptySet(), x -> TypeAndInstanceCheck.isInstance(t, x));
        return new TypeExpr(Collections.singleton(Collections.singleton(t)), samples);
    }

    public boolean contains(Type tp) {
        return disjNormForm.stream().flatMap(conjSet -> conjSet.stream().filter(t -> t.equals(tp))).findFirst().isPresent();
    }

    public TypeExpr or(TypeExpr te) {
        Set<Set<Type>> newDisjNormForm = new HashSet<>(this.disjNormForm);
        newDisjNormForm.addAll(te.disjNormForm);
        return new TypeExpr(newDisjNormForm, samples.or(te.samples));
    }

    public TypeExpr and(TypeExpr te) {
        Set<Set<Type>> newDisjNormForm = new HashSet<>();

        for (Set<Type> conj1 : this.disjNormForm) {
            for (Set<? extends Type> conj2 : te.disjNormForm) {
                Set<Type> newConj = new HashSet<>(conj1);
                newConj.addAll(conj2);
                newDisjNormForm.add(newConj);
            }
        }

        return new TypeExpr(newDisjNormForm, samples.and(te.samples));
    }

    public TypeExpr lower() {
        return lower(new Object());
    }

    public TypeExpr removeWildcards() {
        Set<Set<Type>> newDisjNormForm = new HashSet<>();

        for (Set<Type> conj : this.disjNormForm) {
            newDisjNormForm.add(removeWildcardsFromConj(conj));
        }

        return new TypeExpr(newDisjNormForm, samples);
    }

    private static Set<Type> removeWildcardsFromConj(Set<Type> conj) {
        Set<Type> newConj = new HashSet<>();

        for (Type t : conj) {
            if (t instanceof Not) {
                Type neg = ((Not<?>) t).getNegated();
                if (!(neg instanceof UpperBoundsConjunction && ((UpperBoundsConjunction) neg).isWildcard())) {
                    newConj.add(t);
                } // the negation degenerates to Object.class as long as the negated type is a
                  // wildcard type
            } else if (t instanceof UpperBoundsConjunction) {
                Set<Type> flattened = ((UpperBoundsConjunction) t).flatten();
                flattened = removeWildcardsFromConj(flattened);
                newConj.addAll(flattened);
            } else {
                newConj.add(t);
            }
        }

        return newConj;
    }

    public TypeExpr positiveSamples(Object... sampleValues) {
        return new TypeExpr(disjNormForm, samples.addPositiveSamples(sampleValues));
    }

    public TypeExpr negativeSamples(Object... sampleValues) {
        return new TypeExpr(disjNormForm, samples.addNegativeSamples(sampleValues));
    }

    public TypeExpr lower(Object typeRepr) {
        Set<Set<Type>> newDisjNormForm = new HashSet<>();

        for (Set<Type> conj : disjNormForm) {
            UpperBoundsConjunction upperConj = UpperBoundsConjunction.create(conj.stream()).asWildcard(typeRepr);
            newDisjNormForm.add(Collections.singleton(upperConj));
        }

        return new TypeExpr(newDisjNormForm, samples);
    }

    public static TypeExpr union(Set<Type> types) {
        Set<Set<Type>> disjNormForm = types.stream().map(t -> Collections.singleton(t)).collect(Collectors.toSet());

        Predicate<Type> isInstancePred = x -> types.stream().filter(t -> TypeAndInstanceCheck.isInstance(t, x)).findFirst().isPresent();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Samples<?> samples = new Samples(types.toString(), Collections.emptySet(), Collections.emptySet(), isInstancePred);
        return new TypeExpr(disjNormForm, samples);
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
        Set<TypeExpr> asUnions = disjNormForm.stream().map(conj -> union(negateTypes(conj))).collect(Collectors.toSet());
        TypeExpr conj = asUnions.stream().reduce((res, te) -> res.and(te)).orElse(NOTHING);
        return new TypeExpr(conj.disjNormForm, samples.swap()); // add the negated samples
    }

    private static Set<Type> negateTypes(Set<Type> types) {
        return types.stream().map(t -> Not.negateType(t)).collect(Collectors.toSet());
    }

    public TypeExpr normalize() {
        Set<Set<Type>> conjunctionsNormalized = disjNormForm.stream().map(conj -> normalizeConjunctionSet(conj)).filter(t -> !t.equals(Not.NOTHING)).collect(Collectors.toSet());
        return new TypeExpr(conjunctionsNormalized, samples);
    }

    public Set<Type> toNormalizedConjunctionSet() {
        Set<Type> conjunctionsNormalized = disjNormForm.stream().map(conj -> normalizeConjunction(conj)).filter(t -> !t.equals(Not.NOTHING)).collect(Collectors.toSet());
        return conjunctionsNormalized;
        // return squashDisjunctions(conjunctionsNormalized);
    }

    public Set<Class<?>> toClasses() {
        return toNormalizedConjunctionSet().stream().filter(t -> t instanceof Class).map(t -> (Class<?>) t).collect(Collectors.toSet());
    }

    static Type normalizeConjunction(Set<Type> conj) {
        Set<Type> moreSpecific = normalizeConjunctionSet(conj);
        if (moreSpecific.isEmpty()) {
            return Not.NOTHING;
        } else if (moreSpecific.size() == 1) {
            Type t = moreSpecific.iterator().next();
            return t;
        } else {
            UpperBoundsConjunction ub = UpperBoundsConjunction.create(moreSpecific.stream());
            return ub;
        }
    }

    static Set<Type> normalizeConjunctionSet(Set<Type> conj) {
        Type[] conjArray = conj.toArray(new Type[conj.size()]);
        Set<Type> lessSpecific = new HashSet<>();

        for (int i = 0; i < conj.size(); i++) {
            for (int j = i + 1; j < conj.size(); j++) {

                switch (CastUtils.Casts.isConvertible(conjArray[i], conjArray[j], false)) {
                    case none:
                        // a contradiction found
                        return Collections.emptySet();
                    case potential:
                        break;
                    case partial:
                        if (CastUtils.Casts.isConvertible(conjArray[j], conjArray[i], false) == Cast.Coverage.full) {
                            lessSpecific.add(conjArray[i]);
                        }
                        break;
                    case full:
                        lessSpecific.add(conjArray[j]);
                        break;
                }
            }
        }

        Set<Type> moreSpecific = new HashSet<>(conj);
        moreSpecific.removeAll(lessSpecific);
        moreSpecific = moreSpecific.stream().map(t -> t instanceof TypeAndInstanceCheck ? ((TypeAndInstanceCheck) t).normalize() : t).collect(Collectors.toSet());
        return moreSpecific;
    }

    static boolean isContradiction(Set<Type> conj) {
        Type[] conjArray = conj.toArray(new Type[conj.size()]);
        for (int i = 0; i < conj.size(); i++) {
            for (int j = i + 1; j < conj.size(); j++) {

                switch (CastUtils.Casts.isConvertible(conjArray[i], conjArray[j], true)) {
                    case none:
                        // a contradiction found
                        return true;
                }
            }
        }

        return false;
    }

    public Set<Class<?>> classify() {
        return toNormalizedConjunctionSet().stream().map(t -> classify(t)).filter(ot -> ot.isPresent()).map(ot -> ot.get()).collect(Collectors.toSet());
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
        return toNormalizedConjunctionSet().stream().filter(t -> {
            if (t instanceof TypeAndInstanceCheck) {
                return ((TypeAndInstanceCheck) t).isInstance(x);
            } else {
                assert t instanceof Class;
                return ((Class<?>) t).isInstance(x);
            }
        }).findAny().isPresent();
    }

    public Cast.Coverage isConvertibleFrom(Type from, boolean includeImplicits) {
        return toNormalizedConjunctionSet().stream().map(t -> CastUtils.Casts.isConvertible(from, t, includeImplicits)).reduce((res, cvg) -> res.or(cvg)).orElse(Cast.Coverage.none);
    }

    @Override
    public String toString() {
        return disjNormForm.toString();
    }
}
