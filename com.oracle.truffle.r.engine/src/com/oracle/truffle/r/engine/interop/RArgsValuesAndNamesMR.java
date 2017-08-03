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
import com.oracle.truffle.r.engine.interop.RArgsValuesAndNamesMRFactory.RArgsValuesAndNamesKeyInfoImplNodeGen;
import com.oracle.truffle.r.engine.interop.RArgsValuesAndNamesMRFactory.RArgsValuesAndNamesReadImplNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;

@MessageResolution(receiverType = RArgsValuesAndNames.class)
public class RArgsValuesAndNamesMR {
    @Resolve(message = "IS_BOXED")
    public abstract static class RArgsValuesAndNamesIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RArgsValuesAndNames receiver) {
            return false;
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class RArgsValuesAndNamesHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") RArgsValuesAndNames receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class RArgsValuesAndNamesGetSizeNode extends Node {
        protected Object access(RArgsValuesAndNames receiver) {
            return receiver.getLength();
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RArgsValuesAndNamesIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RArgsValuesAndNames receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RArgsValuesAndNamesReadNode extends Node {
        @Child RArgsValuesAndNamesReadImplNode readNode = RArgsValuesAndNamesReadImplNodeGen.create();

        protected Object access(RArgsValuesAndNames receiver, Object identifier) {
            return readNode.execute(receiver, identifier);
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class RArgsValuesAndNamesKeysNode extends Node {
        protected Object access(RArgsValuesAndNames receiver) {
            return RDataFactory.createStringVector(receiver.getSignature().getNames(), RDataFactory.COMPLETE_VECTOR);
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RArgsValuesAndNamesKeyInfoNode extends Node {
        @Child private RArgsValuesAndNamesKeyInfoImplNode keyInfoNode = RArgsValuesAndNamesKeyInfoImplNodeGen.create();

        protected Object access(VirtualFrame frame, RArgsValuesAndNames receiver, Object obj) {
            return keyInfoNode.execute(frame, receiver, obj);
        }
    }

    @CanResolve
    public abstract static class RArgsValuesAndNamesCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RArgsValuesAndNames;
        }
    }

    abstract static class RArgsValuesAndNamesReadImplNode extends Node {
        @Child private R2Foreign r2Foreign = R2ForeignNodeGen.create();

        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        abstract Object execute(RArgsValuesAndNames receiver, Object identifier);

        @Specialization
        protected Object access(RArgsValuesAndNames receiver, int index) {
            if (unknownIdentifier.profile(index < 0 || index >= receiver.getLength())) {
                throw UnknownIdentifierException.raise("" + index);
            }
            Object value = receiver.getArgument(index);
            return r2Foreign.execute(value);
        }

        @Specialization
        protected Object access(RArgsValuesAndNames receiver, String identifier) {
            ArgumentsSignature sig = receiver.getSignature();
            String[] names = sig.getNames();
            int idx = -1;
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(identifier)) {
                    idx = i;
                    break;
                }
            }
            if (unknownIdentifier.profile(idx < 0)) {
                throw UnknownIdentifierException.raise("" + identifier);
            }

            Object value = receiver.getArgument(idx);
            return r2Foreign.execute(value);
        }

        @Fallback
        protected Object access(@SuppressWarnings("unused") RArgsValuesAndNames receiver, Object identifier) {
            throw UnknownIdentifierException.raise("" + identifier);
        }
    }

    abstract static class RArgsValuesAndNamesKeyInfoImplNode extends Node {
        private final ConditionProfile unknownIdentifier = ConditionProfile.createBinaryProfile();

        abstract Object execute(VirtualFrame frame, RArgsValuesAndNames receiver, Object idx);

        @Specialization
        protected Object access(RArgsValuesAndNames receiver, int idx) {
            if (unknownIdentifier.profile(idx < 0 || idx >= receiver.getLength())) {
                return 0;
            }
            return createKeyInfo(receiver, idx);
        }

        @Specialization
        protected Object access(RArgsValuesAndNames receiver, String identifier) {
            ArgumentsSignature sig = receiver.getSignature();
            String[] names = sig.getNames();
            int idx = -1;
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(identifier)) {
                    idx = i;
                    break;
                }
            }
            if (unknownIdentifier.profile(idx < 0)) {
                return 0;
            }

            return createKeyInfo(receiver, idx);
        }

        private static Object createKeyInfo(RArgsValuesAndNames receiver, int idx) {
            KeyInfo.Builder builder = KeyInfo.newBuilder();
            builder.setReadable(true).setInvocable(receiver.getArgument(idx) instanceof RFunction);
            return builder.build();
        }

        @Fallback
        protected Object access(@SuppressWarnings("unused") RArgsValuesAndNames receiver, @SuppressWarnings("unused") Object field) {
            return 0;
        }
    }
}
