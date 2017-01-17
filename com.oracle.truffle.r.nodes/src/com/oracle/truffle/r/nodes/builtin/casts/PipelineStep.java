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

import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Represents a single step in the cast pipeline. {@code PipelineStep}, {@code Mapper} and
 * {@code Filter} are only symbolic representation of the pipeline, these objects can be transformed
 * to something useful by using corresponding visitors, e.g. {@linek PipelineStepVisitor}. Steps can
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

    public abstract <D> D accept(PipelineStepVisitor<D> visitor);

    public interface PipelineStepVisitor<T> {
        T visit(FindFirstStep<?, ?> step);

        T visit(CoercionStep<?, ?> step);

        T visit(MapStep<?, ?> step);

        T visit(MapIfStep<?, ?> step);

        T visit(FilterStep<?, ?> step);

        T visit(NotNAStep<?> step);

        T visit(DefaultErrorStep<?> step);

        T visit(DefaultWarningStep<?> step);

        T visit(BoxPrimitiveStep<?> step);

        T visit(AttributableCoercionStep<?> step);
    }

    /**
     * Changes the current default error, which is used by steps/filters that do not have error
     * message set explicitly.
     */
    public abstract static class DefaultMessageStep<T> extends PipelineStep<T, T> {
        private final MessageData defaultMessage;

        public DefaultMessageStep(MessageData defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public MessageData getDefaultMessage() {
            return defaultMessage;
        }
    }

    public static final class DefaultErrorStep<T> extends DefaultMessageStep<T> {

        public DefaultErrorStep(MessageData defaultMessage) {
            super(defaultMessage);
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    public static final class DefaultWarningStep<T> extends DefaultMessageStep<T> {

        public DefaultWarningStep(MessageData defaultMessage) {
            super(defaultMessage);
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Boxes all primitive types (integer, string, double, byte) to {@link RAbstractVector}.
     */
    public static final class BoxPrimitiveStep<T> extends PipelineStep<T, T> {
        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
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
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Takes the first element of a vector. If the default value and message are provided, then if
     * the vector is empty, the message is used as a warning. If only default value is provided,
     * then if the vector is empty, the default value is returned without any warning. If the
     * default value is not provided, then error is raised if the vector is empty, the error message
     * chosen in the following order: provided message, explicitly set default error message using
     * {@link PipelineStep.DefaultErrorStep}, default find first message.
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
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Converts the value to a vector of given type (or a vector if Any, or Attributable). Null and
     * missing values are forwarded.
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

        public CoercionStep(RType type, boolean vectorCoercion) {
            this(type, vectorCoercion, false, false, false, true);
        }

        public CoercionStep(RType type, boolean vectorCoercion, boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
            this(type, vectorCoercion, preserveNames, preserveDimensions, preserveAttributes, true);
        }

        public CoercionStep(RType type, boolean vectorCoercion, boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean preserveNonVector) {
            this.type = type;
            this.vectorCoercion = vectorCoercion;
            this.preserveNames = preserveNames;
            this.preserveAttributes = preserveAttributes;
            this.preserveDimensions = preserveDimensions;
            this.preserveNonVector = preserveNonVector;
        }

        public RType getType() {
            return type;
        }

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
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
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
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
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }

    /**
     * Allows to execute on of given pipeline chains depending on the condition.
     */
    public static final class MapIfStep<T, R> extends PipelineStep<T, R> {
        private final Filter<?, ?> filter;
        private final PipelineStep<?, ?> trueBranch;
        private final PipelineStep<?, ?> falseBranch;

        public MapIfStep(Filter<?, ?> filter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
            this.filter = filter;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
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

        @Override
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
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
        public <D> D accept(PipelineStepVisitor<D> visitor) {
            return visitor.visit(this);
        }
    }
}
