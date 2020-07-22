/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.managed;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.ffi.impl.altrep.AltrepDownCallNodeFactoryImpl;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.ffi.AltrepRFFI;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.InvokeCNode;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

/**
 * Operations that can be, at least partially, implemented in Java are implemented, other operations
 * throw {@link RError}.
 */
public final class Managed_RFFIFactory extends RFFIFactory {
    private static final class ManagedRFFIContext extends RFFIContext {

        private ManagedRFFIContext() {
            super(new RFFIContextState(), new CRFFI() {
                @Override
                public InvokeCNode createInvokeCNode() {
                    throw unsupported("invoke");
                }
            }, new BaseRFFI(Managed_DownCallNodeFactory.INSTANCE, Managed_DownCallNodeFactory.INSTANCE),
                            new AltrepRFFI(AltrepDownCallNodeFactoryImpl.INSTANCE),
                            new CallRFFI() {
                                @Override
                                public InvokeCallNode createInvokeCallNode() {
                                    throw unsupported("native code invocation");
                                }

                                @Override
                                public InvokeVoidCallNode createInvokeVoidCallNode() {
                                    throw unsupported("native code invocation");
                                }
                            }, new DLLRFFI() {
                                @Override
                                public DLOpenNode createDLOpenNode() {
                                    throw unsupported("DLL open");
                                }

                                @Override
                                public DLSymNode createDLSymNode() {
                                    throw unsupported("createDLSym");
                                }

                                @Override
                                public DLCloseNode createDLCloseNode() {
                                    throw unsupported("createDLClose");
                                }
                            }, new UserRngRFFI() {
                                @Override
                                public InitNode createInitNode() {
                                    throw unsupported("user defined RNG");
                                }

                                @Override
                                public RandNode createRandNode() {
                                    throw unsupported("user defined RNG");
                                }

                                @Override
                                public NSeedNode createNSeedNode() {
                                    throw unsupported("user defined RNG");
                                }

                                @Override
                                public SeedsNode createSeedsNode() {
                                    throw unsupported("user defined RNG");
                                }
                            }, new ZipRFFI(Managed_DownCallNodeFactory.INSTANCE), new PCRERFFI(Managed_DownCallNodeFactory.INSTANCE), new LapackRFFI(Managed_DownCallNodeFactory.INSTANCE),
                            new StatsRFFI(Managed_DownCallNodeFactory.INSTANCE), new ToolsRFFI(), new REmbedRFFI(Managed_DownCallNodeFactory.INSTANCE),
                            new MiscRFFI(Managed_DownCallNodeFactory.INSTANCE));

        }

        @Override
        public TruffleObject lookupNativeFunction(NativeFunction function) {
            throw unsupported("calling native functions");
        }

        @SuppressWarnings("unchecked")
        @Override
        public <C extends RFFIContext> C as(Class<C> rffiCtxClass) {
            assert rffiCtxClass == ManagedRFFIContext.class;
            return (C) this;
        }

        @Override
        public Type getDefaultRFFIType() {
            return Type.MANAGED;
        }

    }

    @Override
    protected RFFIContext createRFFIContext() {
        CompilerAsserts.neverPartOfCompilation();
        return new ManagedRFFIContext();
    }

    static RError unsupported(String name) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(RError.NO_CALLER, Message.GENERIC, String.format("Feature '%s' is not supported by managed FFI, i.e. it requires running native code.", name));
    }

}
