/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.upcalls;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.data.RTruffleObject;

/**
 * Base class for all Truffle objects representing an up-call from native/LLVM code to FastR.
 */
public class UpCallBase implements RTruffleObject {

    /**
     * Intended to be called from {@code assert} and hence take no effect if assertions are
     * disabled.
     */
    protected static boolean reportException(Throwable ex) {
        System.err.println("ERROR: exception in up-call: " + ex.getClass().getSimpleName());
        System.err.println(ex.getMessage());
        return true;
    }

    @GenerateUncached
    protected abstract static class CallNode extends Node {
        public final Object call(CallTarget target, Object... args) {
            return execute(target, args);
        }

        public abstract Object execute(CallTarget target, Object[] args);

        @Specialization(guards = "cachedTarget == target", limit = "1")
        protected Object doCall(@SuppressWarnings("unused") CallTarget target, Object[] args,
                        @SuppressWarnings("unused") @Cached("target") CallTarget cachedTarget,
                        @Cached("create(cachedTarget)") DirectCallNode callNode) {
            return callNode.call(args);
        }

        @Specialization(replaces = "doCall")
        protected Object doCallGeneric(CallTarget target, Object[] args,
                        @Cached("create()") IndirectCallNode callNode) {
            return callNode.call(target, args);
        }
    }
}
