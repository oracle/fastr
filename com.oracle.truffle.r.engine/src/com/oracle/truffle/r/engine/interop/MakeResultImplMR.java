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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.engine.interop.ffi.nfi.TruffleNFI_PCRE;
import com.oracle.truffle.r.runtime.RInternalError;

@MessageResolution(receiverType = TruffleNFI_PCRE.TruffleNFI_CompileNode.MakeResultImpl.class, language = TruffleRLanguage.class)
public class MakeResultImplMR {
    @CanResolve
    public abstract static class MakeResultImplCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof TruffleNFI_PCRE.TruffleNFI_CompileNode.MakeResultImpl;
        }
    }

    @Resolve(message = "EXECUTE")
    public abstract static class MakeResultImplExecute extends Node {
        protected Object access(@SuppressWarnings("unused") VirtualFrame frame, TruffleNFI_PCRE.TruffleNFI_CompileNode.MakeResultImpl receiver, Object[] arguments) {
            try {
                Object arg1 = arguments[1];
                if (arg1 instanceof TruffleObject) {
                    if (ForeignAccess.sendIsNull(Message.IS_NULL.createNode(), (TruffleObject) arg1)) {
                        arg1 = null;
                    } else {
                        arg1 = ForeignAccess.sendUnbox(Message.UNBOX.createNode(), (TruffleObject) arg1);
                    }
                }
                receiver.makeresult((long) arguments[0], (String) arg1, (int) arguments[2]);
                return receiver;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

}
