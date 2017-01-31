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

import java.util.Optional;
import java.util.function.Supplier;

import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter.ArgumentTypeFilter;
import com.oracle.truffle.r.nodes.builtin.ArgumentMapper;
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
import com.oracle.truffle.r.nodes.builtin.casts.Filter.MissingFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NullFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.ResultForArg;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapperVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.AttributableCoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.BoxPrimitiveStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultErrorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultWarningStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.PipelineStepVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardingAnalysisResult;
import com.oracle.truffle.r.nodes.unary.BypassNode;
import com.oracle.truffle.r.nodes.unary.BypassNodeGen.BypassDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.BypassNodeGen.BypassIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.BypassNodeGen.BypassLogicalMapToBooleanNodeGen;
import com.oracle.truffle.r.nodes.unary.BypassNodeGen.BypassStringNodeGen;
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
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Converts given pipeline into corresponding cast nodes chain.
 */
public final class PipelineToCastNode {

    private PipelineToCastNode() {
        // nop: static class
    }

    public static CastNode convert(PipelineConfig config, PipelineStep<?, ?> firstStep, Optional<ForwardingAnalysisResult> fwdAnalysisResult) {
        return convert(config, firstStep, PipelineConfig.getFilterFactory(), PipelineConfig.getMapperFactory(), fwdAnalysisResult);
    }

    public static CastNode convert(PipelineConfig config, PipelineStep<?, ?> firstStep, ArgumentFilterFactory filterFactory, ArgumentMapperFactory mapperFactory,
                    Optional<ForwardingAnalysisResult> fwdAnalysisResult) {
        if (firstStep == null) {
            return BypassNode.create(config, null, mapperFactory, null);
        }

        Supplier<CastNode> originalPipelineFactory = () -> {
            CastNodeFactory nodeFactory = new CastNodeFactory(filterFactory, mapperFactory, config.getDefaultDefaultMessage());
            SinglePrimitiveOptimization singleOptVisitor = new SinglePrimitiveOptimization(nodeFactory);
            CastNode headNode = convert(firstStep, singleOptVisitor);
            return singleOptVisitor.createBypassNode(config, headNode, mapperFactory);
        };

        if (!config.getValueForwarding()) {
            return originalPipelineFactory.get();
        }

        ForwardingAnalysisResult fwdRes = fwdAnalysisResult.get();
        if (fwdRes != null && fwdRes.isAnythingForwarded()) {
            return ValueForwardingNodeGen.create(fwdRes, originalPipelineFactory);
        } else {
            return originalPipelineFactory.get();
        }
    }

    /**
     * Converts chain of pipeline steps to cast nodes.
     */
    private static CastNode convert(PipelineStep<?, ?> firstStep, PipelineStepVisitor<CastNode> nodeFactory) {
        if (firstStep == null) {
            return null;
        }

        CastNode prevCastNode = null;
        PipelineStep<?, ?> currCastStep = firstStep;
        while (currCastStep != null) {
            CastNode node = currCastStep.accept(nodeFactory);
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

    /**
     * Visitor that is capable of recognizing patterns that permit to bypass single primitive value
     * directly to any casts after find first step or directly to the built-in if there is nothing
     * after find first step.
     */
    private static final class SinglePrimitiveOptimization implements PipelineStepVisitor<CastNode> {
        // Any destructive step or step we cannot analyze changes this to false
        private boolean canBeOptimized = true;
        // Any coercion or check step initialize this or check the existing value, if it does not
        // match -> canBeOptimized = false
        private RType targetType = null;
        // We remember this step so that we can construct another copy of its cast node
        private FindFirstStep<?, ?> findFirstStep = null;
        private final PipelineStepVisitor<CastNode> inner;

        private SinglePrimitiveOptimization(PipelineStepVisitor<CastNode> inner) {
            this.inner = inner;
        }

        /**
         * Creates {@link BypassNode} if there is no optimization opportunity, or creates more
         * specialized child class if the cast pipeline follows the required pattern.
         */
        public CastNode createBypassNode(PipelineConfig pipelineConfig, CastNode wrappedHead, ArgumentMapperFactory mapperFactory) {
            if (canBeOptimized && findFirstStep != null) {
                if (targetType == RType.Integer) {
                    return BypassIntegerNodeGen.create(pipelineConfig, wrappedHead, mapperFactory, getFindFirstWithDefault(), getAfterFindFirstNode());
                } else if (targetType == RType.Double) {
                    return BypassDoubleNodeGen.create(pipelineConfig, wrappedHead, mapperFactory, getFindFirstWithDefault(), getAfterFindFirstNode());
                } else if (targetType == RType.Logical && isNextMapToBoolean(findFirstStep)) {
                    return BypassLogicalMapToBooleanNodeGen.create(pipelineConfig, wrappedHead, mapperFactory, getFindFirstWithDefault(), getAfterFindFirstNode());
                } else if (targetType == RType.Character) {
                    return BypassStringNodeGen.create(pipelineConfig, wrappedHead, mapperFactory, getFindFirstWithDefault(), getAfterFindFirstNode());
                }
            }
            return BypassNode.create(pipelineConfig, wrappedHead, mapperFactory, getFindFirstWithDefault());
        }

        @Override
        public CastNode visit(FindFirstStep<?, ?> step) {
            assert !canBeOptimized || targetType != null : "There must be a coercion step before find first";
            findFirstStep = step;
            return inner.visit(step);
        }

        @Override
        public CastNode visit(CoercionStep<?, ?> step) {
            canBeOptimized(step.type);
            return inner.visit(step);
        }

        @Override
        public CastNode visit(MapStep<?, ?> step) {
            cannotBeOptimizedBeforeFindFirst();
            return inner.visit(step);
        }

        @Override
        public CastNode visit(MapIfStep<?, ?> step) {
            cannotBeOptimizedBeforeFindFirst();
            return inner.visit(step);
        }

        @Override
        public CastNode visit(FilterStep<?, ?> step) {
            targetType = checkFilter(step.getFilter());
            if (targetType == null) {
                canBeOptimized = false;
            }
            return inner.visit(step);
        }

        @Override
        public CastNode visit(NotNAStep<?> step) {
            // TODO: we can remember that we saw not NA and do this check in the BypassNode
            canBeOptimized = false;
            return inner.visit(step);
        }

        @Override
        public CastNode visit(DefaultErrorStep<?> step) {
            return inner.visit(step);
        }

        @Override
        public CastNode visit(DefaultWarningStep<?> step) {
            return inner.visit(step);
        }

        @Override
        public CastNode visit(BoxPrimitiveStep<?> step) {
            canBeOptimized = false;
            return inner.visit(step);
        }

        @Override
        public CastNode visit(AttributableCoercionStep<?> step) {
            cannotBeOptimizedBeforeFindFirst();
            return inner.visit(step);
        }

        private void cannotBeOptimizedBeforeFindFirst() {
            if (findFirstStep == null) {
                canBeOptimized = false;
            }
        }

        private void canBeOptimized(RType newType) {
            if (targetType == null) {
                targetType = newType;
            } else if (targetType != newType) {
                canBeOptimized = false;
            }
        }

        /**
         * Returns null if the filter does not conform to expected type or does not produce some
         * concrete type if there is no expected type.
         */
        private RType checkFilter(Filter<?, ?> filter) {
            if (filter instanceof RTypeFilter) {
                RType type = ((RTypeFilter<?>) filter).getType();
                if (targetType == null) {
                    return type;
                }
                return type == targetType ? type : null;
            } else if (filter instanceof OrFilter) {
                OrFilter<?> or = (OrFilter<?>) filter;
                RType leftType = checkFilter(or.getLeft());
                if (targetType == null) {
                    return leftType;
                }
                RType rightType = checkFilter(or.getRight());
                return rightType == targetType || leftType == targetType ? targetType : null;
            }
            return null;
        }

        private CastNode getFindFirstWithDefault() {
            if (findFirstStep != null && findFirstStep.getDefaultValue() != null) {
                return convert(findFirstStep, inner);
            }
            return null;
        }

        private CastNode getAfterFindFirstNode() {
            if (findFirstStep.getNext() != null) {
                return convert(findFirstStep.getNext(), inner);
            }
            return null;
        }

        private static boolean isNextMapToBoolean(FindFirstStep<?, ?> findFirst) {
            PipelineStep<?, ?> next = findFirst.getNext();
            return next != null && next instanceof MapStep && ((MapStep<?, ?>) next).getMapper() instanceof MapByteToBoolean;
        }
    }

    private static final class CastNodeFactory implements PipelineStepVisitor<CastNode> {
        private final ArgumentFilterFactory filterFactory;
        private final ArgumentMapperFactory mapperFactory;
        private boolean boxPrimitives = false;

        /**
         * Should be used when {@link #defaultError} or {@link #defaultWarning} are not explicitly
         * set by visiting {@link DefaultErrorStep}.
         */
        private final MessageData defaultMessage;

        /**
         * Use {@link #getDefaultErrorIfNull} and {@link #getDefaultWarningIfNull} to always get a
         * non-null message - they supply {@link #defaultMessage} if there is no explicitly set.
         */
        private MessageData defaultError;
        private MessageData defaultWarning;

        CastNodeFactory(ArgumentFilterFactory filterFactory, ArgumentMapperFactory mapperFactory, MessageData defaultMessage) {
            assert defaultMessage != null : "defaultMessage is null";
            this.filterFactory = filterFactory;
            this.mapperFactory = mapperFactory;
            this.defaultMessage = defaultMessage;
        }

        @Override
        public CastNode visit(DefaultErrorStep<?> step) {
            defaultError = step.getDefaultMessage();
            return null;
        }

        @Override
        public CastNode visit(DefaultWarningStep<?> step) {
            defaultWarning = step.getDefaultMessage();
            return null;
        }

        @Override
        public CastNode visit(BoxPrimitiveStep<?> step) {
            return BoxPrimitiveNode.create();
        }

        @Override
        public CastNode visit(FindFirstStep<?, ?> step) {
            boxPrimitives = false;

            // See FindFirstStep documentation on how it should be interpreted
            if (step.getDefaultValue() == null) {
                MessageData msg = step.getError();
                if (msg == null) {
                    // Note: intentional direct use of defaultError
                    msg = defaultError != null ? defaultError : new MessageData(null, RError.Message.LENGTH_ZERO);
                }
                return FindFirstNodeGen.create(step.getElementClass(), msg.getCallObj(), msg.getMessage(), msg.getMessageArgs(), step.getDefaultValue());
            } else {
                MessageData warning = step.getError();
                if (warning == null) {
                    return FindFirstNodeGen.create(step.getElementClass(), step.getDefaultValue());
                } else {
                    return FindFirstNodeGen.create(step.getElementClass(), warning.getCallObj(), warning.getMessage(), warning.getMessageArgs(), step.getDefaultValue());
                }
            }
        }

        @Override
        public CastNode visit(FilterStep<?, ?> step) {
            ArgumentFilter<?, ?> filter = filterFactory.createFilter(step.getFilter());
            MessageData msg = getDefaultIfNull(step.getMessage(), step.isWarning());
            return FilterNode.create(filter, step.isWarning(), msg.getCallObj(), msg.getMessage(), msg.getMessageArgs(), boxPrimitives, ResultForArg.TRUE.equals(step.getFilter().resultForNull()),
                            ResultForArg.TRUE.equals(step.getFilter().resultForMissing()));
        }

        @Override
        public CastNode visit(NotNAStep<?> step) {
            if (step.getReplacement() == null) {
                MessageData msg = getDefaultErrorIfNull(step.getMessage());
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

            RType type = step.getType();
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
                default:
                    throw RInternalError.shouldNotReachHere(Utils.stringFormat("Unsupported type '%s' in AsVectorStep.", type));
            }
        }

        @Override
        public CastNode visit(AttributableCoercionStep<?> step) {
            return CastToAttributableNodeGen.create(step.preserveNames, step.preserveDimensions, step.preserveAttributes);
        }

        @Override
        public CastNode visit(MapStep<?, ?> step) {
            return MapNode.create(mapperFactory.createMapper(step.getMapper()));
        }

        @Override
        public CastNode visit(MapIfStep<?, ?> step) {
            ArgumentFilter<?, ?> condition = filterFactory.createFilter(step.getFilter());
            CastNode trueCastNode = PipelineToCastNode.convert(step.getTrueBranch(), this);
            CastNode falseCastNode = PipelineToCastNode.convert(step.getFalseBranch(), this);
            return ConditionalMapNode.create(condition, trueCastNode, falseCastNode, ResultForArg.TRUE.equals(step.getFilter().resultForNull()),
                            ResultForArg.TRUE.equals(step.getFilter().resultForMissing()), step.isReturns());
        }

        private MessageData getDefaultErrorIfNull(MessageData message) {
            return MessageData.getFirstNonNull(message, defaultError, defaultMessage);
        }

        private MessageData getDefaultWarningIfNull(MessageData message) {
            return MessageData.getFirstNonNull(message, defaultWarning, defaultMessage);
        }

        private MessageData getDefaultIfNull(MessageData message, boolean isWarning) {
            return isWarning ? getDefaultWarningIfNull(message) : getDefaultErrorIfNull(message);
        }
    }

    public interface ArgumentFilterFactory {
        ArgumentFilter<?, ?> createFilter(Filter<?, ?> filter);
    }

    public static final class ArgumentFilterFactoryImpl
                    implements ArgumentFilterFactory, FilterVisitor<ArgumentFilter<?, ?>>, MatrixFilter.OperationVisitor<ArgumentFilter<RAbstractVector, RAbstractVector>>,
                    DoubleFilter.OperationVisitor<ArgumentFilter<Double, Double>>, CompareFilter.SubjectVisitor<ArgumentFilter<?, ?>> {

        public static final ArgumentFilterFactoryImpl INSTANCE = new ArgumentFilterFactoryImpl();

        private ArgumentFilterFactoryImpl() {
            // singleton
        }

        @Override
        public ArgumentFilter<?, ?> createFilter(Filter<?, ?> filter) {
            return filter.accept(this);
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
            } else if (filter.getType() == RType.Raw) {
                return x -> x instanceof RAbstractRawVector;
            } else {
                throw RInternalError.unimplemented("TODO: more types here");
            }
        }

        @Override
        public ArgumentFilter<?, ?> visit(CompareFilter<?> filter) {
            return filter.getSubject().accept(this, filter.getOperation());
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public ArgumentFilter<?, ?> visit(AndFilter<?, ?> filter) {
            ArgumentFilter leftFilter = filter.getLeft().accept(this);
            ArgumentFilter rightFilter = filter.getRight().accept(this);
            return (ArgumentTypeFilter<Object, Object>) arg -> {
                if (!leftFilter.test(arg)) {
                    return false;
                } else {
                    return rightFilter.test(arg);
                }
            };
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public ArgumentFilter<?, ?> visit(OrFilter<?> filter) {
            ArgumentFilter leftFilter = filter.getLeft().accept(this);
            ArgumentFilter rightFilter = filter.getRight().accept(this);
            return (ArgumentTypeFilter<Object, Object>) arg -> {
                if (leftFilter.test(arg)) {
                    return true;
                } else {
                    return rightFilter.test(arg);
                }
            };
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public ArgumentFilter<?, ?> visit(NotFilter<?> filter) {
            ArgumentFilter toNegate = filter.getFilter().accept(this);
            return (ArgumentFilter<Object, Object>) arg -> !toNegate.test(arg);
        }

        @Override
        public ArgumentFilter<?, ?> visit(NullFilter filter) {
            return (ArgumentFilter<Object, Object>) arg -> false;
        }

        @Override
        public ArgumentFilter<?, ?> visit(MissingFilter filter) {
            return (ArgumentFilter<Object, Object>) arg -> false;
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
            return RAbstractVector::isMatrix;
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

    public interface ArgumentMapperFactory {
        ArgumentMapper<?, ?> createMapper(Mapper<?, ?> mapper);
    }

    public static final class ArgumentMapperFactoryImpl implements ArgumentMapperFactory, MapperVisitor<ValuePredicateArgumentMapper<Object, Object>> {

        public static final ArgumentMapperFactoryImpl INSTANCE = new ArgumentMapperFactoryImpl();

        private ArgumentMapperFactoryImpl() {
            // singleton
        }

        @Override
        public ArgumentMapper<Object, Object> createMapper(Mapper<?, ?> mapper) {
            return mapper.accept(this);
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
