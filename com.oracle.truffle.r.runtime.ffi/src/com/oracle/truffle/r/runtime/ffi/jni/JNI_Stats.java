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
package com.oracle.truffle.r.runtime.ffi.jni;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;

// Checkstyle: stop method name
public class JNI_Stats implements StatsRFFI {
    @Override
    @TruffleBoundary
    public void fft_factor(int n, int[] pmaxf, int[] pmaxp) {
        native_fft_factor(fft_factor_address(), n, pmaxf, pmaxp);
    }

    @Override
    @TruffleBoundary
    public int fft_work(double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork) {
        return native_fft_work(fft_work_address(), a, nseg, n, nspn, isn, work, iwork);
    }

    private long fft_factor_address;
    private long fft_work_address;

    private long fft_factor_address() {
        if (fft_factor_address == 0) {
            DLLInfo dllInfo = DLL.findLibrary("stats");
            fft_factor_address = RFFIFactory.getRFFI().getBaseRFFI().dlsym(dllInfo.handle, "fft_factor");
            assert fft_factor_address != DLL.SYMBOL_NOT_FOUND;
        }
        return fft_factor_address;
    }

    private long fft_work_address() {
        if (fft_work_address == 0) {
            DLLInfo dllInfo = DLL.findLibrary("stats");
            fft_work_address = RFFIFactory.getRFFI().getBaseRFFI().dlsym(dllInfo.handle, "fft_work");
            assert fft_work_address != DLL.SYMBOL_NOT_FOUND;
        }
        return fft_work_address;
    }

    private static native void native_fft_factor(long address, int n, int[] pmaxf, int[] pmaxp);

    private static native int native_fft_work(long address, double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork);
}
