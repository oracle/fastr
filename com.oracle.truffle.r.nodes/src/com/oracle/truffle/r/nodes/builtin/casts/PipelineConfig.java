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

import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineConfigBuilder;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;

/**
 * Immutable class with configuration of the pipeline. Create using {@link PipelineConfigBuilder}.
 * If there is no RNull/RMissing mapper, then RNull/RMissing should cause error, if there is
 * RNull/RMissing mapper and the RNull/RMissing message is set, then this message will be used as
 * warning if RNull/RMissing occurs.
 */
public class PipelineConfig {
    private final String argumentName;
    private final ErrorContext callObj;
    private final MessageData defaultError;
    private final MessageData defaultWarning;
    private boolean valueForwarding;

    public PipelineConfig(String argumentName, ErrorContext callObj, MessageData defaultError, MessageData defaultWarning, boolean valueForwarding) {
        this.defaultError = defaultError;
        this.defaultWarning = defaultWarning;
        this.valueForwarding = valueForwarding;
        this.argumentName = argumentName;
        this.callObj = callObj;
    }

    /**
     * Default message that should be used when no explicit default error/warning was set. For the
     * time being this is not configurable.
     */
    public MessageData getDefaultDefaultMessage() {
        return new MessageData(RError.Message.INVALID_ARGUMENT, argumentName);
    }

    public ErrorContext getCallObj() {
        return callObj;
    }

    public MessageData getDefaultError() {
        return defaultError;
    }

    public MessageData getDefaultWarning() {
        return defaultWarning;
    }

    public boolean getValueForwarding() {
        return valueForwarding;
    }
}
