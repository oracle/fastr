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
