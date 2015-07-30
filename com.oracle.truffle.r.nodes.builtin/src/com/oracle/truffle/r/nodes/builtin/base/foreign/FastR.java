/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    protected RExternalBuiltinNode lookupName(RAbstractStringVector name) {
        switch (name.getDataAt(0)) {
            case "createcc":
                return FastRCallCountingFactory.CreateCallCounterNodeGen.create();
            case "getcc":
                return FastRCallCountingFactory.GetCallCounterNodeGen.create();
            case "compile":
                return FastRCompileNodeGen.create();
            case "dumptrees":
                return FastRDumpTreesNodeGen.create();
            case "source":
                return FastRSourceNodeGen.create();
            case "syntaxtree":
                return FastRSyntaxTreeNodeGen.create();
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
            case "context.create":
                return FastRContextFactory.CreateNodeGen.create();
            case "context.print":
                return FastRContextFactory.PrintNodeGen.create();
            case "context.spawn":
                return FastRContextFactory.SpawnNodeGen.create();
            case "context.eval":
                return FastRContextFactory.EvalNodeGen.create();

            default:
                return null;
        }
    }

}
