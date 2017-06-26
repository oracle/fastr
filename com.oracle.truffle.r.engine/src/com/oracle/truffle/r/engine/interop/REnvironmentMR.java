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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.TruffleRLanguageImpl;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.RCloseable;
import com.oracle.truffle.r.runtime.env.REnvironment;

@MessageResolution(receiverType = REnvironment.class)
public class REnvironmentMR {

    @Resolve(message = "IS_BOXED")
    public abstract static class REnvironmentIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") REnvironment receiver) {
            return false;
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class REnvironmentToNativeNode extends Node {
        protected Object access(REnvironment receiver) {
            return new NativePointer(receiver);
        }

    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class REnvironmentHasSizeNode extends Node {
        protected Object access(@SuppressWarnings("unused") REnvironment receiver) {
            return true;
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class REnvironmentIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") REnvironment receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class REnvironmentReadNode extends Node {
        @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

        @SuppressWarnings("try")
        protected Object access(VirtualFrame frame, REnvironment receiver, String field) {
            try (RCloseable c = RContext.withinContext(TruffleRLanguageImpl.getCurrentContext())) {
                return extract.applyAccessField(frame, receiver, field);
            }
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class REnvironmentWriteNode extends Node {
        @Child private ReplaceVectorNode extract = ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, true);

        @SuppressWarnings("try")
        protected Object access(VirtualFrame frame, REnvironment receiver, String field, Object valueObj) {
            try (RCloseable c = RContext.withinContext(TruffleRLanguageImpl.getCurrentContext())) {
                Object value = javaToRPrimitive(valueObj);
                Object x = extract.apply(frame, receiver, new Object[]{field}, value);
                return x;
            }
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

        private static final int READABLE = 1 << 1;
        private static final int WRITABLE = 1 << 2;

        protected Object access(@SuppressWarnings("unused") VirtualFrame frame, REnvironment receiver, String identifier) {
            Object val = receiver.get(identifier);
            if (val == null) {
                return 0;
            }

            int info = READABLE;
            if (!receiver.isLocked() && !receiver.bindingIsLocked(identifier)) {
                info += WRITABLE;
            }
            return info;
        }
    }

    @CanResolve
    public abstract static class REnvironmentCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof REnvironment;
        }
    }
}
