/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.*;
import jnr.ffi.*;
import jnr.ffi.annotations.*;
import jnr.posix.*;
import jnr.constants.platform.Errno;

import com.oracle.truffle.r.runtime.ffi.*;

/**
 * JNR-based factory Implements {@link BaseRFFI} and {@link LapackRFFI} directly.
 */
public class JNR_RFFIFactory extends RFFIFactory implements RFFI, BaseRFFI, LapackRFFI {

    // Base

    @Override
    public BaseRFFI getBaseRFFI() {
        return this;
    }

    /**
     * Functions missing from JNR POSIX.
     */
    public interface LibCX {
        int getcwd(@Out byte[] path);

        long mkdtemp(@In @Out ByteBuffer template);
    }

    private static class LibCXProvider {
        private static LibCX libcx;

        static LibCX libcx() {
            if (libcx == null) {
                libcx = LibraryLoader.create(LibCX.class).load("c");
            }
            return libcx;
        }
    }

    private static LibCX libcx() {
        return LibCXProvider.libcx();
    }

    protected POSIX posix;

    @Override
    protected RFFI createRFFI() {
        return this;
    }

    protected POSIX posix() {
        if (posix == null) {
            posix = POSIXFactory.getPOSIX();
        }
        return posix;
    }

    public int getpid() {
        return posix().getpid();
    }

    public int setwd(String dir) {
        return posix().chdir(dir);
    }

    public String getwd() {
        byte[] buf = new byte[4096];
        int rc = libcx().getcwd(buf);
        if (rc == 0) {
            return null;
        } else {
            int i = 0;
            while (buf[i] != 0 && i < buf.length) {
                i++;
            }
            return new String(buf, 0, i);
        }
    }

    public String readlink(String path) throws IOException {
        String s = posix().readlink(path);
        if (s == null) {
            int n = posix().errno();
            if (n == Errno.EINVAL.intValue()) {
                // not a link
            } else {
                // some other error
                throw new IOException();
            }
        }
        return s;
    }

    public String mkdtemp(String template) {
        ByteBuffer bb = ByteBuffer.wrap(template.getBytes());
        long result = libcx().mkdtemp(bb);
        if (result == 0) {
            return null;
        } else {
            return new String(bb.array());
        }
    }

    // Lapack

    @Override
    public LapackRFFI getLapackRFFI() {
        return this;
    }

    /**
     * Fortran does call by reference for everything, which we handle with arrays. Evidently, this
     * is not as efficient as it could be.
     */
    public interface Lapack {
        // Checkstyle: stop method name
        void ilaver_(@Out int[] major, @Out int[] minor, @Out int[] patch);

        // @formatter:off
        // Checkstyle: stop method name
        void dgeev_(byte[] jobVL, byte[] jobVR, @In int[] n, @In double[] a, @In int[] lda, @Out double[] wr, @Out double[] wi,
                        @Out double[] vl, @In int[] ldvl, @Out double[] vr, @In int[] ldvr,
                        @In @Out double[] work, @In int[] lwork, @Out int[] info);
    }

    private static class LapackProvider {
        private static Lapack lapack;

        static Lapack lapack() {
            if (lapack == null) {
                lapack = LibraryLoader.create(Lapack.class).load("Rlapack");
            }
            return lapack;
        }
    }

    private static Lapack lapack() {
        return LapackProvider.lapack();
    }

    private static class RefScalars_ilaver {
        static int[] major = new int[1];
        static int[] minor = new int[1];
        static int[] patch = new int[1];
    }
    public void ilaver(int[] version) {
        lapack().ilaver_(RefScalars_ilaver.major, RefScalars_ilaver.minor, RefScalars_ilaver.patch);
        version[0] = RefScalars_ilaver.major[0];
        version[1] = RefScalars_ilaver.minor[0];
        version[2] = RefScalars_ilaver.patch[0];
    }

    private static class RefScalars_dgeev {
        static byte[] jobVL = new byte[1];
        static byte[] jobVR = new byte[1];
        static int[] n = new int[1];
        static int[] lda = new int[1];
        static int[] ldvl = new int[1];
        static int[] ldvr = new int[1];
        static int[] lwork = new int[1];
        static int[] info = new int[1];
    }

    // @formatter:off
    public int dgeev(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi,
                    double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork) {
        // assume single threaded calls here
        RefScalars_dgeev.jobVL[0] = (byte) jobVL;
        RefScalars_dgeev.jobVR[0] = (byte) jobVR;
        RefScalars_dgeev.n[0] = n;
        RefScalars_dgeev.lda[0] = lda;
        RefScalars_dgeev.ldvl[0] = ldvl;
        RefScalars_dgeev.ldvr[0] = ldvr;
        RefScalars_dgeev.lwork[0] = lwork;
        // @formatter:off
        lapack().dgeev_(RefScalars_dgeev.jobVL, RefScalars_dgeev.jobVR, RefScalars_dgeev.n, a, RefScalars_dgeev.lda, wr, wi, vl,
                        RefScalars_dgeev.ldvl, vr, RefScalars_dgeev.ldvr, work,
                        RefScalars_dgeev.lwork, RefScalars_dgeev.info);
        return RefScalars_dgeev.info[0];
    }

}
