package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;

/**
 * Holds a chain of {@link PipelineStep} instances, allows to add new steps at the end. This class
 * can be used to create a chain outside of a context of {@link PipelineBuilder}, it can be
 * constructed using the {@code chain} method from {@code Predef}.
 *
 * {@see Predef#chain}
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
