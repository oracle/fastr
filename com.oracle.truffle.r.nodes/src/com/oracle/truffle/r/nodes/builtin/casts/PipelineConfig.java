/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.casts;

import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentFilterFactory;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentFilterFactoryImpl;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentMapperFactory;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentMapperFactoryImpl;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineConfigBuilder;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Immutable class with configuration of the pipeline. Create using {@link PipelineConfigBuilder}.
 * If there is no RNull/RMissing mapper, then RNull/RMissing should cause error, if there is
 * RNull/RMissing mapper and the RNull/RMissing message is set, then this message will be used as
 * warning if RNull/RMissing occurs.
 */
public class PipelineConfig {
    private static ArgumentFilterFactory filterFactory = ArgumentFilterFactoryImpl.INSTANCE;
    private static ArgumentMapperFactory mapperFactory = ArgumentMapperFactoryImpl.INSTANCE;

    private final String argumentName;
    private final MessageData defaultError;
    private final MessageData defaultWarning;
    private final Mapper<? super RMissing, ?> missingMapper;
    private final Mapper<? super RNull, ?> nullMapper;
    private final MessageData missingMsg;
    private final MessageData nullMsg;
    private boolean valueForwarding;

    public PipelineConfig(String argumentName, MessageData defaultError, MessageData defaultWarning, Mapper<? super RMissing, ?> missingMapper, Mapper<? super RNull, ?> nullMapper,
                    boolean valueForwarding,
                    MessageData missingMsg,
                    MessageData nullMsg) {
        this.defaultError = defaultError;
        this.defaultWarning = defaultWarning;
        this.missingMapper = missingMapper;
        this.nullMapper = nullMapper;
        this.valueForwarding = valueForwarding;
        this.missingMsg = missingMsg;
        this.nullMsg = nullMsg;
        this.argumentName = argumentName;
    }

    /**
     * Default message that should be used when no explicit default error/warning was set. For the
     * time being this is not configurable.
     */
    public MessageData getDefaultDefaultMessage() {
        return new MessageData(null, RError.Message.INVALID_ARGUMENT, argumentName);
    }

    public MessageData getDefaultError() {
        return defaultError;
    }

    public MessageData getDefaultWarning() {
        return defaultWarning;
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

    public boolean getValueForwarding() {
        return valueForwarding;
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
