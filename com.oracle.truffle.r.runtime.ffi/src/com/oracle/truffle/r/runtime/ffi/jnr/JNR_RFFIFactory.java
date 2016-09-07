/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.jnr;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.RPlatform;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.ContextState;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.CRFFI;
import com.oracle.truffle.r.runtime.ffi.CallRFFI;
import com.oracle.truffle.r.runtime.ffi.GridRFFI;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.LibPaths;
import com.oracle.truffle.r.runtime.ffi.PCRERFFI;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;
import com.oracle.truffle.r.runtime.ffi.ZipRFFI;
import com.oracle.truffle.r.runtime.ffi.generic.Generic_Grid;
import com.oracle.truffle.r.runtime.ffi.generic.Generic_Tools;

/**
 * JNR/JNI-based factory. The majority of the FFI instances are instantiated on demand.
 */
public class JNR_RFFIFactory extends RFFIFactory implements RFFI {

    public JNR_RFFIFactory() {
    }

    @Override
    protected void initialize(boolean runtime) {
        // This must load early as package libraries reference symbols in it.
        getCallRFFI();
        /*
         * Some package C code calls these functions and, therefore, expects the linpack symbols to
         * be available, which will not be the case unless one of the functions has already been
         * called from R code. So we eagerly load the library to define the symbols.
         *
         * There is an additional problem when running without a *_LIBRARY_PATH being set which is
         * mandated by Mac OSX El Capitan, which is we must tell JNR where to find the libraries.
         */
        String jnrLibPath = LibPaths.getBuiltinLibPath();
        if (RPlatform.getOSInfo().osName.equals("Mac OS X")) {
            // Why this is necessary is a JNR mystery
            jnrLibPath += ":/usr/lib";
        }
        System.setProperty("jnr.ffi.library.path", jnrLibPath);
        JNR_RAppl.linpack();
        JNR_Lapack.lapack();
    }

    /**
     * Placeholder class for context-specific native state.
     */
    private static class ContextStateImpl implements RContext.ContextState {

    }

    @Override
    public ContextState newContextState() {
        return new ContextStateImpl();
    }

    @Override
    protected RFFI createRFFI() {
        return this;
    }

    @CompilationFinal private BaseRFFI baseRFFI;

    @Override
    public BaseRFFI getBaseRFFI() {
        if (baseRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            baseRFFI = new JNR_Base();
        }
        return baseRFFI;
    }

    @CompilationFinal private LapackRFFI lapackRFFI;

    @Override
    public LapackRFFI getLapackRFFI() {
        if (lapackRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lapackRFFI = new JNR_Lapack();
        }
        return lapackRFFI;
    }

    @CompilationFinal private RApplRFFI rApplRFFI;

    @Override
    public RApplRFFI getRApplRFFI() {
        if (rApplRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rApplRFFI = new JNR_RAppl();
        }
        return rApplRFFI;
    }

    @CompilationFinal private StatsRFFI statsRFFI;

    @Override
    public StatsRFFI getStatsRFFI() {
        if (statsRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            statsRFFI = new JNR_Stats();
        }
        return statsRFFI;
    }

    @CompilationFinal private ToolsRFFI toolsRFFI;

    @Override
    public ToolsRFFI getToolsRFFI() {
        if (toolsRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toolsRFFI = new Generic_Tools();
        }
        return toolsRFFI;
    }

    @CompilationFinal private GridRFFI gridRFFI;

    @Override
    public GridRFFI getGridRFFI() {
        if (gridRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            gridRFFI = new Generic_Grid();
        }
        return gridRFFI;
    }

    @CompilationFinal private UserRngRFFI userRngRFFI;

    @Override
    public UserRngRFFI getUserRngRFFI() {
        if (userRngRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            userRngRFFI = new JNR_UserRng();
        }
        return userRngRFFI;
    }

    @CompilationFinal private CRFFI cRFFI;

    @Override
    public CRFFI getCRFFI() {
        if (cRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cRFFI = new CRFFI_JNR_Invoke();
        }
        return cRFFI;
    }

    @CompilationFinal private CallRFFI callRFFI;

    @Override
    public CallRFFI getCallRFFI() {
        if (callRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callRFFI = new JNI_CallRFFI();
        }
        return callRFFI;
    }

    @CompilationFinal private ZipRFFI zipRFFI;

    @Override
    public ZipRFFI getZipRFFI() {
        if (zipRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            zipRFFI = new JNR_Zip();
        }
        return zipRFFI;
    }

    @CompilationFinal private PCRERFFI pcreRFFI;

    @Override
    public PCRERFFI getPCRERFFI() {
        if (pcreRFFI == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pcreRFFI = new JNR_PCRE();
        }
        return pcreRFFI;
    }

    private REmbedRFFI rEmbedRFFI;

    @Override
    public REmbedRFFI getREmbedRFFI() {
        if (rEmbedRFFI == null) {
            rEmbedRFFI = new JNI_REmbed();
        }
        return rEmbedRFFI;
    }

}
