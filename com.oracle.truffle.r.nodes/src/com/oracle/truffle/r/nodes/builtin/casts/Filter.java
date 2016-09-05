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
package com.oracle.truffle.r.nodes.builtin.casts;

import static com.oracle.truffle.r.nodes.builtin.casts.CastStep.FilterStep;
import static com.oracle.truffle.r.nodes.builtin.casts.CastStep.MapStep;

import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.runtime.RType;

/**
 * Represents filters that can be used in {@link FilterStep} and as condition in {@link MapStep}.
 */
public abstract class Filter {

    public abstract <T> T accept(FilterVisitor<T> visitor);

    public interface FilterVisitor<T> {
        T visit(TypeFilter filter);

        T visit(RTypeFilter filter);

        T visit(CompareFilter filter);

        T visit(AndFilter filter);

        T visit(OrFilter filter);

        T visit(NotFilter filter);

        T visit(NumericFilter filter);
    }

    /**
     * Filters specific Java class.
     */
    public static final class TypeFilter extends Filter {
        private final Class<?> type;
        private final ArgumentFilter<Object, Boolean> instanceOfLambda;

        public TypeFilter(Class<?> type, ArgumentFilter<Object, Boolean> instanceOfLambda) {
            this.type = type;
            this.instanceOfLambda = instanceOfLambda;
        }

        public Class<?> getType() {
            return type;
        }

        /**
         * This is lambda in form of 'x instanceof type' in order to avoid reflective
         * Class.instanceOf call.
         */
        public ArgumentFilter<Object, Boolean> getInstanceOfLambda() {
            return instanceOfLambda;
        }

        @Override
        public <T> T accept(FilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Filters specified set of type in R sense, supports only vector types minus list.
     */
    public static final class RTypeFilter extends Filter {
        private final RType type;

        public RTypeFilter(RType type) {
            assert type.isVector() && type != RType.List : "RTypeFilter supports only vector types minus list.";
            this.type = type;
        }

        public RType getType() {
            return type;
        }

        @Override
        public <T> T accept(FilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static final class NumericFilter extends Filter {
        @Override
        public <T> T accept(FilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Compares the real value against given value using given operation. Use the constants defined
     * within this class for the operation.
     */
    public static final class CompareFilter extends Filter {
        public static final byte EQ = 0;
        public static final byte GT = 1;
        public static final byte LT = 2;
        public static final byte GE = 3;
        public static final byte LE = 4;

        private final byte operation;
        private final Object value;

        public CompareFilter(byte operation, Object value) {
            assert operation <= LE : "wrong operation value";
            this.operation = operation;
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public byte getOperation() {
            return operation;
        }

        @Override
        public <T> T accept(FilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static final class AndFilter extends Filter {
        private final Filter left;
        private final Filter right;

        public AndFilter(Filter left, Filter right) {
            this.left = left;
            this.right = right;
        }

        public Filter getLeft() {
            return left;
        }

        public Filter getRight() {
            return right;
        }

        @Override
        public <T> T accept(FilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static final class OrFilter extends Filter {
        private final Filter left;
        private final Filter right;

        public OrFilter(Filter left, Filter right) {
            this.left = left;
            this.right = right;
        }

        public Filter getLeft() {
            return left;
        }

        public Filter getRight() {
            return right;
        }

        @Override
        public <T> T accept(FilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static final class NotFilter extends Filter {
        private final Filter filter;

        public NotFilter(Filter filter) {
            this.filter = filter;
        }

        public Filter getFilter() {
            return filter;
        }

        @Override
        public <T> T accept(FilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
