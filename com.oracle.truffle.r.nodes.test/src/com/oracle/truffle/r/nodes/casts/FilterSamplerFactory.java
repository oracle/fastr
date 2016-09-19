/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.casts.CastUtils.samples;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;

import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.Dim;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ElementAt;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.NATest;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.ScalarValue;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.StringLength;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter.VectorSize;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.DoubleFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.FilterVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MatrixFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentFilterFactory;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class FilterSamplerFactory
                implements ArgumentFilterFactory, FilterVisitor<ArgumentFilterSampler<?, ?>>, MatrixFilter.OperationVisitor<ArgumentFilterSampler<RAbstractVector, RAbstractVector>>,
                DoubleFilter.OperationVisitor<ArgumentFilterSampler<Double, Double>>, CompareFilter.SubjectVisitor<ArgumentFilterSampler<?, ?>> {

    public static final FilterSamplerFactory INSTANCE = new FilterSamplerFactory();

    private FilterSamplerFactory() {
// singleton
    }

    @Override
    public ArgumentFilter<?, ?> createFilter(Filter<?, ?> filter) {
        return filter.accept(this);
    }

    private static <T, R> Predicate<T> toPredicate(ArgumentFilter<T, R> filter) {
        return x -> filter.test(x);
    }

    @Override
    public ArgumentFilterSampler<?, ?> visit(TypeFilter<?, ?> filter) {
        return TypePredicateArgumentFilterSampler.fromLambda(toPredicate(filter.getInstanceOfLambda()),
                        CastUtils.sampleValuesForClases(filter.getType()), CastUtils.samples(null), filter.getType());
    }

    @Override
    public ArgumentFilterSampler<?, ?> visit(RTypeFilter<?> filter) {
        if (filter.getType() == RType.Integer) {
            return visit(new TypeFilter<>(x -> x instanceof Integer || x instanceof RAbstractIntVector, Integer.class, RAbstractIntVector.class));
        } else if (filter.getType() == RType.Double) {
            return visit(new TypeFilter<>(x -> x instanceof Double || x instanceof RAbstractDoubleVector, Double.class, RAbstractDoubleVector.class));
        } else if (filter.getType() == RType.Logical) {
            return visit(new TypeFilter<>(x -> x instanceof Byte || x instanceof RAbstractLogicalVector, Byte.class, RAbstractLogicalVector.class));
        } else if (filter.getType() == RType.Complex) {
            return visit(new TypeFilter<>(x -> x instanceof RAbstractComplexVector, RAbstractComplexVector.class));
        } else if (filter.getType() == RType.Character) {
            return visit(new TypeFilter<>(x -> x instanceof String || x instanceof RAbstractStringVector, String.class, RAbstractStringVector.class));
        } else if (filter.getType() == RType.Raw) {
            return visit(new TypeFilter<>(x -> x instanceof RAbstractRawVector, RAbstractRawVector.class));
        } else {
            throw RInternalError.unimplemented("TODO: more types here");
        }
    }

    @Override
    public ArgumentFilterSampler<?, ?> visit(CompareFilter<?> filter) {
        return filter.getSubject().accept(this, filter.getOperation());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ArgumentFilterSampler<?, ?> visit(AndFilter<?, ?> filter) {
        ArgumentFilterSampler leftFilter = filter.getLeft().accept(this);
        ArgumentFilterSampler rightFilter = filter.getRight().accept(this);
        return new ArgumentFilterSampler<Object, Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public boolean test(Object arg) {
                if (!leftFilter.test(arg)) {
                    return false;
                } else {
                    return rightFilter.test(arg);
                }
            }

            @Override
            public TypeExpr falseBranchType() {
                return filter.isNarrowing() ? trueBranchType().not() : trueBranchType();
            }

            @Override
            public TypeExpr trueBranchType() {
                return leftFilter.trueBranchType().and(rightFilter.trueBranchType());
            }

            @SuppressWarnings({"unchecked", "cast"})
            @Override
            public Samples<Object> collectSamples(TypeExpr inputType) {
                Samples thisSamples = rightFilter.collectSamples(inputType);
                Samples otherSamples = leftFilter.collectSamples(inputType);

                return thisSamples.and(otherSamples);
            }

        };
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ArgumentFilterSampler<?, ?> visit(OrFilter<?> filter) {
        ArgumentFilterSampler leftFilter = filter.getLeft().accept(this);
        ArgumentFilterSampler rightFilter = filter.getRight().accept(this);
        return new ArgumentFilterSampler<Object, Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public boolean test(Object arg) {
                if (leftFilter.test(arg)) {
                    return true;
                } else {
                    return rightFilter.test(arg);
                }
            }

            @Override
            public TypeExpr falseBranchType() {
                return trueBranchType();
            }

            @Override
            public TypeExpr trueBranchType() {
                return leftFilter.trueBranchType().or(rightFilter.trueBranchType());
            }

            @SuppressWarnings({"unchecked", "cast"})
            @Override
            public Samples<Object> collectSamples(TypeExpr inputType) {
                Samples thisSamples = leftFilter.collectSamples(inputType);
                Samples otherSamples = rightFilter.collectSamples(inputType);
                return Samples.<Object> anything().and(thisSamples).or(otherSamples);
            }

        };
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ArgumentFilterSampler<?, ?> visit(NotFilter<?> filter) {
        ArgumentFilterSampler toNegate = filter.getFilter().accept(this);
        return new ArgumentFilterSampler<Object, Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public boolean test(Object arg) {
                return !toNegate.test(arg);
            }

            @Override
            public TypeExpr falseBranchType() {
                return filter.getFilter().isNarrowing() ? trueBranchType() : trueBranchType();
            }

            @Override
            public TypeExpr trueBranchType() {
                return filter.getFilter().isNarrowing() ? trueBranchType().not() : trueBranchType();
            }

            @SuppressWarnings({"unchecked", "cast"})
            @Override
            public Samples<Object> collectSamples(TypeExpr inputType) {
                Samples thisSamples = toNegate.collectSamples(inputType);
                return (Samples<Object>) thisSamples.swap();
            }

        };
    }

    @Override
    public ArgumentFilterSampler<?, ?> visit(MatrixFilter<?> filter) {
        return filter.acceptOperation(this);
    }

    @Override
    public ArgumentFilterSampler<?, ?> visit(DoubleFilter filter) {
        return filter.acceptOperation(this);
    }

    @Override
    public ArgumentFilterSampler<RAbstractVector, RAbstractVector> visitIsMatrix() {
        return new VectorPredicateArgumentFilterSampler<>("matrix", x -> x.isMatrix());
    }

    @Override
    public ArgumentFilterSampler<RAbstractVector, RAbstractVector> visitIsSquareMatrix() {
        return new VectorPredicateArgumentFilterSampler<>("squareMatrix", x -> x.isMatrix() && x.getDimensions()[0] == x.getDimensions()[1], 3);
    }

    @Override
    public ArgumentFilterSampler<Double, Double> visitIsFinite() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double x) -> !Double.isInfinite(x), samples(0d), samples(RRuntime.DOUBLE_NA), Double.class);
    }

    @Override
    public ArgumentFilterSampler<Double, Double> visitIsFractional() {
        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double x) -> !RRuntime.isNAorNaN(x) && !Double.isInfinite(x) && x != Math.floor(x), samples(0d), samples(RRuntime.DOUBLE_NA),
                        Double.class);
    }

    @Override
    public ArgumentFilterSampler<?, ?> visit(ScalarValue scalarValue, byte operation) {
        switch (operation) {
            case CompareFilter.EQ:
                switch (scalarValue.type) {
                    case Character:
                        final String s = (String) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg.equals(s), samples(s), CastUtils.samples(null), String.class);
                    case Integer:
                        final int i = (int) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg == i, samples(i), CastUtils.<Integer> samples(i + 1), Integer.class);
                    case Double:
                        final double d = (double) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg == d, samples(d), CastUtils.<Double> samples(d + 1), Double.class);
                    case Logical:
                        final byte l = (byte) scalarValue.value;
                        Set<Byte> negativeSamples = null;
                        switch (l) {
                            case RRuntime.LOGICAL_TRUE:
                                negativeSamples = CastUtils.samples(RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_NA);
                                break;
                            case RRuntime.LOGICAL_FALSE:
                                negativeSamples = CastUtils.samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_NA);
                                break;
                            case RRuntime.LOGICAL_NA:
                                negativeSamples = CastUtils.samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE);
                                break;
                        }
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Byte arg) -> arg == l, samples(l), negativeSamples, Byte.class);
                    case Any:
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(arg -> arg.equals(scalarValue.value), samples(), CastUtils.samples(null), scalarValue.value.getClass());
                    default:
                        throw RInternalError.unimplemented("TODO: more types here ");
                }
            case CompareFilter.GT:
                switch (scalarValue.type) {
                    case Integer:
                        final int i = (int) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg > i, samples(i + 1), samples(i), Integer.class);
                    case Double:
                        final double d = (double) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg > d, CastUtils.<Double> samples(), samples(d), Double.class);
                    default:
                        throw RInternalError.unimplemented("TODO: more types here");
                }
            case CompareFilter.LT:
                switch (scalarValue.type) {
                    case Integer:
                        final int i = (int) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg < i, samples(i - 1), samples(i), Integer.class);
                    case Double:
                        final double d = (double) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg < d, CastUtils.<Double> samples(), samples(d), Double.class);
                    default:
                        throw RInternalError.unimplemented("TODO: more types here");
                }
            case CompareFilter.GE:
                switch (scalarValue.type) {
                    case Integer:
                        final int i = (int) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg >= i, samples(i), samples(i - 1), Integer.class);
                    case Double:
                        final double d = (double) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg >= d, samples(d), samples(d - 1), Double.class);
                    default:
                        throw RInternalError.unimplemented("TODO: more types here");
                }
            case CompareFilter.LE:
                switch (scalarValue.type) {
                    case Integer:
                        final int i = (int) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer arg) -> arg <= i, samples(i), samples(i + 1), Integer.class);
                    case Double:
                        final double d = (double) scalarValue.value;
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double arg) -> arg <= d, samples(d), samples(d + 1), Double.class);
                    default:
                        throw RInternalError.unimplemented("TODO: more types here");
                }
            case CompareFilter.SAME:
                return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples(arg -> arg == scalarValue.value, samples(scalarValue.value), CastUtils.samples(null), scalarValue.value.getClass());

            default:
                throw RInternalError.unimplemented("TODO: more operations here");
        }
    }

    @Override
    public ArgumentFilterSampler<?, ?> visit(NATest naTest, byte operation) {
        switch (operation) {
            case CompareFilter.EQ:
                switch (naTest.type) {
                    case Integer:
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Integer x) -> RRuntime.isNA(x), samples(RRuntime.INT_NA), samples(0), Integer.class);
                    case Double:
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Double x) -> RRuntime.isNAorNaN(x), samples(RRuntime.DOUBLE_NA), samples(0d), Double.class);
                    case Logical:
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((Byte x) -> RRuntime.isNA(x), samples(RRuntime.LOGICAL_NA),
                                        samples(RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE), Byte.class);
                    case Character:
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String x) -> RRuntime.isNA(x), samples(RRuntime.STRING_NA), samples(""), String.class);
                    case Complex:
                        return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((RComplex x) -> x.isNA(), samples(RDataFactory.createComplex(RRuntime.COMPLEX_NA_REAL_PART, 0)),
                                        samples(RDataFactory.createComplex(0, 0)), RComplex.class);
                    default:
                        throw RInternalError.unimplemented("TODO: more types here");
                }
            default:
                throw RInternalError.unimplemented("TODO: more operations here");
        }
    }

    @Override
    public ArgumentFilterSampler<String, String> visit(StringLength stringLength, byte operation) {
        final int l = stringLength.length;
        switch (operation) {
            case CompareFilter.EQ:
                return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg.length() == l, samples(sampleString(l)),
                                samples(sampleString(l + 1)), String.class);

            case CompareFilter.GT:
                return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg.length() > l, samples(sampleString(l + 1)),
                                samples(sampleString(l)), String.class);

            case CompareFilter.LT:
                return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg.length() < l, samples(sampleString(l - 1)),
                                samples(sampleString(l)), String.class);

            case CompareFilter.GE:
                return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg.length() >= l, samples(sampleString(l)),
                                samples(sampleString(l - 1)), String.class);

            case CompareFilter.LE:
                return ValuePredicateArgumentFilterSampler.fromLambdaWithSamples((String arg) -> arg.length() <= l, samples(sampleString(l)),
                                samples(sampleString(l + 1)), String.class);

            default:
                throw RInternalError.unimplemented("TODO: more operations here");
        }
    }

    private static String sampleString(int len) {
        if (len < 0) {
            return null;
        } else if (len == 0) {
            return "";
        } else {
            char[] ch = new char[len];
            Arrays.fill(ch, 'a');
            return String.valueOf(ch);
        }
    }

    @Override
    public ArgumentFilterSampler<RAbstractVector, RAbstractVector> visit(VectorSize vectorSize, byte operation) {
        final int s = vectorSize.size;
        switch (operation) {
            case CompareFilter.EQ:
                if (s == 0) {
                    return new VectorPredicateArgumentFilterSampler<>("size(int)", x -> x.getLength() == s, s - 1, s + 1);
                } else {
                    return new VectorPredicateArgumentFilterSampler<>("size(int)", x -> x.getLength() == s, 0, s - 1, s + 1);
                }
            case CompareFilter.GT:
                return new VectorPredicateArgumentFilterSampler<>("sizeGt(int)", x -> x.getLength() > s, s - 1, s);

            case CompareFilter.LT:
                return new VectorPredicateArgumentFilterSampler<>("sizeLt(int)", x -> x.getLength() < s, s, s + 1);

            case CompareFilter.GE:
                return new VectorPredicateArgumentFilterSampler<>("sizeGe(int)", x -> x.getLength() >= s, s - 1);

            case CompareFilter.LE:
                return new VectorPredicateArgumentFilterSampler<>("sizeLe(int)", x -> x.getLength() <= s, s + 1);

            default:
                throw RInternalError.unimplemented("TODO: more operations here");
        }
    }

    @Override
    public ArgumentFilterSampler<RAbstractVector, RAbstractVector> visit(ElementAt elementAt, byte operation) {
        final int index = elementAt.index;
        switch (operation) {
            case CompareFilter.EQ:
                switch (elementAt.type) {
                    case Integer:
                        int i = (int) elementAt.value;
                        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && i == (int) (x.getDataAtAsObject(index)), 0, index);
                    case Double:
                        double d = (double) elementAt.value;
                        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && d == (double) (x.getDataAtAsObject(index)), 0, index);
                    case Logical:
                        byte l = (byte) elementAt.value;
                        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && l == (byte) (x.getDataAtAsObject(index)), 0, index);
                    case Character:
                    case Complex:
                        return new VectorPredicateArgumentFilterSampler<>("elementAt", x -> index < x.getLength() && elementAt.value.equals(x.getDataAtAsObject(index)), 0, index);
                    default:
                        throw RInternalError.unimplemented("TODO: more types here");
                }
            default:
                throw RInternalError.unimplemented("TODO: more operations here");
        }
    }

    @Override
    public ArgumentFilterSampler<RAbstractVector, RAbstractVector> visit(Dim dim, byte operation) {
        switch (operation) {
            case CompareFilter.EQ:
                return new VectorPredicateArgumentFilterSampler<>("dimEq", v -> v.isMatrix() && v.getDimensions().length > dim.dimIndex && v.getDimensions()[dim.dimIndex] == dim.dimSize, 3);
            case CompareFilter.GT:
                return new VectorPredicateArgumentFilterSampler<>("dimGt", v -> v.isMatrix() && v.getDimensions().length > dim.dimIndex && v.getDimensions()[dim.dimIndex] > dim.dimSize, 3);
            default:
                throw RInternalError.unimplemented("TODO: more operations here");
        }
    }

}
