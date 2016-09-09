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

import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.PipelineConfigBuilder;
import com.oracle.truffle.r.nodes.builtin.ValuePredicateArgumentMapper;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.AndFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.CompareFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.FilterVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NotFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.NumericFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.OrFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapDoubleToInt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToCharAt;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapToValue;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapperVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.AsVectorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultErrorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.PipelineStepVisitor;
import com.oracle.truffle.r.nodes.unary.BypassNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.ConditionalMapNode;
import com.oracle.truffle.r.nodes.unary.FilterNode;
import com.oracle.truffle.r.nodes.unary.FindFirstNodeGen;
import com.oracle.truffle.r.nodes.unary.MapNode;
import com.oracle.truffle.r.nodes.unary.NonNANodeGen;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/**
 * Converts given pipeline into corresponding cast nodes chain.
 */
public final class PipelineToCastNode {

    public static CastNode convert(PipelineConfigBuilder configBuilder, PipelineStep lastStep) {
        // TODO: where to get the caller node? argument to this method? and default error?
        CastNodeFactory nodeFactory = new CastNodeFactory(new MessageData(null, null, null), true);
        CastNode headNode = convert(lastStep, nodeFactory);
        return BypassNode.create(configBuilder, headNode);
    }

    /**
     * Converts chain of pipeline steps to cast nodes. The steps must not contain
     * {@code PipelineConfStep} anymore. This method is also invoked when we build mapIf node to
     * convert {@code trueBranch} and {@code falseBranch}.
     */
    private static CastNode convert(PipelineStep firstStep, CastNodeFactory nodeFactory) {
        if (firstStep == null) {
            return null;
        }

        CastNode prevCastNode = null;
        PipelineStep currCastStep = firstStep;
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

    private static final class CastNodeFactory implements PipelineStepVisitor<CastNode> {
        private MessageData defaultMessage;
        private final boolean boxPrimitives;

        public CastNodeFactory(MessageData defaultMessage, boolean boxPrimitives) {
            this.defaultMessage = defaultMessage;
            this.boxPrimitives = boxPrimitives;
        }

        public CastNode create(PipelineStep step) {
            return step.accept(this);
        }

        @Override
        public CastNode visit(DefaultErrorStep step) {
            defaultMessage = step.getDefaultMessage();
            return null;
        }

        @Override
        public CastNode visit(FindFirstStep step) {
            return FindFirstNodeGen.create(step.getElementClass(), step.getDefaultValue());
        }

        @Override
        public CastNode visit(FilterStep step) {
            ArgumentFilter<Object, Boolean> filter = ArgumentFilterFactory.create(step.getFilter());
            MessageData msg = getDefaultIfNull(step.getFilter().getMessage());
            return FilterNode.create(filter, /* TODO: isWarning?? */false, msg.getCallObj(), msg.getMessage(), msg.getMessageArgs(), boxPrimitives);
        }

        @Override
        public CastNode visit(NotNAStep step) {
            Object replacement = step.getReplacement();
            if (replacement != null) {
                return NonNANodeGen.create(replacement);
            } else {
                MessageData message = step.getMessage();
                return NonNANodeGen.create(message.getCallObj(), message.getMessage(), message.getMessageArgs(), null);
            }
        }

        @Override
        public CastNode visit(AsVectorStep step) {
            RType type = step.getType();
            if (type == RType.Integer) {
                return CastIntegerNodeGen.create(false, false, false);
            } else if (type == RType.Double) {
                return CastDoubleNodeGen.create(false, false, false);
            } else if (type == RType.Character) {
                return CastStringNodeGen.create(false, false, false);
            } else if (type == RType.Complex) {
                return CastComplexNodeGen.create(false, false, false);
            } else if (type == RType.Logical) {
                return CastLogicalNodeGen.create(false, false, false);
            } else if (type == RType.Raw) {
                return CastRawNodeGen.create(false, false, false);
            }
            throw RInternalError.shouldNotReachHere(String.format("Unexpected type '%s' in AsVectorStep.", type.getName()));
        }

        @Override
        public CastNode visit(MapStep step) {
            return MapNode.create(MapperNodeFactory.create(step.getMapper()));
        }

        @Override
        public CastNode visit(MapIfStep step) {
            ArgumentFilter<Object, Boolean> condition = ArgumentFilterFactory.create(step.getFilter());
            CastNode trueCastNode = PipelineToCastNode.convert(step.getTrueBranch(), this);
            CastNode falseCastNode = PipelineToCastNode.convert(step.getFalseBranch(), this);
            return ConditionalMapNode.create(condition, trueCastNode, falseCastNode);
        }

        private MessageData getDefaultIfNull(MessageData message) {
            return message == null ? defaultMessage : message;
        }
    }

    private static final class ArgumentFilterFactory implements FilterVisitor<ArgumentFilter<Object, Boolean>> {

        private static final ArgumentFilterFactory INSTANCE = new ArgumentFilterFactory();

        private ArgumentFilterFactory() {
            // singleton
        }

        public static ArgumentFilter<Object, Boolean> create(Filter filter) {
            return filter.accept(INSTANCE);
        }

        @Override
        public ArgumentFilter<Object, Boolean> visit(TypeFilter filter) {
            return filter.getInstanceOfLambda();
        }

        @Override
        public ArgumentFilter<Object, Boolean> visit(RTypeFilter filter) {
            if (filter.getType() == RType.Integer) {
                return x -> x instanceof Integer || x instanceof RAbstractIntVector;
            } else if (filter.getType() == RType.Double) {
                return x -> x instanceof Double || x instanceof RDoubleVector;
            } else {
                throw RInternalError.unimplemented("TODO: more types here");
            }
        }

        @Override
        public ArgumentFilter<Object, Boolean> visit(NumericFilter filter) {
            return x -> x instanceof Integer || x instanceof RAbstractIntVector || x instanceof Double || x instanceof RAbstractDoubleVector || x instanceof Byte ||
                            x instanceof RAbstractLogicalVector;
        }

        @Override
        public ArgumentFilter<Object, Boolean> visit(CompareFilter filter) {
            return null;
        }

        @Override
        public ArgumentFilter<Object, Boolean> visit(AndFilter filter) {
            ArgumentFilter<Object, Boolean> leftFilter = filter.getLeft().accept(this);
            ArgumentFilter<Object, Boolean> rightFilter = filter.getRight().accept(this);
            // TODO: create and filter...
            return null;
        }

        @Override
        public ArgumentFilter<Object, Boolean> visit(OrFilter filter) {
            ArgumentFilter<Object, Boolean> leftFilter = filter.getLeft().accept(this);
            ArgumentFilter<Object, Boolean> rightFilter = filter.getRight().accept(this);
            // TODO: create or filter
            return null;
        }

        @Override
        public ArgumentFilter<Object, Boolean> visit(NotFilter filter) {
            ArgumentFilter<Object, Boolean> toNegate = filter.accept(this);
            // TODO: create not filter
            return null;
        }
    }

    private static final class MapperNodeFactory implements MapperVisitor<ValuePredicateArgumentMapper<Object, Object>> {
        private static final MapperNodeFactory INSTANCE = new MapperNodeFactory();

        private MapperNodeFactory() {
            // singleton
        }

        public static ValuePredicateArgumentMapper<Object, Object> create(Mapper mapper) {
            return mapper.accept(MapperNodeFactory.INSTANCE);
        }

        @Override
        public ValuePredicateArgumentMapper<Object, Object> visit(MapToValue mapper) {
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
            final ConditionProfile profile = ConditionProfile.createBinaryProfile();
            final ConditionProfile profile2 = ConditionProfile.createBinaryProfile();
            final char defaultValue = mapper.getDefaultValue();
            final int index = mapper.getIndex();
            return ValuePredicateArgumentMapper.fromLambda(x -> {
                String str = (String) x;
                if (profile.profile(x == null || str.isEmpty())) {
                    return defaultValue;
                } else {
                    if (profile2.profile(x == RRuntime.STRING_NA)) {
                        return RRuntime.INT_NA;
                    } else {
                        return (int) str.charAt(index);
                    }
                }
            });
        }
    }
}
