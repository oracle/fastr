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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.DLLInfo;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI;

import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;

// Checkstyle: stop method name
public class JNR_Stats implements StatsRFFI {
    public interface Stats {
        void fft_factor(int n, @Out int[] pmaxf, @Out int[] pmaxp);

        int fft_work(double[] a, int nseg, int n, int nspn, int isn, @In double[] work, @In int[] iwork);
    }

    private static class StatsProvider {
        private static Stats stats;

        @TruffleBoundary
        private static Stats createAndLoadLib() {
            // fft is in the stats package .so
            DLLInfo dllInfo = DLL.findLibraryContainingSymbol("fft");
            return LibraryLoader.create(Stats.class).load(dllInfo.path);
        }

        static Stats fft() {
            if (stats == null) {
                stats = createAndLoadLib();
            }
            return stats;
        }
    }

    private static Stats stats() {
        return StatsProvider.fft();
    }

    @Override
    @TruffleBoundary
    public void fft_factor(int n, int[] pmaxf, int[] pmaxp) {
        stats().fft_factor(n, pmaxf, pmaxp);
    }

    @Override
    @TruffleBoundary
    public int fft_work(double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork) {
        return stats().fft_work(a, nseg, n, nspn, isn, work, iwork);
    }
}
