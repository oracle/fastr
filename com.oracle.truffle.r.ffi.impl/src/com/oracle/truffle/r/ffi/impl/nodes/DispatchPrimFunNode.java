/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;

@GenerateUncached
public abstract class DispatchPrimFunNode extends FFIUpCallNode.Arg4 {

    public static DispatchPrimFunNode create() {
        return DispatchPrimFunNodeGen.create();
    }

    public static DispatchPrimFunNode getUncached() {
        return DispatchPrimFunNodeGen.getUncached();
    }

    static RRuntimeASTAccess.ExplicitFunctionCall createFunctionCallNode() {
        return RContext.getRRuntimeASTAccess().createExplicitFunctionCall();
    }

    static RRuntimeASTAccess.ExplicitFunctionCall createSlowPathFunctionCallNode() {
        return RContext.getRRuntimeASTAccess().createSlowPathExplicitFunctionCall();
    }

    static MaterializedFrame getCurrentRFrame(RContext ctxRef) {
        RFFIContext context = ctxRef.getStateRFFI();
        return context.rffiContextState.currentDowncallFrame;
    }

    protected FrameDescriptor getCurrentRFrameDescriptor() {
        return getCurrentRFrame(RContext.getInstance(this)).getFrameDescriptor();
    }

    @Specialization(guards = "getCurrentRFrameDescriptor() == cachedFrameDesc")
    @ExplodeLoop
    Object dispatchCached(@SuppressWarnings("unused") Object call, RFunction function, RPairList args, @SuppressWarnings("unused") Object rho,
                    @Cached(value = "createFunctionCallNode()", uncached = "createSlowPathFunctionCallNode()") RRuntimeASTAccess.ExplicitFunctionCall callNode,
                    @SuppressWarnings("unused") @Cached("getCurrentRFrameDescriptor()") FrameDescriptor cachedFrameDesc) {
        RList argsList = args.toRList();
        RContext ctx = RContext.getInstance(this);
        RArgsValuesAndNames argsAndNames;
        if (argsList.getLength() == 0) {
            argsAndNames = RArgsValuesAndNames.EMPTY;
        } else {
            ArgumentsSignature argSig = ArgumentsSignature.fromNamesAttribute(argsList.getNames());
            if (argSig == null) {
                argSig = ArgumentsSignature.empty(argsList.getLength());
            }
            Object[] argsWrapped = (Object[]) argsList.getDataTemp();
            argsAndNames = new RArgsValuesAndNames(argsWrapped, argSig);
        }

        boolean primFunBeingDispatchedSaved = ctx.getStateRFFI().rffiContextState.primFunBeingDispatched;

        ctx.getStateRFFI().rffiContextState.primFunBeingDispatched = true;
        try {
            return callNode.call(getCurrentRFrame(ctx), function, argsAndNames);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (ReturnException e) {
            if (e.getTarget() == RContext.dispatchPrimFunNodeCaller) {
                return e.getResult();
            } else {
                throw e;
            }
        } finally {
            ctx.getStateRFFI().rffiContextState.primFunBeingDispatched = primFunBeingDispatchedSaved;
        }
    }

    @Specialization(replaces = "dispatchCached")
    Object dispatchGeneric(@SuppressWarnings("unused") Object call, RFunction function, RPairList args, @SuppressWarnings("unused") Object rho,
                    @Cached(value = "createSlowPathFunctionCallNode()", uncached = "createSlowPathFunctionCallNode()") RRuntimeASTAccess.ExplicitFunctionCall callNode) {
        return dispatchCached(call, function, args, rho, callNode, null);
    }

}
