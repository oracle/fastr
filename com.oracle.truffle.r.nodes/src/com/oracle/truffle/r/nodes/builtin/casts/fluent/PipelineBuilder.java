package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep.TargetType;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultErrorStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.DefaultWarningStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapIfStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.NotNAStep;
import com.oracle.truffle.r.runtime.RError.Message;
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
        append(new CoercionStep<>(TargetType.Attributable, false, preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendAsVector(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean preserveNonVector) {
        append(new CoercionStep<>(TargetType.Any, true, preserveNames, preserveDimensions, preserveAttributes, preserveNonVector));
    }

    public void appendAsVector() {
        appendAsVector(false, false, false, false);
    }

    public void appendAsRawVector() {
        append(new CoercionStep<>(TargetType.Raw, true, false, false, false));
    }

    public void appendAsComplexVector() {
        append(new CoercionStep<>(TargetType.Complex, true, false, false, false));
    }

    public void appendAsStringVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new CoercionStep<>(TargetType.Character, true, preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendAsStringVector() {
        append(new CoercionStep<>(TargetType.Character, true, false, false, false));
    }

    public void appendAsLogicalVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new CoercionStep<>(TargetType.Logical, true, preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendAsDoubleVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new CoercionStep<>(TargetType.Double, true, preserveNames, dimensionsPreservation, attrPreservation));
    }

    public void appendAsIntegerVector(boolean preserveNames, boolean dimensionsPreservation, boolean attrPreservation) {
        append(new CoercionStep<>(TargetType.Integer, true, preserveNames, dimensionsPreservation, attrPreservation));
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
