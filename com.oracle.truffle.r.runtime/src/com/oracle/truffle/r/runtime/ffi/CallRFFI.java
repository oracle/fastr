/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInterface;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Support for the {.Call} and {.External} calls.
 */
public interface CallRFFI {
    interface InvokeCallNode extends NodeInterface {
        /**
         * Invoke the native function identified by {@code symbolInfo} passing it the arguments in
         * {@code args}. The values in {@code args} can be any of the types used to represent
         * {@code R} values in the implementation.
         */
        Object execute(NativeCallInfo nativeCallInfo, Object[] args);
    }

    interface InvokeVoidCallNode extends NodeInterface {
        /**
         * Variant that does not return a result (primarily for library "init" methods).
         */
        void execute(NativeCallInfo nativeCallInfo, Object[] args);
    }

    InvokeCallNode createInvokeCallNode();

    InvokeVoidCallNode createInvokeVoidCallNode();

    final class InvokeCallRootNode extends RFFIRootNode<InvokeCallNode> {
        private static InvokeCallRootNode invokeCallRootNode;

        private InvokeCallRootNode() {
            super(RFFIFactory.getCallRFFI().createInvokeCallNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((NativeCallInfo) args[0], (Object[]) args[1]);
        }

        public static InvokeCallRootNode create() {
            if (invokeCallRootNode == null) {
                invokeCallRootNode = new InvokeCallRootNode();
            }
            return invokeCallRootNode;
        }
    }

    final class InvokeVoidCallRootNode extends RFFIRootNode<InvokeVoidCallNode> {
        private static InvokeVoidCallRootNode InvokeVoidCallRootNode;

        private InvokeVoidCallRootNode() {
            super(RFFIFactory.getCallRFFI().createInvokeVoidCallNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            rffiNode.execute((NativeCallInfo) args[0], (Object[]) args[1]);
            return RNull.instance; // unused
        }

        public static InvokeVoidCallRootNode create() {
            if (InvokeVoidCallRootNode == null) {
                InvokeVoidCallRootNode = new InvokeVoidCallRootNode();
            }
            return InvokeVoidCallRootNode;
        }
    }
}
