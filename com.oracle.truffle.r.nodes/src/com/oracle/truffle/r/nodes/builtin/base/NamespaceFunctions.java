/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class NamespaceFunctions {

    @RBuiltin(name = "getRegisteredNamespace", kind = INTERNAL)
    public abstract static class GetRegisteredNamespace extends RBuiltinNode {
        @Specialization
        public Object doGetRegisteredNamespace(String name) {
            controlVisibility();
            Object result = REnvironment.getRegisteredNamespace(name);
            if (result == null) {
                return RNull.instance;
            } else {
                return result;
            }
        }

        @Specialization
        public Object doGetRegisteredNamespace(RSymbol name) {
            controlVisibility();
            return doGetRegisteredNamespace(name.getName());
        }
    }

    @RBuiltin(name = "isNamespaceEnv", kind = INTERNAL)
    public abstract static class IsNamespaceEnv extends RBuiltinNode {
        @Specialization
        public byte doIsNamespaceEnv(REnvironment env) {
            return RRuntime.asLogical(env.isNamespaceEnv());
        }
    }

    @RBuiltin(name = "getNamespaceRegistry", kind = INTERNAL)
    public abstract static class GetNamespaceRegistry extends RBuiltinNode {
        @Specialization
        public REnvironment doGetNamespaceRegistry(@SuppressWarnings("unused") RMissing missing) {
            return REnvironment.getNamespaceRegistry();
        }
    }

    @RBuiltin(name = "registerNamespace", kind = INTERNAL)
    public abstract static class RegisterNamespace extends RBuiltinNode {
        @Specialization
        public RNull registerNamespace(String name, REnvironment env) {
            REnvironment.registerNamespace(name, env);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "unregisterNamespace", kind = INTERNAL)
    public abstract static class UnregisterNamespace extends RBuiltinNode {
        @Specialization
        public RNull unregisterNamespace(@SuppressWarnings("unused") RAbstractStringVector name) {
            // REnvironment.unregisterNamespace(name, env);
            return RNull.instance;
        }

        @Specialization
        public Object doGetRegisteredNamespace(RSymbol name) {
            controlVisibility();
            return doGetRegisteredNamespace(name);
        }
    }
}
