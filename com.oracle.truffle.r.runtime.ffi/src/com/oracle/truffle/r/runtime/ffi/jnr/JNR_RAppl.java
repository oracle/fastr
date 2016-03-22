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

import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.wrapDouble;
import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.wrapInt;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;

import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;

//Checkstyle: stop method name
public class JNR_RAppl implements RApplRFFI {
    public interface Linpack {
        void dqrdc2_(double[] x, @In int[] ldx, @In int[] n, @In int[] p, @In double[] tol, int[] rank, double[] qraux, int[] pivot, @Out double[] work);

        void dqrcf_(double[] x, @In int[] n, @In int[] k, double[] qraux, double[] y, @In int[] ny, double[] b, int[] info);
    }

    private static class LinpackProvider {
        private static Linpack linpack;

        @TruffleBoundary
        private static Linpack createAndLoadLib() {
            // need to load blas lib as Fortran functions in appl lib need it
            return LibraryLoader.create(Linpack.class).library("Rblas").library("R").load();
        }

        static Linpack linpack() {
            if (linpack == null) {
                linpack = createAndLoadLib();
            }
            return linpack;
        }
    }

    public static Linpack linpack() {
        return LinpackProvider.linpack();
    }

    @Override
    @TruffleBoundary
    public void dqrdc2(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work) {
        linpack().dqrdc2_(x, wrapInt(ldx), wrapInt(n), wrapInt(p), wrapDouble(tol), rank, qraux, pivot, work);
    }

    @Override
    @TruffleBoundary
    public void dqrcf(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info) {
        linpack().dqrcf_(x, wrapInt(n), wrapInt(k), qraux, y, wrapInt(ny), b, info);
    }
}
