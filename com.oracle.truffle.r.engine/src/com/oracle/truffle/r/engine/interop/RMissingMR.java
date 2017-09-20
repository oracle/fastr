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

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RMissing;

@MessageResolution(receiverType = RMissing.class)
public class RMissingMR {

    @Resolve(message = "KEY_INFO")
    public abstract static class RMissingKeyInfoNode extends Node {
        protected Object access(@SuppressWarnings("unused") TruffleObject receiver, @SuppressWarnings("unused") Object identifier) {
            return 0;
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
    public abstract static class RMissingCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RMissing;
        }
    }
}
