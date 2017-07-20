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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;

public class TruffleNFI_Stats implements StatsRFFI {

    private static class TruffleNFI_FactorNode extends Node implements FactorNode {
        private static final String FFT_FACTOR = "fft_factor";
        private static final String FFT_FACTOR_SIGNATURE = "(sint32, [sint32], [sint32]): void";

        @Child private Node factorMessage = Message.createExecute(3).createNode();
        @Child private DLLRFFI.DLSymNode dlsymNode = RFFIFactory.getDLLRFFI().createDLSymNode();

        @CompilationFinal private TruffleObject fftFactorFunction;

        @Override
        public void execute(int n, int[] pmaxf, int[] pmaxp) {
            try {
                if (fftFactorFunction == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    Node bind = Message.createInvoke(1).createNode();
                    fftFactorFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, findSymbol(FFT_FACTOR, dlsymNode).asTruffleObject(), "bind", FFT_FACTOR_SIGNATURE);
                }
                ForeignAccess.sendExecute(factorMessage, fftFactorFunction, n, JavaInterop.asTruffleObject(pmaxf), JavaInterop.asTruffleObject(pmaxp));
            } catch (InteropException t) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    private static class TruffleNFI_WorkNode extends Node implements WorkNode {
        private static final String FFT_WORK = "fft_work";
        private static final String FFT_WORK_SIGNATURE = "([double], sint32, sint32, sint32, sint32, [double], [sint32]): sint32";

        @Child private DLLRFFI.DLSymNode dlsymNode = RFFIFactory.getDLLRFFI().createDLSymNode();
        @Child private Node workMessage = Message.createExecute(7).createNode();

        @CompilationFinal private TruffleObject fftWorkFunction;

        @Override
        public int execute(double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork) {
            try {
                if (fftWorkFunction == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    Node bind = Message.createInvoke(1).createNode();
                    fftWorkFunction = (TruffleObject) ForeignAccess.sendInvoke(bind, findSymbol(FFT_WORK, dlsymNode).asTruffleObject(), "bind", FFT_WORK_SIGNATURE);
                }
                return (int) ForeignAccess.sendExecute(workMessage, fftWorkFunction, JavaInterop.asTruffleObject(a), nseg, n, nspn, isn,
                                JavaInterop.asTruffleObject(work), JavaInterop.asTruffleObject(iwork));
            } catch (InteropException t) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    private static SymbolHandle findSymbol(String symbol, DLLRFFI.DLSymNode dlsymNode) {
        SymbolHandle fftAddress;
        DLLInfo dllInfo = DLL.findLibrary("stats");
        assert dllInfo != null;
        // maybe DLL.findSymbol(symbol, dllInfo); ?
        fftAddress = dlsymNode.execute(dllInfo.handle, symbol);
        assert fftAddress != DLL.SYMBOL_NOT_FOUND;
        return fftAddress;
    }

    @Override
    public FactorNode createFactorNode() {
        return new TruffleNFI_FactorNode();
    }

    @Override
    public WorkNode createWorkNode() {
        return new TruffleNFI_WorkNode();
    }
}
