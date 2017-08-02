/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.engine.interop.RS4ObjectMRFactory.RS4ObjectKeyInfoImplNodeGen;
import com.oracle.truffle.r.engine.interop.RS4ObjectMRFactory.RS4ObjectReadImplNodeGen;
import com.oracle.truffle.r.engine.interop.RS4ObjectMRFactory.RS4ObjectWriteImplNodeGen;
import com.oracle.truffle.r.nodes.attributes.ArrayAttributeNode;
import com.oracle.truffle.r.nodes.attributes.GetAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.Foreign2RNodeGen;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;

@MessageResolution(receiverType = RS4Object.class)
public class RS4ObjectMR {

    @Resolve(message = "IS_BOXED")
    public abstract static class RS4ObjectIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RS4Object receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RS4ObjectHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RS4Object receiver) {
            return false;
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RS4ObjectIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RS4Object receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RS4ObjectReadNode extends Node {
        @Child private RS4ObjectReadImplNode readNode = RS4ObjectReadImplNodeGen.create();

        protected Object access(RS4Object receiver, Object identifier) {
            return readNode.execute(receiver, identifier);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class RS4ObjectWriteNode extends Node {
        @Child private RS4ObjectWriteImplNode writeNode = RS4ObjectWriteImplNodeGen.create();

        protected Object access(RS4Object receiver, Object identifier, Object valueObj) {
            return writeNode.execute(receiver, identifier, valueObj);
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class RS4ObjectKeysNode extends Node {
        @Child private ArrayAttributeNode arrayAttrAccess = ArrayAttributeNode.create();

        protected Object access(RS4Object receiver) {
            return getKeys(receiver, arrayAttrAccess);
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RS4ObjectNode extends Node {
        @Node.Child private RS4ObjectKeyInfoImplNode keyInfoNode = RS4ObjectKeyInfoImplNodeGen.create();

        protected Object access(VirtualFrame frame, RS4Object receiver, Object obj) {
            return keyInfoNode.execute(receiver, obj);
        }
    }

    private static RAbstractStringVector getKeys(RS4Object s4, ArrayAttributeNode arrayAttrAccess) {
        RAttribute[] attributes = arrayAttrAccess.execute(s4.getAttributes());
        String[] data = new String[attributes.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = attributes[i].getName();
        }
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    @CanResolve
    public abstract static class RS4ObjectCheck extends Node {
        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RS4Object;
        }
    }

    abstract static class RS4ObjectReadImplNode extends Node {
        @Child private GetAttributeNode getAttributeNode;
        @Child private R2Foreign r2Foreign;

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        abstract Object execute(RS4Object receiver, Object identifier);

        @Specialization
        protected Object access(RS4Object receiver, String identifier,
                        @Cached("createKeyInfoNode()") RS4ObjectKeyInfoImplNode keyInfo) {
            int info = keyInfo.execute(receiver, identifier);
            if (unknownIdentifier.profile(!KeyInfo.isExisting(info))) {
                throw UnknownIdentifierException.raise("" + identifier);
            }

            if (getAttributeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getAttributeNode = insert(GetAttributeNode.create());
            }
            Object value = getAttributeNode.execute(receiver, identifier);
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
        protected Object access(RS4Object receiver, Object identifier) {
            throw UnknownIdentifierException.raise("" + identifier);
        }

        protected static ArrayAttributeNode createArrayAttributeNode() {
            return ArrayAttributeNode.create();
        }

        protected static RS4ObjectKeyInfoImplNode createKeyInfoNode() {
            return RS4ObjectKeyInfoImplNodeGen.create();
        }
    }

    abstract static class RS4ObjectWriteImplNode extends Node {
        @Child private SetAttributeNode setAttributeNode;
        @Child private Foreign2R foreign2R;

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        abstract Object execute(RS4Object receiver, Object identifier, Object valueObj);

        @Specialization
        protected Object access(RS4Object receiver, String identifier, Object valueObj,
                        @Cached("createKeyInfoNode()") RS4ObjectKeyInfoImplNode keyInfo) {
            int info = keyInfo.execute(receiver, identifier);
            if (unknownIdentifier.profile(!KeyInfo.isExisting(info))) {
                throw UnknownIdentifierException.raise("" + identifier);
            }

            if (!KeyInfo.isWritable(info)) {
                throw UnsupportedMessageException.raise(Message.WRITE);
            }

            if (foreign2R == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreign2R = insert(Foreign2RNodeGen.create());
            }
            Object value = foreign2R.execute(valueObj);
            if (setAttributeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setAttributeNode = insert(SetAttributeNode.create());
            }
            setAttributeNode.execute(receiver, identifier, value);
            return valueObj;
        }

        @Fallback
        protected Object access(RS4Object receiver, Object identifier, Object valueObj) {
            throw UnknownIdentifierException.raise("" + identifier);
        }

        protected static RS4ObjectKeyInfoImplNode createKeyInfoNode() {
            return RS4ObjectKeyInfoImplNodeGen.create();
        }
    }

    abstract static class RS4ObjectKeyInfoImplNode extends Node {
        @Child private GetAttributeNode getAttributeNode;

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        abstract int execute(RS4Object receiver, Object idx);

        @Specialization
        protected int access(RS4Object receiver, String identifier,
                        @Cached("createArrayAttributeNode()") ArrayAttributeNode arrayAttrAccess) {
            int idx = getAttrIndex(receiver, identifier, arrayAttrAccess);
            if (unknownIdentifier.profile(idx < 0)) {
                return 0;
            }

            if (getAttributeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getAttributeNode = insert(GetAttributeNode.create());
            }
            Builder builder = KeyInfo.newBuilder();
            builder.setReadable(true).setWritable(!identifier.equals("class"));
            builder.setInvocable(getAttributeNode.execute(receiver, identifier) instanceof RFunction);
            return builder.build();
        }

        protected static ArrayAttributeNode createArrayAttributeNode() {
            return ArrayAttributeNode.create();
        }

        @Fallback
        protected int access(RS4Object receiver, Object field) {
            return 0;
        }
    }

    private static int getAttrIndex(RS4Object receiver, String identifier, ArrayAttributeNode arrayAttrAccess) {
        RAttribute[] attributes = arrayAttrAccess.execute(receiver.getAttributes());
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getName().equals(identifier)) {
                return i;
            }
        }
        return -1;
    }
}
