package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Defines fluent API methods for building cast pipeline steps for a value that has been taken as a
 * head of a vector. This is the last phase in the pipeline building fluent API and comes after
 * {@link CoercedPhaseBuilder} phase.
 */
public final class HeadPhaseBuilder<T> extends ArgCastBuilder<T, HeadPhaseBuilder<T>> {

    public HeadPhaseBuilder(PipelineBuilder builder) {
        super(builder);
    }

    public <S> HeadPhaseBuilder<S> map(Mapper<T, S> mapFn) {
        // state().castBuilder().insert(state().index(), () -> MapNode.create(mapFn));
        pipelineBuilder().appendMap(mapFn);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, Mapper<S, R> trueBranchMapper, Mapper<T, ?> falseBranchMapper) {
        pipelineBuilder().appendMapIf(argFilter, trueBranchMapper, falseBranchMapper);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<S, ?> trueBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T, R> HeadPhaseBuilder<Object> mapIf(Filter<? super T, S> argFilter, PipelineStep<S, R> trueBranch, PipelineStep<T, ?> falseBranch) {
        pipelineBuilder().appendMapIf(argFilter, trueBranch, falseBranch);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendMustBeStep(argFilter, callObj, message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendMustBeStep(argFilter, null, message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Filter<? super T, S> argFilter) {
        return mustBe(argFilter, null, null, (Object[]) null);
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Class<S> cls, RError.Message message, Object... messageArgs) {
        mustBe(Predef.instanceOf(cls), message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S extends T> HeadPhaseBuilder<S> mustBe(Class<S> cls) {
        mustBe(Predef.instanceOf(cls));
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        shouldBe(Predef.instanceOf(cls), callObj, message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls, RError.Message message, Object... messageArgs) {
        shouldBe(Predef.instanceOf(cls), message, messageArgs);
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public <S> HeadPhaseBuilder<S> shouldBe(Class<S> cls) {
        shouldBe(Predef.instanceOf(cls));
        return new HeadPhaseBuilder<>(pipelineBuilder());
    }

    public HeadPhaseBuilder<T> notNA(RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(null, callObj, message, messageArgs);
        return this;
    }

    public HeadPhaseBuilder<T> notNA(RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(null, null, message, messageArgs);
        return this;
    }

    public HeadPhaseBuilder<T> notNA(T naReplacement, RBaseNode callObj, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(naReplacement, callObj, message, messageArgs);
        return this;
    }

    public HeadPhaseBuilder<T> notNA(T naReplacement, RError.Message message, Object... messageArgs) {
        pipelineBuilder().appendNotNA(naReplacement, null, message, messageArgs);
        return this;
    }

    public HeadPhaseBuilder<T> notNA() {
        pipelineBuilder().appendNotNA(null, null, null, null);
        return this;
    }

    public HeadPhaseBuilder<T> notNA(T naReplacement) {
        pipelineBuilder().appendNotNA(naReplacement, null, null, null);
        return this;
    }

}
