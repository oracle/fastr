/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ffi.CallRFFI.InvokeCallNode;
import com.oracle.truffle.r.runtime.ffi.DLL.RFindSymbolNode;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.NativeCallInfo;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI.EmbeddedSuicideNode;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public abstract class RSuicide {
    private RSuicide() {
    }

    /**
     * Please use {@link #rSuicide(RContext, String)} overload whenever the context is available.
     */
    public static RuntimeException rSuicide(String msg) {
        throw rSuicide(RContext.getInstance(), msg);
    }

    /**
     * Called when the system encounters a fatal internal error and must commit suicide (i.e.
     * terminate). It allows an embedded client to override the default (although they typically
     * invoke the default eventually).
     */
    public static RuntimeException rSuicide(RContext ctx, String msg) {
        invokeUserDefinedSuicide(ctx, msg);
        throw rSuicideDefault(msg);
    }

    public static RuntimeException rSuicide(RContext ctx, Throwable cause, String msg) {
        invokeUserDefinedSuicide(ctx, msg);
        throw rSuicideDefault(msg);
    }

    /**
     * The default, non-overrideable, suicide call. It prints the message and throws
     * {@link ExitException}.
     *
     * @param msg
     */
    public static RuntimeException rSuicideDefault(String msg) {
        System.err.println("FastR unexpected failure: " + msg);
        throw new ExitException(2, false);
    }

    private static void invokeUserDefinedSuicide(RContext ctx, String msg) {
        if (ctx != null && RInterfaceCallbacks.R_Suicide.isOverridden()) {
            RootCallTarget invokeUserCleanup = ctx.getOrCreateCachedCallTarget(UserDefinedSuicideRootNode.class, () -> new UserDefinedSuicideRootNode(ctx).getCallTarget());
            invokeUserCleanup.call(msg);
        }
    }

    private static final class UserDefinedSuicideRootNode extends RootNode {
        protected UserDefinedSuicideRootNode(RContext ctx) {
            super(null);
            suicideNode = ctx.getRFFI().embedRFFI.createEmbeddedSuicideNode();
            Truffle.getRuntime().createCallTarget(this);
        }

        @Child private EmbeddedSuicideNode suicideNode;

        @Override
        public Object execute(VirtualFrame frame) {
            suicideNode.execute((String) frame.getArguments()[0]);
            return null;
        }
    }
}
