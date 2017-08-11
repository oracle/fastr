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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.KeyInfo.Builder;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.engine.interop.REnvironmentMRFactory.REnvironmentKeyInfoImplNodeGen;
import com.oracle.truffle.r.engine.interop.REnvironmentMRFactory.REnvironmentReadImplNodeGen;
import com.oracle.truffle.r.engine.interop.REnvironmentMRFactory.REnvironmentWriteImplNodeGen;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.Foreign2RNodeGen;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;

@MessageResolution(receiverType = REnvironment.class)
public class REnvironmentMR {

    @Resolve(message = "TO_NATIVE")
    public abstract static class REnvironmentToNativeNode extends Node {
        protected Object access(REnvironment receiver) {
            return NativeDataAccess.toNative(receiver);
        }
    }

    @Resolve(message = "READ")
    public abstract static class REnvironmentReadNode extends Node {
        @Child private REnvironmentReadImplNode readNode = REnvironmentReadImplNodeGen.create();

        protected Object access(VirtualFrame frame, REnvironment receiver, Object identifier) {
            return readNode.execute(frame, receiver, identifier);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class REnvironmentWriteNode extends Node {
        @Child private REnvironmentWriteImplNode writeNode = REnvironmentWriteImplNodeGen.create();

        protected Object access(VirtualFrame frame, REnvironment receiver, Object field, Object valueObj) {
            return writeNode.execute(frame, receiver, field, valueObj);
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class REnvironmentKeysNode extends Node {

        protected Object access(REnvironment receiver) {
            return receiver.ls(true, null, true);
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class REnvironmentKeyInfoNode extends Node {
        @Child private REnvironmentKeyInfoImplNode keyInfoNode = REnvironmentKeyInfoImplNodeGen.create();

        protected Object access(REnvironment receiver, Object obj) {
            return keyInfoNode.execute(receiver, obj);
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
    public abstract static class REnvironmentCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof REnvironment;
        }
    }

    abstract static class REnvironmentReadImplNode extends Node {
        @Child private ExtractVectorNode extract;
        @Child private R2Foreign r2Foreign;

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        protected abstract Object execute(VirtualFrame frame, TruffleObject receiver, Object identifier);

        @Specialization
        protected Object access(REnvironment receiver, String identifier,
                        @Cached("createKeyInfoNode()") REnvironmentKeyInfoImplNode keyInfo) {
            int info = keyInfo.execute(receiver, identifier);
            if (unknownIdentifier.profile(!KeyInfo.isExisting(info))) {
                throw UnknownIdentifierException.raise("" + identifier);
            }

            initExtractNode();
            Object value = extract.applyAccessField(receiver, identifier);
            initR2ForeignNode();
            return r2Foreign.execute(value);
        }

        @Fallback
        protected Object access(@SuppressWarnings("unused") TruffleObject receiver, Object identifier) {
            throw UnknownIdentifierException.raise("" + identifier);
        }

        protected REnvironmentKeyInfoImplNode createKeyInfoNode() {
            return REnvironmentKeyInfoImplNodeGen.create();
        }

        private void initR2ForeignNode() {
            if (r2Foreign == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                r2Foreign = insert(R2ForeignNodeGen.create());
            }
        }

        private void initExtractNode() {
            if (extract == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extract = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
        }
    }

    abstract static class REnvironmentWriteImplNode extends Node {
        @Child private Foreign2R foreign2R;
        @Child private ReplaceVectorNode replace;

        private final ConditionProfile roIdentifier = ConditionProfile.createBinaryProfile();

        protected abstract Object execute(VirtualFrame frame, TruffleObject receiver, Object identifier, Object valueObj);

        @Specialization
        protected Object access(REnvironment receiver, String identifier, Object valueObj,
                        @Cached("createKeyInfoNode()") REnvironmentKeyInfoImplNode keyInfo) {

            int info = keyInfo.execute(receiver, identifier);
            if (KeyInfo.isExisting(info)) {
                if (roIdentifier.profile(!KeyInfo.isWritable(info))) {
                    // TODO - this is a bit weird - should be Message.WRITE and identifier
                    throw UnsupportedMessageException.raise(Message.WRITE);
                }
            } else if (receiver.isLocked()) {
                throw UnsupportedMessageException.raise(Message.WRITE);
            }
            if (foreign2R == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreign2R = insert(Foreign2RNodeGen.create());
            }
            Object value = foreign2R.execute(valueObj);
            if (replace == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                replace = insert(ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
            return replace.apply(receiver, new Object[]{identifier}, value);
        }

        @Fallback
        protected Object access(@SuppressWarnings("unused") TruffleObject receiver, Object identifier, @SuppressWarnings("unused") Object valueObj) {
            throw UnknownIdentifierException.raise("" + identifier);
        }

        protected REnvironmentKeyInfoImplNode createKeyInfoNode() {
            return REnvironmentKeyInfoImplNodeGen.create();
        }
    }

    abstract static class REnvironmentKeyInfoImplNode extends Node {

        protected abstract int execute(REnvironment receiver, Object identifier);

        @Specialization
        protected int access(REnvironment receiver, String identifier) {
            Object val = receiver.get(identifier);
            if (val == null) {
                return 0;
            }
            Builder builder = KeyInfo.newBuilder();
            builder.setReadable(true);
            if (!receiver.isLocked() && !receiver.bindingIsLocked(identifier)) {
                builder.setWritable(true);
            }
            builder.setInvocable(val instanceof RFunction);
            return builder.build();
        }

        @Fallback
        protected int access(@SuppressWarnings("unused") REnvironment receiver, @SuppressWarnings("unused") Object identifier) {
            return 0;
        }
    }
}
