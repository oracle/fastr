/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.interop.pcre;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;

@MessageResolution(receiverType = CaptureNamesResult.class)
public class CaptureNamesResultMR {
    @CanResolve
    public abstract static class CaptureNamesCallbackCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof CaptureNamesResult;
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    public abstract static class CaptureNamesResultIsExecutable extends Node {
        protected Object access(@SuppressWarnings("unused") CaptureNamesResult receiver) {
            return true;
        }
    }

    @Resolve(message = "EXECUTE")
    public abstract static class CaptureNamesCallbackExecute extends Node {

        @Child private Node isNullNode = Message.IS_NULL.createNode();
        @Child private Node unboxNode = Message.UNBOX.createNode();

        protected Object access(CaptureNamesResult receiver, Object[] arguments) {
            try {
                Object arg1 = arguments[1];
                if (arg1 instanceof TruffleObject) {
                    if (ForeignAccess.sendIsNull(isNullNode, (TruffleObject) arg1)) {
                        arg1 = null;
                    } else {
                        arg1 = ForeignAccess.sendUnbox(unboxNode, (TruffleObject) arg1);
                    }
                }
                receiver.addName((int) arguments[0], (String) arg1);
                return receiver;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }
}
