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

    Samples<R> collectSamples(TypeExpr inputType);

    TypeExpr trueBranchType();

    TypeExpr falseBranchType();

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
                public TypeExpr trueBranchType() {
                    return NarrowingArgumentFilterSampler.this.trueBranchType().or(other.trueBranchType());
                }

                @Override
                public Samples<T> collectSamples(TypeExpr inputType) {
                    Samples<R> thisSamples = NarrowingArgumentFilterSampler.this.collectSamples(inputType);
                    Samples<S> otherSamples = other.collectSamples(inputType);
                    return Samples.<T> anything().and(thisSamples).or(otherSamples);
                }

            };
        }
    }

    interface ArgumentValueFilterSampler<T> extends ArgumentValueFilter<T>, NarrowingArgumentFilterSampler<T, T> {

        @Override
        default TypeExpr falseBranchType() {
            this.or(null);
            return trueBranchType();
        }

        @Override
        default <S extends T> ArgumentValueFilterSampler<T> or(ArgumentValueFilter<T> o) {
            final ArgumentValueFilterSampler<T> other = (ArgumentValueFilterSampler<T>) o;

            return new ArgumentValueFilterSampler<T>() {

                @Override
                public boolean test(T arg) {
                    if (ArgumentValueFilterSampler.this.test(arg)) {
                        return true;
                    } else {
                        return other.test(arg);
                    }
                }

                @Override
                public TypeExpr trueBranchType() {
                    return ArgumentValueFilterSampler.this.trueBranchType().or(other.trueBranchType());
                }

                @Override
                public Samples<T> collectSamples(TypeExpr inputType) {
                    Samples<T> thisSamples = ArgumentValueFilterSampler.this.collectSamples(inputType);
                    Samples<T> otherSamples = other.collectSamples(inputType);
                    return Samples.<T> anything().and(thisSamples).or(otherSamples);
                }

            };
        }

        @Override
        default ArgumentValueFilterSampler<T> and(ArgumentValueFilter<T> o) {
            final ArgumentValueFilterSampler<T> other = (ArgumentValueFilterSampler<T>) o;

            return new ArgumentValueFilterSampler<T>() {

                @Override
                public boolean test(T arg) {
                    return ArgumentValueFilterSampler.this.test(arg) && other.test(arg);
                }

                @Override
                public TypeExpr trueBranchType() {
                    return ArgumentValueFilterSampler.this.trueBranchType().and(other.trueBranchType());
                }

                @Override
                public Samples<T> collectSamples(TypeExpr inputType) {
                    Samples<T> thisSamples = ArgumentValueFilterSampler.this.collectSamples(inputType);
                    Samples<T> otherSamples = other.collectSamples(inputType);

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
                public TypeExpr trueBranchType() {
                    return ArgumentValueFilterSampler.this.trueBranchType().and(other.trueBranchType());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<S> collectSamples(TypeExpr inputType) {
                    Samples<T> thisSamples = ArgumentValueFilterSampler.this.collectSamples(inputType);
                    Samples<S> otherSamples = other.collectSamples(inputType);

                    return (Samples<S>) thisSamples.and(otherSamples);
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
                public TypeExpr trueBranchType() {
                    return ArgumentValueFilterSampler.this.trueBranchType();
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<T> collectSamples(TypeExpr inputType) {
                    Samples<T> thisSamples = ArgumentValueFilterSampler.this.collectSamples(inputType);
                    return (Samples<T>) thisSamples.swap();
                }
            };
        }
    }

    interface ArgumentTypeFilterSampler<T, R extends T> extends ArgumentTypeFilter<T, R>, NarrowingArgumentFilterSampler<T, R> {

        @Override
        default TypeExpr falseBranchType() {
            return trueBranchType().not();
        }

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
                public TypeExpr trueBranchType() {
                    return ArgumentTypeFilterSampler.this.trueBranchType().and(other.trueBranchType());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<S> collectSamples(TypeExpr inputType) {
                    Samples<R> thisSamples = ArgumentTypeFilterSampler.this.collectSamples(inputType);
                    Samples<S> otherSamples = other.collectSamples(inputType);

                    return (Samples<S>) thisSamples.and(otherSamples);
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
                public TypeExpr trueBranchType() {
                    return ArgumentTypeFilterSampler.this.trueBranchType().and(other.trueBranchType());
                }

                @SuppressWarnings("cast")
                @Override
                public Samples<R> collectSamples(TypeExpr inputType) {
                    Samples<R> thisSamples = ArgumentTypeFilterSampler.this.collectSamples(inputType);
                    Samples<R> otherSamples = other.collectSamples(inputType);

                    return otherSamples.and(thisSamples);
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
        public TypeExpr trueBranchType() {
            return orig.trueBranchType().not();
        }

        @Override
        public TypeExpr falseBranchType() {
            return orig.falseBranchType().not();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Samples<Object> collectSamples(TypeExpr inputType) {
            Samples<? extends R> thisSamples = orig.collectSamples(inputType);
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
                public TypeExpr trueBranchType() {
                    return InverseArgumentFilterSampler.this.trueBranchType().and(other.trueBranchType());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<S> collectSamples(TypeExpr inputType) {
                    Samples<Object> thisSamples = InverseArgumentFilterSampler.this.collectSamples(inputType);
                    Samples<S> otherSamples = other.collectSamples(inputType);

                    return (Samples<S>) thisSamples.and(otherSamples);
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
                public TypeExpr trueBranchType() {
                    return InverseArgumentFilterSampler.this.trueBranchType().and(other.trueBranchType());
                }

                @SuppressWarnings("unchecked")
                @Override
                public Samples<S> collectSamples(TypeExpr inputType) {
                    Samples<Object> thisSamples = InverseArgumentFilterSampler.this.collectSamples(inputType);
                    Samples<S> otherSamples = other.collectSamples(inputType);

                    return (Samples<S>) thisSamples.and(otherSamples);
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
