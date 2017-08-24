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

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Represents a single step in the cast pipeline. {@code PipelineStep}, {@code Mapper} and
 * {@code Filter} are only symbolic representation of the pipeline, these objects can be transformed
 * to something useful by using corresponding visitors, e.g. {@link PipelineStepVisitor}. Steps can
 * be chained as a linked list by setting the next step in the chain using
 * {@link #setNext(PipelineStep)}. The order of steps should be the same as the order of cast
 * pipeline API invocations.
 */
public abstract class PipelineStep<T, R> {

    private PipelineStep<?, ?> next;

    public final PipelineStep<?, ?> getNext() {
        return next;
    }

    public final PipelineStep<?, ?> setNext(PipelineStep<?, ?> next) {
        this.next = next;
        return this;
    }

    public abstract <D> D accept(PipelineStepVisitor<D> visitor, D previous);

    public <D> D acceptPipeline(PipelineStepVisitor<D> visitor, D initial) {
        PipelineStep<?, ?> curStep = this;
        D result = initial;
        while (curStep != null) {
            result = curStep.accept(visitor, result);
            curStep = curStep.getNext();
        }
        return result;
    }

    public interface PipelineStepVisitor<T> {
        T visit(FindFirstStep<?, ?> step, T previous);

        T visit(CoercionStep<?, ?> step, T previous);

        T visit(MapStep<?, ?> step, T previous);

        T visit(MapIfStep<?, ?> step, T previous);

        T visit(FilterStep<?, ?> step, T previous);

        T visit(NotNAStep<?> step, T previous);

        T visit(BoxPrimitiveStep<?> step, T previous);

        T visit(AttributableCoercionStep<?> step, T previous);
    }

    /**
     * Boxes all primitive types (integer, string, double, byte) to {@link RAbstractVector}.
     */
    public static final class BoxPrimitiveStep<T> extends PipelineStep<T, T> {
        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    /**
     * If the replacement is set (!= null), then maps NA values to the replacement, otherwise raises
     * given error on NA value of any type. If both the replacement and the message are set, then
     * the replacement is accompanied by a warning.
     */
    public static final class NotNAStep<T> extends PipelineStep<T, T> {
        private final MessageData message;
        private final Object replacement;

        public NotNAStep(Object replacement, MessageData message) {
            assert !(replacement instanceof RBaseNode || replacement instanceof RError.Message);
            this.replacement = replacement;
            this.message = message;
        }

        public MessageData getMessage() {
            return message;
        }

        public Object getReplacement() {
            return replacement;
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    /**
     * Takes the first element of a vector. If the default value and message are provided, then if
     * the vector is empty, the message is used as a warning. If only default value is provided,
     * then if the vector is empty, the default value is returned without any warning. If the
     * default value is not provided, then error is raised if the vector is empty, the error message
     * chosen in the following order: provided message, explicitly set default error message,
     * default find first message.
     */
    public static final class FindFirstStep<V, E> extends PipelineStep<V, E> {
        private final MessageData error;
        private final Object defaultValue;
        private final Class<?> elementClass;

        public FindFirstStep(Object defaultValue, Class<?> elementClass, MessageData error) {
            this.defaultValue = defaultValue;
            this.elementClass = elementClass;
            this.error = error;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public Class<?> getElementClass() {
            return elementClass;
        }

        public MessageData getError() {
            return error;
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    /**
     * Converts the value to a vector of given type (or a vector if Any, or Attributable). Null,
     * missing and primitive of given type are forwarded. Primitive values of other types are
     * converted to primitive value of the target type.
     */
    public static final class CoercionStep<T, V> extends PipelineStep<T, V> {
        public final RType type;
        public final boolean preserveNames;
        public final boolean preserveDimensions;
        public final boolean preserveAttributes;

        /**
         * Whether RNull/RMissing should be preserved, or converted to an empty list.
         */
        public final boolean preserveNonVector;

        /**
         * Coercion to scalar or vector type.
         */
        public final boolean vectorCoercion;

        /**
         * Allows the cast node to create and use wrappers for vectors. Only use if you know the
         * vector to be casted won't escape and preferably if the vector is just used read-only.
         */
        public final boolean useClosure;

        public CoercionStep(RType type, boolean vectorCoercion) {
            this(type, vectorCoercion, false, false, false, true, false);
        }

        public CoercionStep(RType type, boolean vectorCoercion, boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            this(type, vectorCoercion, preserveNames, preserveDimensions, preserveAttributes, true, false);
        }

        public CoercionStep(RType type, boolean vectorCoercion, boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean preserveNonVector, boolean useClosure) {
            this.type = type;
            this.vectorCoercion = vectorCoercion;
            this.preserveNames = preserveNames;
            this.preserveAttributes = preserveAttributes;
            this.preserveDimensions = preserveDimensions;
            this.preserveNonVector = preserveNonVector;
            this.useClosure = useClosure;
        }

        public RType getType() {
            return type;
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    /**
     * Converts the value to a {@link com.oracle.truffle.r.runtime.data.RAttributable} instance.
     */
    public static final class AttributableCoercionStep<T> extends PipelineStep<T, RAttributable> {
        public final boolean preserveNames;
        public final boolean preserveDimensions;
        public final boolean preserveAttributes;

        public AttributableCoercionStep(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            this.preserveNames = preserveNames;
            this.preserveDimensions = preserveDimensions;
            this.preserveAttributes = preserveAttributes;
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    public static final class MapStep<T, R> extends PipelineStep<T, R> {
        private final Mapper<?, ?> mapper;

        public MapStep(Mapper<?, ?> mapper) {
            this.mapper = mapper;
        }

        public Mapper<?, ?> getMapper() {
            return mapper;
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }

    /**
     * Allows to execute on of given pipeline chains depending on the condition.
     */
    public static final class MapIfStep<T, R> extends PipelineStep<T, R> {
        private final Filter<?, ?> filter;
        private final PipelineStep<?, ?> trueBranch;
        private final PipelineStep<?, ?> falseBranch;
        private final boolean returns;

        public MapIfStep(Filter<?, ?> filter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch, boolean returns) {
            this.filter = filter;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
            this.returns = returns;
        }

        public Filter<?, ?> getFilter() {
            return filter;
        }

        public PipelineStep<?, ?> getTrueBranch() {
            return trueBranch;
        }

        public PipelineStep<?, ?> getFalseBranch() {
            return falseBranch;
        }

        public boolean isReturns() {
            return returns;
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }

        public MapIfStep<T, R> withoutReturns() {
            if (!returns) {
                return this;
            }
            return new MapIfStep<>(filter, trueBranch, falseBranch, false);
        }
    }

    /**
     * Raises an error if the value does not conform to the given filter.
     */
    public static final class FilterStep<T, R extends T> extends PipelineStep<T, R> {
        private final Filter<?, ?> filter;
        private final MessageData message;
        private final boolean isWarning;

        public FilterStep(Filter<?, ?> filter, MessageData message, boolean isWarning) {
            this.filter = filter;
            this.message = message;
            this.isWarning = isWarning;
        }

        public Filter<?, ?> getFilter() {
            return filter;
        }

        public MessageData getMessage() {
            return message;
        }

        public boolean isWarning() {
            return isWarning;
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor, D previous) {
            return visitor.visit(this, previous);
        }
    }
}
