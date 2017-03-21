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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.RCloseable;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

@MessageResolution(receiverType = RFunction.class, language = TruffleRLanguage.class)
public class RFunctionMR {
    @Resolve(message = "IS_EXECUTABLE")
    public abstract static class RFunctionIsExecutabledNode extends Node {
        protected Object access(@SuppressWarnings("unused") RFunction receiver) {
            return true;
        }
    }

    @Resolve(message = "EXECUTE")
    public abstract static class RFunctionExecuteNode extends Node {
        private static final FrameDescriptor emptyFrameDescriptor = new FrameDescriptor("R interop frame");
        private static final Object argsIdentifier = new Object();
        private static final FrameSlot slot = emptyFrameDescriptor.addFrameSlot(argsIdentifier, FrameSlotKind.Object);

        static {
            FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<function>", emptyFrameDescriptor);
        }

        @Child private RCallBaseNode call = RCallNode.createExplicitCall(argsIdentifier);
        @Child private Node findContext = TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();

        @SuppressWarnings("try")
        protected Object access(@SuppressWarnings("unused") VirtualFrame frame, RFunction receiver, Object[] arguments) {
            Object[] dummyFrameArgs = RArguments.createUnitialized();
            VirtualFrame dummyFrame = Truffle.getRuntime().createVirtualFrame(dummyFrameArgs, emptyFrameDescriptor);

            RArgsValuesAndNames actualArgs = new RArgsValuesAndNames(arguments, ArgumentsSignature.empty(arguments.length));
            try (RCloseable c = RContext.withinContext(TruffleRLanguage.INSTANCE.actuallyFindContext0(findContext))) {
                try {
                    dummyFrame.setObject(slot, actualArgs);
                    return call.execute(dummyFrame, receiver);
                } finally {
                    dummyFrame.setObject(slot, null);
                }
            }
        }
    }

    @CanResolve
    public abstract static class RFunctionCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RFunction;
        }
    }
}
