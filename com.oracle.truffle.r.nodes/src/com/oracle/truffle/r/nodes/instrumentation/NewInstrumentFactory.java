/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.instrumentation;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrumentation.debug.DebugHandling;
import com.oracle.truffle.r.nodes.instrumentation.trace.TraceHandling;
import com.oracle.truffle.r.nodes.instrument.factory.RInstrumentFactory;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RFunction;

public class NewInstrumentFactory extends RInstrumentFactory {

    public NewInstrumentFactory(TruffleLanguage.Env env) {
        RInstrumentation.initialize(env.lookup(com.oracle.truffle.api.instrumentation.Instrumenter.class));
    }

    @Override
    public void registerFunctionDefinitionNode(FunctionDefinitionNode fdn) {
        RInstrumentation.registerFunctionDefinition(fdn);
    }

    @Override
    public void checkDebugRequested(RFunction func) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean enableDebug(RFunction func, Object text, Object condition, boolean once) {
        return DebugHandling.enableDebug(func, text, condition, once);
    }

    @Override
    public boolean undebug(RFunction func) {
        return DebugHandling.undebug(func);
    }

    @Override
    public boolean isDebugged(RFunction func) {
        return DebugHandling.isDebugged(func);
    }

    @Override
    public boolean enableTrace(RFunction func) {
        return TraceHandling.enableTrace(func);
    }

    @Override
    public boolean disableTrace(RFunction func) {
        return TraceHandling.disableTrace(func);
    }

    @Override
    public void setTracingState(boolean state) {
        TraceHandling.setTracingState(state);
    }

    @Override
    public Object findSingleProbe(RFunction func, Object tag) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public boolean installCounter(RFunction func) {
        REntryCounters.FunctionListener.installCounter(func);
        return true;
    }

    @Override
    public int getCounter(RFunction func) {
        return REntryCounters.FunctionListener.findCounter(func).getEnterCount();
    }

    @Override
    public boolean installFunctionTimer(RFunction func) {
        RNodeTimer.StatementListener.installTimer(func);
        return true;
    }

    @Override
    public long getFunctionTime(RFunction func) {
        long cumTime = RNodeTimer.StatementListener.findTimer(func);
        return cumTime;
    }

}
