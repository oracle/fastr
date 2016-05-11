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
package com.oracle.truffle.r.nodes.builtin;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.oracle.truffle.r.nodes.builtin.ArgumentFilter.ArgumentTypeFilter;
import com.oracle.truffle.r.nodes.unary.CastNode.Samples;
import com.oracle.truffle.r.nodes.unary.CastNode.TypeExpr;
import com.oracle.truffle.r.runtime.data.RNull;

public class TypePredicateArgumentFilter<T, R extends T> extends AbstractPredicateArgumentFilter<T, R> implements ArgumentTypeFilter<T, R> {

    public TypePredicateArgumentFilter(Predicate<? super T> valuePredicate, Set<? extends R> positiveSamples, Set<?> negativeSamples, Set<Class<?>> allowedTypeSet, boolean isNullable) {
        super(valuePredicate, positiveSamples, negativeSamples, allowedTypeSet, isNullable);
    }

    public static <T, R extends T> TypePredicateArgumentFilter<T, R> fromLambda(Predicate<? super T> predicate, Set<? extends R> positiveSamples, Set<?> negativeSamples,
                    Class<?> resultClass) {
        return new TypePredicateArgumentFilter<>(predicate, positiveSamples, negativeSamples, Collections.singleton(resultClass), false);
    }

    public static <T, R extends T> TypePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate, Class<?>... resultClass) {
        return new TypePredicateArgumentFilter<>(predicate, Collections.emptySet(), Collections.emptySet(), Arrays.asList(resultClass).stream().collect(Collectors.toSet()), false);
    }

    public static <T, R extends T> TypePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate, Set<? extends R> positiveSamples, Set<?> negativeSamples) {
        return new TypePredicateArgumentFilter<>(predicate, positiveSamples, negativeSamples, Collections.emptySet(), false);
    }

    public static <T, R extends T> TypePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate) {
        return new TypePredicateArgumentFilter<>(predicate, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), false);
    }

    public static <T, R extends T> TypePredicateArgumentFilter<T, R> fromLambda(Predicate<T> predicate, @SuppressWarnings("unused") Class<R> commonAncestorClass, Set<Class<?>> resultClasses) {
        return new TypePredicateArgumentFilter<>(predicate, Collections.emptySet(), Collections.emptySet(), resultClasses, false);
    }
}
