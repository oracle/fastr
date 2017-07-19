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

import static com.oracle.truffle.r.engine.interop.Utils.javaToRPrimitive;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.KeyInfo.Builder;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;

@MessageResolution(receiverType = RList.class)
public class RListMR {

    @Resolve(message = "IS_BOXED")
    public abstract static class RListIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RList receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RListHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RList receiver) {
            return true;
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RListIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RList receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RListReadNode extends Node {
        @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

        protected Object access(VirtualFrame frame, RList receiver, String field) {
            return extract.applyAccessField(frame, receiver, field);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class RListWriteNode extends Node {
        @Child private ReplaceVectorNode replace = ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

        protected Object access(VirtualFrame frame, RList receiver, String field, Object valueObj) {
            Object value = javaToRPrimitive(valueObj);
            return replace.apply(frame, receiver, new Object[]{field}, value);
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class RListKeysNode extends Node {
        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

        protected Object access(RList receiver) {
            RStringVector names = getNamesNode.getNames(receiver);
            return names != null ? names : RNull.instance;
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RListKeyInfoNode extends Node {
        @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
        @Child private ExtractVectorNode extractNode;

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        protected Object access(VirtualFrame frame, RList receiver, String identifier) {
            RStringVector names = getNamesNode.getNames(receiver);
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

            Builder builder = KeyInfo.newBuilder();
            builder.setReadable(true).setWritable(true);
            if (extractNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractNode = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true));
            }
            Object value = extractNode.applyAccessField(frame, receiver, identifier);
            builder.setInvocable(value instanceof RFunction);
            return builder.build();
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class RListToNativeNode extends Node {
        protected Object access(RList receiver) {
            return new NativePointer(receiver);
        }
    }

    @CanResolve
    public abstract static class RListCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RList;
        }
    }
}
