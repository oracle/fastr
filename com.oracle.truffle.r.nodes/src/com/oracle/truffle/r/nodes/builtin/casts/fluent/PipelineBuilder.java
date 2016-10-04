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

import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.AttributableCoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultErrorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultWarningStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Class that holds the data for a pipeline for a single parameter. It holds the cast pipeline steps
 * created so far and some data related to the selected argument for which we are creating the
 * pipeline.
 */
public final class PipelineBuilder {

    private final PipelineConfigBuilder pcb;
    private ChainBuilder<?> chainBuilder;

    public PipelineBuilder(PipelineConfigBuilder pcb) {
        this.pcb = pcb;
    }

    // TODO: can be package private once the legacy API is removed
    public void append(PipelineStep<?, ?> step) {
        if (chainBuilder == null) {
            chainBuilder = new ChainBuilder<>(step);
        } else {
            chainBuilder.addStep(step);
        }
    }

    public PipelineConfigBuilder getPipelineConfig() {
        return pcb;
    }

    public PipelineStep<?, ?> getFirstStep() {
        return chainBuilder != null ? chainBuilder.getFirstStep() : null;
    }

    public void appendFindFirst(Object defaultValue, Class<?> elementClass, RBaseNode callObj, Message message, Object[] messageArgs) {
        append(new FindFirstStep<>(defaultValue, elementClass, createMessage(callObj, message, messageArgs)));
    }

    public void appendAsAttributable(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new AttributableCoercionStep<>(preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendAsVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean preserveNonVector) {
        append(new CoercionStep<>(RType.Any, true, preserveNames, preserveDimensions, preserveAttributes, preserveNonVector));
    }

    public void appendAsVector() {
        appendAsVector(false, false, false, true);
    }

    public void appendAsRawVector() {
        append(new CoercionStep<>(RType.Raw, true, false, false, false));
    }

    public void appendAsComplexVector() {
        append(new CoercionStep<>(RType.Complex, true, false, false, false));
    }

    public void appendAsStringVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new CoercionStep<>(RType.Character, true, preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendAsStringVector() {
        append(new CoercionStep<>(RType.Character, true, false, false, false));
    }

    public void appendAsLogicalVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new CoercionStep<>(RType.Logical, true, preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendAsDoubleVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new CoercionStep<>(RType.Double, true, preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendAsIntegerVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new CoercionStep<>(RType.Integer, true, preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendNotNA(Object naReplacement, RBaseNode callObj, Message message, Object[] messageArgs) {
        append(new NotNAStep<>(naReplacement, createMessage(callObj, message, messageArgs)));
    }

    public void appendMapIf(Filter<?, ?> argFilter, Mapper<?, ?> trueBranchMapper) {
        appendMapIf(argFilter, trueBranchMapper, null);
    }

    public void appendMapIf(Filter<?, ?> argFilter, Mapper<?, ?> trueBranchMapper, Mapper<?, ?> falseBranchMapper) {
        appendMapIf(argFilter, new MapStep<>(trueBranchMapper), falseBranchMapper == null ? null : new MapStep<>(falseBranchMapper));
    }

    public void appendMapIf(Filter<?, ?> argFilter, PipelineStep<?, ?> trueBranch) {
        appendMapIf(argFilter, trueBranch, null);
    }

    public void appendMapIf(Filter<?, ?> argFilter, PipelineStep<?, ?> trueBranch, PipelineStep<?, ?> falseBranch) {
        append(new MapIfStep<>(argFilter, trueBranch, falseBranch));
    }

    public void appendMap(Mapper<?, ?> mapFn) {
        append(new MapStep<>(mapFn));
    }

    public void appendMustBeStep(Filter<?, ?> argFilter, RBaseNode callObj, Message message, Object[] messageArgs) {
        append(new FilterStep<>(argFilter, createMessage(callObj, message, messageArgs), false));
    }

    public void appendShouldBeStep(Filter<?, ?> argFilter, Message message, Object[] messageArgs) {
        appendShouldBeStep(argFilter, null, message, messageArgs);
    }

    public void appendShouldBeStep(Filter<?, ?> argFilter, RBaseNode callObj, Message message, Object[] messageArgs) {
        append(new FilterStep<>(argFilter, createMessage(callObj, message, messageArgs), true));
    }

    public void appendDefaultWarningStep(RBaseNode callObj, Message message, Object[] args) {
        append(new DefaultWarningStep<>(createMessage(callObj, message, args)));
    }

    public void appendDefaultErrorStep(RBaseNode callObj, Message message, Object[] args) {
        append(new DefaultErrorStep<>(createMessage(callObj, message, args)));
    }

    private MessageData createMessage(RBaseNode callObj, Message message, Object[] messageArgs) {
        return message == null ? null : new MessageData(callObj, message, messageArgs);
    }
}
