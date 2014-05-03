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
package com.oracle.truffle.r.runtime.ffi.gnfi;

import static com.oracle.graal.graph.UnsafeAccess.*;

import java.io.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.runtime.*;
import com.oracle.graal.compiler.target.*;
import com.oracle.graal.runtime.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.ffi.*;

/**
 * BaseRFFI using the Graal Native Function Interface (GNFI). N.B. Pointers, e.g. {@code char*} are
 * denoted as {@code long} in argument signatures.
 *
 * N.B. This code is very unsafe!
 *
 */
public class GNFI_RFFIFactory extends RFFIFactory implements RFFI, BaseRFFI {

    private static NativeFunctionInterface nfi;
    private static NativeFunctionHandle malloc;
    private static NativeFunctionHandle free;
    private static NativeFunctionHandle readlink;
    private static NativeFunctionHandle chdir;
    private static NativeFunctionHandle getcwd;
    private static NativeFunctionHandle mkdtemp;

    private static class AutoMemory implements AutoCloseable {
        long address;

        AutoMemory(long address) {
            this.address = address;
        }

        static AutoMemory create(int size) {
            return new AutoMemory((long) malloc.call(size));
        }

        public void close() {
            if (address != 0) {
                free.call(address);
                address = 0;
            }
        }

    }

    private static class CString extends AutoMemory {

        CString(String s) {
            super(createCString(s));
        }

        static String create(final long nativeAddress, final int length, final boolean lookForZero) {
            byte[] result = new byte[length + 1];
            long buf = nativeAddress;
            int elength = length;
            for (int i = 0; i < length; i++) {
                byte b = unsafe.getByte(buf++);
                if (lookForZero && b == 0) {
                    elength = i;
                    break;
                }
                result[i] = b;
            }
            return new String(result, 0, elength);
        }
    }

    @Override
    protected RFFI createRFFI() {
        try {
            RuntimeProvider runtimeProvider = Graal.getRequiredCapability(RuntimeProvider.class);
            nfi = ((HostBackend) runtimeProvider.getHostBackend()).getNativeFunctionInterface();
            malloc = nfi.getFunctionHandle("malloc", long.class, int.class);
            free = nfi.getFunctionHandle("free", void.class, long.class);
            readlink = nfi.getFunctionHandle("readlink", int.class, long.class, long.class, int.class);
            chdir = nfi.getFunctionHandle("chdir", int.class, long.class);
            getcwd = nfi.getFunctionHandle("getcwd", long.class, long.class, int.class);
            mkdtemp = nfi.getFunctionHandle("mkdtemp", long.class, long.class);
            return this;
        } catch (UnsupportedOperationException ex) {
            Utils.fail(ex.getMessage());
            return null;
        }
    }

    public int getpid() {
        NativeFunctionHandle getpid = nfi.getFunctionHandle("getpid", int.class);
        return (int) getpid.call();
    }

    public String getwd() {
        try (AutoMemory resultBuf = AutoMemory.create(4096)) {
            long wd = (long) getcwd.call(resultBuf.address, 4096);
            if (wd == 0) {
                return null;
            } else {
                return CString.create(resultBuf.address, 4096, true);
            }
        }
    }

    public int setwd(String dir) {
        try (CString cString = new CString(dir)) {
            return (int) chdir.call(cString.address);
        }
    }

    public String readlink(String path) throws IOException {
        try (CString cString = new CString(path); AutoMemory resultBuf = AutoMemory.create(4096)) {
            int length = (int) readlink.call(cString.address, resultBuf.address, 4096);
            if (length == -1) {
                // how to get errno?
                if (new File(path).exists()) {
                    // not a link
                    return null;
                } else {
                    // some other error
                    throw new IOException();
                }
            } else {
                return CString.create(resultBuf.address, length, false);
            }
        }
    }

    public String mkdtemp(String template) {
        try (CString cString = new CString(template)) {
            long result = (long) mkdtemp.call(cString.address);
            if (result == 0) {
                return null;
            } else {
                return CString.create(cString.address, template.length(), true);
            }
        }
    }

    public BaseRFFI getBaseRFFI() {
        return this;
    }

    public Object dlopen(String path, boolean local, boolean now) {
        Utils.fail("dlopen not implemented");
        return 0;
    }

    public long dlsym(Object handle, String symbol) {
        Utils.fail("dlsym not implemented");
        return 0;
    }

    public int dlclose(Object handle) {
        Utils.fail("dlclose not implemented");
        return 0;
    }

    public String dlerror() {
        Utils.fail("dlerror not implemented");
        return null;
    }

}
