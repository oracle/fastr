/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.interop.RNullMRContextState;
import com.oracle.truffle.r.runtime.interop.RObjectNativeWrapper;

@MessageResolution(receiverType = RNull.class)
public class RNullMR {

    @Resolve(message = "IS_NULL")
    public abstract static class RNullIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RNull receiver) {
            return RContext.getInstance().stateRNullMR.isNull();
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RNullKeyInfoNode extends Node {
        protected Object access(@SuppressWarnings("unused") TruffleObject receiver, @SuppressWarnings("unused") Object identifier) {
            return 0;
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class RNullToNativeNode extends Node {
        protected Object access(RNull receiver) {
            return new RObjectNativeWrapper(receiver);
        }
    }

    @Resolve(message = "IS_POINTER")
    public abstract static class IsPointerNode extends Node {
        protected boolean access(@SuppressWarnings("unused") Object receiver) {
            return false;
        }
    }

    @CanResolve
    public abstract static class RNullCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RNull;
        }
    }

    /**
     * Workaround to avoid NFI converting {@link RNull} to {@code null}.
     */
    static boolean setIsNull(boolean value) {
        RNullMRContextState state = RContext.getInstance().stateRNullMR;
        boolean prev = state.isNull();
        state.setIsNull(value);
        return prev;
    }
}
