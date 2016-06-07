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

import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;

public interface ArgumentFilterSampler<T, R> extends ArgumentFilter<T, R> {

    Samples<R> collectSamples(Samples<? extends R> downStreamSamples);

    TypeExpr allowedTypes();

    interface NarrowingArgumentFilterSampler<T, R extends T> extends NarrowingArgumentFilter<T, R>, ArgumentFilterSampler<T, R> {

        @Override
        default <S extends T> ArgumentTypeFilterSampler<T, T> or(ArgumentFilter<T, S> o) {
            final ArgumentFilterSampler<T, S> other = (ArgumentFilterSampler<T, S>) o;

            return new ArgumentTypeFilterSampler<T, T>() {

                @Override
                public boolean test(T arg) {
                    if (NarrowingArgumentFilterSampler.this.test(arg)) {
                        return true;
                    } else {
                        return other.test(arg);
                    }
                }

                @Override
                public TypeExpr allowedTypes() {
                    return NarrowingArgumentFilterSampler.this.allowedTypes().or(other.allowedTypes());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {

                    Samples<R> downStreamSamplesForThis = downStreamSamples.filter(x -> NarrowingArgumentFilterSampler.this.allowedTypes().isInstance(x)).map(x -> (R) x, x -> x);
                    Samples<R> thisSamples = NarrowingArgumentFilterSampler.this.collectSamples(downStreamSamplesForThis);

                    Samples<S> downStreamSamplesForOther = downStreamSamples.filter(x -> other.allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamplesForOther);

                    return Samples.<T> empty().or(thisSamples).or(otherSamples);
                }

            };
        }
    }

    interface ArgumentValueFilterSampler<T> extends ArgumentValueFilter<T>, NarrowingArgumentFilterSampler<T, T> {

        @Override
        default ArgumentValueFilterSampler<T> and(ArgumentValueFilter<T> o) {
            final ArgumentValueFilterSampler<T> other = (ArgumentValueFilterSampler<T>) o;

            return new ArgumentValueFilterSampler<T>() {

                @Override
                public boolean test(T arg) {
                    return ArgumentValueFilterSampler.this.test(arg) && other.test(arg);
                }

                @Override
                public TypeExpr allowedTypes() {
                    return ArgumentValueFilterSampler.this.allowedTypes().and(other.allowedTypes());
                }

                @Override
                public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
                    Samples<T> thisSamples = ArgumentValueFilterSampler.this.collectSamples(downStreamSamples);
                    Samples<T> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        @Override
        default <S extends T> ArgumentTypeFilterSampler<T, S> and(ArgumentTypeFilter<T, S> o) {
            final ArgumentTypeFilterSampler<T, S> other = (ArgumentTypeFilterSampler<T, S>) o;

            return new ArgumentTypeFilterSampler<T, S>() {

                @Override
                public boolean test(T arg) {
                    return ArgumentValueFilterSampler.this.test(arg) && other.test(arg);
                }

                @Override
                public TypeExpr allowedTypes() {
                    return ArgumentValueFilterSampler.this.allowedTypes().and(other.allowedTypes());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<S> collectSamples(Samples<? extends S> downStreamSamples) {
                    Samples<S> thisSamples = ArgumentValueFilterSampler.this.collectSamples(downStreamSamples).filter(x -> allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        @Override
        default ArgumentValueFilterSampler<T> not() {
            return new ArgumentValueFilterSampler<T>() {

                @Override
                public boolean test(T arg) {
                    return !ArgumentValueFilterSampler.this.test(arg);
                }

                @Override
                public TypeExpr allowedTypes() {
                    return ArgumentValueFilterSampler.this.allowedTypes();
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
                    Samples<T> thisSamples = ArgumentValueFilterSampler.this.collectSamples(downStreamSamples);
                    return thisSamples.swap().filter(x -> allowedTypes().isInstance(x)).map(x -> (T) x, x -> x);
                }
            };
        }

    }

    interface ArgumentTypeFilterSampler<T, R extends T> extends ArgumentTypeFilter<T, R>, NarrowingArgumentFilterSampler<T, R> {

        @Override
        default <S extends R> ArgumentTypeFilterSampler<T, S> and(ArgumentTypeFilter<R, S> o) {
            final ArgumentTypeFilterSampler<R, S> other = (ArgumentTypeFilterSampler<R, S>) o;

            return new ArgumentTypeFilterSampler<T, S>() {

                @SuppressWarnings("unchecked")
                @Override
                public boolean test(T arg) {
                    return ArgumentTypeFilterSampler.this.test(arg) && other.test((R) arg);
                }

                @Override
                public TypeExpr allowedTypes() {
                    return ArgumentTypeFilterSampler.this.allowedTypes().and(other.allowedTypes());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<S> collectSamples(Samples<? extends S> downStreamSamples) {
                    Samples<S> thisSamples = ArgumentTypeFilterSampler.this.collectSamples(downStreamSamples).filter(x -> other.allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        @Override
        default ArgumentTypeFilter<T, R> and(ArgumentValueFilter<R> o) {
            final ArgumentValueFilterSampler<R> other = (ArgumentValueFilterSampler<R>) o;

            return new ArgumentTypeFilterSampler<T, R>() {

                @SuppressWarnings("unchecked")
                @Override
                public boolean test(T arg) {
                    return ArgumentTypeFilterSampler.this.test(arg) && other.test((R) arg);
                }

                @Override
                public TypeExpr allowedTypes() {
                    return ArgumentTypeFilterSampler.this.allowedTypes().and(other.allowedTypes());
                }

                @SuppressWarnings("cast")
                @Override
                public Samples<R> collectSamples(Samples<? extends R> downStreamSamples) {
                    Samples<R> thisSamples = ArgumentTypeFilterSampler.this.collectSamples(downStreamSamples);
                    Samples<R> otherSamples = other.collectSamples(downStreamSamples).filter(x -> ArgumentTypeFilterSampler.this.allowedTypes().isInstance(x)).map(x -> (R) x, x -> x);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        @Override
        default InverseArgumentFilterSampler<T, R> not() {
            return new InverseArgumentFilterSampler<>(this);
        }

    }

    class InverseArgumentFilterSampler<T, R extends T> extends InverseArgumentFilter<T, R> implements ArgumentFilterSampler<T, Object> {

        private final ArgumentTypeFilterSampler<T, R> orig;

        public InverseArgumentFilterSampler(ArgumentTypeFilter<T, R> o) {
            super(o);
            this.orig = (ArgumentTypeFilterSampler<T, R>) o;
        }

        @Override
        public TypeExpr allowedTypes() {
            return orig.allowedTypes().not();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Samples<Object> collectSamples(Samples<?> downStreamSamples) {
            Samples<R> swappedSamples = downStreamSamples.swap().filter(x -> orig.allowedTypes().isInstance(x)).map(x -> (R) x, x -> x);
            Samples<R> thisSamples = orig.collectSamples(swappedSamples);
            return thisSamples.swap();
        }

        @Override
        public ArgumentTypeFilterSampler<T, R> not() {
            return orig;
        }

        @Override
        public <S extends T> ArgumentTypeFilterSampler<T, S> and(ArgumentTypeFilter<T, S> o) {
            final ArgumentTypeFilterSampler<T, S> other = (ArgumentTypeFilterSampler<T, S>) o;

            return new ArgumentTypeFilterSampler<T, S>() {

                @Override
                public boolean test(T arg) {
                    return InverseArgumentFilterSampler.this.test(arg) && other.test(arg);
                }

                @Override
                public TypeExpr allowedTypes() {
                    return InverseArgumentFilterSampler.this.allowedTypes().and(other.allowedTypes());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<S> collectSamples(Samples<? extends S> downStreamSamples) {
                    Samples<S> thisSamples = InverseArgumentFilterSampler.this.collectSamples(downStreamSamples).filter(x -> other.allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        @Override
        public <S extends T> ArgumentValueFilterSampler<S> and(ArgumentValueFilter<S> o) {
            final ArgumentValueFilterSampler<S> other = (ArgumentValueFilterSampler<S>) o;

            return new ArgumentValueFilterSampler<S>() {

                @Override
                public boolean test(S arg) {
                    return InverseArgumentFilterSampler.this.test(arg) && other.test(arg);
                }

                @Override
                public TypeExpr allowedTypes() {
                    return InverseArgumentFilterSampler.this.allowedTypes().and(other.allowedTypes());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<S> collectSamples(Samples<? extends S> downStreamSamples) {
                    Samples<S> thisSamples = InverseArgumentFilterSampler.this.collectSamples(downStreamSamples).filter(x -> other.allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        @Override
        public <S extends T> InverseArgumentFilterSampler<T, T> and(InverseArgumentFilter<T, S> o) {
            InverseArgumentFilterSampler<T, S> other = (InverseArgumentFilterSampler<T, S>) o;
            return new InverseArgumentFilterSampler<>(other.orig.or(this.orig));
        }

    }

}
