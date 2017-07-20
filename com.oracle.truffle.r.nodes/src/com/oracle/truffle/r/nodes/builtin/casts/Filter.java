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

import java.util.function.Predicate;

import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Represents filters that can be used in {@link FilterStep} and as condition in {@link MapStep}.
 */
public abstract class Filter<T, R extends T> {

    protected Filter() {
    }

    public ResultForArg resultForNull() {
        return ResultForArg.UNDEFINED;
    }

    public ResultForArg resultForMissing() {
        return ResultForArg.UNDEFINED;
    }

    public abstract <D> D accept(FilterVisitor<D> visitor, D previous);

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
        D visit(TypeFilter<?, ?> filter, D previous);

        D visit(RTypeFilter<?> filter, D previous);

        D visit(CompareFilter<?> filter, D previous);

        D visit(AndFilter<?, ?> filter, D previous);

        D visit(OrFilter<?> filter, D previous);

        D visit(NotFilter<?> filter, D previous);

        D visit(MatrixFilter<?> filter, D previous);

        D visit(DoubleFilter filter, D previous);

        D visit(NullFilter filter, D previous);

        D visit(MissingFilter filter, D previous);
    }

    /**
     * Filters specific Java class.
     */
    public static final class TypeFilter<T, R extends T> extends Filter<T, R> {
        private final Class<?> type1;
        private final Class<?> type2;
        private final Predicate<R> extraCondition;

        @SuppressWarnings("rawtypes")
        public TypeFilter(Class<R> type) {
            assert type != null;
            this.type1 = type;
            this.type2 = null;
            this.extraCondition = null;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public TypeFilter(Class<R> type, Predicate<R> extraCondition) {
            assert type != null;
            this.type1 = type;
            this.type2 = null;
            this.extraCondition = extraCondition;
        }

        @SuppressWarnings("rawtypes")
        public TypeFilter(Class<?> type1, Class<?> type2) {
            assert type1 != null && type2 != null;
            assert type1 != Object.class && type2 != Object.class;
            this.type1 = type1;
            this.type2 = type2;
            this.extraCondition = null;
        }

        public Class<?> getType1() {
            return type1;
        }

        public Class<?> getType2() {
            return type2;
        }

        public Predicate<R> getExtraCondition() {
            return extraCondition;
        }

        @SuppressWarnings("unchecked")
        public ArgumentFilter<Object, Object> getInstanceOfLambda() {
            final ArgumentFilter<Object, Object> instanceOfLambda;
            if (type2 == null) {
                if (extraCondition == null) {
                    if (type1 == Object.class) {
                        instanceOfLambda = x -> true;
                    } else {
                        instanceOfLambda = x -> type1.isInstance(x);
                    }
                } else {
                    if (type1 == Object.class) {
                        instanceOfLambda = x -> extraCondition.test((R) x);
                    } else {
                        instanceOfLambda = x -> type1.isInstance(x) && extraCondition.test((R) x);
                    }
                }
            } else {
                instanceOfLambda = x -> type1.isInstance(x) || type2.isInstance(x);
            }
            return instanceOfLambda;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        @Override
        public ResultForArg resultForNull() {
            return ResultForArg.FALSE;
        }

        @Override
        public ResultForArg resultForMissing() {
            return ResultForArg.FALSE;
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
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        @Override
        public ResultForArg resultForNull() {
            return ResultForArg.FALSE;
        }

        @Override
        public ResultForArg resultForMissing() {
            return ResultForArg.FALSE;
        }
    }

    public static final class NullFilter extends Filter<Object, RNull> {

        public static final NullFilter INSTANCE = new NullFilter();

        private NullFilter() {
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        @Override
        public ResultForArg resultForNull() {
            return ResultForArg.TRUE;
        }

        @Override
        public ResultForArg resultForMissing() {
            return ResultForArg.FALSE;
        }
    }

    public static final class MissingFilter extends Filter<Object, RMissing> {

        public static final MissingFilter INSTANCE = new MissingFilter();

        private MissingFilter() {
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        @Override
        public ResultForArg resultForNull() {
            return ResultForArg.FALSE;
        }

        @Override
        public ResultForArg resultForMissing() {
            return ResultForArg.TRUE;
        }
    }

    /**
     * Compares the real value against given value using given operation. Use the constants defined
     * within this class for the operation.
     */
    public static final class CompareFilter<T> extends Filter<T, T> {

        public interface Subject {
            <D> D accept(SubjectVisitor<D> visitor, byte operation, D previous);
        }

        public interface SubjectVisitor<D> {
            D visit(ScalarValue scalarValue, byte operation, D previous);

            D visit(NATest naTest, byte operation, D previous);

            D visit(StringLength stringLength, byte operation, D previous);

            D visit(VectorSize vectorSize, byte operation, D previous);

            D visit(ElementAt elementAt, byte operation, D previous);

            D visit(Dim dim, byte operation, D previous);
        }

        public static final class ScalarValue implements Subject {
            public final Object value;
            public final RType type;

            public ScalarValue(Object value, RType type) {
                this.value = value;
                this.type = type;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation, D previous) {
                return visitor.visit(this, operation, previous);
            }
        }

        public static final class NATest implements Subject {
            public final RType type;

            public NATest(RType type) {
                this.type = type;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation, D previous) {
                return visitor.visit(this, operation, previous);
            }
        }

        public static final class StringLength implements Subject {
            public final int length;

            public StringLength(int length) {
                this.length = length;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation, D previous) {
                return visitor.visit(this, operation, previous);
            }
        }

        public static final class VectorSize implements Subject {
            public final int size;

            public VectorSize(int size) {
                this.size = size;
            }

            @Override
            public <D> D accept(SubjectVisitor<D> visitor, byte operation, D previous) {
                return visitor.visit(this, operation, previous);
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
            public <D> D accept(SubjectVisitor<D> visitor, byte operation, D previous) {
                return visitor.visit(this, operation, previous);
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
            public <D> D accept(SubjectVisitor<D> visitor, byte operation, D previous) {
                return visitor.visit(this, operation, previous);
            }
        }

        public static final byte EQ = 0;
        public static final byte GT = 1;
        public static final byte LT = 2;
        public static final byte GE = 3;
        public static final byte LE = 4;
        public static final byte STRING_EQ = 5;

        private final byte operation;
        private final Subject subject;

        public CompareFilter(byte operation, Subject subject) {
            assert operation <= STRING_EQ : "wrong operation value";
            this.operation = operation;
            this.subject = subject;
        }

        public Subject getSubject() {
            return subject;
        }

        public byte getOperation() {
            return operation;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        @Override
        public ResultForArg resultForNull() {
            if (subject instanceof VectorSize && ((VectorSize) subject).size == 0) {
                return ResultForArg.TRUE;
            } else {
                return ResultForArg.FALSE;
            }
        }
    }

    public abstract static class MatrixFilter<T extends RAbstractVector> extends Filter<T, T> {

        private static final MatrixFilter<RAbstractVector> IS_MATRIX = new MatrixFilter<RAbstractVector>() {
            @Override
            public <D> D acceptOperation(OperationVisitor<D> visitor, D previous) {
                return visitor.visitIsMatrix(previous);
            }
        };

        private static final MatrixFilter<RAbstractVector> IS_SQUARE_MATRIX = new MatrixFilter<RAbstractVector>() {
            @Override
            public <D> D acceptOperation(OperationVisitor<D> visitor, D previous) {
                return visitor.visitIsSquareMatrix(previous);
            }
        };

        public interface OperationVisitor<D> {
            D visitIsMatrix(D previous);

            D visitIsSquareMatrix(D previous);
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

        public abstract <D> D acceptOperation(OperationVisitor<D> visitor, D previous);

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    public abstract static class DoubleFilter extends Filter<Double, Double> {

        public static final DoubleFilter IS_FINITE = new DoubleFilter() {
            @Override
            public <D> D acceptOperation(OperationVisitor<D> visitor, D previous) {
                return visitor.visitIsFinite(previous);
            }
        };

        public static final DoubleFilter IS_FRACTIONAL = new DoubleFilter() {
            @Override
            public <D> D acceptOperation(OperationVisitor<D> visitor, D previous) {
                return visitor.visitIsFractional(previous);
            }
        };

        public interface OperationVisitor<D> {
            D visitIsFinite(D previous);

            D visitIsFractional(D previous);
        }

        private DoubleFilter() {

        }

        public abstract <D> D acceptOperation(OperationVisitor<D> visitor, D previous);

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    public static final class AndFilter<T, R extends T> extends Filter<T, R> {
        private final Filter<?, ?> left;
        private final Filter<?, ?> right;

        public AndFilter(Filter<?, ?> left, Filter<?, ?> right) {
            this.left = left;
            this.right = right;
        }

        public Filter<?, ?> getLeft() {
            return left;
        }

        public Filter<?, ?> getRight() {
            return right;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        @Override
        public ResultForArg resultForNull() {
            return left.resultForNull().and(right.resultForNull());
        }

        @Override
        public ResultForArg resultForMissing() {
            return left.resultForMissing().and(right.resultForMissing());
        }
    }

    public static final class OrFilter<T> extends Filter<T, T> {
        private final Filter<?, ?> left;
        private final Filter<?, ?> right;

        public OrFilter(Filter<?, ?> left, Filter<?, ?> right) {
            this.left = left;
            this.right = right;
        }

        public Filter<?, ?> getLeft() {
            return left;
        }

        public Filter<?, ?> getRight() {
            return right;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        @Override
        public ResultForArg resultForNull() {
            return left.resultForNull().or(right.resultForNull());
        }

        @Override
        public ResultForArg resultForMissing() {
            return left.resultForMissing().or(right.resultForMissing());
        }
    }

    public static final class NotFilter<T> extends Filter<T, T> {
        private final Filter<?, ?> filter;

        public NotFilter(Filter<?, ?> filter) {
            this.filter = filter;
        }

        public Filter<?, ?> getFilter() {
            return filter;
        }

        @Override
        public <D> D accept(FilterVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        @Override
        public ResultForArg resultForNull() {
            return filter.resultForNull().not();
        }

        @Override
        public ResultForArg resultForMissing() {
            return filter.resultForMissing().not();
        }
    }

    /**
     * This is an enumeration of possible fixed outcomes of a filter's test method for a given input
     * value. It is used now only in connection with {@link RNull} and {@link RMissing} as input
     * values.
     * <P>
     * The <code>FALSE</code>, resp. <code>TRUE</code>, indicates that the filter will always return
     * <code>false</code>, resp. <code>true</code>, for the given input value.
     * <p>
     * The <code>UNDEFINED</code> indicates that the the given input value is out of the filter's
     * domain.
     *
     * @see Filter#resultForNull()
     * @see Filter#resultForMissing()
     */
    public enum ResultForArg {
        TRUE {

            @Override
            public ResultForArg not() {
                return FALSE;
            }

            @Override
            public ResultForArg and(ResultForArg other) {
                return other;
            }

            @Override
            public ResultForArg or(ResultForArg other) {
                return TRUE;
            }
        },
        FALSE {

            @Override
            public ResultForArg not() {
                return TRUE;
            }

            @Override
            public ResultForArg and(ResultForArg other) {
                return FALSE;
            }

            @Override
            public ResultForArg or(ResultForArg other) {
                return other;
            }
        },
        UNDEFINED {

            @Override
            public ResultForArg not() {
                return UNDEFINED;
            }

            @Override
            public ResultForArg and(ResultForArg other) {
                return UNDEFINED;
            }

            @Override
            public ResultForArg or(ResultForArg other) {
                return other;
            }
        };

        public abstract ResultForArg not();

        public abstract ResultForArg and(ResultForArg other);

        public abstract ResultForArg or(ResultForArg other);
    }
}
