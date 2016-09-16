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

import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter.ArgumentTypeFilter;
import com.oracle.truffle.r.nodes.builtin.ArgumentMapper;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.PipelineConfigBuilder;
import com.oracle.truffle.r.nodes.builtin.ValuePredicateArgumentMapper;
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
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapperVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep.TargetType;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultErrorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultWarningStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.PipelineStepVisitor;
import com.oracle.truffle.r.nodes.unary.BypassNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleBaseNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerBaseNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalBaseNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringBaseNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToAttributableNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.ConditionalMapNode;
import com.oracle.truffle.r.nodes.unary.FilterNode;
import com.oracle.truffle.r.nodes.unary.FindFirstNodeGen;
import com.oracle.truffle.r.nodes.unary.MapNode;
import com.oracle.truffle.r.nodes.unary.NonNANodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Converts given pipeline into corresponding cast nodes chain.
 */
public final class PipelineToCastNode {

    public static CastNode convert(PipelineConfigBuilder configBuilder, PipelineStep<?, ?> firstStep) {
        if (firstStep == null) {
            return BypassNode.create(configBuilder, null);
        } else {
            // TODO: where to get the caller node? argument to this method? and default error?
            CastNodeFactory nodeFactory = new CastNodeFactory(configBuilder.getDefaultDefaultMessage());
            CastNode headNode = convert(firstStep, nodeFactory);
            return BypassNode.create(configBuilder, headNode);
        }
    }

    /**
     * Converts chain of pipeline steps to cast nodes. The steps must not contain
     * {@code PipelineConfStep} anymore. This method is also invoked when we build mapIf node to
     * convert {@code trueBranch} and {@code falseBranch}.
     */
    private static CastNode convert(PipelineStep<?, ?> firstStep, CastNodeFactory nodeFactory) {
        if (firstStep == null) {
            return null;
        }

        CastNode prevCastNode = null;
        PipelineStep<?, ?> currCastStep = firstStep;
        while (currCastStep != null) {
            CastNode node = nodeFactory.create(currCastStep);
            if (node != null) {
                if (prevCastNode == null) {
                    prevCastNode = node;
                } else {
                    CastNode finalPrevCastNode = prevCastNode;
                    prevCastNode = new ChainedCastNode(() -> finalPrevCastNode, () -> node);
                }
            }

            currCastStep = currCastStep.getNext();
        }
        return prevCastNode;
    }

    public static ArgumentFilter<?, ?> convert(Filter<?, ?> filter) {
        return filter == null ? null : ArgumentFilterFactory.create(filter);
    }

    public static ArgumentMapper<?, ?> convert(Mapper<?, ?> mapper) {
        return mapper == null ? null : MapperNodeFactory.create(mapper);
    }

    private static final class CastNodeFactory implements PipelineStepVisitor<CastNode> {
        private final CastNodeFactory parentFactory;
        private MessageData defaultErrorMessage;
        private MessageData defaultWarningMessage;
        private boolean boxPrimitives = false;

        CastNodeFactory(MessageData defaultMessage) {
            this(null, defaultMessage);
        }

        CastNodeFactory(CastNodeFactory parentFactory, MessageData defaultMessage) {
            this.parentFactory = parentFactory;
            this.defaultErrorMessage = defaultMessage;
        }

        public CastNode create(PipelineStep<?, ?> step) {
            return step.accept(this);
        }

        @Override
        public CastNode visit(DefaultErrorStep<?> step) {
            defaultErrorMessage = step.getDefaultMessage();
            return null;
        }

        @Override
        public CastNode visit(DefaultWarningStep<?> step) {
            defaultWarningMessage = step.getDefaultMessage();
            return null;
        }

        @Override
        public CastNode visit(FindFirstStep<?, ?> step) {
            boxPrimitives = false;

            if (step.getDefaultValue() == null) {
                MessageData msg = getDefaultIfNull(step.getError(), false);
                return FindFirstNodeGen.create(step.getElementClass(), msg.getCallObj(), msg.getMessage(), msg.getMessageArgs(), step.getDefaultValue());
            } else {
                MessageData msg = step.getError();
                if (msg == null) {
                    return FindFirstNodeGen.create(step.getElementClass(), step.getDefaultValue());
                } else {
                    return FindFirstNodeGen.create(step.getElementClass(), msg.getCallObj(), msg.getMessage(), msg.getMessageArgs(), step.getDefaultValue());
                }
            }
        }

        @Override
        public CastNode visit(FilterStep<?, ?> step) {
            ArgumentFilter<?, ?> filter = ArgumentFilterFactory.create(step.getFilter());
            MessageData msg = getDefaultIfNull(step.getMessage(), step.isWarning());
            return FilterNode.create(filter, step.isWarning(), msg.getCallObj(), msg.getMessage(), msg.getMessageArgs(), boxPrimitives);
        }

        @Override
        public CastNode visit(NotNAStep<?> step) {
            if (step.getReplacement() == null) {
                MessageData msg = getDefaultIfNull(step.getMessage(), false);
                return NonNANodeGen.create(msg.getCallObj(), msg.getMessage(), msg.getMessageArgs(), step.getReplacement());
            } else {
                MessageData msg = step.getMessage();
                if (msg == null) {
                    return NonNANodeGen.create(null, null, null, step.getReplacement());
                } else {
                    return NonNANodeGen.create(msg.getCallObj(), msg.getMessage(), msg.getMessageArgs(), step.getReplacement());
                }
            }
        }

        @Override
        public CastNode visit(CoercionStep<?, ?> step) {
            boxPrimitives = true;

            TargetType type = step.getType();
            switch (type) {
                case Integer:
                    return step.vectorCoercion ? CastIntegerNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes)
                                    : CastIntegerBaseNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes);
                case Double:
                    return step.vectorCoercion ? CastDoubleNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes)
                                    : CastDoubleBaseNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes);
                case Character:
                    return step.vectorCoercion ? CastStringNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes)
                                    : CastStringBaseNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes);
                case Logical:
                    return step.vectorCoercion ? CastLogicalNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes)
                                    : CastLogicalBaseNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes);
                case Complex:
                    return CastComplexNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes);
                case Raw:
                    return CastRawNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes);
                case Any:
                    return CastToVectorNodeGen.create(step.preserveNonVector);
                case Attributable:
                    return CastToAttributableNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes);
                default:
                    throw RInternalError.shouldNotReachHere(String.format("Unexpected type '%s' in AsVectorStep.", type));
            }
        }

        @Override
        public CastNode visit(MapStep<?, ?> step) {
            return MapNode.create(MapperNodeFactory.create(step.getMapper()));
        }

        @Override
        public CastNode visit(MapIfStep<?, ?> step) {
            ArgumentFilter<?, ?> condition = ArgumentFilterFactory.create(step.getFilter());
            CastNode trueCastNode = PipelineToCastNode.convert(step.getTrueBranch(), this);
            CastNode falseCastNode = PipelineToCastNode.convert(step.getFalseBranch(), this);
            return ConditionalMapNode.create(condition, trueCastNode, falseCastNode);
        }

        private MessageData getDefaultIfNull(MessageData message, boolean isWarning) {
            return message == null ? (isWarning ? defaultWarningMessage : defaultErrorMessage) : message;
        }

    }

    private static final class ArgumentFilterFactory implements FilterVisitor<ArgumentFilter<?, ?>>, MatrixFilter.OperationVisitor<ArgumentFilter<RAbstractVector, RAbstractVector>>,
                    DoubleFilter.OperationVisitor<ArgumentFilter<Double, Double>>, CompareFilter.SubjectVisitor<ArgumentFilter<?, ?>> {

        private static final ArgumentFilterFactory INSTANCE = new ArgumentFilterFactory();

        private ArgumentFilterFactory() {
            // singleton
        }

        public static ArgumentFilter<?, ?> create(Filter<?, ?> filter) {
            return filter.accept(INSTANCE);
        }

        @Override
        public ArgumentFilter<?, ?> visit(TypeFilter<?, ?> filter) {
            return filter.getInstanceOfLambda();
        }

        @Override
        public ArgumentFilter<?, ?> visit(RTypeFilter<?> filter) {
            if (filter.getType() == RType.Integer) {
                return x -> x instanceof Integer || x instanceof RAbstractIntVector;
            } else if (filter.getType() == RType.Double) {
                return x -> x instanceof Double || x instanceof RAbstractDoubleVector;
            } else if (filter.getType() == RType.Logical) {
                return x -> x instanceof Byte || x instanceof RAbstractLogicalVector;
            } else if (filter.getType() == RType.Complex) {
                return x -> x instanceof RAbstractComplexVector;
            } else if (filter.getType() == RType.Character) {
                return x -> x instanceof String || x instanceof RAbstractStringVector;
            } else {
                throw RInternalError.unimplemented("TODO: more types here");
            }
        }

        @Override
        public ArgumentFilter<?, ?> visit(CompareFilter<?> filter) {
            return filter.getSubject().accept(this, filter.getOperation());
        }

        @SuppressWarnings("rawtypes")
        @Override
        public ArgumentFilter<?, ?> visit(AndFilter<?, ?> filter) {
            ArgumentFilter leftFilter = filter.getLeft().accept(this);
            ArgumentFilter rightFilter = filter.getRight().accept(this);
            return new ArgumentTypeFilter<Object, Object>() {

                @SuppressWarnings("unchecked")
                @Override
                public boolean test(Object arg) {
                    if (!leftFilter.test(arg)) {
                        return false;
                    } else {
                        return rightFilter.test(arg);
                    }
                }

            };
        }

        @SuppressWarnings("rawtypes")
        @Override
        public ArgumentFilter<?, ?> visit(OrFilter<?> filter) {
            ArgumentFilter leftFilter = filter.getLeft().accept(this);
            ArgumentFilter rightFilter = filter.getRight().accept(this);
            return new ArgumentTypeFilter<Object, Object>() {

                @SuppressWarnings("unchecked")
                @Override
                public boolean test(Object arg) {
                    if (leftFilter.test(arg)) {
                        return true;
                    } else {
                        return rightFilter.test(arg);
                    }
                }

            };
        }

        @SuppressWarnings("rawtypes")
        @Override
        public ArgumentFilter<?, ?> visit(NotFilter<?> filter) {
            ArgumentFilter toNegate = filter.getFilter().accept(this);
            return new ArgumentFilter<Object, Object>() {

                @SuppressWarnings("unchecked")
                @Override
                public boolean test(Object arg) {
                    return !toNegate.test(arg);
                }

            };
        }

        @Override
        public ArgumentFilter<?, ?> visit(MatrixFilter<?> filter) {
            return filter.acceptOperation(this);
        }

        @Override
        public ArgumentFilter<?, ?> visit(DoubleFilter filter) {
            return filter.acceptOperation(this);
        }

        @Override
        public ArgumentFilter<RAbstractVector, RAbstractVector> visitIsMatrix() {
            return x -> x.isMatrix();
        }

        @Override
        public ArgumentFilter<RAbstractVector, RAbstractVector> visitIsSquareMatrix() {
            return x -> x.isMatrix() && x.getDimensions()[0] == x.getDimensions()[1];
        }

        @Override
        public ArgumentFilter<Double, Double> visitIsFinite() {
            return x -> !Double.isInfinite(x);
        }

        @Override
        public ArgumentFilter<Double, Double> visitIsFractional() {
            return x -> !RRuntime.isNAorNaN(x) && !Double.isInfinite(x) && x != Math.floor(x);
        }

        @Override
        public ArgumentFilter<?, ?> visit(ScalarValue scalarValue, byte operation) {
            switch (operation) {
                case CompareFilter.EQ:
                    switch (scalarValue.type) {
                        case Character:
                            return (String arg) -> arg.equals(scalarValue.value);
                        case Integer:
                            return (Integer arg) -> arg == (int) scalarValue.value;
                        case Double:
                            return (Double arg) -> arg == (double) scalarValue.value;
                        case Logical:
                            return (Byte arg) -> arg == (byte) scalarValue.value;
                        case Any:
                            return arg -> arg.equals(scalarValue.value);
                        default:
                            throw RInternalError.unimplemented("TODO: more types here ");
                    }
                case CompareFilter.GT:
                    switch (scalarValue.type) {
                        case Integer:
                            return (Integer arg) -> arg > (int) scalarValue.value;
                        case Double:
                            return (Double arg) -> arg > (double) scalarValue.value;
                        case Logical:
                            return (Byte arg) -> arg > (byte) scalarValue.value;
                        default:
                            throw RInternalError.unimplemented("TODO: more types here");
                    }
                case CompareFilter.LT:
                    switch (scalarValue.type) {
                        case Integer:
                            return (Integer arg) -> arg < (int) scalarValue.value;
                        case Double:
                            return (Double arg) -> arg < (double) scalarValue.value;
                        case Logical:
                            return (Byte arg) -> arg < (byte) scalarValue.value;
                        default:
                            throw RInternalError.unimplemented("TODO: more types here");
                    }
                case CompareFilter.GE:
                    switch (scalarValue.type) {
                        case Integer:
                            return (Integer arg) -> arg >= (int) scalarValue.value;
                        case Double:
                            return (Double arg) -> arg >= (double) scalarValue.value;
                        case Logical:
                            return (Byte arg) -> arg >= (byte) scalarValue.value;
                        default:
                            throw RInternalError.unimplemented("TODO: more types here");
                    }
                case CompareFilter.LE:
                    switch (scalarValue.type) {
                        case Integer:
                            return (Integer arg) -> arg <= (int) scalarValue.value;
                        case Double:
                            return (Double arg) -> arg <= (double) scalarValue.value;
                        case Logical:
                            return (Byte arg) -> arg <= (byte) scalarValue.value;
                        default:
                            throw RInternalError.unimplemented("TODO: more types here");
                    }
                case CompareFilter.SAME:
                    return arg -> arg == scalarValue.value;

                default:
                    throw RInternalError.unimplemented("TODO: more operations here");
            }
        }

        @Override
        public ArgumentFilter<?, ?> visit(NATest naTest, byte operation) {
            switch (operation) {
                case CompareFilter.EQ:
                    switch (naTest.type) {
                        case Integer:
                            return arg -> RRuntime.isNA((int) arg);
                        case Double:
                            return arg -> RRuntime.isNAorNaN((double) arg);
                        case Logical:
                            return arg -> RRuntime.isNA((byte) arg);
                        case Character:
                            return arg -> RRuntime.isNA((String) arg);
                        case Complex:
                            return arg -> RRuntime.isNA((RComplex) arg);
                        default:
                            throw RInternalError.unimplemented("TODO: more types here");
                    }
                default:
                    throw RInternalError.unimplemented("TODO: more operations here");
            }
        }

        @Override
        public ArgumentFilter<String, String> visit(StringLength stringLength, byte operation) {
            switch (operation) {
                case CompareFilter.EQ:
                    return arg -> arg.length() == stringLength.length;

                case CompareFilter.GT:
                    return arg -> arg.length() > stringLength.length;

                case CompareFilter.LT:
                    return arg -> arg.length() < stringLength.length;

                case CompareFilter.GE:
                    return arg -> arg.length() >= stringLength.length;

                case CompareFilter.LE:
                    return arg -> arg.length() <= stringLength.length;

                default:
                    throw RInternalError.unimplemented("TODO: more operations here");
            }
        }

        @Override
        public ArgumentFilter<RAbstractVector, RAbstractVector> visit(VectorSize vectorSize, byte operation) {
            switch (operation) {
                case CompareFilter.EQ:
                    return arg -> arg.getLength() == vectorSize.size;

                case CompareFilter.GT:
                    return arg -> arg.getLength() > vectorSize.size;

                case CompareFilter.LT:
                    return arg -> arg.getLength() < vectorSize.size;

                case CompareFilter.GE:
                    return arg -> arg.getLength() >= vectorSize.size;

                case CompareFilter.LE:
                    return arg -> arg.getLength() <= vectorSize.size;

                default:
                    throw RInternalError.unimplemented("TODO: more operations here");
            }
        }

        @Override
        public ArgumentFilter<RAbstractVector, RAbstractVector> visit(ElementAt elementAt, byte operation) {
            switch (operation) {
                case CompareFilter.EQ:
                    switch (elementAt.type) {
                        case Integer:
                            return arg -> elementAt.index < arg.getLength() && (int) elementAt.value == (int) arg.getDataAtAsObject(elementAt.index);
                        case Double:
                            return arg -> elementAt.index < arg.getLength() && (double) elementAt.value == (double) arg.getDataAtAsObject(elementAt.index);
                        case Logical:
                            return arg -> elementAt.index < arg.getLength() && (byte) elementAt.value == (byte) arg.getDataAtAsObject(elementAt.index);
                        case Character:
                        case Complex:
                            return arg -> elementAt.index < arg.getLength() && elementAt.value.equals(arg.getDataAtAsObject(elementAt.index));
                        default:
                            throw RInternalError.unimplemented("TODO: more types here");
                    }
                default:
                    throw RInternalError.unimplemented("TODO: more operations here");
            }
        }

        @Override
        public ArgumentFilter<RAbstractVector, RAbstractVector> visit(Dim dim, byte operation) {
            switch (operation) {
                case CompareFilter.EQ:
                    return v -> v.isMatrix() && v.getDimensions().length > dim.dimIndex && v.getDimensions()[dim.dimIndex] == dim.dimSize;
                case CompareFilter.GT:
                    return v -> v.isMatrix() && v.getDimensions().length > dim.dimIndex && v.getDimensions()[dim.dimIndex] > dim.dimSize;
                default:
                    throw RInternalError.unimplemented("TODO: more operations here");
            }
        }

    }

    private static final class MapperNodeFactory implements MapperVisitor<ValuePredicateArgumentMapper<Object, Object>> {

        private static final MapperNodeFactory INSTANCE = new MapperNodeFactory();

        private MapperNodeFactory() {
            // singleton
        }

        public static ValuePredicateArgumentMapper<Object, Object> create(Mapper<?, ?> mapper) {
            return mapper.accept(MapperNodeFactory.INSTANCE);
        }

        @Override
        public ValuePredicateArgumentMapper<Object, Object> visit(MapToValue<?, ?> mapper) {
            final Object value = mapper.getValue();
            return ValuePredicateArgumentMapper.fromLambda(x -> value);
        }

        @Override
        public ValuePredicateArgumentMapper<Object, Object> visit(MapByteToBoolean mapper) {
            return ValuePredicateArgumentMapper.fromLambda(x -> RRuntime.fromLogical((Byte) x));
        }

        @Override
        public ValuePredicateArgumentMapper<Object, Object> visit(MapDoubleToInt mapper) {
            final NACheck naCheck = NACheck.create();
            return ValuePredicateArgumentMapper.fromLambda(x -> {
                double d = (Double) x;
                naCheck.enable(d);
                return naCheck.convertDoubleToInt(d);
            });
        }

        @Override
        public ValuePredicateArgumentMapper<Object, Object> visit(MapToCharAt mapper) {
            final int defaultValue = mapper.getDefaultValue();
            final int index = mapper.getIndex();
            return ValuePredicateArgumentMapper.fromLambda(x -> {
                String str = (String) x;
                if (x == null || str.isEmpty()) {
                    return defaultValue;
                } else {
                    if (x == RRuntime.STRING_NA) {
                        return RRuntime.INT_NA;
                    } else {
                        return (int) str.charAt(index);
                    }
                }
            });
        }
    }
}
