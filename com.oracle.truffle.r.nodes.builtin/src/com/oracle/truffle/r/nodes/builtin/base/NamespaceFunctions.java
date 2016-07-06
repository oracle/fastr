/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

public class NamespaceFunctions {

    @RBuiltin(name = "getRegisteredNamespace", kind = INTERNAL, parameterNames = {"name"})
    public abstract static class GetRegisteredNamespace extends RBuiltinNode {
        @Specialization
        protected Object doGetRegisteredNamespace(RAbstractStringVector name) {
            Object result = REnvironment.getRegisteredNamespace(name.getDataAt(0));
            if (result == null) {
                return RNull.instance;
            } else {
                return result;
            }
        }

        @Specialization
        protected Object doGetRegisteredNamespace(RSymbol name) {
            Object result = REnvironment.getRegisteredNamespace(name.getName());
            if (result == null) {
                return RNull.instance;
            } else {
                return result;
            }
        }
    }

    @RBuiltin(name = "isRegisteredNamespace", kind = INTERNAL, parameterNames = {"name"})
    public abstract static class IsRegisteredNamespace extends RBuiltinNode {
        @Specialization
        protected byte doIsRegisteredNamespace(RAbstractStringVector name) {
            Object result = REnvironment.getRegisteredNamespace(name.getDataAt(0));
            if (result == null) {
                return RRuntime.LOGICAL_FALSE;
            } else {
                return RRuntime.LOGICAL_TRUE;
            }
        }

        @Specialization
        protected Object doIsRegisteredNamespace(RSymbol name) {
            Object result = REnvironment.getRegisteredNamespace(name.getName());
            if (result == null) {
                return RRuntime.LOGICAL_FALSE;
            } else {
                return RRuntime.LOGICAL_TRUE;
            }
        }
    }

    @RBuiltin(name = "isNamespaceEnv", kind = INTERNAL, parameterNames = {"env"})
    public abstract static class IsNamespaceEnv extends RBuiltinNode {
        @Specialization
        protected byte doIsNamespaceEnv(REnvironment env) {
            return RRuntime.asLogical(env.isNamespaceEnv());
        }

        @Specialization
        protected byte doIsNamespaceEnv(@SuppressWarnings("unused") RNull env) {
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @RBuiltin(name = "getNamespaceRegistry", kind = INTERNAL, parameterNames = {})
    public abstract static class GetNamespaceRegistry extends RBuiltinNode {
        @Specialization
        protected REnvironment doGetNamespaceRegistry() {
            return REnvironment.getNamespaceRegistry();
        }
    }

    @RBuiltin(name = "registerNamespace", kind = INTERNAL, parameterNames = {"name", "env"})
    public abstract static class RegisterNamespace extends RBuiltinNode {
        @Specialization
        protected RNull registerNamespace(String name, REnvironment env) {
            if (REnvironment.registerNamespace(name, env) == null) {
                throw RError.error(this, RError.Message.NS_ALREADY_REG);
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "unregisterNamespace", kind = INTERNAL, parameterNames = {"name"})
    public abstract static class UnregisterNamespace extends RBuiltinNode {
        @Specialization
        protected RNull unregisterNamespace(RAbstractStringVector name) {
            doUnregisterNamespace(name.getDataAt(0));
            return RNull.instance;
        }

        @Specialization
        protected Object unregisterNamespace(RSymbol name) {
            doUnregisterNamespace(name.getName());
            return RNull.instance;
        }

        private void doUnregisterNamespace(String name) {
            Object ns = REnvironment.unregisterNamespace(name);
            if (ns == null) {
                throw RError.error(this, RError.Message.NS_NOTREG);
            }
        }
    }
}
