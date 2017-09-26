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
package com.oracle.truffle.r.ffi.impl.managed;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.CallRFFI.HandleUpCallExceptionNode;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFI;
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
public class Managed_RFFIFactory extends RFFIFactory {
    @Override
    protected RFFI createRFFI() {
        CompilerAsserts.neverPartOfCompilation();
        return new RFFI() {

            @Override
            public BaseRFFI getBaseRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new Managed_Base();
            }

            @Override
            public LapackRFFI getLapackRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new Managed_LapackRFFI();
            }

            @Override
            public StatsRFFI getStatsRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new StatsRFFI() {
                    @Override
                    public FactorNode createFactorNode() {
                        throw unsupported("factor");
                    }

                    @Override
                    public WorkNode createWorkNode() {
                        throw unsupported("work");
                    }
                };
            }

            @Override
            public ToolsRFFI getToolsRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new ToolsRFFI() {
                    @Override
                    public ParseRdNode createParseRdNode() {
                        throw unsupported("parseRD");
                    }
                };
            }

            @Override
            public CRFFI getCRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new CRFFI() {
                    @Override
                    public InvokeCNode createInvokeCNode() {
                        throw unsupported("invoke");
                    }
                };
            }

            class IgnoreUpCallExceptionNode extends Node implements HandleUpCallExceptionNode {
                @Override
                public void execute(Throwable ex) {
                    // nop
                }
            }

            @Override
            public CallRFFI getCallRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new CallRFFI() {
                    @Override
                    public InvokeCallNode createInvokeCallNode() {
                        throw unsupported("native code invocation");
                    }

                    @Override
                    public InvokeVoidCallNode createInvokeVoidCallNode() {
                        throw unsupported("native code invocation");
                    }

                    @Override
                    public HandleUpCallExceptionNode createHandleUpCallExceptionNode() {
                        return new IgnoreUpCallExceptionNode();
                    }
                };
            }

            @Override
            public UserRngRFFI getUserRngRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new UserRngRFFI() {
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
                };
            }

            @Override
            public PCRERFFI getPCRERFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new Managed_PCRERFFI();
            }

            @Override
            public ZipRFFI getZipRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new ZipRFFI() {
                    @Override
                    public CompressNode createCompressNode() {
                        throw unsupported("zip compression");
                    }

                    @Override
                    public UncompressNode createUncompressNode() {
                        throw unsupported("zip decompression");
                    }
                };
            }

            @Override
            public DLLRFFI getDLLRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new DLLRFFI() {
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
                };
            }

            @Override
            public REmbedRFFI getREmbedRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new Managed_REmbedRFFI();
            }

            @Override
            public MiscRFFI getMiscRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                return new MiscRFFI() {
                    @Override
                    public ExactSumNode createExactSumNode() {
                        throw unsupported("exactsum");
                    }

                    @Override
                    public DqrlsNode createDqrlsNode() {
                        throw unsupported("dqrls");
                    }
                };
            }
        };
    }

    @Override
    public RFFIContext newContextState() {
        return new RFFIContext() {
            @Override
            public ContextState initialize(RContext context) {
                return this;
            }
        };
    }

    @TruffleBoundary
    static RError unsupported(String name) {
        throw RError.error(RError.NO_CALLER, Message.GENERIC, String.format("Feature '%s' is not supported by managed FFI, i.e. it requires running native code.", name));
    }
}
