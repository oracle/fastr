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

import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;

/**
 * Holds a chain of {@link PipelineStep} instances, allows to add new steps at the end. This class
 * can be used to create a chain outside of a context of {@link PipelineBuilder}, it can be
 * constructed using the {@code chain} method from {@code Predef}.
 *
 * @see Predef#chain
 */
public final class ChainBuilder<T> {
    private final PipelineStep<?, ?> firstStep;
    private PipelineStep<?, ?> lastStep;

    // TODO: can be package private once Predef is moved to fluent package
    public ChainBuilder(PipelineStep<?, ?> firstStep) {
        assert firstStep != null : "firstStep must not be null";
        this.firstStep = firstStep;
        lastStep = firstStep;
    }

    PipelineStep<?, ?> getFirstStep() {
        return firstStep;
    }

    void addStep(PipelineStep<?, ?> nextStep) {
        lastStep.setNext(nextStep);
        lastStep = nextStep;
    }

    @SuppressWarnings("overloads")
    public ChainBuilder<T> with(PipelineStep<?, ?> nextStep) {
        addStep(nextStep);
        return this;
    }

    @SuppressWarnings("overloads")
    public ChainBuilder<T> with(Mapper<?, ?> mapper) {
        addStep(new MapStep<>(mapper));
        return this;
    }

    public PipelineStep<?, ?> end() {
        return firstStep;
    }
}
