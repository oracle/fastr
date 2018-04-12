/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.interop.UnsafeAdapter;

public final class TryRfEvalNode extends FFIUpCallNode.Arg4 {
    @Child private RfEvalNode rfEvalNode = RfEvalNode.create();
    @Child private Node isNullNode = Message.IS_NULL.createNode();
    @Child private Node writeErrorFlagNode;
    @Child private Node isPointerNode;
    @Child private Node asPointerNode;

    @Override
    public Object executeObject(Object expr, Object env, Object errorFlag, Object silent) {
        Object handlerStack = RErrorHandling.getHandlerStack();
        Object restartStack = RErrorHandling.getRestartStack();
        boolean ok = true;
        Object result = RNull.instance;
        try {
            // TODO handle silent
            RErrorHandling.resetStacks();
            result = rfEvalNode.executeObject(expr, env);
        } catch (Throwable t) {
            ok = false;
            result = RNull.instance;
        } finally {
            RErrorHandling.restoreStacks(handlerStack, restartStack);
        }
        TruffleObject errorFlagTO = (TruffleObject) errorFlag;
        if (!ForeignAccess.sendIsNull(isNullNode, errorFlagTO)) {
            if (isPointerNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isPointerNode = insert(Message.IS_POINTER.createNode());
            }
            if (ForeignAccess.sendIsPointer(isPointerNode, errorFlagTO)) {
                if (asPointerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    asPointerNode = insert(Message.AS_POINTER.createNode());
                }
                long errorFlagPtr;
                try {
                    errorFlagPtr = ForeignAccess.sendAsPointer(asPointerNode, errorFlagTO);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
                UnsafeAdapter.UNSAFE.putInt(errorFlagPtr, ok ? 0 : 1);
            } else {
                try {
                    ForeignAccess.sendWrite(writeErrorFlagNode, errorFlagTO, 0, 1);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere("Rf_tryEval errorFlag should be either pointer or support WRITE message");
                }
            }
        }
        return result;
    }
}
