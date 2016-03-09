/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.library.fastr.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * This is a FastR-specific primitive that supports the extensions in the {@code fastr} package.
 */
@RBuiltin(name = ".FastR", kind = RBuiltinKind.PRIMITIVE, parameterNames = {".NAME", "..."})
public abstract class FastR extends RBuiltinNode {
    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY};
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "1", guards = {"cached.getDataAt(0).equals(name.getDataAt(0))", "builtin != null"})
    @TruffleBoundary
    protected Object doFastR(RAbstractStringVector name, RArgsValuesAndNames args, @Cached("name") RAbstractStringVector cached, @Cached("lookupName(name)") RExternalBuiltinNode builtin) {
        controlVisibility();
        return builtin.call(args);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object doFastR(Object name, Object args) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    protected RExternalBuiltinNode lookupName(RAbstractStringVector name) {
        switch (name.getDataAt(0)) {
            case "Interop.import":
                return InteropImportNodeGen.create();
            case "Interop.export":
                return InteropExportNodeGen.create();
            case "Interop.eval":
                return InteropEvalNodeGen.create();
            case "createcc":
                return FastRCallCountingFactory.CreateCallCounterNodeGen.create();
            case "getcc":
                return FastRCallCountingFactory.GetCallCounterNodeGen.create();
            case "createtimer":
                return FastRFunctionTimerFactory.CreateFunctionTimerNodeGen.create();
            case "gettimer":
                return FastRFunctionTimerFactory.GetFunctionTimerNodeGen.create();
            case "compile":
                return FastRCompileNodeGen.create();
            case "dumptrees":
                return FastRDumpTreesNodeGen.create();
            case "syntaxtree":
                return FastRSyntaxTreeNodeGen.create();
            case "treestats":
                return FastRTreeStatsNodeGen.create();
            case "tree":
                return FastRTreeNodeGen.create();
            case "typeof":
                return FastRTypeofNodeGen.create();
            case "stacktrace":
                return FastRStackTraceNodeGen.create();
            case "debug":
                return FastRDebugNodeGen.create();
            case "inspect":
                return new FastRInspect();
            case "pkgsource.pre":
                return FastRPkgSourceFactory.PreLoadNodeGen.create();
            case "pkgsource.post":
                return FastRPkgSourceFactory.PostLoadNodeGen.create();
            case "pkgsource.done":
                return FastRPkgSourceFactory.DoneNodeGen.create();
            case "context.get":
                return FastRContextFactory.GetNodeGen.create();
            case "context.create":
                return FastRContextFactory.CreateNodeGen.create();
            case "context.print":
                return FastRContextFactory.PrintNodeGen.create();
            case "context.spawn":
                return FastRContextFactory.SpawnNodeGen.create();
            case "context.join":
                return FastRContextFactory.JoinNodeGen.create();
            case "context.eval":
                return FastRContextFactory.EvalNodeGen.create();
            case "fastr.channel.create":
                return FastRContextFactory.CreateChannelNodeGen.create();
            case "fastr.channel.get":
                return FastRContextFactory.GetChannelNodeGen.create();
            case "fastr.channel.close":
                return FastRContextFactory.CloseChannelNodeGen.create();
            case "fastr.channel.send":
                return FastRContextFactory.ChannelSendNodeGen.create();
            case "fastr.channel.receive":
                return FastRContextFactory.ChannelReceiveNodeGen.create();
            case "fastr.channel.poll":
                return FastRContextFactory.ChannelPollNodeGen.create();
            case "fastr.channel.select":
                return FastRContextFactory.ChannelSelectNodeGen.create();
            case "fastr.throw":
                return FastRThrowItFactory.ThrowItNodeGen.create();
            case "fastr.trace":
                return FastRTraceFactory.TraceNodeGen.create();
            default:
                return null;
        }
    }
}
