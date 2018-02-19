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
package com.oracle.truffle.r.ffi.impl.managed;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DownCallNodeFactory;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;

public final class Managed_DownCallNodeFactory extends DownCallNodeFactory {

    static final Managed_DownCallNodeFactory INSTANCE = new Managed_DownCallNodeFactory();

    private Managed_DownCallNodeFactory() {
    }

    @Override
    public DownCallNode createDownCallNode(NativeFunction function) {
        return new DownCallNode(function) {
            @Override
            protected TruffleObject getTarget(NativeFunction function) {
                return new DummyFunctionObject(function);
            }

            @Override
            protected void wrapArguments(TruffleObject function, Object[] args) {
                // Report unsupported functions at invocation time
                assert function instanceof DummyFunctionObject;
                throw Managed_RFFIFactory.unsupported(((DummyFunctionObject) function).function.getCallName());
            }

            @Override
            protected void finishArguments(Object[] args) {
                throw RInternalError.shouldNotReachHere();
            }
        };
    }

    private static final class DummyFunctionObject implements TruffleObject {
        final NativeFunction function;

        private DummyFunctionObject(NativeFunction function) {
            this.function = function;
        }

        @Override
        public ForeignAccess getForeignAccess() {
            return null;
        }
    }
}
