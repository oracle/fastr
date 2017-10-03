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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;

import com.oracle.truffle.r.nodes.builtin.casts.Mapper;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Adds methods to {@link InitialPhaseBuilder} that allow to set up the pipeline configuration.
 * Invocation of some methods means that the pre-initialization phase has been finishes, i.e.
 * pipeline fully configured, those methods return this object cast to {@link InitialPhaseBuilder}
 * so that user then cannot invoke methods that change the pipeline configuration. Any method from
 * {@link InitialPhaseBuilder} returns that type, so once the user steps outside the configuration,
 * there is no way to invoke configuration related methods defined here.
 */
public final class PreinitialPhaseBuilder extends InitialPhaseBuilder<Object> {

    PreinitialPhaseBuilder(PipelineBuilder pipelineBuilder) {
        super(pipelineBuilder);
    }

    public PreinitialPhaseBuilder allowNull() {
        returnIf(nullValue());
        return this;
    }

    public PreinitialPhaseBuilder mustNotBeNull() {
        mustBe(nullValue().not());
        return this;
    }

    public PreinitialPhaseBuilder mustNotBeNull(RError.Message errorMsg, Object... msgArgs) {
        mustBe(nullValue().not(), errorMsg, msgArgs);
        return this;
    }

    public PreinitialPhaseBuilder mapNull(Mapper<RNull, ?> mapper) {
        mapIf(nullValue(), mapper);
        return this;
    }

    public PreinitialPhaseBuilder allowMissing() {
        returnIf(missingValue());
        return this;
    }

    public PreinitialPhaseBuilder mustNotBeMissing() {
        mustBe(missingValue().not());
        return this;
    }

    public PreinitialPhaseBuilder mustNotBeMissing(RError.Message errorMsg, Object... msgArgs) {
        mustBe(missingValue().not(), errorMsg, msgArgs);
        return this;
    }

    public PreinitialPhaseBuilder mapMissing(Mapper<RMissing, ?> mapper) {
        mapIf(missingValue(), mapper);
        return this;
    }

    public PreinitialPhaseBuilder allowNullAndMissing() {
        returnIf(nullValue().or(missingValue()));
        return this;
    }

    public PreinitialPhaseBuilder defaultError(RError.Message message, Object... args) {
        pipelineBuilder().getPipelineConfig().setDefaultError(new MessageData(message, args));
        return this;
    }

    /**
     * Determines whether foreign arrays are implicitly casted to a R vector/list or not. <br>
     * The default setting is <code>true</code>.
     *
     * @param flag if true foreign objects will be cast
     */
    public PreinitialPhaseBuilder castForeignObjects(boolean flag) {
        pipelineBuilder().getPipelineConfig().setCastForeignObjects(flag);
        return this;
    }

    public PreinitialPhaseBuilder defaultWarning(Message message, Object... args) {
        pipelineBuilder().getPipelineConfig().setDefaultWarning(new MessageData(message, args));
        return this;
    }
}
