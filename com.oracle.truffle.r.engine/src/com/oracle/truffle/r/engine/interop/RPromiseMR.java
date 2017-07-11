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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.TruffleRLanguageImpl;
import com.oracle.truffle.r.ffi.impl.interop.NativePointer;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.RCloseable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;

@MessageResolution(receiverType = RPromise.class)
public class RPromiseMR {

    private static final String PROP_VALUE = "value";
    private static final String PROP_IS_EVALUATED = "isEvaluated";
    private static final String PROP_EXPR = "expression";

    @Resolve(message = "IS_BOXED")
    public abstract static class RPromiseIsBoxedNode extends Node {
        protected Object access(@SuppressWarnings("unused") RPromise receiver) {
            return false;
        }
    }

    @Resolve(message = "IS_NULL")
    public abstract static class RPromiseIsNullNode extends Node {
        protected Object access(@SuppressWarnings("unused") RPromise receiver) {
            return false;
        }
    }

    @Resolve(message = "READ")
    public abstract static class RPromiseReadNode extends Node {

        @SuppressWarnings("try")
        @TruffleBoundary
        protected Object access(RPromise receiver, String field) {
            if (PROP_EXPR.equals(field)) {
                try (RCloseable c = RContext.withinContext(TruffleRLanguageImpl.getCurrentContext())) {
                    return RDataFactory.createLanguage(receiver.getRep());
                }
            }
            if (PROP_IS_EVALUATED.equals(field)) {
                return RRuntime.asLogical(receiver.isEvaluated());
            }
            if (PROP_VALUE.equals(field)) {
                // only read value if evaluated
                if (receiver.isEvaluated()) {
                    return receiver.getValue();
                }
                return RNull.instance;
            }
            throw UnknownIdentifierException.raise(field);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class RPromiseWriteNode extends Node {

        @SuppressWarnings("try")
        protected Object access(RPromise receiver, String field, Object valueObj) {
            if (PROP_IS_EVALUATED.equals(field)) {
                if (!(valueObj instanceof Boolean)) {
                    throw UnsupportedTypeException.raise(new Object[]{valueObj});
                }

                boolean newVal = (boolean) valueObj;

                if (!receiver.isEvaluated() && newVal) {
                    try (RCloseable c = RContext.withinContext(TruffleRLanguageImpl.getCurrentContext())) {
                        PromiseHelperNode.evaluateSlowPath(receiver);
                    }
                } else if (receiver.isEvaluated() && !newVal) {
                    try (RCloseable c = RContext.withinContext(TruffleRLanguageImpl.getCurrentContext())) {
                        receiver.resetValue();
                    }

                }
                return RRuntime.asLogical(receiver.isEvaluated());
            }
            throw UnknownIdentifierException.raise(field);
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class RPromiseKeysNode extends Node {

        protected Object access(@SuppressWarnings("unused") RPromise receiver) {
            return RDataFactory.createStringVector(new String[]{PROP_VALUE, PROP_IS_EVALUATED, PROP_EXPR}, true);
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class RPromiseKeyInfoNode extends Node {

        private static final int EXISTS = 1 << 0;
        private static final int READABLE = 1 << 1;
        private static final int WRITABLE = 1 << 2;

        protected Object access(@SuppressWarnings("unused") RPromise receiver, String identifier) {
            if (PROP_EXPR.equals(identifier) || PROP_VALUE.equals(identifier)) {
                return EXISTS + READABLE;
            }

            if (PROP_IS_EVALUATED.equals(identifier)) {
                return EXISTS + READABLE + WRITABLE;
            }
            return 0;
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class RPromiseToNativeNode extends Node {
        protected Object access(RPromise receiver) {
            return new NativePointer(receiver);
        }
    }

    @CanResolve
    public abstract static class RPromiseCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RPromise;
        }
    }
}
