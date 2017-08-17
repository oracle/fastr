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
package com.oracle.truffle.r.nodes.builtin.casts.fluent;

import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Provides fluent API for building the pipeline configuration {@link PipelineConfig}: default
 * error/warning message and handling of {@link RNull} and {@link RMissing}.
 */
public final class PipelineConfigBuilder {

    private final String argumentName;
    private MessageData defaultError;
    private MessageData defaultWarning;

    private boolean valueForwarding = true;
    private boolean castForeign = true;

    public PipelineConfigBuilder(String argumentName) {
        this.argumentName = argumentName;
        defaultError = new MessageData(RError.Message.INVALID_ARGUMENT, argumentName);
        defaultWarning = defaultError;
    }

    public PipelineConfig build() {
        return new PipelineConfig(argumentName, defaultError, defaultWarning, valueForwarding, castForeign);
    }

    void setDefaultError(MessageData defaultError) {
        this.defaultError = defaultError;
    }

    void setDefaultWarning(MessageData defaultWarning) {
        this.defaultWarning = defaultWarning;
    }

    public PipelineConfigBuilder setValueForwarding(boolean flag) {
        this.valueForwarding = flag;
        return this;
    }

    public PipelineConfigBuilder setCastForeignObjects(boolean flag) {
        this.castForeign = flag;
        return this;
    }
}
