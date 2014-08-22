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
import com.oracle.truffle.r.runtime.env.*;

public class NamespaceFunctions {

    @RBuiltin(name = "getRegisteredNamespace", kind = INTERNAL, parameterNames = {"name"})
    public abstract static class GetRegisteredNamespace extends RBuiltinNode {
        @Specialization
        protected Object doGetRegisteredNamespace(String name) {
            controlVisibility();
            Object result = REnvironment.getRegisteredNamespace(name);
            if (result == null) {
                return RNull.instance;
            } else {
                return result;
            }
        }

        @Specialization
        protected Object doGetRegisteredNamespace(RSymbol name) {
            controlVisibility();
            return doGetRegisteredNamespace(name.getName());
        }
    }

    @RBuiltin(name = "isNamespaceEnv", kind = INTERNAL, parameterNames = {"env"})
    public abstract static class IsNamespaceEnv extends RBuiltinNode {
        @Specialization
        protected byte doIsNamespaceEnv(REnvironment env) {
            controlVisibility();
            return RRuntime.asLogical(env.isNamespaceEnv());
        }
    }

    @RBuiltin(name = "getNamespaceRegistry", kind = INTERNAL, parameterNames = {})
    public abstract static class GetNamespaceRegistry extends RBuiltinNode {
        @Specialization
        protected REnvironment doGetNamespaceRegistry(@SuppressWarnings("unused") RMissing missing) {
            controlVisibility();
            return REnvironment.getNamespaceRegistry();
        }
    }

    @RBuiltin(name = "registerNamespace", kind = INTERNAL, parameterNames = {"name", "env"})
    public abstract static class RegisterNamespace extends RBuiltinNode {
        @Specialization
        protected RNull registerNamespace(String name, REnvironment env) {
            controlVisibility();
            REnvironment.registerNamespace(name, env);
            return RNull.instance;
        }
    }

    @RBuiltin(name = "unregisterNamespace", kind = INTERNAL, parameterNames = {"name"})
    public abstract static class UnregisterNamespace extends RBuiltinNode {
        @Specialization
        protected RNull unregisterNamespace(@SuppressWarnings("unused") RAbstractStringVector name) {
            controlVisibility();
            // TODO implement
            return RNull.instance;
        }

        @Specialization
        protected Object unregisterNamespace(@SuppressWarnings("unused") RSymbol name) {
            controlVisibility();
            // TODO implement
            return RNull.instance;
        }
    }

}
