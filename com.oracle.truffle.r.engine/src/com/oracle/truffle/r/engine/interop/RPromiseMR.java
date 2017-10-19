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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.engine.interop.RPromiseMRFactory.RPromiseKeyInfoImplNodeGen;
import com.oracle.truffle.r.engine.interop.RPromiseMRFactory.RPromiseReadImplNodeGen;
import com.oracle.truffle.r.engine.interop.RPromiseMRFactory.RPromiseWriteImplNodeGen;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;

@MessageResolution(receiverType = RPromise.class)
public class RPromiseMR {

    private static final String PROP_VALUE = "value";
    private static final String PROP_IS_EVALUATED = "isEvaluated";
    private static final String PROP_EXPR = "expression";

    @Resolve(message = "READ")
    public abstract static class RPromiseReadNode extends Node {
        @Child private RPromiseReadImplNode readNode = RPromiseReadImplNodeGen.create();

        @TruffleBoundary
        protected Object access(RPromise receiver, String field) {
            return readNode.execute(receiver, field);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class RPromiseWriteNode extends Node {
        @Child private RPromiseWriteImplNode writeNode = RPromiseWriteImplNodeGen.create();

        protected Object access(RPromise receiver, String field, Object valueObj) {
            return writeNode.execute(receiver, field, valueObj);
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
        @Child private RPromiseKeyInfoImplNode keyInfoNode = RPromiseKeyInfoImplNodeGen.create();

        protected Object access(RPromise receiver, String identifier) {
            return keyInfoNode.execute(receiver, identifier);
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class RPromiseToNativeNode extends Node {
        protected Object access(RPromise receiver) {
            return NativeDataAccess.toNative(receiver);
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
    public abstract static class RPromiseCheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof RPromise;
        }
    }

    abstract static class RPromiseWriteImplNode extends Node {

        protected abstract Object execute(RPromise receiver, Object identifier, Object valueObj);

        @Specialization
        protected Object access(RPromise receiver, String identifier, Object valueObj) {
            if (PROP_IS_EVALUATED.equals(identifier)) {
                if (!(valueObj instanceof Boolean)) {
                    throw UnsupportedTypeException.raise(new Object[]{valueObj});
                }

                boolean newVal = (boolean) valueObj;
                if (!receiver.isEvaluated() && newVal) {
                    PromiseHelperNode.evaluateSlowPath(receiver);
                } else if (receiver.isEvaluated() && !newVal) {
                    receiver.resetValue();
                }
                return RRuntime.asLogical(receiver.isEvaluated());
            }
            throw UnknownIdentifierException.raise(identifier);
        }

        @Fallback
        protected Object access(@SuppressWarnings("unused") RPromise receiver, Object identifier, @SuppressWarnings("unused") Object valueObj) {
            throw UnknownIdentifierException.raise("" + identifier);
        }
    }

    abstract static class RPromiseReadImplNode extends Node {

        protected abstract Object execute(RPromise receiver, Object identifier);

        @Specialization
        protected Object access(RPromise receiver, String identifier) {
            if (PROP_EXPR.equals(identifier)) {
                return RDataFactory.createLanguage(receiver.getClosure());
            }
            if (PROP_IS_EVALUATED.equals(identifier)) {
                return RRuntime.asLogical(receiver.isEvaluated());
            }
            if (PROP_VALUE.equals(identifier)) {
                // only read value if evaluated
                if (receiver.isEvaluated()) {
                    return receiver.getValue();
                }
                return RNull.instance;
            }
            throw UnknownIdentifierException.raise(identifier);
        }

        @Fallback
        protected Object access(@SuppressWarnings("unused") RPromise receiver, Object identifier) {
            throw UnknownIdentifierException.raise("" + identifier);
        }
    }

    abstract static class RPromiseKeyInfoImplNode extends Node {

        protected abstract Object execute(RPromise receiver, Object identifier);

        @Specialization
        protected Object access(@SuppressWarnings("unused") RPromise receiver, String identifier) {
            if (PROP_EXPR.equals(identifier) || PROP_VALUE.equals(identifier)) {
                return KeyInfo.newBuilder().setReadable(true).build();
            } else if (PROP_IS_EVALUATED.equals(identifier)) {
                return KeyInfo.newBuilder().setReadable(true).setWritable(true).build();
            } else {
                return 0;
            }
        }

        @Fallback
        protected Object access(@SuppressWarnings("unused") RPromise receiver, @SuppressWarnings("unused") Object identifier) {
            return 0;
        }
    }
}
