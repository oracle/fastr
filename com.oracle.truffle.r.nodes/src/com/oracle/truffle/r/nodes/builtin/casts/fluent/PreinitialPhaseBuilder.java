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
