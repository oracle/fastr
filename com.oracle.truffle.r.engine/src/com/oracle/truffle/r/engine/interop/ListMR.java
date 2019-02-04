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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.engine.interop.ListMRFactory.ListKeyInfoImplNodeGen;
import com.oracle.truffle.r.engine.interop.ListMRFactory.ListReadImplNodeGen;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.interop.R2Foreign;

public class ListMR {

    @MessageResolution(receiverType = RList.class)
    public static class RListMR extends ListMR {

        @Resolve(message = "HAS_SIZE")
        public abstract static class RListHasSizeNode extends Node {
            protected Object access(RList receiver) {
                return hasSize(receiver);
            }
        }

        @Resolve(message = "GET_SIZE")
        public abstract static class RListGetSizeNode extends Node {
            @Child private RLengthNode lengthNode = RLengthNode.create();

            protected Object access(RList receiver) {
                return getSize(receiver, lengthNode);
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
            @SuppressWarnings("unused")
            protected Object access(RList receiver, Object identifier, Object valueObj) {
                throw UnsupportedMessageException.raise(Message.WRITE);
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

            protected Object access(TruffleObject receiver, Object idx) {
                return keyInfoNode.execute(receiver, idx);
            }
        }

        @Resolve(message = "IS_POINTER")
        public abstract static class IsPointerNode extends Node {
            protected boolean access(@SuppressWarnings("unused") Object receiver) {
                return true;
            }
        }

        @Resolve(message = "AS_POINTER")
        public abstract static class AsPointerNode extends Node {
            protected Object access(Object receiver) {
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
        public abstract static class RListCheck extends Node {

            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof RList;
            }
        }
    }

    @MessageResolution(receiverType = RExpression.class)
    public static class RExpressionMR extends ListMR {
        @Resolve(message = "IS_BOXED")
        public abstract static class RExpressionIsBoxedNode extends Node {
            protected Object access(RExpression receiver) {
                return isBoxed(receiver);
            }
        }

        @Resolve(message = "HAS_SIZE")
        public abstract static class RExpressionHasSizeNode extends Node {
            protected Object access(RExpression receiver) {
                return hasSize(receiver);
            }
        }

        @Resolve(message = "GET_SIZE")
        public abstract static class RExpressionGetSizeNode extends Node {
            @Child private RLengthNode lengthNode = RLengthNode.create();

            protected Object access(RExpression receiver) {
                return getSize(receiver, lengthNode);
            }
        }

        @Resolve(message = "IS_NULL")
        public abstract static class RExpressionIsNullNode extends Node {
            protected Object access(RExpression receiver) {
                return isNull(receiver);
            }
        }

        @Resolve(message = "READ")
        public abstract static class RExpressionReadNode extends Node {
            @Child private ListReadImplNode read = ListReadImplNodeGen.create();

            protected Object access(VirtualFrame frame, RExpression receiver, Object identifier) {
                return read.execute(frame, receiver, identifier);
            }
        }

        @Resolve(message = "WRITE")
        public abstract static class RExpressionWriteNode extends Node {
            @SuppressWarnings("unused")
            protected Object access(RExpression receiver, Object identifier, Object valueObj) {
                throw UnsupportedMessageException.raise(Message.WRITE);
            }
        }

        @Resolve(message = "KEYS")
        public abstract static class RExpressionKeysNode extends Node {
            @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

            protected Object access(RExpression receiver) {
                return listKeys(receiver, getNamesNode);
            }
        }

        @Resolve(message = "KEY_INFO")
        public abstract static class RExpressionKeyInfoNode extends Node {
            @Child private ListKeyInfoImplNode keyInfoNode = ListKeyInfoImplNodeGen.create();

            protected Object access(TruffleObject receiver, Object idx) {
                return keyInfoNode.execute(receiver, idx);
            }
        }

        @Resolve(message = "IS_POINTER")
        public abstract static class IsPointerNode extends Node {
            protected boolean access(@SuppressWarnings("unused") Object receiver) {
                return true;
            }
        }

        @Resolve(message = "AS_POINTER")
        public abstract static class AsPointerNode extends Node {
            protected Object access(Object receiver) {
                return NativeDataAccess.asPointer(receiver);
            }
        }

        @Resolve(message = "TO_NATIVE")
        public abstract static class ToNativeNode extends Node {
            protected Object access(@SuppressWarnings("unused") Object receiver) {
                return receiver;
            }
        }

        @CanResolve
        public abstract static class RExpressionCheck extends Node {

            protected static boolean test(TruffleObject receiver) {
                return receiver instanceof RExpression;
            }
        }
    }

    @MessageResolution(receiverType = RPairList.class)
    public static class RPairListMR {

        @Resolve(message = "HAS_SIZE")
        public abstract static class RPairListHasSizeNode extends Node {
            protected Object access(RPairList receiver) {
                return hasSize(receiver);
            }
        }

        @Resolve(message = "GET_SIZE")
        public abstract static class RPairListGetSizeNode extends Node {
            @Child private RLengthNode lengthNode = RLengthNode.create();

            protected Object access(RPairList receiver) {
                return getSize(receiver, lengthNode);
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
            @SuppressWarnings("unused")
            protected Object access(RPairList receiver, Object identifier, Object valueObj) {
                throw UnsupportedMessageException.raise(Message.WRITE);
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

            protected Object access(TruffleObject receiver, Object idx) {
                return keyInfoNode.execute(receiver, idx);
            }
        }

        @Resolve(message = "IS_POINTER")
        public abstract static class IsPointerNode extends Node {
            protected boolean access(@SuppressWarnings("unused") Object receiver) {
                return true;
            }
        }

        @Resolve(message = "AS_POINTER")
        public abstract static class AsPointerNode extends Node {
            protected Object access(Object receiver) {
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
        protected Object read(TruffleObject receiver, double idx,
                        @Cached("createKeyInfoNode()") ListKeyInfoImplNode keyInfo) {
            return read(receiver, (int) idx, keyInfo);
        }

        @Specialization
        protected Object read(TruffleObject receiver, long idx,
                        @Cached("createKeyInfoNode()") ListKeyInfoImplNode keyInfo) {
            return read(receiver, (int) idx, keyInfo);
        }

        @Specialization
        protected Object read(TruffleObject receiver, int idx,
                        @Cached("createKeyInfoNode()") ListKeyInfoImplNode keyInfo) {
            int info = keyInfo.execute(receiver, idx);
            if (unknownIdentifier.profile(!KeyInfo.isExisting(info))) {
                throw UnknownIdentifierException.raise(String.valueOf(idx));
            }
            initExtractNode();
            // idx + 1 R is indexing from 1
            Object value = extract.apply(receiver, new Object[]{idx + 1}, RLogical.valueOf(false), RMissing.instance);
            initR2ForeignNode();
            return r2Foreign.execute(value);
        }

        @Specialization
        protected Object read(TruffleObject receiver, String field,
                        @Cached("createKeyInfoNode()") ListKeyInfoImplNode keyInfo) {
            // reading by an unknown name returns null,
            // reading by an unknown index returns subscript out of bounds;
            // let's be consistent at this place, the name should be known to the caller anyway
            int info = keyInfo.execute(receiver, field);
            if (unknownIdentifier.profile(!KeyInfo.isExisting(info))) {
                throw UnknownIdentifierException.raise(field);
            }
            initExtractNode();
            Object value = extract.applyAccessField(receiver, field);
            initR2ForeignNode();
            return r2Foreign.execute(value);
        }

        @Fallback
        protected Object read(@SuppressWarnings("unused") TruffleObject receiver, Object field) {
            throw UnknownIdentifierException.raise(String.valueOf(field));
        }

        protected ListKeyInfoImplNode createKeyInfoNode() {
            return ListKeyInfoImplNodeGen.create();
        }

        private void initR2ForeignNode() {
            if (r2Foreign == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                r2Foreign = insert(R2Foreign.create());
            }
        }

        private void initExtractNode() {
            if (extract == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extract = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
        }
    }

    abstract static class ListKeyInfoImplNode extends Node {
        @Child private ExtractVectorNode extractNode;

        abstract int execute(TruffleObject receiver, Object idx);

        @Specialization
        protected int keyInfo(TruffleObject receiver, int idx,
                        @Cached("createBinaryProfile()") ConditionProfile outOfBounds,
                        @Cached("createLengthNode()") RLengthNode lenghtNode) {
            return keyInfo(receiver, (double) idx, outOfBounds, lenghtNode);
        }

        @Specialization
        protected int keyInfo(TruffleObject receiver, double idx,
                        @Cached("createBinaryProfile()") ConditionProfile outOfBounds,
                        @Cached("createLengthNode()") RLengthNode lengthNode) {

            int length = lengthNode.executeInteger(receiver);
            if (outOfBounds.profile(idx < 0 || idx >= length)) {
                return KeyInfo.NONE;
            }
            initExtractNode();
            return buildKeys(extractNode.apply(receiver, new Object[]{idx + 1}, RLogical.valueOf(false), RMissing.instance));
        }

        @Specialization
        protected int keyInfo(TruffleObject receiver, String identifier,
                        @Cached("createBinaryProfile()") ConditionProfile noNames,
                        @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                        @Cached("createNamesNode()") GetNamesAttributeNode namesNode) {
            RStringVector names = namesNode.getNames(receiver);
            if (noNames.profile(names == null)) {
                return KeyInfo.NONE;
            }
            boolean exists = false;
            for (int i = 0; i < names.getLength(); i++) {
                if (identifier.equals(names.getDataAt(i))) {
                    exists = true;
                    break;
                }
            }
            if (unknownIdentifier.profile(!exists)) {
                return KeyInfo.NONE;
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
            int result = KeyInfo.READABLE;
            if (value instanceof RFunction) {
                result |= KeyInfo.INVOCABLE;
            }
            return result;
        }

        private void initExtractNode() {
            if (extractNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractNode = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
        }

        @Fallback
        protected int access(@SuppressWarnings("unused") TruffleObject receiver, @SuppressWarnings("unused") Object field) {
            return KeyInfo.NONE;
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
        return names != null ? names : RDataFactory.createEmptyStringVector();
    }
}
