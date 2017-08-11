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
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.Foreign2RNodeGen;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;

@MessageResolution(receiverType = RFunction.class)
public class RFunctionMR {

    @Resolve(message = "KEY_INFO")
    public abstract static class RFunctionKeyInfoNode extends Node {
        protected Object access(@SuppressWarnings("unused") TruffleObject receiver, @SuppressWarnings("unused") Object identifier) {
            return 0;
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    public abstract static class RFunctionIsExecutabledNode extends Node {
        protected Object access(@SuppressWarnings("unused") RFunction receiver) {
            return true;
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class RFunctionToNativeNode extends Node {
        protected Object access(RFunction receiver) {
            return NativeDataAccess.toNative(receiver);
        }
    }

    @Resolve(message = "EXECUTE")
    public abstract static class RFunctionExecuteNode extends Node {
        @Child private Foreign2R foreign2R = Foreign2RNodeGen.create();
        @Child private R2Foreign r2Foreign = R2ForeignNodeGen.create();

        private static final FrameDescriptor emptyFrameDescriptor = FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<interop>", new FrameDescriptor("R interop frame"));
        private static final RFrameSlot argsIdentifier = RFrameSlot.createTemp(false);
        private static final FrameSlot slot = FrameSlotChangeMonitor.findOrAddFrameSlot(emptyFrameDescriptor, argsIdentifier, FrameSlotKind.Object);

        static {
            FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<function>", emptyFrameDescriptor);
        }

        @Child private RCallBaseNode call = RCallNode.createExplicitCall(argsIdentifier);

        protected Object access(RFunction receiver, Object[] arguments) {
            Object[] dummyFrameArgs = RArguments.createUnitialized();
            VirtualFrame dummyFrame = Truffle.getRuntime().createVirtualFrame(dummyFrameArgs, emptyFrameDescriptor);

            Object[] convertedArguments = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                convertedArguments[i] = foreign2R.execute(arguments[i]);
            }
            RArgsValuesAndNames actualArgs = new RArgsValuesAndNames(convertedArguments, ArgumentsSignature.empty(arguments.length));
            try {
                FrameSlotChangeMonitor.setObject(dummyFrame, slot, actualArgs);
                Object value = call.execute(dummyFrame, receiver);
                return r2Foreign.execute(value);
            } finally {
                FrameSlotChangeMonitor.setObject(dummyFrame, slot, null);
            }
        }
    }

    @Resolve(message = "IS_POINTER")
    public abstract static class IsPointerNode extends Node {
        protected boolean access(Object receiver) {
            return NativeDataAccess.isPointer(receiver);
        }
    }

    @Resolve(message = "AS_POINTER")
    public abstract static class AsPointerNode extends Node {
        protected long access(Object receiver) {
            return NativeDataAccess.asPointer(receiver);
        }
    }

    @CanResolve
    public abstract static class RFunctionCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RFunction;
        }
    }
}
