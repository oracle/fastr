package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentFilterFactory;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentFilterFactoryImpl;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentMapperFactory;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentMapperFactoryImpl;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Provides fluent API for building the pipeline configuration: default error/warning message and
 * handling of {@link RNull} and {@link RMissing}.
 */
public final class PipelineConfigBuilder {

    // TODO: create immutable class representing the configuration that this builder will build

    private static ArgumentFilterFactory filterFactory = ArgumentFilterFactoryImpl.INSTANCE;
    private static ArgumentMapperFactory mapperFactory = ArgumentMapperFactoryImpl.INSTANCE;

    private final String argumentName;
    private MessageData defaultError;
    private MessageData defaultWarning;

    private Mapper<? super RMissing, ?> missingMapper = null;
    private Mapper<? super RNull, ?> nullMapper = null;
    private MessageData missingMsg;
    private MessageData nullMsg;

    // Note: to be removed with legacy API
    private boolean wasLegacyAsVectorCall = false;

    public PipelineConfigBuilder(String argumentName) {
        defaultError = new MessageData(null, RError.Message.INVALID_ARGUMENT, argumentName);
        defaultWarning = defaultError;
        this.argumentName = argumentName;
    }

    public void setWasLegacyAsVectorCall() {
        wasLegacyAsVectorCall = true;
    }

    public boolean wasLegacyAsVectorCall() {
        return wasLegacyAsVectorCall;
    }

    public String getArgumentName() {
        return argumentName;
    }

    public void setDefaultError(MessageData defaultError) {
        this.defaultError = defaultError;
    }

    /**
     * The preinitialization phase where one can configure RNull and RMissing handling may also
     * define default error, in such case it is remembered the {@link PipelineConfigBuilder} and
     * also set default error step is added to the pipeline.The default error saved here is used for
     * RNull and RMissing handling. It may be null, if no default error was set explicitly.
     */
    public MessageData getDefaultError() {
        return defaultError;
    }

    public void setDefaultWarning(MessageData defaultWarning) {
        this.defaultWarning = defaultWarning;
    }

    /**
     * The same as in {@link #getDefaultError()} applies for default warning.
     */
    public MessageData getDefaultWarning() {
        return defaultWarning;
    }

    /**
     * Default message that should be used when no explicit default error/warning was set. For the
     * time being this is not configurable.
     */
    public MessageData getDefaultDefaultMessage() {
        return new MessageData(null, RError.Message.INVALID_ARGUMENT, argumentName);
    }

    public Mapper<? super RMissing, ?> getMissingMapper() {
        return missingMapper;
    }

    public Mapper<? super RNull, ?> getNullMapper() {
        return nullMapper;
    }

    public MessageData getMissingMessage() {
        return missingMsg;
    }

    public MessageData getNullMessage() {
        return nullMsg;
    }

    public PipelineConfigBuilder mustNotBeMissing(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
        missingMapper = null;
        missingMsg = new MessageData(callObj, errorMsg, msgArgs);
        return this;
    }

    public PipelineConfigBuilder mapMissing(Mapper<? super RMissing, ?> mapper) {
        missingMapper = mapper;
        missingMsg = null;
        return this;
    }

    public PipelineConfigBuilder mapMissing(Mapper<? super RMissing, ?> mapper, RBaseNode callObj, RError.Message warningMsg, Object... msgArgs) {
        missingMapper = mapper;
        missingMsg = new MessageData(callObj, warningMsg, msgArgs);
        return this;
    }

    public PipelineConfigBuilder allowMissing() {
        return mapMissing(Predef.missingConstant());
    }

    public PipelineConfigBuilder allowMissing(RBaseNode callObj, RError.Message warningMsg, Object... msgArgs) {
        return mapMissing(Predef.missingConstant(), callObj, warningMsg, msgArgs);
    }

    public PipelineConfigBuilder mustNotBeNull(RBaseNode callObj, RError.Message errorMsg, Object... msgArgs) {
        nullMapper = null;
        nullMsg = new MessageData(callObj, errorMsg, msgArgs);
        return this;
    }

    public PipelineConfigBuilder mapNull(Mapper<? super RNull, ?> mapper) {
        nullMapper = mapper;
        nullMsg = null;
        return this;
    }

    public PipelineConfigBuilder mapNull(Mapper<? super RNull, ?> mapper, RBaseNode callObj, RError.Message warningMsg, Object... msgArgs) {
        nullMapper = mapper;
        nullMsg = new MessageData(callObj, warningMsg, msgArgs);
        return this;
    }

    public PipelineConfigBuilder allowNull() {
        return mapNull(Predef.nullConstant());
    }

    public PipelineConfigBuilder allowNull(RBaseNode callObj, RError.Message warningMsg, Object... msgArgs) {
        return mapNull(Predef.nullConstant(), callObj, warningMsg, msgArgs);
    }

    public static ArgumentFilterFactory getFilterFactory() {
        return filterFactory;
    }

    public static ArgumentMapperFactory getMapperFactory() {
        return mapperFactory;
    }

    public static void setFilterFactory(ArgumentFilterFactory ff) {
        filterFactory = ff;
    }

    public static void setMapperFactory(ArgumentMapperFactory mf) {
        mapperFactory = mf;
    }
}
