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
import com.oracle.truffle.r.nodes.builtin.casts.CastStep.AsVectorStep;
import com.oracle.truffle.r.nodes.builtin.casts.CastStep.CastStepVisitor;
import com.oracle.truffle.r.nodes.builtin.casts.CastStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.CastStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.CastStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.CastStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.CastStep.NotNAStep;
import com.oracle.truffle.r.nodes.builtin.casts.CastStep.PipelineConfStep;
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
import com.oracle.truffle.r.nodes.unary.BypassNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.FilterNode;
import com.oracle.truffle.r.nodes.unary.FindFirstNodeGen;
import com.oracle.truffle.r.nodes.unary.MapNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Converts given pipeline into corresponding cast nodes chain.
 */
public final class CastStepToCastNode {

    public static CastNode convert(PipelineConfStep firstStep) {
        CastNodeFactory nodeFactory = new CastNodeFactory(null, null, null, true); // TODO: default
                                                                                   // error instead
                                                                                   // of nulls
        CastNode prevCastNode = null;
        CastStep currCastStep = firstStep.getNext();
        while (currCastStep != null) {
            CastNode node = nodeFactory.create(currCastStep);
            if (prevCastNode == null) {
                prevCastNode = node;
            } else {
                CastNode finalPrevCastNode = prevCastNode;
                prevCastNode = new ChainedCastNode(() -> node, () -> finalPrevCastNode);
            }

            currCastStep = currCastStep.getNext();
        }
        return BypassNode.create(firstStep.getConfigBuilder(), prevCastNode);
    }

    private static final class CastNodeFactory implements CastStepVisitor<CastNode> {
        private final RBaseNode defaultCallObj;
        private final RError.Message defaultMessage;
        private final Object[] defaultMessageArgs;
        private final boolean boxPrimitives;

        public CastNodeFactory(RBaseNode defaultCallObj, Message defaultMessage, Object[] defaultMessageArgs, boolean boxPrimitives) {
            this.defaultCallObj = defaultCallObj;
            this.defaultMessage = defaultMessage;
            this.defaultMessageArgs = defaultMessageArgs;
            this.boxPrimitives = boxPrimitives;
        }

        public CastNode create(CastStep step) {
            return step.accept(this);
        }

        @Override
        public CastNode visit(PipelineConfStep step) {
            throw RInternalError.shouldNotReachHere("There can be only one PipelineConfStep " +
                            "in pipeline as the first node and it should have been handled by the convert method.");
        }

        @Override
        public CastNode visit(FindFirstStep step) {
            return FindFirstNodeGen.create(step.getElementClass(), step.getDefaultValue());
        }

        @Override
        public CastNode visit(FilterStep step) {
            ArgumentFilter<Object, Boolean> filter = ArgumentFilterFactory.create(step.getFilter());
            // TODO: check error in step and use it instead of the default one
            return FilterNode.create(filter, /* TODO: isWarning?? */false, defaultCallObj, defaultMessage, defaultMessageArgs, boxPrimitives);
        }

        @Override
        public CastNode visit(NotNAStep step) {
            return null;
        }

        @Override
        public CastNode visit(AsVectorStep step) {
            return null;
        }

        @Override
        public CastNode visit(MapStep step) {
            return null;
        }

        @Override
        public CastNode visit(MapIfStep step) {
            return null;
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
            // TODO: create or filter...
            return null;
        }

        @Override
        public ArgumentFilter<Object, Boolean> visit(NotFilter filter) {
            ArgumentFilter<Object, Boolean> toNegate = filter.accept(this);
            // TODO: create not filter
            return null;
        }
    }

    private static final class MapperNodeFactory implements MapperVisitor<MapNode> {

        @Override
        public MapNode visit(MapToValue mapper) {
            final Object value = mapper.getValue();
            return MapNode.create(x -> value);
        }

        @Override
        public MapNode visit(MapByteToBoolean mapper) {
            return null;
        }

        @Override
        public MapNode visit(MapDoubleToInt mapper) {
            return null;
        }

        @Override
        public MapNode visit(MapToCharAt mapper) {
            return null;
        }
    }
}
