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
package com.oracle.truffle.r.ffi.impl.common;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public final class UpCallUnwrap extends Node {

    @Child private Node isBoxed;
    @Child private Node unbox;
    private final BranchProfile nativePointerProfile = isLLVM() ? BranchProfile.create() : null;

    /**
     * There are three possibilities as enumerated below.
     * <ul>
     * <li>For an {@link RTruffleObject} there is nothing to do, and indeed, calling {@code unbox}
     * would be disastrous, as that means, e.g., for a RVector, extract the first element!</li>
     * <li>Or we could get a {@code TruffleObject} from another language domain, e.g a
     * {@code JavaObject} that wraps, say, an {@code Integer}.S Such a value has to be unboxed.
     * Similarly a {@code NativePointer} encoding, say, a C char array. One special case in the LLVM
     * implementation is {@code NativePointer} that represents an object stored to memory, which
     * requires a lookup (and not an {@code UNBOX}).</li>
     * <li>We could also get a plain {@link Integer} or similar type in which case there is nothing
     * to do.</li>
     * </ul>
     */
    public Object unwrap(Object x) {
        if (x instanceof RTruffleObject) {
            return x;
        } else if (x instanceof TruffleObject) {
            if (isBoxed == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isBoxed = insert(Message.IS_BOXED.createNode());
            }
            TruffleObject xTo = (TruffleObject) x;
            if (ForeignAccess.sendIsBoxed(isBoxed, xTo)) {
                if (unbox == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    unbox = insert(Message.UNBOX.createNode());
                }
                try {
                    return ForeignAccess.sendUnbox(unbox, xTo);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere(e, "UNBOX message fails after IS_BOXED=true");
                }
            } else {
                // didn't UNBOX or really was null (e.g. null String)
                if (isLLVM()) {
                    nativePointerProfile.enter();
                    TruffleObject xtoObject = checkNativePointer(xTo);
                    if (xtoObject != null) {
                        return xtoObject;
                    }
                }
                return x;
            }
        } else {
            return x;
        }
    }

    private static boolean isLLVM() {
        return RFFIFactory.getType() == RFFIFactory.Type.LLVM;
    }

    @TruffleBoundary
    private static TruffleObject checkNativePointer(TruffleObject xto) {
        return NativePointer.check(xto);
    }
}
