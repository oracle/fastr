/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardingAnalysisResult;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

public abstract class ValueForwardingNode extends CastNode {

    protected final ForwardingAnalysisResult forwardingResult;
    private final Supplier<CastNode> pipelineFactory;

    protected ValueForwardingNode(ForwardingAnalysisResult forwardingResult, Supplier<CastNode> pipelineFactory) {
        this.forwardingResult = forwardingResult;
        this.pipelineFactory = pipelineFactory;
    }

    @Specialization(guards = "forwardingResult.isNullForwarded()")
    protected Object bypassNull(RNull x) {
        return x;
    }

    @Specialization(guards = "forwardingResult.isMissingForwarded()")
    protected Object bypassMissing(RMissing x) {
        return x;
    }

    @Specialization(guards = "forwardingResult.isIntegerForwarded()")
    protected int bypassInteger(int x) {
        return x;
    }

    @Specialization(guards = "forwardingResult.isLogicalForwarded()")
    protected byte bypassLogical(byte x) {
        return x;
    }

    @Specialization(guards = "forwardingResult.isLogicalMappedToBoolean()")
    protected boolean mapLogicalToBoolean(byte x) {
        return RRuntime.fromLogical(x);
    }

    @Specialization(guards = "forwardingResult.isDoubleForwarded()")
    protected double bypassDouble(double x) {
        return x;
    }

    @Specialization(guards = "forwardingResult.isComplexForwarded()")
    protected RComplex bypassComplex(RComplex x) {
        return x;
    }

    @Specialization(guards = "forwardingResult.isStringForwarded()")
    protected String bypassString(String x) {
        return x;
    }

    protected CastNode createPipeline() {
        return pipelineFactory.get();
    }

    @Specialization
    protected Object executeOriginalPipeline(Object x, @Cached("createPipeline()") CastNode pipelineHeadNode) {
        return pipelineHeadNode.execute(x);
    }
}
