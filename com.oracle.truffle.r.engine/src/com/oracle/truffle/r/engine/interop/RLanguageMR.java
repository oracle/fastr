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
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;

@MessageResolution(receiverType = RLanguage.class)
public class RLanguageMR {

    @Resolve(message = "HAS_SIZE")
    public abstract static class RLanguageHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RLanguage receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class RLanguageGetSizeNode extends Node {
        protected Object access(RLanguage receiver) {
            return receiver.getLength();
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class RLanguageToNativeNode extends Node {
        protected Object access(RLanguage receiver) {
            return new NativePointer(receiver);
        }
    }

    @Resolve(message = "READ")
    public abstract static class RLanguageReadNode extends Node {
        @Child private ReadNode readNode = RLanguageMRFactory.ReadNodeGen.create();

        protected Object access(VirtualFrame frame, RLanguage receiver, Object identifier) {
            return readNode.execute(frame, receiver, identifier);
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RLanguageNode extends Node {
        @Node.Child private KeyInfoNode keyInfoNode = RLanguageMRFactory.KeyInfoNodeGen.create();

        protected Object access(RLanguage receiver, Object obj) {
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
    public abstract static class RLanguageCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RLanguage;
        }
    }

    abstract static class ReadNode extends Node {
        @Child private ExtractVectorNode extract;
        @Child private R2Foreign r2Foreign;

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        abstract Object execute(VirtualFrame frame, RLanguage receiver, Object identifier);

        @Specialization
        protected Object access(RLanguage receiver, int idx,
                        @Cached("createKeyInfoNode()") KeyInfoNode keyInfo) {

            int info = keyInfo.execute(receiver, idx);
            if (unknownIdentifier.profile(!KeyInfo.isExisting(info))) {
                throw UnknownIdentifierException.raise("" + idx);
            }

            if (extract == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extract = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
            Object value = extract.apply(receiver, new Object[]{idx + 1}, RLogical.TRUE, RLogical.TRUE);
            if (value == null) {
                return RNull.instance;
            }

            if (r2Foreign == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                r2Foreign = insert(R2ForeignNodeGen.create());
            }
            return r2Foreign.execute(value);
        }

        @Fallback
        protected Object access(@SuppressWarnings("unused") RLanguage receiver, Object identifier) {
            throw UnknownIdentifierException.raise("" + identifier);
        }

        protected static KeyInfoNode createKeyInfoNode() {
            return RLanguageMRFactory.KeyInfoNodeGen.create();
        }
    }

    abstract static class KeyInfoNode extends Node {
        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        abstract int execute(RLanguage receiver, Object identifier);

        @Specialization
        protected int access(RLanguage receiver, int idx) {
            if (unknownIdentifier.profile(idx < 0 || idx >= receiver.getLength())) {
                return 0;
            }

            KeyInfo.Builder builder = KeyInfo.newBuilder();
            builder.setReadable(true);
            // TODO what about writeble/invocable/...
            return builder.build();
        }

        @Fallback
        protected int access(@SuppressWarnings("unused") RLanguage receiver, @SuppressWarnings("unused") Object identifier) {
            return 0;
        }
    }
}
