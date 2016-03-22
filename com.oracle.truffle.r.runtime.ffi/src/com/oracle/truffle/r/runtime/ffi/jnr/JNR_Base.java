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

import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.ioex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

import jnr.constants.platform.Errno;
import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;

public class JNR_Base implements BaseRFFI {
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

    protected POSIX posix() {
        if (posix == null) {
            posix = POSIXFactory.getPOSIX();
        }
        return posix;
    }

    @Override
    public int getpid() {
        return posix().getpid();
    }

    @Override
    public int setwd(String dir) {
        return posix().chdir(dir);
    }

    @Override
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

    @Override
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

    @Override
    public String mkdtemp(String template) {
        ByteBuffer bb = ByteBuffer.wrap(template.getBytes());
        long result = libcx().mkdtemp(bb);
        if (result == 0) {
            return null;
        } else {
            return new String(bb.array());
        }
    }

    @Override
    public void mkdir(String dir, int mode) throws IOException {
        try {
            posix().mkdir(dir, mode);
        } catch (RuntimeException ex) {
            throw ioex(Errno.valueOf(posix().errno()).description());
        }
    }

    @Override
    public int chmod(String path, int mode) {
        return posix().chmod(path, mode);
    }

    @Override
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

    @Override
    public Object dlopen(String path, boolean local, boolean now) {
        int flags = (local ? com.kenai.jffi.Library.LOCAL : com.kenai.jffi.Library.GLOBAL) | (now ? com.kenai.jffi.Library.NOW : com.kenai.jffi.Library.LAZY);
        return com.kenai.jffi.Library.getCachedInstance(path, flags);
    }

    @Override
    public long dlsym(Object handle, String symbol) {
        return ((com.kenai.jffi.Library) handle).getSymbolAddress(symbol);
    }

    @Override
    public int dlclose(Object handle) {
        // TODO JNR provides no (public) way to close a library
        return 1;
    }

    @Override
    public String dlerror() {
        return com.kenai.jffi.Library.getLastError();
    }

    @Override
    public UtsName uname() {
        return JNI_OSExtras.uname();
    }

    @Override
    public ArrayList<String> glob(String pattern) {
        return JNI_OSExtras.glob(pattern);
    }
}
