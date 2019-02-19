/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RForeignObjectWrapper;

@MessageResolution(receiverType = RForeignObjectWrapper.class)
public class RForeignObjectWrapperMR {

    @Resolve(message = "IS_NULL")
    public abstract static class IsNullNode extends Node {

        @Child private Node msgNode = Message.IS_NULL.createNode();

        protected Object access(RForeignObjectWrapper receiver) {
            return ForeignAccess.sendIsNull(msgNode, receiver.getDelegate());
        }
    }

    
    @Resolve(message = "KEY_INFO")
    public abstract static class KeyInfoNode extends Node {

        @Child private Node msgNode = Message.KEY_INFO.createNode();

        protected Object access(RForeignObjectWrapper receiver, Object identifier) {
            return ForeignAccess.sendKeyInfo(msgNode, receiver.getDelegate(), identifier);
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class HasSizeNode extends Node {

        @Child private Node msgNode = Message.HAS_SIZE.createNode();

        protected Object access(RForeignObjectWrapper receiver) {
            return ForeignAccess.sendHasSize(msgNode, receiver.getDelegate());
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class GetSizeNode extends Node {

        @Child private Node msgNode = Message.GET_SIZE.createNode();

        protected Object access(RForeignObjectWrapper receiver) {
            try {
                return ForeignAccess.sendGetSize(msgNode, receiver.getDelegate());
            } catch (UnsupportedMessageException e) {
                throw e.raise();
            }
        }
    }

    @Resolve(message = "HAS_KEYS")
    public abstract static class HasKeysNode extends Node {

        @Child private Node msgNode = Message.HAS_KEYS.createNode();

        protected Object access(RForeignObjectWrapper receiver) {
            return ForeignAccess.sendHasKeys(msgNode, receiver.getDelegate());
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class KeysNode extends Node {

        @Child private Node msgNode = Message.KEYS.createNode();

        protected Object access(RForeignObjectWrapper receiver) {
            try {
                return ForeignAccess.sendKeys(msgNode, receiver.getDelegate());
            } catch (UnsupportedMessageException e) {
                throw e.raise();
            }
        }
    }

    @Resolve(message = "READ")
    public abstract static class ReadNode extends Node {

        @Child private Node msgNode = Message.READ.createNode();

        protected Object access(RForeignObjectWrapper receiver, Object identifier) {
            try {
                return ForeignAccess.sendRead(msgNode, receiver.getDelegate(), identifier);
            } catch (InteropException e) {
                throw e.raise();
            }
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class WriteNode extends Node {

        @Child private Node msgNode = Message.WRITE.createNode();

        protected Object access(RForeignObjectWrapper receiver, Object identifier, Object valueObj) {
            try {
                return ForeignAccess.sendWrite(msgNode, receiver.getDelegate(), identifier, valueObj);
            } catch (InteropException e) {
                throw e.raise();
            }
        }
    }

    @Resolve(message = "IS_BOXED")
    public abstract static class IsBoxedNode extends Node {

        @Child private Node msgNode = Message.IS_BOXED.createNode();

        protected Object access(RForeignObjectWrapper receiver) {
            return ForeignAccess.sendIsBoxed(msgNode, receiver.getDelegate());
        }
    }

    @Resolve(message = "UNBOX")
    public abstract static class UnboxNode extends Node {

        @Child private Node msgNode = Message.UNBOX.createNode();

        protected Object access(RForeignObjectWrapper receiver) {
            try {
                return ForeignAccess.sendUnbox(msgNode, receiver.getDelegate());
            } catch (UnsupportedMessageException e) {
                throw e.raise();
            }
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    public abstract static class IsExecutableNode extends Node {

        @Child private Node msgNode = Message.IS_EXECUTABLE.createNode();

        protected Object access(RForeignObjectWrapper receiver) {
            return ForeignAccess.sendIsExecutable(msgNode, receiver.getDelegate());
        }
    }

    @Resolve(message = "EXECUTE")
    public abstract static class ExecuteNode extends Node {

        @Child private Node msgNode = Message.EXECUTE.createNode();

        protected Object access(RForeignObjectWrapper receiver, Object... arguments) {
            try {
                return ForeignAccess.sendExecute(msgNode, receiver.getDelegate(), arguments);
            } catch (InteropException e) {
                throw e.raise();
            }
        }
    }

    @Resolve(message = "IS_POINTER")
    public abstract static class IsPointerNode extends Node {
        protected boolean access(@SuppressWarnings("unused") RForeignObjectWrapper receiver) {
            return true;
        }
    }

    @Resolve(message = "AS_POINTER")
    public abstract static class AsPointerNode extends Node {
        protected Object access(RForeignObjectWrapper receiver) {
            return NativeDataAccess.asPointer(receiver);
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class ToNativeNode extends Node {
        protected Object access(Object receiver) {
            return receiver;
        }
    }

    @CanResolve
    public abstract static class RForeignObjectWrapperCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RForeignObjectWrapper;
        }
    }
}
