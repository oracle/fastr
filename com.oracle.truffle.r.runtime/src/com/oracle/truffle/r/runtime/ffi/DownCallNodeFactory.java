/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Factory for RFFI implementation specific {@link DownCallNode} which is responsible for
 * implementing the invocation of functions from {@link NativeFunction}.
 */
public abstract class DownCallNodeFactory {
    public abstract DownCallNode createDownCallNode();

    /**
     * This node has RFFI backend (LLVM/NFI) specific implementation and its purpose is to provide
     * functionality to invoke functions from {@link NativeFunction}.
     */
    @GenerateUncached
    public abstract static class DownCallNode extends Node {

        protected abstract Object execute(Frame frame, NativeFunction f, Object[] args);

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
        public final Object call(NativeFunction f, Object... args) {
            return call(null, f, args);
        }

        public final Object call(Frame frame, NativeFunction f, Object... args) {
            assert f != null;
            return execute(frame, f, args);
        }

        /**
         * The implementations should declare one specialization that forwards to this method.
         */
        protected Object doCallImpl(Frame frame, NativeFunction f, Object[] args) {
            CompilerAsserts.partialEvaluationConstant(f);
            TruffleObject target = createTarget(RContext.getInstance(this), f);
            Object before = -1;
            try {
                before = beforeCall(frame, f, target, args);
                return InteropLibrary.getFactory().getUncached().execute(target, args);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            } finally {
                afterCall(frame, before, f, target, args);
            }
        }

        /**
         * Should return a {@link TruffleObject} that will invoke the given function upon the
         * {@code EXECUTE} message.
         */
        protected abstract TruffleObject createTarget(RContext ctx, NativeFunction f);

        /**
         * Allows to transform the arguments before the execute message is sent to the result of
         * {@link #createTarget(RContext, NativeFunction)}.
         */
        protected abstract Object beforeCall(Frame frame, NativeFunction nativeFunction, TruffleObject f, Object[] args);

        /**
         * Allows to post-process the arguments after the execute message was sent to the result of
         * {@link #createTarget(RContext, NativeFunction)}. If the call to
         * {@link #beforeCall(Frame, NativeFunction, TruffleObject, Object[])} was not successful,
         * the {@code before} parameter will have value {@code -1}.
         */
        protected abstract void afterCall(Frame frame, Object before, NativeFunction f, TruffleObject t, Object[] args);

        protected static MaterializedFrame maybeMaterializeFrame(Frame frame, NativeFunction nativeFunction) {
            return frame == null || !nativeFunction.hasComplexInteraction() ? null : frame.materialize();
        }

    }
}
