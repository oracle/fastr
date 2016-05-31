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

import com.oracle.truffle.api.profiles.ConditionProfile;

public interface ArgumentFilter<T, R> {

    boolean test(T arg);

    interface NarrowingArgumentFilter<T, R extends T> extends ArgumentFilter<T, R> {

        default <S extends T> ArgumentTypeFilter<T, T> or(ArgumentFilter<T, S> other) {
            return new ArgumentTypeFilter<T, T>() {

                private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

                public boolean test(T arg) {
                    if (profile.profile(NarrowingArgumentFilter.this.test(arg))) {
                        return true;
                    } else {
                        return other.test(arg);
                    }
                }

            };
        }

    }

    interface ArgumentValueFilter<T> extends NarrowingArgumentFilter<T, T> {

        default ArgumentValueFilter<T> and(ArgumentValueFilter<T> other) {
            return new ArgumentValueFilter<T>() {

                private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

                public boolean test(T arg) {
                    if (profile.profile(!ArgumentValueFilter.this.test(arg))) {
                        return false;
                    } else {
                        return other.test(arg);
                    }
                }

            };
        }

        default <S extends T> ArgumentTypeFilter<T, S> and(ArgumentTypeFilter<T, S> other) {
            return new ArgumentTypeFilter<T, S>() {

                private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

                public boolean test(T arg) {
                    if (profile.profile(!ArgumentValueFilter.this.test(arg))) {
                        return false;
                    } else {
                        return other.test(arg);
                    }
                }

            };
        }

        default ArgumentValueFilter<T> not() {
            return new ArgumentValueFilter<T>() {

                public boolean test(T arg) {
                    return !ArgumentValueFilter.this.test(arg);
                }

            };
        }

    }

    interface ArgumentTypeFilter<T, R extends T> extends NarrowingArgumentFilter<T, R> {

        default <S extends R> ArgumentTypeFilter<T, S> and(ArgumentTypeFilter<R, S> other) {
            return new ArgumentTypeFilter<T, S>() {

                private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

                @SuppressWarnings({"unchecked"})
                public boolean test(T arg) {
                    if (profile.profile(!ArgumentTypeFilter.this.test(arg))) {
                        return false;
                    } else {
                        return other.test((R) arg);
                    }
                }

            };
        }

        default ArgumentTypeFilter<T, R> and(ArgumentValueFilter<R> other) {
            return new ArgumentTypeFilter<T, R>() {

                private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

                @SuppressWarnings({"unchecked"})
                public boolean test(T arg) {
                    if (profile.profile(!ArgumentTypeFilter.this.test(arg))) {
                        return false;
                    } else {
                        return other.test((R) arg);
                    }
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

        public ArgumentTypeFilter<T, R> not() {
            return orig;
        }

        public <S extends T> ArgumentTypeFilter<T, S> and(ArgumentTypeFilter<T, S> other) {
            return new ArgumentTypeFilter<T, S>() {

                private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

                @SuppressWarnings({"cast", "unchecked"})
                public boolean test(T arg) {
                    if (profile.profile(!InverseArgumentFilter.this.test(arg))) {
                        return false;
                    } else {
                        return other.test((R) arg);
                    }
                }

            };
        }

        public <S extends T> ArgumentValueFilter<S> and(ArgumentValueFilter<S> other) {
            return new ArgumentValueFilter<S>() {

                private final ConditionProfile profile = ConditionProfile.createBinaryProfile();

                public boolean test(S arg) {
                    if (profile.profile(!InverseArgumentFilter.this.test(arg))) {
                        return false;
                    } else {
                        return other.test(arg);
                    }
                }

            };
        }

        public <S extends T> InverseArgumentFilter<T, T> and(InverseArgumentFilter<T, S> other) {
            return new InverseArgumentFilter<>(other.orig.or(this.orig));
        }

    }

}
