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
package com.oracle.truffle.r.runtime.ffi.jni;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.DLLRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;

public class JNI_Stats implements StatsRFFI {

    public static class JNI_WorkNode extends WorkNode {
        @Child DLLRFFI.DLSymNode dlSymNode = RFFIFactory.getRFFI().getDLLRFFI().createDLSymNode();

        private SymbolHandle fftWorkAddress;

        @Override
        @TruffleBoundary
        public int execute(double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork) {
            if (fftWorkAddress == null) {
                fftWorkAddress = fftAddress("fft_work", dlSymNode);
            }
            return native_fft_work(fftWorkAddress.asAddress(), a, nseg, n, nspn, isn, work, iwork);
        }
    }

    public static class JNI_FactorNode extends FactorNode {
        @Child DLLRFFI.DLSymNode dlSymNode = RFFIFactory.getRFFI().getDLLRFFI().createDLSymNode();
        private SymbolHandle fftFactorAddress;

        @Override
        @TruffleBoundary
        public void execute(int n, int[] pmaxf, int[] pmaxp) {
            if (fftFactorAddress == null) {
                fftFactorAddress = fftAddress("fft_factor", dlSymNode);
            }
            native_fft_factor(fftFactorAddress.asAddress(), n, pmaxf, pmaxp);

        }

    }

    private static SymbolHandle fftAddress(String symbol, DLLRFFI.DLSymNode dlSymNode) {
        SymbolHandle fftAddress;
        DLLInfo dllInfo = DLL.findLibrary("stats");
        assert dllInfo != null;
        fftAddress = dlSymNode.execute(dllInfo.handle, symbol);
        assert fftAddress != DLL.SYMBOL_NOT_FOUND;
        return fftAddress;
    }

    @Override
    public FactorNode createFactorNode() {
        return new JNI_FactorNode();
    }

    @Override
    public WorkNode createWorkNode() {
        return new JNI_WorkNode();
    }

    // Checkstyle: stop method name
    private static native void native_fft_factor(long address, int n, int[] pmaxf, int[] pmaxp);

    private static native int native_fft_work(long address, double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork);

}
