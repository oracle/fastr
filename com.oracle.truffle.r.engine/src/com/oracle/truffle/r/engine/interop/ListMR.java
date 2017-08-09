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
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.engine.interop.ListMRFactory.ListKeyInfoImplNodeGen;
import com.oracle.truffle.r.engine.interop.ListMRFactory.ListReadImplNodeGen;
import com.oracle.truffle.r.engine.interop.ListMRFactory.ListWriteImplNodeGen;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.Foreign2RNodeGen;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;

public class ListMR {

    @MessageResolution(receiverType = RList.class)
    public static class RListMR extends ListMR {
        @Resolve(message = "IS_BOXED")
        public abstract static class RListIsBoxedNode extends Node {
            protected Object access(RList receiver) {
                return isBoxed(receiver);
            }
        }

        @Resolve(message = "HAS_SIZE")
        public abstract static class RListHasSizeNode extends Node {
            protected Object access(RList receiver) {
                return hasSize(receiver);
            }
        }

        @Resolve(message = "GET_SIZE")
        public abstract static class RListGetSizeNode extends Node {
            @Child private RLengthNode lengthNode = RLengthNode.create();

            protected Object access(VirtualFrame frame, RList receiver) {
                return getSize(receiver, lengthNode);
            }
        }

        @Resolve(message = "IS_NULL")
        public abstract static class RListIsNullNode extends Node {
            protected Object access(RList receiver) {
                return isNull(receiver);
            }
        }

        @Resolve(message = "READ")
        public abstract static class RListReadNode extends Node {
            @Child private ListReadImplNode read = ListReadImplNodeGen.create();

            protected Object access(VirtualFrame frame, RList receiver, Object identifier) {
                return read.execute(frame, receiver, identifier);
            }
        }

        @Resolve(message = "WRITE")
        public abstract static class RListWriteNode extends Node {
            @Child private ListWriteImplNode writeNode = ListWriteImplNodeGen.create();

            protected Object access(VirtualFrame frame, RList receiver, Object identifier, Object valueObj) {
                return writeNode.execute(receiver, identifier, valueObj);
            }
        }

        @Resolve(message = "KEYS")
        public abstract static class RListKeysNode extends Node {
            @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

            protected Object access(RList receiver) {
                return listKeys(receiver, getNamesNode);
            }
        }

        @Resolve(message = "KEY_INFO")
        public abstract static class RListKeyInfoNode extends Node {
            @Child private ListKeyInfoImplNode keyInfoNode = ListKeyInfoImplNodeGen.create();

            protected Object access(VirtualFrame frame, TruffleObject receiver, Object idx) {
                return keyInfoNode.execute(receiver, idx);
            }
        }

        @Resolve(message = "TO_NATIVE")
        public abstract static class RListToNativeNode extends Node {
            protected Object access(RTruffleObject receiver) {
                return toNativePointer(receiver);
            }
        }

        @CanResolve
        public abstract static class RListCheck extends Node {

            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof RList;
            }
        }
    }

    @MessageResolution(receiverType = RPairList.class)
    public static class RPairListMR {
        @Resolve(message = "IS_BOXED")
        public abstract static class RPairListIsBoxedNode extends Node {
            protected Object access(RPairList receiver) {
                return isBoxed(receiver);
            }
        }

        @Resolve(message = "HAS_SIZE")
        public abstract static class RPairListHasSizeNode extends Node {
            protected Object access(RPairList receiver) {
                return hasSize(receiver);
            }
        }

        @Resolve(message = "GET_SIZE")
        public abstract static class RPairListGetSizeNode extends Node {
            @Child private RLengthNode lengthNode = RLengthNode.create();

            protected Object access(VirtualFrame frame, RPairList receiver) {
                return getSize(receiver, lengthNode);
            }
        }

        @Resolve(message = "IS_NULL")
        public abstract static class RPairListIsNullNode extends Node {
            protected Object access(RPairList receiver) {
                return isNull(receiver);
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        public abstract static class RPairListIsExecutableNode extends Node {
            protected Object access(@SuppressWarnings("unused") RPairList receiver) {
                return false;
            }
        }

        @Resolve(message = "READ")
        public abstract static class RPairListReadNode extends Node {
            @Child private ListReadImplNode read = ListReadImplNodeGen.create();

            protected Object access(VirtualFrame frame, RPairList receiver, Object identifier) {
                return read.execute(frame, receiver, identifier);
            }
        }

        @Resolve(message = "WRITE")
        public abstract static class RPairListWriteNode extends Node {
            @Child private ListWriteImplNode writeNode = ListWriteImplNodeGen.create();

            protected Object access(VirtualFrame frame, RPairList receiver, Object identifier, Object valueObj) {
                return writeNode.execute(receiver, identifier, valueObj);
            }
        }

        @Resolve(message = "KEYS")
        public abstract static class RPairListKeysNode extends Node {
            @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

            protected Object access(RPairList receiver) {
                return listKeys(receiver, getNamesNode);
            }
        }

        @Resolve(message = "KEY_INFO")
        public abstract static class RPairListKeyInfoNode extends Node {
            @Child private ListKeyInfoImplNode keyInfoNode = ListKeyInfoImplNodeGen.create();

            protected Object access(VirtualFrame frame, TruffleObject receiver, Object idx) {
                return keyInfoNode.execute(receiver, idx);
            }
        }

        @Resolve(message = "TO_NATIVE")
        public abstract static class RPairListToNativeNode extends Node {
            protected Object access(RPairList receiver) {
                return toNativePointer(receiver);
            }
        }

        @CanResolve
        public abstract static class RPairListCheck extends Node {
            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof RPairList;
            }
        }
    }

    abstract static class ListReadImplNode extends Node {
        @Child private ExtractVectorNode extract;
        @Child private R2Foreign r2Foreign;

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        protected abstract Object execute(VirtualFrame frame, TruffleObject receiver, Object idx);

        @Specialization
        protected Object read(VirtualFrame frame, TruffleObject receiver, double idx,
                        @Cached("createKeyInfoNode()") ListKeyInfoImplNode keyInfo) {
            return read(frame, receiver, (int) idx, keyInfo);
        }

        @Specialization
        protected Object read(VirtualFrame frame, TruffleObject receiver, long idx,
                        @Cached("createKeyInfoNode()") ListKeyInfoImplNode keyInfo) {
            return read(frame, receiver, (int) idx, keyInfo);
        }

        @Specialization
        protected Object read(VirtualFrame frame, TruffleObject receiver, int idx,
                        @Cached("createKeyInfoNode()") ListKeyInfoImplNode keyInfo) {
            int info = keyInfo.execute(receiver, idx);
            if (unknownIdentifier.profile(!KeyInfo.isExisting(info))) {
                throw UnknownIdentifierException.raise("" + idx);
            }
            initExtractNode();
            // idx + 1 R is indexing from 1
            Object value = extract.apply(receiver, new Object[]{idx + 1}, RLogical.valueOf(false), RMissing.instance);
            initR2ForeignNode();
            return r2Foreign.execute(value);
        }

        @Specialization
        protected Object read(VirtualFrame frame, TruffleObject receiver, String field,
                        @Cached("createKeyInfoNode()") ListKeyInfoImplNode keyInfo) {
            // reading by an unknown name returns null,
            // reading by an unknown index returns subscript out of bounds;
            // let's be consistent at this place, the name should be known to the caller anyway
            int info = keyInfo.execute(receiver, field);
            if (unknownIdentifier.profile(!KeyInfo.isExisting(info))) {
                throw UnknownIdentifierException.raise("" + field);
            }
            initExtractNode();
            Object value = extract.applyAccessField(receiver, field);
            initR2ForeignNode();
            return r2Foreign.execute(value);
        }

        @Fallback
        protected Object read(@SuppressWarnings("unused") TruffleObject receiver, Object field) {
            throw UnknownIdentifierException.raise("" + field);
        }

        protected ListKeyInfoImplNode createKeyInfoNode() {
            return ListKeyInfoImplNodeGen.create();
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

    abstract static class ListWriteImplNode extends Node {
        @Child private ReplaceVectorNode replace;
        @Child private Foreign2R foreign2R;

        protected abstract Object execute(TruffleObject receiver, Object identifier, Object valueObj);

        @Specialization
        protected Object write(TruffleObject receiver, int idx, Object valueObj) {
            // idx + 1 R is indexing from 1
            return write(receiver, new Object[]{idx + 1}, valueObj);
        }

        @Specialization
        protected Object write(TruffleObject receiver, String field, Object valueObj) {
            return write(receiver, new Object[]{field}, valueObj);
        }

        private Object write(TruffleObject receiver, Object[] positions, Object valueObj) {
            if (foreign2R == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                foreign2R = insert(Foreign2RNodeGen.create());
            }
            Object value = foreign2R.execute(valueObj);
            if (replace == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                replace = insert(ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
            return replace.apply(receiver, positions, value);
        }

        @Fallback
        protected Object write(@SuppressWarnings("unused") TruffleObject receiver, Object field, @SuppressWarnings("unused") Object object) {
            throw UnknownIdentifierException.raise("" + field);
        }
    }

    abstract static class ListKeyInfoImplNode extends Node {
        @Child private ExtractVectorNode extractNode;

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        abstract int execute(TruffleObject receiver, Object idx);

        @Specialization
        protected int keyInfo(TruffleObject receiver, int idx,
                        @Cached("createLengthNode()") RLengthNode lenghtNode) {
            return keyInfo(receiver, (double) idx, lenghtNode);
        }

        @Specialization
        protected int keyInfo(TruffleObject receiver, double idx,
                        @Cached("createLengthNode()") RLengthNode lengthNode) {

            int length = lengthNode.executeInteger(receiver);
            if (unknownIdentifier.profile(idx < 0 || idx >= length)) {
                return 0;
            }
            initExtractNode();
            return buildKeys(extractNode.apply(receiver, new Object[]{idx + 1}, RLogical.valueOf(false), RMissing.instance));
        }

        @Specialization
        protected int keyInfo(TruffleObject receiver, String identifier,
                        @Cached("createNamesNode()") GetNamesAttributeNode namesNode) {
            RStringVector names = namesNode.getNames(receiver);
            boolean exists = false;
            for (int i = 0; i < names.getLength(); i++) {
                if (identifier.equals(names.getDataAt(i))) {
                    exists = true;
                    break;
                }
            }
            if (unknownIdentifier.profile(!exists)) {
                return 0;
            }
            initExtractNode();
            return buildKeys(extractNode.applyAccessField(receiver, identifier));
        }

        protected RLengthNode createLengthNode() {
            return RLengthNode.create();
        }

        protected GetNamesAttributeNode createNamesNode() {
            return GetNamesAttributeNode.create();
        }

        private static int buildKeys(Object value) {
            Builder builder = KeyInfo.newBuilder();
            builder.setReadable(true).setWritable(true).setInvocable(value instanceof RFunction);
            return builder.build();
        }

        private void initExtractNode() {
            if (extractNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractNode = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
        }

        @Fallback
        protected int access(@SuppressWarnings("unused") TruffleObject receiver, @SuppressWarnings("unused") Object field) {
            return 0;
        }
    }

    private static boolean isBoxed(@SuppressWarnings("unused") TruffleObject receiver) {
        return false;
    }

    private static boolean isNull(@SuppressWarnings("unused") TruffleObject receiver) {
        return false;
    }

    private static boolean hasSize(@SuppressWarnings("unused") TruffleObject receiver) {
        return true;
    }

    private static Object getSize(TruffleObject receiver, RLengthNode lengthNode) {
        return lengthNode.executeInteger(receiver);
    }

    private static Object listKeys(TruffleObject receiver, GetNamesAttributeNode getNamesNode) {
        RStringVector names = getNamesNode.getNames(receiver);
        return names != null ? names : RNull.instance;
    }

    private static Object toNativePointer(RTruffleObject receiver) {
        return new NativePointer(receiver);
    }
}
