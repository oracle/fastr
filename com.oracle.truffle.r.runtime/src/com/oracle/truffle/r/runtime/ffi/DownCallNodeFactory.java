/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;

/**
 * Factory for RFFI implementation specific {@link DownCallNode} which is responsible for
 * implementing the invocation of functions from {@link NativeFunction}.
 */
public abstract class DownCallNodeFactory {
    public abstract DownCallNode createDownCallNode(NativeFunction function);

    /**
     * This node has RFFI backend (LLVM/NFI) specific implementation and its purpose is to provide
     * functionality to invoke functions from {@link NativeFunction}.
     */
    public abstract static class DownCallNode extends Node {

        private final NativeFunction function;
        @Child private Node message;
        // TODO: can this be really shared across contexts?
        @CompilationFinal private TruffleObject target;

        protected DownCallNode(NativeFunction function) {
            assert function != null;
            this.function = function;
        }

        /**
         * The arguments may contain primitive java types, Strings, arrays of any primitive Java
         * types, {@link TruffleObject}s,
         * {@link com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray}s and
         * {@link com.oracle.truffle.r.runtime.ffi.interop.NativeRawArray}s. {@link TruffleObject}
         * should be passed to LLVM/NFI as is.
         * {@link com.oracle.truffle.r.runtime.ffi.interop.NativeRawArray} and
         * {@link com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray} have special handling in
         * NFI where the array should be passed as Java array, not as Truffle Object.
         */
        public final Object call(Object... args) {
            try {
                if (message == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    message = insert(Message.createExecute(function.getArgumentCount()).createNode());
                }
                if (target == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    target = getTarget(function);
                }
                wrapArguments(target, args);
                return ForeignAccess.sendExecute(message, target, args);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            } finally {
                finishArguments(args);
            }
        }

        /**
         * Should return a {@link TruffleObject} that will invoke the given function upon the
         * {@code EXECUTE} message.
         */
        protected abstract TruffleObject getTarget(NativeFunction function);

        /**
         * Allows to transform the arguments before the execute message is sent to the result of
         * {@link #getTarget(NativeFunction)}. Allways invoked even if {@code args.length == 0}.
         */
        protected abstract void wrapArguments(TruffleObject function, Object[] args);

        /**
         * Allows to post-process the arguments after the execute message was sent to the result of
         * {@link #getTarget(NativeFunction)}.
         */
        protected abstract void finishArguments(Object[] args);
    }
}
