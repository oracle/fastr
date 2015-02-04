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

import jnr.constants.platform.*;
import jnr.ffi.*;
import jnr.ffi.annotations.*;
import jnr.posix.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.*;

/**
 * JNR-based factory.
 */
public class JNR_RFFIFactory extends RFFIFactory implements RFFI, BaseRFFI, RDerivedRFFI, LapackRFFI, UserRngRFFI {

    public JNR_RFFIFactory() {
    }

    @Override
    protected void initialize() {
        // This must load early as package libraries reference symbols in it.
        getCallRFFI();
    }

    // Base

    @Override
    public BaseRFFI getBaseRFFI() {
        return this;
    }

    /**
     * Functions missing from JNR POSIX.
     */
    public interface LibCX {
        int getwd(@Out byte[] path);

        long mkdtemp(@In @Out ByteBuffer template);

        long strtol(@In String dir, @In String end, int base);

        int uname(@In long[] utsname);
    }

    private static class LibCXProvider {
        private static LibCX libcx;

        @TruffleBoundary
        private static LibCX createAndLoadLib() {
            return LibraryLoader.create(LibCX.class).load("c");
        }

        static LibCX libcx() {
            if (libcx == null) {
                libcx = createAndLoadLib();
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

    @TruffleBoundary
    public String getwd() {
        byte[] buf = new byte[4096];
        int rc = libcx().getwd(buf);
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
                throw ioex(Errno.valueOf(n).description());
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

    public void mkdir(String dir, int mode) throws IOException {
        try {
            posix().mkdir(dir, mode);
        } catch (RuntimeException ex) {
            throw ioex(Errno.valueOf(posix().errno()).description());
        }
    }

    public long strtol(String s, int base) throws IllegalArgumentException {
        posix().errno(0);
        long result = libcx().strtol(s, null, base);
        int e = posix().errno();
        if (e != 0) {
            throw new IllegalArgumentException(Errno.valueOf(e).description());
        } else {
            return result;
        }
    }

    public Object dlopen(String path, boolean local, boolean now) {
        int flags = (local ? com.kenai.jffi.Library.LOCAL : com.kenai.jffi.Library.GLOBAL) | (now ? com.kenai.jffi.Library.NOW : com.kenai.jffi.Library.LAZY);
        return com.kenai.jffi.Library.getCachedInstance(path, flags);
    }

    public long dlsym(Object handle, String symbol) {
        return ((com.kenai.jffi.Library) handle).getSymbolAddress(symbol);
    }

    public int dlclose(Object handle) {
        // TODO JNR provides no (public) way to close a library
        return 1;
    }

    public String dlerror() {
        return com.kenai.jffi.Library.getLastError();
    }

    public UtsName uname() {
        // TODO do the grunt work to make this a first class JNR call?
        return JNIUtsName.get();
    }

    /*
     * Lapack methods.
     */

    @Override
    public LapackRFFI getLapackRFFI() {
        return this;
    }

    /**
     * Fortran does call by reference for everything, which we handle with arrays. Evidently, this
     * is not as efficient as it could be. This implementation assumes a single-threaded
     * environment.
     */
    public interface Lapack {
        // Checkstyle: stop method name
        // @formatter:off
        void ilaver_(@Out int[] major, @Out int[] minor, @Out int[] patch);

        void dgeev_(@In byte[] jobVL, @In byte[] jobVR, @In int[] n, @In double[] a, @In int[] lda, @Out double[] wr, @Out double[] wi,
                        @Out double[] vl, @In int[] ldvl, @Out double[] vr, @In int[] ldvr,
                        @Out double[] work, @In int[] lwork, @Out int[] info);

        void dgeqp3_(@In int[] m, @In int[] n, double[] a, @In int[] lda, int[] jpvt, @Out double[] tau, @Out double[] work,
                        @In int[] lwork, @Out int[] info);

        void dormqr_(@In byte[] side, @In byte[] trans, @In int[] m, @In int[] n, @In int[] k, @In double[] a, @In int[] lda,
                        @In double[] tau, double[] c, @In int[] ldc, @Out double[] work, @In int[] lwork, @Out int[] info);

       void dtrtrs_(@In byte[] uplo, @In byte[] trans, @In byte[] diag, @In int[] n, @In int[] nrhs, @In double[] a, @In int[] lda,
                       double[] b, @In int[] ldb, @Out int[] info);

       void dgetrf_(@In int[] m, @In int[] n, double[] a, @In int[] lda, @Out int[] ipiv, @Out int[] info);

       void dpotrf_(@In byte[] uplo, @In int[] n, double[] a, @In int[] lda, @Out int[] info);

       void dpstrf_(@In byte[] uplo, @In int[] n, double[] a, @In int[] lda, @Out int[] piv, @Out int[] rank, @In double[] tol, @Out double[] work, @Out int[] info);
    }

    // @formatter:on

    private static class LapackProvider {
        private static Lapack lapack;

        @TruffleBoundary
        private static Lapack createAndLoadLib() {
            return LibraryLoader.create(Lapack.class).load("Rlapack");
        }

        static Lapack lapack() {
            if (lapack == null) {
                lapack = createAndLoadLib();
            }
            return lapack;
        }
    }

    private static Lapack lapack() {
        return LapackProvider.lapack();
    }

    private static abstract class RefScalars_basic {
        static int[] lwork = new int[1];
        static int[] info = new int[1];
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

    private static class RefScalars_dgeev extends RefScalars_basic {
        static byte[] jobVL = new byte[1];
        static byte[] jobVR = new byte[1];
        static int[] n = new int[1];
        static int[] lda = new int[1];
        static int[] ldvl = new int[1];
        static int[] ldvr = new int[1];
    }

    // @formatter:off
    public int dgeev(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi,
                    double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork) {
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

    private static class RefScalars_dgeqp3 extends RefScalars_basic {
        static int[] m = new int[1];
        static int[] n = new int[1];
        static int[] lda = new int[1];
    }

    public int dgeqp3(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork) {
        RefScalars_dgeqp3.m[0] = m;
        RefScalars_dgeqp3.n[0] = n;
        RefScalars_dgeqp3.lda[0] = lda;
        RefScalars_dgeqp3.lwork[0] = lwork;
        // @formatter:off
        lapack().dgeqp3_(RefScalars_dgeqp3.m, RefScalars_dgeqp3.n, a, RefScalars_dgeqp3.lda, jpvt, tau, work,
                        RefScalars_dgeqp3.lwork, RefScalars_dgeqp3.info);
        return RefScalars_dgeqp3.info[0];
    }

    private static class RefScalars_dormqr extends RefScalars_basic {
        static byte[] side = new byte[1];
        static byte[] trans = new byte[1];
        static int[] m = new int[1];
        static int[] n = new int[1];
        static int[] k = new int[1];
        static int[] lda = new int[1];
        static int[] ldc = new int[1];
   }

    // @formatter:off
    public int dormqr(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc,
                    double[] work, int lwork) {
        RefScalars_dormqr.side[0] = (byte) side;
        RefScalars_dormqr.trans[0] = (byte) trans;
        RefScalars_dormqr.m[0] = m;
        RefScalars_dormqr.n[0] = n;
        RefScalars_dormqr.k[0] = k;
        RefScalars_dormqr.lda[0] = lda;
        RefScalars_dormqr.ldc[0] = ldc;
        RefScalars_dormqr.lwork[0] = lwork;
        // @formatter:off
        lapack().dormqr_(RefScalars_dormqr.side, RefScalars_dormqr.trans, RefScalars_dormqr.m, RefScalars_dormqr.n,
                        RefScalars_dormqr.k, a, RefScalars_dormqr.lda, tau, c, RefScalars_dormqr.ldc, work,
                        RefScalars_dormqr.lwork, RefScalars_dormqr.info);
        return RefScalars_dormqr.info[0];
    }

    private static class RefScalars_dtrtrs extends RefScalars_basic {
        static byte[] uplo = new byte[1];
        static byte[] trans = new byte[1];
        static byte[] diag = new byte[1];
        static int[] n = new int[1];
        static int[] nrhs = new int[1];
        static int[] lda = new int[1];
        static int[] ldb = new int[1];
   }

   public int dtrtrs(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb) {
       RefScalars_dtrtrs.uplo[0] = (byte) uplo;
       RefScalars_dtrtrs.trans[0] = (byte) trans;
       RefScalars_dtrtrs.diag[0] = (byte) diag;
       RefScalars_dtrtrs.n[0] = (byte) n;
       RefScalars_dtrtrs.nrhs[0] = (byte) nrhs;
       RefScalars_dtrtrs.lda[0] = lda;
       RefScalars_dtrtrs.ldb[0] = ldb;
       // @formatter:off
       lapack().dtrtrs_(RefScalars_dtrtrs.uplo, RefScalars_dtrtrs.trans, RefScalars_dtrtrs.diag, RefScalars_dtrtrs.n,
                       RefScalars_dtrtrs.nrhs, a, RefScalars_dtrtrs.lda, b, RefScalars_dtrtrs.ldb, RefScalars_dtrtrs.info);
       return RefScalars_dtrtrs.info[0];
   }

   private static class RefScalars_dgetrf {
       static int[] m = new int[1];
       static int[] n = new int[1];
       static int[] lda = new int[1];
       static int[] info = new int[1];
   }

   public int dgetrf(int m, int n, double[] a, int lda, int[] ipiv) {
       RefScalars_dgetrf.m[0] = m;
       RefScalars_dgetrf.n[0] = n;
       RefScalars_dgetrf.lda[0] = lda;
       lapack().dgetrf_(RefScalars_dgetrf.m, RefScalars_dgetrf.n, a, RefScalars_dgetrf.lda, ipiv, RefScalars_dgetrf.info);
       return RefScalars_dgetrf.info[0];
   }

   private static class RefScalars_dpotrf {
       static byte[] uplo = new byte[1];
       static int[] n = new int[1];
       static int[] lda = new int[1];
       static int[] info = new int[1];
   }


   public int dpotrf(char uplo, int n, double[] a, int lda) {
       RefScalars_dpotrf.uplo[0] = (byte) uplo;
       RefScalars_dpotrf.n[0] = n;
       RefScalars_dpotrf.lda[0] = lda;
       lapack().dpotrf_(RefScalars_dpotrf.uplo, RefScalars_dpotrf.n, a, RefScalars_dpotrf.lda, RefScalars_dpotrf.info);
       return RefScalars_dpotrf.info[0];
   }

   private static class RefScalars_dpstrf extends RefScalars_dpotrf {
       static double[] tol = new double[1];
   }

   public int dpstrf(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work) {
       RefScalars_dpstrf.uplo[0] = (byte) uplo;
       RefScalars_dpstrf.n[0] = n;
       RefScalars_dpstrf.lda[0] = lda;
       RefScalars_dpstrf.tol[0] = tol;
       lapack().dpstrf_(RefScalars_dpstrf.uplo, RefScalars_dpstrf.n, a, RefScalars_dpstrf.lda, piv, rank, RefScalars_dpstrf.tol, work, RefScalars_dpstrf.info);
       return RefScalars_dpstrf.info[0];
   }


   /*
    * Linpack functions
    *
    * TODO In order to finesse a problem that "libR" does not exists separately on Linux, the Linpack functions should be amalgamated with FFT in RDerived.
    */

   @Override
   public RDerivedRFFI getRDerivedRFFI() {
       return this;
   }

   public interface Linpack {
       // @formatter:off
       void dqrdc2_(double[] x, @In int[] ldx, @In int[] n, @In int[] p, @In double[] tol, int[] rank, double[] qraux,
                       int[] pivot, @Out double[] work);
       void dqrcf_(double[] x, @In int[] n, @In int[] k, double[] qraux, double[] y, @In int[] ny, double[] b, int[] info);
   }

   private static class LinpackProvider {
       private static Linpack linpack;

       @TruffleBoundary
       private static Linpack createAndLoadLib() {
           // need to load blas lib as Fortran functions in RDerived lib need it
           LibraryLoader.create(Linpack.class).load("Rblas");
           return LibraryLoader.create(Linpack.class).load("RDerived");
       }

       static Linpack linpack() {
           if (linpack == null) {
               linpack = createAndLoadLib();
           }
           return linpack;
       }
   }

   private static Linpack linpack() {
       return LinpackProvider.linpack();
   }

   private static class RefScalars_dqrdc2 {
       static int[] ldx = new int[1];
       static int[] n = new int[1];
       static int[] p = new int[1];
       static double[] tol = new double[1];
   }

   public void dqrdc2(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work) {
       RefScalars_dqrdc2.ldx[0] = ldx;
       RefScalars_dqrdc2.n[0] = n;
       RefScalars_dqrdc2.p[0] = p;
       RefScalars_dqrdc2.tol[0] = tol;
       linpack().dqrdc2_(x, RefScalars_dqrdc2.ldx, RefScalars_dqrdc2.n, RefScalars_dqrdc2.p, RefScalars_dqrdc2.tol, rank, qraux, pivot, work);
   }


   private static class RefScalars_dqrcf {
       static int[] n = new int[1];
       static int[] k = new int[1];
       static int[] ny = new int[1];
   }

   public void dqrcf(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info) {
       RefScalars_dqrcf.n[0] = n;
       RefScalars_dqrcf.k[0] = k;
       RefScalars_dqrcf.ny[0] = ny;
       linpack().dqrcf_(x, RefScalars_dqrcf.n, RefScalars_dqrcf.k, qraux, y, RefScalars_dqrcf.ny, b, info);
   }

   // fft functions

   public interface FFT {
       // TODO add @In/@Out to any arrays that are known to be either @In or @Out (default is @Inout)

       // @formatter:off
       void fft_factor(@In int[] n, int[] pmaxf, int[] pmaxp);

       int fft_work(double[] a, @In int[] nseg, @In int[] n, @In int[] nspn, @In int[] isn, double[] work, int[] iwork);
   }

   private static class FFTProvider {
       private static FFT fft;

       @TruffleBoundary
       private static FFT createAndLoadLib() {
           return LibraryLoader.create(FFT.class).load("RDerived");
       }

       static FFT fft() {
           if (fft == null) {
               fft = createAndLoadLib();
           }
           return fft;
       }
   }

   private static FFT fft() {
       return FFTProvider.fft();
   }


   private static class RefScalars_fft_factor {
       static int[] n = new int[1];
   }

   public void fft_factor(int n, int[] pmaxf, int[] pmaxp) {
       RefScalars_fft_factor.n[0] = n;
       fft().fft_factor(RefScalars_fft_factor.n, pmaxf, pmaxp);
   }

   private static class RefScalars_fft_work extends RefScalars_fft_factor {
       static int[] nseg = new int[1];
       static int[] nspn = new int[1];
       static int[] isn = new int[1];
   }

   public int fft_work(double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork) {
       RefScalars_fft_work.n[0] = n;
       RefScalars_fft_work.nseg[0] = nseg;
       RefScalars_fft_work.nspn[0] = nspn;
       RefScalars_fft_work.isn[0] = isn;
       return fft().fft_work(a, RefScalars_fft_work.nseg, RefScalars_fft_work.n, RefScalars_fft_work.nspn, RefScalars_fft_work.isn, work, iwork);
   }


    /*
     * UserRng.
     * This is a singleton instance, although the actual library may vary from run to run.
     */


    @Override
    public UserRngRFFI getUserRngRFFI() {
        return this;
    }

    public interface UserRng {
        void user_unif_init(@In int seed);
        Pointer user_unif_rand();
        Pointer user_unif_nseed();
        Pointer user_unif_seedloc();
    }

    private static class UserRngProvider {
        private static String libPath;
        private static UserRng userRng;

        UserRngProvider(String libPath) {
            UserRngProvider.libPath = libPath;
        }

        @TruffleBoundary
        private static UserRng createAndLoadLib() {
            return LibraryLoader.create(UserRng.class).load(libPath);
        }

        static UserRng userRng() {
            if (userRng == null) {
                userRng = createAndLoadLib();
            }
            return userRng;
        }
    }

    private static UserRng userRng() {
        return UserRngProvider.userRng();
    }

    @SuppressWarnings("unused")
    public void setLibrary(String path) {
        new UserRngProvider(path);

    }

    public void init(int seed) {
        userRng().user_unif_init(seed);
    }

    public double rand() {
        Pointer pDouble = userRng().user_unif_rand();
        return pDouble.getDouble(0);
    }

    public int nSeed() {
        return userRng().user_unif_nseed().getInt(0);
    }

    public void seeds(int[] n) {
        Pointer pInt = userRng().user_unif_seedloc();
        for (int i = 0; i < n.length; i++) {
            n[i] = pInt.getInt(i);
        }
    }

    /*
     * .C methods
     */

    private static CRFFI cRFFI;

    @Override
    public CRFFI getCRFFI() {
        if (cRFFI == null) {
           cRFFI =  new CRFFI_JNR_Invoke();
        }
        return cRFFI;
    }

    /*
     * .C methods
     */

    private static CallRFFI callRFFI;

    @Override
    public CallRFFI getCallRFFI() {
        if (callRFFI == null) {
            callRFFI =  new CallRFFIWithJNI();
        }
        return callRFFI;
    }

    // zip

    public interface Zip {
        int uncompress(@Out byte[] dest, @In long[] destlen, @In byte[] source, long sourcelen);
    }

    private static class ZipProvider {
        private static Zip zip;

        @TruffleBoundary
        private static Zip createAndLoadLib() {
            return LibraryLoader.create(Zip.class).load("z");
        }

        static Zip zip() {
            if (zip == null) {
                zip = createAndLoadLib();
            }
            return zip;
        }
    }

    private static Zip zip() {
        return ZipProvider.zip();
    }

    public int uncompress(byte[] dest, long[] destlen, byte[] source) {
        return zip().uncompress(dest, destlen, source, source.length);
    }

}
