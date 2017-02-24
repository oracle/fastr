/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Defines fluent API methods for building cast pipeline steps for argument that has been cast to a
 * specific type. Any method that represents casting or mapping of the argument to a another
 * specific type returns different instance of {@link CoercedPhaseBuilder} with its generic
 * parameters set accordingly. Methods that map vectors to single element return
 * {@link HeadPhaseBuilder}.
 */
public final class CoercedPhaseBuilder<T extends RAbstractVector, S> extends ArgCastBuilder<T, CoercedPhaseBuilder<T, S>> {

    private final Class<?> elementClass;

    public CoercedPhaseBuilder(PipelineBuilder builder, Class<?> elementClass) {
        super(builder);
        this.elementClass = elementClass;
    }

    /**
     * The inserted cast node returns the default value if the input vector is empty. It also
     * reports the warning message.
     */
    public HeadPhaseBuilder<S> findFirst(S defaultValue, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendFindFirst(defaultValue, elementClass, null, message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public HeadPhaseBuilder<S> findFirst(S defaultValue, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendFindFirst(defaultValue, elementClass, callObj, message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    /**
     * The inserted cast node raises an error if the input vector is empty.
     */
    public HeadPhaseBuilder<S> findFirst(RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendFindFirst(null, elementClass, null, message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public HeadPhaseBuilder<S> findFirst(RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendFindFirst(null, elementClass, callObj, message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    /**
     * The inserted cast node raises the public error, if defined, or RError.Message.LENGTH_ZERO
     * error if the input vector is empty.
     */
    public HeadPhaseBuilder<S> findFirst() {
        pipelineBuilder().appendFindFirst(null, elementClass, null, null, null);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    /**
     * The inserted cast node returns the default value if the input vector is empty. It reports no
     * warning message.
     */
    public HeadPhaseBuilder<S> findFirst(S defaultValue) {
        assert defaultValue != null : "defaultValue cannot be null";
        pipelineBuilder().appendFindFirst(defaultValue, elementClass, null, null, null);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public HeadPhaseBuilder<S> findFirstOrNull() {
        pipelineBuilder().appendFindFirst(RNull.instance, elementClass, null, null, null);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendMustBeStep(argFilter, callObj, message, messageArgs);
        return this;
    }

    public CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendMustBeStep(argFilter, null, message, messageArgs);
        return this;
    }

    public CoercedPhaseBuilder<T, S> mustBe(Filter<? super T, ? extends T> argFilter) {
        return mustBe(argFilter, null, null, (Object[]) null);
    }

    public <R extends T> CoercedPhaseBuilder<R, S> mustBe(Class<R> cls) {
        return new CoercedPhaseBuilder<R, S>(pipelineBuilder(), elementClass).mustBe(Predef.instanceOf(cls));
    }
}
