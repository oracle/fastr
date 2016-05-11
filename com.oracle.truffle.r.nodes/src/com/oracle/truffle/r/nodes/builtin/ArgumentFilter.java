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

import com.oracle.truffle.r.nodes.unary.CastNode.Samples;
import com.oracle.truffle.r.nodes.unary.CastNode.TypeExpr;

public interface ArgumentFilter<T, R> {

    boolean test(T arg);

    Samples<R> collectSamples(Samples<? extends R> downStreamSamples);

    TypeExpr allowedTypes();

    interface NarrowingArgumentFilter<T, R extends T> extends ArgumentFilter<T, R> {

        default <S extends T> ArgumentTypeFilter<T, T> or(ArgumentFilter<T, S> other) {
            return new ArgumentTypeFilter<T, T>() {

                public boolean test(T arg) {
                    if (NarrowingArgumentFilter.this.test(arg)) {
                        return true;
                    } else {
                        return other.test(arg);
                    }
                }

                public TypeExpr allowedTypes() {
                    return NarrowingArgumentFilter.this.allowedTypes().or(other.allowedTypes());
                }

                @Override
                public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
                    Samples<R> downStreamSamplesForThis = downStreamSamples.filter(x -> NarrowingArgumentFilter.this.allowedTypes().isInstance(x)).map(x -> (R) x, x -> x);
                    Samples<R> thisSamples = NarrowingArgumentFilter.this.collectSamples(downStreamSamplesForThis);

                    Samples<S> downStreamSamplesForOther = downStreamSamples.filter(x -> other.allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamplesForOther);

                    return Samples.<T> empty().or(thisSamples).or(otherSamples);
                }

            };
        }

    }

    interface ArgumentValueFilter<T> extends NarrowingArgumentFilter<T, T> {

        default ArgumentValueFilter<T> and(ArgumentValueFilter<T> other) {
            return new ArgumentValueFilter<T>() {

                public boolean test(T arg) {
                    return ArgumentValueFilter.this.test(arg) && other.test(arg);
                }

                public TypeExpr allowedTypes() {
                    return ArgumentValueFilter.this.allowedTypes().and(other.allowedTypes());
                }

                @Override
                public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
                    Samples<T> thisSamples = ArgumentValueFilter.this.collectSamples(downStreamSamples);
                    Samples<T> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        default <S extends T> ArgumentTypeFilter<T, S> and(ArgumentTypeFilter<T, S> other) {
            return new ArgumentTypeFilter<T, S>() {

                public boolean test(T arg) {
                    return ArgumentValueFilter.this.test(arg) && other.test(arg);
                }

                public TypeExpr allowedTypes() {
                    return ArgumentValueFilter.this.allowedTypes().and(other.allowedTypes());
                }

                @Override
                public Samples<S> collectSamples(Samples<? extends S> downStreamSamples) {
                    Samples<S> thisSamples = ArgumentValueFilter.this.collectSamples(downStreamSamples).filter(x -> allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        default ArgumentValueFilter<T> not() {
            return new ArgumentValueFilter<T>() {

                public boolean test(T arg) {
                    return !ArgumentValueFilter.this.test(arg);
                }

                public TypeExpr allowedTypes() {
                    return ArgumentValueFilter.this.allowedTypes();
                }

                @Override
                public Samples<T> collectSamples(Samples<? extends T> downStreamSamples) {
                    Samples<T> thisSamples = ArgumentValueFilter.this.collectSamples(downStreamSamples);
                    return thisSamples.swap().filter(x -> allowedTypes().isInstance(x)).map(x -> (T) x, x -> x);
                }
            };
        }

    }

    interface ArgumentTypeFilter<T, R extends T> extends NarrowingArgumentFilter<T, R> {

        default <S extends R> ArgumentTypeFilter<T, S> and(ArgumentTypeFilter<R, S> other) {
            return new ArgumentTypeFilter<T, S>() {

                public boolean test(T arg) {
                    return ArgumentTypeFilter.this.test(arg) && other.test((R) arg);
                }

                public TypeExpr allowedTypes() {
                    return ArgumentTypeFilter.this.allowedTypes().and(other.allowedTypes());
                }

                @Override
                public Samples<S> collectSamples(Samples<? extends S> downStreamSamples) {
                    Samples<S> thisSamples = ArgumentTypeFilter.this.collectSamples(downStreamSamples).filter(x -> other.allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        default ArgumentTypeFilter<T, R> and(ArgumentValueFilter<T> other) {
            return new ArgumentTypeFilter<T, R>() {

                public boolean test(T arg) {
                    return ArgumentTypeFilter.this.test(arg) && other.test(arg);
                }

                public TypeExpr allowedTypes() {
                    return ArgumentTypeFilter.this.allowedTypes().and(other.allowedTypes());
                }

                @Override
                public Samples<R> collectSamples(Samples<? extends R> downStreamSamples) {
                    Samples<R> thisSamples = ArgumentTypeFilter.this.collectSamples(downStreamSamples);
                    Samples<R> otherSamples = other.collectSamples(downStreamSamples).filter(x -> ArgumentTypeFilter.this.allowedTypes().isInstance(x)).map(x -> (R) x, x -> x);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        default InverseArgumentFilter<T, R> not() {
            return new InverseArgumentFilter<>(this);
        }

    }

    class InverseArgumentFilter<T, R extends T> implements ArgumentFilter<T, Object> {

        private final ArgumentTypeFilter<T, R> orig;

        public InverseArgumentFilter(ArgumentTypeFilter<T, R> orig) {
            this.orig = orig;
        }

        public boolean test(T arg) {
            return !orig.test(arg);
        }

        public TypeExpr allowedTypes() {
            return orig.allowedTypes().not();
        }

        @Override
        public Samples<Object> collectSamples(Samples<?> downStreamSamples) {
            Samples<R> swappedSamples = downStreamSamples.swap().filter(x -> orig.allowedTypes().isInstance(x)).map(x -> (R) x, x -> x);
            Samples<R> thisSamples = orig.collectSamples(swappedSamples);
            return thisSamples.swap();
        }

        public ArgumentTypeFilter<T, R> not() {
            return orig;
        }

        public <S extends T> ArgumentTypeFilter<T, S> and(ArgumentTypeFilter<T, S> other) {
            return new ArgumentTypeFilter<T, S>() {

                public boolean test(T arg) {
                    return InverseArgumentFilter.this.test(arg) && other.test(arg);
                }

                public TypeExpr allowedTypes() {
                    return InverseArgumentFilter.this.allowedTypes().and(other.allowedTypes());
                }

                @Override
                public Samples<S> collectSamples(Samples<? extends S> downStreamSamples) {
                    Samples<S> thisSamples = InverseArgumentFilter.this.collectSamples(downStreamSamples).filter(x -> other.allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        public <S extends T> ArgumentValueFilter<S> and(ArgumentValueFilter<S> other) {
            return new ArgumentValueFilter<S>() {

                public boolean test(S arg) {
                    return InverseArgumentFilter.this.test(arg) && other.test(arg);
                }

                public TypeExpr allowedTypes() {
                    return InverseArgumentFilter.this.allowedTypes().and(other.allowedTypes());
                }

                @Override
                public Samples<S> collectSamples(Samples<? extends S> downStreamSamples) {
                    Samples<S> thisSamples = InverseArgumentFilter.this.collectSamples(downStreamSamples).filter(x -> other.allowedTypes().isInstance(x)).map(x -> (S) x, x -> x);
                    Samples<S> otherSamples = other.collectSamples(downStreamSamples);

                    return thisSamples.and(otherSamples);
                }
            };
        }

        public <S extends T> InverseArgumentFilter<T, T> and(InverseArgumentFilter<T, S> other) {
            return new InverseArgumentFilter<>(other.orig.or(this.orig));
        }

    }

}
