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

import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Provides fluent API for building the pipeline configuration {@link PipelineConfig}: default
 * error/warning message and handling of {@link RNull} and {@link RMissing}.
 */
public final class PipelineConfigBuilder {

    private final String argumentName;
    private MessageData defaultError;
    private MessageData defaultWarning;

    private Mapper<? super RMissing, ?> missingMapper;
    private Mapper<? super RNull, ?> nullMapper;
    private MessageData missingMsg;
    private MessageData nullMsg;

    PipelineConfigBuilder(String argumentName) {
        this.argumentName = argumentName;
        defaultError = new MessageData(null, RError.Message.INVALID_ARGUMENT, argumentName);
        defaultWarning = defaultError;
    }

    public PipelineConfig build() {
        return new PipelineConfig(argumentName, defaultError, defaultWarning, missingMapper, nullMapper, missingMsg, nullMsg);
    }

    void setDefaultError(MessageData defaultError) {
        this.defaultError = defaultError;
    }

    void setDefaultWarning(MessageData defaultWarning) {
        this.defaultWarning = defaultWarning;
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
}
