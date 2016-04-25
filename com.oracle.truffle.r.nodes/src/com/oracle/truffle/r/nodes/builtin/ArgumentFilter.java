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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.r.nodes.unary.CastNode.Samples;

public interface ArgumentFilter<T, R extends T> {

    boolean test(T arg);

    Samples<T> collectSamples(Samples<? extends T> downStreamSamples);

    Set<Class<?>> allowedTypes();

    default <S extends T> ArgumentFilter<T, ?> union(ArgumentFilter<T, S> other) {
        return new ArgumentFilter<T, S>() {

            public boolean test(T arg) {
                if (ArgumentFilter.this.test(arg)) {
                    return true;
                } else {
                    return other.test(arg);
                }
            }

            public Set<Class<?>> allowedTypes() {
                final Set<Class<?>> typesUnion;
                Set<Class<?>> tu = new HashSet<>(ArgumentFilter.this.allowedTypes());
                tu.addAll(other.allowedTypes());
                typesUnion = Collections.unmodifiableSet(tu);
                return typesUnion;
            }

            @Override
            public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
                Samples<T> thisSamples = ArgumentFilter.this.collectSamples(downStreamSamples);
                Samples<T> otherSamples = other.collectSamples(downStreamSamples);

                return thisSamples.or(otherSamples);
            }

        };
    }

    default ArgumentFilter<T, T> not() {
        return new ArgumentFilter<T, T>() {

            public boolean test(T arg) {
                return !ArgumentFilter.this.test(arg);
            }

            public Set<Class<?>> allowedTypes() {
                return Collections.emptySet();
            }

            @Override
            public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
                Samples<T> thisSamples = ArgumentFilter.this.collectSamples(downStreamSamples);
                return thisSamples.swap();
            }

        };
    }

    @SuppressWarnings("unchecked")
    default <S extends T> ArgumentFilter<T, T> or(ArgumentFilter<T, S> other) {
        return (ArgumentFilter<T, T>) union(other);
    }

    default <S extends T> ArgumentFilter<T, S> and(ArgumentFilter<T, S> other) {
        return new ArgumentFilter<T, S>() {

            public boolean test(T arg) {
                return ArgumentFilter.this.test(arg) && other.test(arg);
            }

            public Set<Class<?>> allowedTypes() {
                final Set<Class<?>> typesIntersection;
                Set<Class<?>> ti = new HashSet<>(ArgumentFilter.this.allowedTypes());
                ti.removeAll(other.allowedTypes());
                typesIntersection = Collections.unmodifiableSet(ti);
                return typesIntersection;
            }

            @Override
            public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
                Samples<T> thisSamples = ArgumentFilter.this.collectSamples(downStreamSamples);
                Samples<T> otherSamples = other.collectSamples(downStreamSamples);

                return thisSamples.and(otherSamples);
            }

        };
    }
}
