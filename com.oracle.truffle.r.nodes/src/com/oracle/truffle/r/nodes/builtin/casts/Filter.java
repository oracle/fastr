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
package com.oracle.truffle.r.nodes.builtin.casts;

import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Represents filters that can be used in {@link FilterStep} and as condition in {@link MapStep}.
 */
public abstract class Filter<T, R extends T> {

    protected Filter() {
    }

    /**
     * @return true if this filter narrows the input type to a sub-type
     */
    public abstract boolean isNarrowing();

    public abstract <D> D accept(FilterVisitor<D> visitor);

    public <S extends R> AndFilter<T, S> and(Filter<? super R, S> other) {
        return new AndFilter<>(this, other);
    }

    public OrFilter<T> or(Filter<T, ? extends T> other) {
        return new OrFilter<>(this, other);
    }

    public NotFilter<T> not() {
        return new NotFilter<>(this);
    }

    public interface FilterVisitor<D> {
        D visit(TypeFilter<?, ?> filter);

        D visit(RTypeFilter<?> filter);

        D visit(CompareFilter<?> filter);

        D visit(AndFilter<?, ?> filter);

        D visit(OrFilter<?> filter);

        D visit(NotFilter<?> filter);

        D visit(MatrixFilter<?> filter);

        D visit(DoubleFilter filter);
    }

    /**
     * Filters specific Java class.
     */
    public static final class TypeFilter<T, R extends T> extends Filter<T, R> {
        private final Class<?>[] type;
        private final ArgumentFilter<Object, Object> instanceOfLambda;

        public TypeFilter(ArgumentFilter<Object, Object> instanceOfLambda, Class<?>... type) {
            this.type = type;
            this.instanceOfLambda = instanceOfLambda;
        }

        public Class<?>[] getType() {
            return type;
        }

        @Override
        public boolean isNarrowing() {
            return true;
        }

        public ArgumentFilter<Object, Object> getInstanceOfLambda() {
            return instanceOfLambda;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Filters specified set of type in R sense, supports only vector types minus list.
     */
    public static final class RTypeFilter<T extends RAbstractVector> extends Filter<Object, T> {
        private final RType type;

        public RTypeFilter(RType type) {
            assert type.isVector() && type != RType.List : "RTypeFilter supports only vector types minus list.";
            this.type = type;
        }

        public RType getType() {
            return type;
        }

        @Override
        public boolean isNarrowing() {
            return true;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Compares the real value against given value using given operation. Use the constants defined
     * within this class for the operation.
     */
    public static final class CompareFilter<T> extends Filter<T, T> {

        public interface Subject {
            <D> D accept(SubjectVisitor<D> visitor, byte operation);
        }

        public interface SubjectVisitor<D> {
            D visit(ScalarValue scalarValue, byte operation);

            D visit(NATest naTest, byte operation);

            D visit(StringLength stringLength, byte operation);

            D visit(VectorSize vectorSize, byte operation);

            D visit(ElementAt elementAt, byte operation);

            D visit(Dim dim, byte operation);
        }

        public static final class ScalarValue implements Subject {
            public final Object value;
            public final RType type;

            public ScalarValue(Object value, RType type) {
                this.value = value;
                this.type = type;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation) {
                return visitor.visit(this, operation);
            }
        }

        public static final class NATest implements Subject {
            public final RType type;

            public NATest(RType type) {
                this.type = type;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation) {
                return visitor.visit(this, operation);
            }
        }

        public static final class StringLength implements Subject {
            public final int length;

            public StringLength(int length) {
                this.length = length;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation) {
                return visitor.visit(this, operation);
            }
        }

        public static final class VectorSize implements Subject {
            public final int size;

            public VectorSize(int size) {
                this.size = size;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation) {
                return visitor.visit(this, operation);
            }
        }

        public static final class ElementAt implements Subject {
            public final int index;
            public final Object value;
            public final RType type;

            public ElementAt(int index, Object value, RType type) {
                this.index = index;
                this.value = value;
                this.type = type;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation) {
                return visitor.visit(this, operation);
            }
        }

        public static final class Dim implements Subject {
            public final int dimIndex;
            public final int dimSize;

            public Dim(int dimIndex, int dimSize) {
                this.dimIndex = dimIndex;
                this.dimSize = dimSize;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation) {
                return visitor.visit(this, operation);
            }
        }

        public static final byte EQ = 0;
        public static final byte GT = 1;
        public static final byte LT = 2;
        public static final byte GE = 3;
        public static final byte LE = 4;
        public static final byte SAME = 5;

        private final byte operation;
        private final Subject subject;

        public CompareFilter(byte operation, Subject subject) {
            assert operation <= SAME : "wrong operation value";
            this.operation = operation;
            this.subject = subject;
        }

        @Override
        public boolean isNarrowing() {
            return false;
        }

        public Subject getSubject() {
            return subject;
        }

        public byte getOperation() {
            return operation;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    public abstract static class MatrixFilter<T extends RAbstractVector> extends Filter<T, T> {

        private static final MatrixFilter<RAbstractVector> IS_MATRIX = new MatrixFilter<RAbstractVector>() {
            @Override
            public <D> D acceptOperation(OperationVisitor<D> visitor) {
                return visitor.visitIsMatrix();
            }
        };

        private static final MatrixFilter<RAbstractVector> IS_SQUARE_MATRIX = new MatrixFilter<RAbstractVector>() {
            @Override
            public <D> D acceptOperation(OperationVisitor<D> visitor) {
                return visitor.visitIsSquareMatrix();
            }
        };

        public interface OperationVisitor<D> {
            D visitIsMatrix();

            D visitIsSquareMatrix();
        }

        @SuppressWarnings("unchecked")
        public static <T extends RAbstractVector> MatrixFilter<T> isMatrixFilter() {
            return (MatrixFilter<T>) IS_MATRIX;
        }

        @SuppressWarnings("unchecked")
        public static <T extends RAbstractVector> MatrixFilter<T> isSquareMatrixFilter() {
            return (MatrixFilter<T>) IS_SQUARE_MATRIX;
        }

        private MatrixFilter() {
        }

        @Override
        public boolean isNarrowing() {
            return false;
        }

        public abstract <D> D acceptOperation(OperationVisitor<D> visitor);

        @Override
        public <D> D accept(FilterVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    public abstract static class DoubleFilter extends Filter<Double, Double> {

        public static final DoubleFilter IS_FINITE = new DoubleFilter() {
            @Override
            public <D> D acceptOperation(OperationVisitor<D> visitor) {
                return visitor.visitIsFinite();
            }
        };

        public static final DoubleFilter IS_FRACTIONAL = new DoubleFilter() {
            @Override
            public <D> D acceptOperation(OperationVisitor<D> visitor) {
                return visitor.visitIsFractional();
            }
        };

        public interface OperationVisitor<D> {
            D visitIsFinite();

            D visitIsFractional();
        }

        private DoubleFilter() {

        }

        @Override
        public boolean isNarrowing() {
            return false;
        }

        public abstract <D> D acceptOperation(OperationVisitor<D> visitor);

        @Override
        public <D> D accept(FilterVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    public static final class AndFilter<T, R extends T> extends Filter<T, R> {
        private final Filter<?, ?> left;
        private final Filter<?, ?> right;

        public AndFilter(Filter<?, ?> left, Filter<?, ?> right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isNarrowing() {
            return left.isNarrowing() || right.isNarrowing();
        }

        public Filter<?, ?> getLeft() {
            return left;
        }

        public Filter<?, ?> getRight() {
            return right;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    public static final class OrFilter<T> extends Filter<T, T> {
        private final Filter<?, ?> left;
        private final Filter<?, ?> right;

        public OrFilter(Filter<?, ?> left, Filter<?, ?> right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean isNarrowing() {
            return false;
        }

        public Filter<?, ?> getLeft() {
            return left;
        }

        public Filter<?, ?> getRight() {
            return right;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    public static final class NotFilter<T> extends Filter<T, T> {
        private final Filter<?, ?> filter;

        public NotFilter(Filter<?, ?> filter) {
            this.filter = filter;
        }

        @Override
        public boolean isNarrowing() {
            return false;
        }

        public Filter<?, ?> getFilter() {
            return filter;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }
}
