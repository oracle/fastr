/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
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

public class TruffleLLVM_RFFIFactory extends RFFIFactory {

    @Override
    public RFFIContext newContextState() {
        return new TruffleLLVM_RFFIContextState();
    }

    @Override
    protected RFFI createRFFI() {
        CompilerAsserts.neverPartOfCompilation();
        return new RFFI() {

            @CompilationFinal private BaseRFFI baseRFFI;

            @Override
            public BaseRFFI getBaseRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (baseRFFI == null) {
                    baseRFFI = new TruffleLLVM_Base();
                }
                return baseRFFI;
            }

            @CompilationFinal private CRFFI cRFFI;

            @Override
            public CRFFI getCRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (cRFFI == null) {
                    cRFFI = new TruffleLLVM_C();
                }
                return cRFFI;
            }

            @CompilationFinal private DLLRFFI dllRFFI;

            @Override
            public DLLRFFI getDLLRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (dllRFFI == null) {
                    dllRFFI = new TruffleLLVM_DLL();
                }
                return dllRFFI;
            }

            @CompilationFinal private UserRngRFFI truffleUserRngRFFI;

            @Override
            public UserRngRFFI getUserRngRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (truffleUserRngRFFI == null) {
                    truffleUserRngRFFI = new TruffleLLVM_UserRng();
                }
                return truffleUserRngRFFI;
            }

            @CompilationFinal private CallRFFI truffleCallRFFI;

            @Override
            public CallRFFI getCallRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (truffleCallRFFI == null) {
                    truffleCallRFFI = new TruffleLLVM_Call();
                }
                return truffleCallRFFI;
            }

            @CompilationFinal private StatsRFFI truffleStatsRFFI;

            @Override
            public StatsRFFI getStatsRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (truffleStatsRFFI == null) {
                    truffleStatsRFFI = new TruffleLLVM_Stats();
                }
                return truffleStatsRFFI;
            }

            @CompilationFinal private LapackRFFI lapackRFFI;

            @Override
            public LapackRFFI getLapackRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (lapackRFFI == null) {
                    lapackRFFI = new TruffleLLVM_Lapack();
                }
                return lapackRFFI;
            }

            @CompilationFinal private ToolsRFFI toolsRFFI;

            @Override
            public ToolsRFFI getToolsRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (toolsRFFI == null) {
                    toolsRFFI = new TruffleLLVM_Tools();
                }
                return toolsRFFI;
            }

            @CompilationFinal private PCRERFFI pcreRFFI;

            @Override
            public PCRERFFI getPCRERFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (pcreRFFI == null) {
                    pcreRFFI = new TruffleLLVM_PCRE();
                }
                return pcreRFFI;
            }

            @CompilationFinal private ZipRFFI zipRFFI;

            @Override
            public ZipRFFI getZipRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (zipRFFI == null) {
                    zipRFFI = new TruffleLLVM_Zip();
                }
                return zipRFFI;
            }

            @CompilationFinal private MiscRFFI miscRFFI;

            @Override
            public MiscRFFI getMiscRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (miscRFFI == null) {
                    miscRFFI = new TruffleLLVM_Misc();
                }
                return miscRFFI;
            }

            private REmbedRFFI rEmbedRFFI;

            @Override
            public REmbedRFFI getREmbedRFFI() {
                CompilerAsserts.neverPartOfCompilation();
                if (rEmbedRFFI == null) {
                    rEmbedRFFI = new TruffleLLVM_REmbed();
                }
                return rEmbedRFFI;
            }
        };
    }
}
