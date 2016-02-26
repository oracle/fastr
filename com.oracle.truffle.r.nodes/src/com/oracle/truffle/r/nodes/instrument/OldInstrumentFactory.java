/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.instrument;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrument.debug.DebugHandling;
import com.oracle.truffle.r.nodes.instrument.factory.RInstrumentFactory;
import com.oracle.truffle.r.nodes.instrument.trace.TraceHandling;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;

public class OldInstrumentFactory extends RInstrumentFactory {

    public OldInstrumentFactory(TruffleLanguage.Env env) {
        RInstrument.initialize(env.instrumenter());
        RASTProber prober = RInstrument.instrumentingEnabled() ? RASTProber.getRASTProber() : null;
        if (prober != null) {
            env.instrumenter().registerASTProber(prober);
        }
    }

    private static FunctionUID getFunctionUID(RFunction fun) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) fun.getRootNode();
        return fdn.getUID();
    }

    @Override
    public void registerFunctionDefinitionNode(FunctionDefinitionNode fdn) {
        RInstrument.registerFunctionDefinition(fdn);
    }

    @Override
    public void checkDebugRequested(RFunction func) {
        RInstrument.checkDebugRequested(func);
    }

    @Override
    public Object findSingleProbe(RFunction func, Object tag) {
        return RInstrument.findSingleProbe(getFunctionUID(func), (StandardSyntaxTag) tag);
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
    public boolean installCounter(RFunction func) {
        FunctionUID uuid = getFunctionUID(func);
        if (REntryCounters.findCounter(uuid) == null) {
            Probe probe = RInstrument.findSingleProbe(uuid, StandardSyntaxTag.START_METHOD);
            if (probe == null) {
                return false;
            }
            REntryCounters.Function counter = new REntryCounters.Function(uuid);
            RInstrument.getInstrumenter().attach(probe, counter, REntryCounters.Function.INFO);
        }
        return true;
    }

    @Override
    public int getCounter(RFunction func) {
        REntryCounters.Function counter = (REntryCounters.Function) REntryCounters.findCounter(getFunctionUID(func));
        if (counter == null) {
            return -1;
        } else {
            return counter.getEnterCount();
        }
    }

    @Override
    public boolean installFunctionTimer(RFunction func) {
        throw RError.nyi(RError.NO_CALLER, "installFunctionTimer");
    }

    @Override
    public long getFunctionTime(RFunction func) {
        return 0;
    }

}
