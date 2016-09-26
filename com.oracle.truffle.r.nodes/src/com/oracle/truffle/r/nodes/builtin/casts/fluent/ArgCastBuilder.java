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
package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import java.util.function.Function;
import java.util.function.Supplier;

import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CustomNodeStep;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Defines fluent API methods for building cast pipeline steps that are available in every phase of
 * the pipeline building, i.e. in {@link }
 */
@SuppressWarnings("unchecked")
public class ArgCastBuilder<T, THIS> {

    private final PipelineBuilder builder;

    public ArgCastBuilder(PipelineBuilder builder) {
        this.builder = builder;
    }

    public PipelineBuilder pipelineBuilder() {
        return builder;
    }

    public THIS defaultError(RBaseNode callObj, RError.Message message, Object... args) {
        pipelineBuilder().appendDefaultErrorStep(callObj, message, args);
        return (THIS) this;
    }

    public THIS defaultError(RError.Message message, Object... args) {
        defaultError(null, message, args);
        return (THIS) this;
    }

    public THIS defaultWarning(RBaseNode callObj, RError.Message message, Object... args) {
        pipelineBuilder().appendDefaultWarningStep(callObj, message, args);
        return (THIS) this;
    }

    public THIS shouldBe(Filter<? super T, ?> argFilter, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendShouldBeStep(argFilter, message, messageArgs);
        return (THIS) this;
    }

    public THIS shouldBe(Filter<? super T, ?> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendShouldBeStep(argFilter, callObj, message, messageArgs);
        return (THIS) this;
    }

    public THIS shouldBe(Filter<? super T, ?> argFilter) {
        pipelineBuilder().appendShouldBeStep(argFilter, null, null, null);
        return (THIS) this;
    }

    /**
     * Custom nodes in cast pipeline block optimisations and analysis, use them sparsely.
     */
    public THIS customNode(Supplier<CastNode> castNodeFactory) {
        pipelineBuilder().append(new CustomNodeStep<>(castNodeFactory));
        return (THIS) this;
    }

    public <R, THAT extends ArgCastBuilder<R, THAT>> THAT alias(Function<THIS, THAT> aliaser) {
        return aliaser.apply((THIS) this);
    }

}
