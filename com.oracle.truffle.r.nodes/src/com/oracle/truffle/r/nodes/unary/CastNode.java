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

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.builtin.CastUtils;
import com.oracle.truffle.r.nodes.builtin.CastUtils.Cast;
import com.oracle.truffle.r.nodes.builtin.CastUtils.Cast.Coverage;
import com.oracle.truffle.r.nodes.builtin.CastUtils.Casts;
import com.oracle.truffle.r.nodes.unary.CastNode.Not;
import com.oracle.truffle.r.nodes.unary.CastNode.TypeAndInstanceCheck;
import com.sun.prism.PixelFormat;

import sun.text.resources.ja.CollationData_ja;

/**
 * Cast nodes behave like unary nodes, but in many cases it is useful to have a specific type for
 * casts.
 */
public abstract class CastNode extends UnaryNode {

    public final TypeExpr resultTypes() {
        return resultTypes(TypeExpr.ANYTHING);
    }

    protected TypeExpr resultTypes(TypeExpr inputType) {
        return CastUtils.Casts.createCastNodeCasts(getClass()).narrow(inputType);
    }

    public final Samples<?> collectSamples() {
        Set<Type> resTypes = resultTypes().normalize().stream().filter(cls -> cls != Object.class).collect(Collectors.toSet());

        Set<?> defaultPositiveSamples;
        if (resTypes.isEmpty()) {
            defaultPositiveSamples = CastUtils.sampleValuesForType(Object.class);
        } else {
            defaultPositiveSamples = resTypes.stream().flatMap(cls -> CastUtils.sampleValuesForType(cls).stream()).collect(Collectors.toSet());
        }

        return collectSamples(TypeExpr.ANYTHING, new Samples<>(defaultPositiveSamples, Collections.emptySet()));
    }

    @SuppressWarnings("unused")
    protected Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
        return Samples.EMPTY;
    }

    public static final class Samples<T> {

        private static final Samples<?> EMPTY = new Samples<>(Collections.emptySet(), Collections.emptySet());

        @SuppressWarnings("unchecked")
        public static <T> Samples<T> empty() {
            return (Samples<T>) EMPTY;
        }

        private final Set<? extends T> posSamples;
        private final Set<?> negSamples;

        public Samples(Set<? extends T> positiveSamples, Set<?> negativeSamples) {
            this.posSamples = positiveSamples;
            this.negSamples = negativeSamples;
        }

        public Set<? extends T> positiveSamples() {
            return posSamples;
        }

        public Set<?> negativeSamples() {
            return negSamples;
        }

        public <R> Samples<R> map(Function<T, R> posMapper, Function<Object, Object> negMapper) {
            Set<R> mappedPositive = positiveSamples().stream().map(posMapper).collect(Collectors.toSet());
            Set<Object> mappedNegative = negativeSamples().stream().map(negMapper).collect(Collectors.toSet());
            return new Samples<>(mappedPositive, mappedNegative);
        }

        public Samples<T> filter(Predicate<T> posCondition) {
            Set<T> newPositive = positiveSamples().stream().filter(posCondition).collect(Collectors.toSet());
            Set<T> newNegativeFromPositive = positiveSamples().stream().filter(x -> !posCondition.test(x)).collect(Collectors.toSet());
            Set<Object> newNegative = new HashSet<>(negativeSamples());
            newNegative.addAll(newNegativeFromPositive);
            return new Samples<>(newPositive, newNegative);
        }

        public Samples<T> and(Samples<? extends T> other) {
            Set<Object> negativeUnion = new HashSet<>(other.negativeSamples());
            negativeUnion.addAll(negativeSamples());
            Set<T> positiveUnion = new HashSet<>(other.positiveSamples());
            positiveUnion.addAll(positiveSamples());
            positiveUnion.removeAll(negativeUnion);

            return new Samples<>(positiveUnion, negativeUnion);
        }

        public Samples<T> or(Samples<? extends T> other) {
            Set<T> positiveUnion = new HashSet<>(other.positiveSamples());
            positiveUnion.addAll(positiveSamples());

            Set<Object> negativeUnion = new HashSet<>(other.negativeSamples());
            negativeUnion.addAll(negativeSamples());
            negativeUnion.removeAll(positiveUnion);

            return new Samples<>(positiveUnion, negativeUnion);
        }

        public Samples<Object> swap() {
            return new Samples<>(negSamples, posSamples);
        }

        @Override
        public String toString() {
            return posSamples.toString() + ":" + negSamples.toString();
        }
    }

    public interface TypeAndInstanceCheck {
        boolean isInstance(Object x);

        Optional<Class<?>> classify();

        static Coverage coverage(Type from, Type to, boolean includeImplicits) {
            assert (to instanceof Not || to instanceof Class);
            assert (from instanceof Not || from instanceof Class);

            if (Not.negateType(to).equals(from)) {
                return Coverage.none;
            }

            boolean toPositive = !Not.isNegative(to);
            boolean fromPositive = !Not.isNegative(from);
            Class<?> positiveToCls = (Class<?>) CastUtils.Casts.boxType(Not.getPositiveType(to));
            Class<?> positiveFromCls = (Class<?>) CastUtils.Casts.boxType(Not.getPositiveType(from));

            Cast.Coverage positiveCoverage;
            if (((Class<?>) positiveToCls).isAssignableFrom(positiveFromCls)) {
                positiveCoverage = Cast.Coverage.full;
            } else if (((Class<?>) positiveFromCls).isAssignableFrom(positiveToCls)) {
                positiveCoverage = Cast.Coverage.partial;
            } else if (positiveToCls.isInterface() && (positiveFromCls.isInterface() || !(Modifier.isFinal(positiveFromCls.getModifiers()) || !Modifier.isAbstract(positiveFromCls.getModifiers())))) {
                positiveCoverage = Cast.Coverage.potential;
            } else if (positiveFromCls.isInterface() && (positiveToCls.isInterface() || !(Modifier.isFinal(positiveToCls.getModifiers()) || !Modifier.isAbstract(positiveToCls.getModifiers())))) {
                positiveCoverage = Cast.Coverage.potential;
            } else {
                positiveCoverage = Cast.Coverage.none;
            }

            Cast.Coverage implicitCvrg = Cast.Coverage.none;
            if (includeImplicits && Casts.hasImplicitCast(positiveFromCls, positiveToCls)) {
                implicitCvrg = Cast.Coverage.potential;
            }

            positiveCoverage = implicitCvrg.or(positiveCoverage);

            return positiveCoverage.transpose(positiveFromCls, positiveToCls, fromPositive, toPositive);
        }

    }

    public static final class TypeExpr {

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
            return disjNormForm.stream().
                            map(conj -> normalize(conj)).
                            filter(t -> !t.equals(Not.NOTHING)).
                            collect(Collectors.toSet());
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
                            if (CastUtils.Casts.isConvertible(conjArray[j], conjArray[i], true) == Coverage.full) {
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
            return normalize().stream().
                            map(t -> classify(t)).
                            filter(ot -> ot.isPresent()).
                            map(ot -> ot.get()).
                            collect(Collectors.toSet());
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

        public Coverage coverageFrom(Type from, boolean includeImplicits) {
            return normalize().stream().map(t -> CastUtils.Casts.isConvertible(from, t, includeImplicits)).reduce((res, cvg) -> res.or(cvg)).orElse(Coverage.none);
        }

        @Override
        public String toString() {
            return disjNormForm.toString();
        }

    }

    public static final class Not<T> implements Type, TypeAndInstanceCheck {

        public static final Not<Object> NOTHING = new Not<>(Object.class);

        private final Class<T> negated;

        private Not(Class<T> negated) {
            this.negated = negated;
        }

        public Class<T> getNegated() {
            return this.negated;
        }

        @Override
        public boolean isInstance(Object x) {
            return !negated.isInstance(x);
        }

        public static boolean isNegative(Type t) {
            return t instanceof Not;
        }

        public static Type getPositiveType(Type t) {
            if (isNegative(t)) {
                return ((Not<?>) t).getNegated();
            } else {
                return t;
            }
        }

        public static Type negateType(Type t) {
            if (isNegative(t)) {
                return getPositiveType(t);
            } else {
                assert t instanceof Class;
                return new Not<>((Class<?>) t);
            }
        }

        @Override
        public String toString() {
            return "Not(" + negated.getSimpleName() + ")";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((negated == null) ? 0 : negated.hashCode());
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
            Not<?> other = (Not<?>) obj;
            return other.negated == this.negated;
        }

        @Override
        public Optional<Class<?>> classify() {
            return Optional.empty();
        }

    }

    public static final class TypeConjunction implements WildcardType, TypeAndInstanceCheck {

        private final Set<Type> upperBounds;

        TypeConjunction(Set<Type> upperBounds) {
            this.upperBounds = new HashSet<>(upperBounds);
        }

        public static TypeConjunction create(Type... types) {
            Set<Type> upperBounds = Arrays.asList(types).stream().
                            flatMap(t -> (t instanceof TypeConjunction ? ((TypeConjunction) t).upperBounds : Collections.singleton(t)).stream()).
                            collect(Collectors.toSet());
            return new TypeConjunction(upperBounds);
        }

        public static TypeConjunction fromType(Type t) {
            if (t instanceof TypeConjunction) {
                return (TypeConjunction) t;
            } else {
                return new TypeConjunction(Collections.singleton(t));
            }
        }

        public Type[] getUpperBounds() {
            return upperBounds.toArray(new Type[upperBounds.size()]);
        }

        public Type[] getLowerBounds() {
            return new Type[0];
        }

        @Override
        public boolean isInstance(Object x) {
            return upperBounds.stream().filter(t -> ((TypeAndInstanceCheck) t).isInstance(x)).findAny().isPresent();
        }

        public Coverage coverageFrom(TypeConjunction from, boolean includeImplicits) {
            return upperBounds.stream().
                            map(ubt -> from.coverageTo(ubt, includeImplicits)).
                            reduce((res, cvg) -> cvg.and(res)).
                            orElse(Coverage.none);
        }

        public Coverage coverageTo(Type to, boolean includeImplicits) {
            assert to instanceof Not || to instanceof Class;
            return upperBounds.stream().
                            map(ubt -> TypeAndInstanceCheck.coverage(ubt, to, includeImplicits)).
                            reduce((res, cvg) -> res == Coverage.none || cvg == Coverage.none ? Coverage.none : cvg.or(res)).
                            orElse(Coverage.none);
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

}
