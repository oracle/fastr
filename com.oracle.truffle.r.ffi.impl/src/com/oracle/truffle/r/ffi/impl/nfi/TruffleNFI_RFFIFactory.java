/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;

public class TruffleNFI_RFFIFactory extends RFFIFactory {

    private static class ContextStateImpl implements RContext.ContextState {
        @Override
        public ContextState initialize(RContext context) {
            String librffiPath = LibPaths.getBuiltinLibPath("R");
            if (context.isInitial()) {
                DLL.loadLibR(librffiPath);
            } else {
                // force initialization of NFI
                DLLRFFI.DLOpenRootNode.create(context).call(librffiPath, false, false);
            }
            return this;
        }
    }

    @Override
    public ContextState newContextState() {
        return new ContextStateImpl();
    }

    @Override
    protected RFFI createRFFI() {
        CompilerAsserts.neverPartOfCompilation();
        return new RFFI() {

            @CompilationFinal private CRFFI cRFFI;

            @Override
            public CRFFI getCRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (cRFFI == null) {
                    cRFFI = new TruffleNFI_C();
                }
                return cRFFI;
            }

            @CompilationFinal private BaseRFFI baseRFFI;

            @Override
            public BaseRFFI getBaseRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (baseRFFI == null) {
                    baseRFFI = new TruffleNFI_Base();
                }
                return baseRFFI;
            }

            @CompilationFinal private CallRFFI callRFFI;

            @Override
            public CallRFFI getCallRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (callRFFI == null) {
                    callRFFI = new TruffleNFI_Call();
                }
                return callRFFI;
            }

            @CompilationFinal private DLLRFFI dllRFFI;

            @Override
            public DLLRFFI getDLLRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                CompilerAsserts.neverPartOfCompilation();
                if (dllRFFI == null) {
                    dllRFFI = new TruffleNFI_DLL();
                }
                return dllRFFI;
            }

            @CompilationFinal private UserRngRFFI userRngRFFI;

            @Override
            public UserRngRFFI getUserRngRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (userRngRFFI == null) {
                    userRngRFFI = new TruffleNFI_UserRng();
                }
                return userRngRFFI;
            }

            @CompilationFinal private ZipRFFI zipRFFI;

            @Override
            public ZipRFFI getZipRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (zipRFFI == null) {
                    zipRFFI = new TruffleNFI_Zip();
                }
                return zipRFFI;
            }

            @CompilationFinal private PCRERFFI pcreRFFI;

            @Override
            public PCRERFFI getPCRERFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (pcreRFFI == null) {
                    pcreRFFI = new TruffleNFI_PCRE();
                }
                return pcreRFFI;
            }

            @CompilationFinal private LapackRFFI lapackRFFI;

            @Override
            public LapackRFFI getLapackRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (lapackRFFI == null) {
                    lapackRFFI = new TruffleNFI_Lapack();
                }
                return lapackRFFI;
            }

            @CompilationFinal private StatsRFFI statsRFFI;

            @Override
            public StatsRFFI getStatsRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (statsRFFI == null) {
                    statsRFFI = new TruffleNFI_Stats();
                }
                return statsRFFI;
            }

            @CompilationFinal private ToolsRFFI toolsRFFI;

            @Override
            public ToolsRFFI getToolsRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (toolsRFFI == null) {
                    toolsRFFI = new TruffleNFI_Tools();
                }
                return toolsRFFI;
            }

            private REmbedRFFI rEmbedRFFI;

            @Override
            public REmbedRFFI getREmbedRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (rEmbedRFFI == null) {
                    rEmbedRFFI = new TruffleNFI_REmbed();
                }
                return rEmbedRFFI;
            }

            private MiscRFFI miscRFFI;

            @Override
            public MiscRFFI getMiscRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (miscRFFI == null) {
                    miscRFFI = new TruffleNFI_Misc();
                }
                return miscRFFI;
            }
        };
    }
}
