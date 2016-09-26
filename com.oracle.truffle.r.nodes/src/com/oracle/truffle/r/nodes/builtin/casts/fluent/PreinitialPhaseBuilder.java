package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import java.util.function.Consumer;

import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Adds methods to {@link InitialPhaseBuilder} that allow to set up the pipeline configuration.
 * Invocation of some methods means that the pre-initilization phase has been finishes, i.e.
 * pipeline fully configured, those methods return this object cast to {@link InitialPhaseBuilder}
 * so that user then cannot invoke methods that change the pipeline configuration. Any method from
 * {@link InitialPhaseBuilder} returns that type, so once the user steps outside the configuration,
 * there is no way to invoke configuration related methods defined here.
 */
public final class PreinitialPhaseBuilder<T> extends InitialPhaseBuilder<T> {

    public PreinitialPhaseBuilder(PipelineBuilder pipelineBuilder) {
        super(pipelineBuilder);
    }

    public PreinitialPhaseBuilder<T> conf(Consumer<PipelineConfigBuilder> cfgLambda) {
        cfgLambda.accept(pipelineBuilder().getPipelineConfig());
        return this;
    }

    public InitialPhaseBuilder<T> allowNull() {
        return conf(c -> c.allowNull());
    }

    public InitialPhaseBuilder<T> mustNotBeNull() {
        return conf(c -> c.mustNotBeNull(null, null, (Object[]) null));
    }

    public InitialPhaseBuilder<T> mustNotBeNull(RError.Message errorMsg, Object... msgArgs) {
        return conf(c -> c.mustNotBeNull(null, errorMsg, msgArgs));
    }

    public InitialPhaseBuilder<T> mustNotBeNull(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
        return conf(c -> c.mustNotBeNull(callObj, errorMsg, msgArgs));
    }

    public InitialPhaseBuilder<T> mapNull(Mapper<? super RNull, ?> mapper) {
        return conf(c -> c.mapNull(mapper));
    }

    public InitialPhaseBuilder<T> allowMissing() {
        return conf(c -> c.allowMissing());
    }

    public InitialPhaseBuilder<T> mustNotBeMissing() {
        return conf(c -> c.mustNotBeMissing(null, null, (Object[]) null));
    }

    public InitialPhaseBuilder<T> mustNotBeMissing(RError.Message errorMsg, Object... msgArgs) {
        return conf(c -> c.mustNotBeMissing(null, errorMsg, msgArgs));
    }

    public InitialPhaseBuilder<T> mustNotBeMissing(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
        return conf(c -> c.mustNotBeMissing(callObj, errorMsg, msgArgs));
    }

    public InitialPhaseBuilder<T> mapMissing(Mapper<? super RMissing, ?> mapper) {
        return conf(c -> c.mapMissing(mapper));
    }

    public InitialPhaseBuilder<T> allowNullAndMissing() {
        return conf(c -> c.allowMissing().allowNull());
    }

    @Override
    public PreinitialPhaseBuilder<T> defaultError(RBaseNode callObj, RError.Message message, Object... args) {
        pipelineBuilder().getPipelineConfig().setDefaultError(new MessageData(callObj, message, args));
        pipelineBuilder().appendDefaultErrorStep(callObj, message, args);
        return this;
    }

    @Override
    public PreinitialPhaseBuilder<T> defaultError(Message message, Object... args) {
        defaultError(null, message, args);
        return this;
    }

    @Override
    public PreinitialPhaseBuilder<T> defaultWarning(RBaseNode callObj, Message message, Object... args) {
        pipelineBuilder().getPipelineConfig().setDefaultWarning(new MessageData(callObj, message, args));
        pipelineBuilder().appendDefaultWarningStep(callObj, message, args);
        return this;
    }
}
