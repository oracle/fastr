package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Allows to convert find first into a valid step when used in {@code chain}, for example
 * {@code chain(findFirst().stringElement())}.
 */
public final class FindFirstNodeBuilder {
    private final RBaseNode callObj;
    private final Message message;
    private final Object[] messageArgs;

    public FindFirstNodeBuilder(RBaseNode callObj, Message message, Object[] messageArgs) {
        this.callObj = callObj;
        this.message = message;
        this.messageArgs = messageArgs;
    }

    private <V, E> PipelineStep<V, E> create(Class<?> elementClass, Object defaultValue) {
        return new FindFirstStep<>(defaultValue, elementClass, new MessageData(callObj, message, messageArgs));
    }

    public PipelineStep<RAbstractLogicalVector, Byte> logicalElement() {
        return create(Byte.class, null);
    }

    public PipelineStep<RAbstractLogicalVector, Byte> logicalElement(byte defaultValue) {
        return create(Byte.class, defaultValue);
    }

    public PipelineStep<RAbstractDoubleVector, Double> doubleElement() {
        return create(Double.class, null);
    }

    public PipelineStep<RAbstractDoubleVector, Double> doubleElement(double defaultValue) {
        return create(Double.class, defaultValue);
    }

    public PipelineStep<RAbstractIntVector, Integer> integerElement() {
        return create(Integer.class, null);
    }

    public PipelineStep<RAbstractIntVector, Integer> integerElement(int defaultValue) {
        return create(Integer.class, defaultValue);
    }

    public PipelineStep<RAbstractStringVector, String> stringElement() {
        return create(String.class, null);
    }

    public PipelineStep<RAbstractStringVector, String> stringElement(String defaultValue) {
        return create(String.class, defaultValue);
    }

    public PipelineStep<RAbstractComplexVector, RComplex> complexElement() {
        return create(String.class, null);
    }

    public PipelineStep<RAbstractComplexVector, RComplex> complexElement(RComplex defaultValue) {
        return create(String.class, defaultValue);
    }

    public PipelineStep<RAbstractVector, Object> objectElement() {
        return create(Object.class, null);
    }

    public PipelineStep<RAbstractVector, Object> objectElement(Object defaultValue) {
        return create(Object.class, defaultValue);
    }
}
