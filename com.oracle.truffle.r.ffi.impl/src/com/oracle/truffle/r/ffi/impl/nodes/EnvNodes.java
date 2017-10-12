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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.guaranteeInstanceOf;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.env.REnvironment;

public class EnvNodes {

    @TypeSystemReference(RTypes.class)
    public abstract static class LockBindingNode extends FFIUpCallNode.Arg2 {

        @Specialization
        int lock(RSymbol sym, REnvironment env) {
            // TODO copied from EnvFunctions.LockBinding
            env.lockBinding(sym.getName());
            return 0;
        }

        @Fallback
        int lock(Object sym, Object env) {
            guaranteeInstanceOf(sym, RSymbol.class);
            guaranteeInstanceOf(env, REnvironment.class);
            throw RInternalError.shouldNotReachHere();
        }

        public static LockBindingNode create() {
            return EnvNodesFactory.LockBindingNodeGen.create();
        }
    }

    @TypeSystemReference(RTypes.class)
    public abstract static class UnlockBindingNode extends FFIUpCallNode.Arg2 {

        @Specialization
        int unlock(RSymbol sym, REnvironment env) {
            // TODO copied from EnvFunctions.LockBinding
            env.unlockBinding(sym.getName());
            return 0;
        }

        @Fallback
        int unlock(Object sym, Object env) {
            guaranteeInstanceOf(sym, RSymbol.class);
            guaranteeInstanceOf(env, REnvironment.class);
            throw RInternalError.shouldNotReachHere();
        }

        public static UnlockBindingNode create() {
            return EnvNodesFactory.UnlockBindingNodeGen.create();
        }
    }
}
