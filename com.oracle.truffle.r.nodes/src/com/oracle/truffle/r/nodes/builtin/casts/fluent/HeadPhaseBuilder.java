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
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.runtime.RError;

/**
 * Defines fluent API methods for building cast pipeline steps for a value that has been taken as a
 * head of a vector. This is the last phase in the pipeline building fluent API and comes after
 * {@link CoercedPhaseBuilder} phase.
 */
@SuppressWarnings("unchecked")
public final class HeadPhaseBuilder<T> extends ArgCastBuilder<T, HeadPhaseBuilder<T>> {

    public HeadPhaseBuilder(PipelineBuilder builder) {
        super(builder);
    }

    public <S> HeadPhaseBuilder<S> map(Mapper<T, S> mapFn) {
        // state().castBuilder().insert(state().index(), () -> MapNode.create(mapFn));
        pipelineBuilder().appendMap(mapFn);
        return (HeadPhaseBuilder<S>) this;
    }

    public HeadPhaseBuilder<Object> returnIf(Filter<? super T, ?> argFilter) {
        pipelineBuilder().appendMapIf(argFilter, (PipelineStep<?, ?>) null, (PipelineStep<?, ?>) null, true);
        return (HeadPhaseBuilder<Object>) this;
    }

    public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, false);
        return (HeadPhaseBuilder<Object>) this;
    }

    public <S extends T, R> HeadPhaseBuilder<T> returnIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, true);
        return this;
    }

    public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper, false);
        return (HeadPhaseBuilder<Object>) this;
    }

    public <S extends T, R> HeadPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper, true);
        return (HeadPhaseBuilder<Object>) this;
    }

    public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<S, ?> trueBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, false);
        return (HeadPhaseBuilder<Object>) this;
    }

    public <S extends T, R> HeadPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, PipelineStep<S, ?> trueBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, true);
        return (HeadPhaseBuilder<Object>) this;
    }

    public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<S, R> trueBranch, PipelineStep<T, ?> falseBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch, false);
        return (HeadPhaseBuilder<Object>) this;
    }

    public <S extends T, R> HeadPhaseBuilder<Object> returnIf(Filter<? super T, S> argFilter, PipelineStep<S, R> trueBranch, PipelineStep<T, ?> falseBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch, true);
        return (HeadPhaseBuilder<Object>) this;
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendMustBeStep(argFilter, message, messageArgs);
        return (HeadPhaseBuilder<S>) this;
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter) {
        return mustBe(argFilter, null, null, (Object[]) null);
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
        mustBe(Predef.instanceOf(cls), message, messageArgs);
        return (HeadPhaseBuilder<S>) this;
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Class<S> cls) {
        mustBe(Predef.instanceOf(cls));
        return (HeadPhaseBuilder<S>) this;
    }

    public <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls, RError.Message message, Object... messageArgs) {
        shouldBe(Predef.instanceOf(cls), message, messageArgs);
        return (HeadPhaseBuilder<S>) this;
    }

    public <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls) {
        shouldBe(Predef.instanceOf(cls));
        return (HeadPhaseBuilder<S>) this;
    }

    public HeadPhaseBuilder<T> mustNotBeNA(RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(null, message, messageArgs);
        return this;
    }

    public HeadPhaseBuilder<T> shouldNotBeNA(T naReplacement, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(naReplacement, message, messageArgs);
        return this;
    }

    public HeadPhaseBuilder<T> mustNotBeNA() {
        pipelineBuilder().appendNotNA(null, null, null);
        return this;
    }

    public HeadPhaseBuilder<T> replaceNA(T naReplacement) {
        pipelineBuilder().appendNotNA(naReplacement, null, null);
        return this;
    }
}
