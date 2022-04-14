/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.FastRConfig;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.EventLoopState;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

/**
 * This FastR specific builtin is the entry point to the infrastructure for dispatching native IO
 * handlers. A separate native event loop is spawn and two named pipes are created by executing the
 * <code>initEventLoop</code> native function. When an event occurs in an IO descriptor (registered
 * via the <code>addInputHandler</code> native function), the native loop notifies the Java side via
 * the <code>fifoIn</code> named pipe, to which a dedicated Java thread is listening (running out of
 * any {@link RContext}). At this point the native event loop gets blocked by listening to
 * <code>fifoOut</code>, whíle the Java thread submits a task to the {@link RContext#getExecutor()
 * single-threaded executor} to dispatch the native IO event(s) through executing another native
 * function <code>dispatchNativeHandlers</code>. When done, the <code>dispatchNativeHandlers</code>
 * uses the <code>fifoOut</code> named pipe to release the native event loop. The described
 * procedure ensures that the native event handlers handle events within a single FastR context
 */
@RBuiltin(name = ".fastr.initEventLoop", kind = PRIMITIVE, behavior = COMPLEX, parameterNames = {})
public abstract class FastRInitEventLoop extends RBuiltinNode.Arg0 {

    static {
        Casts.noCasts(FastRInitEventLoop.class);
    }

    @Child private BaseRFFI.InitEventLoopNode initEventLoopNode = BaseRFFI.InitEventLoopNode.create();

    @Specialization
    public Object initEventLoop() {
        if (FastRConfig.UseNativeEventLoop) {
            TruffleFile tmpDir;
            final RContext ctx = getRContext();
            try {
                tmpDir = ctx.getEnv().createTempDirectory(null, "fastr-fifo");
            } catch (Exception e) {
                return RDataFactory.createList(new Object[]{1}, RDataFactory.createStringVector("result"));
            }
            ctx.eventLoopState = new EventLoopState(tmpDir);
            String fifoInPath = tmpDir.resolve("event-loop-fifo-in").toString();
            String fifoOutPath = tmpDir.resolve("event-loop-fifo-out").toString();
            int result = initEventLoopNode.execute(fifoInPath, fifoOutPath);
            return RDataFactory.createList(new Object[]{result, fifoInPath, fifoOutPath},
                            RDataFactory.createStringVector(new String[]{"result", "fifoInPath", "fifoOutPath"}, RDataFactory.COMPLETE_VECTOR));
        } else {
            return RNull.instance;
        }
    }
}
