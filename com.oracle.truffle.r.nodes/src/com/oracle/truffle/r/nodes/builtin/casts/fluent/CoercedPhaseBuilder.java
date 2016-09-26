package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.casts.Filter;
import com.oracle.truffle.r.runtime.RError;
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
}
