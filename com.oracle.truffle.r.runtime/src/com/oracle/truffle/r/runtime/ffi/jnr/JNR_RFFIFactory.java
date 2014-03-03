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
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;

import jnr.ffi.*;
import jnr.ffi.annotations.*;
import jnr.posix.*;
import jnr.posix.util.*;
import jnr.constants.platform.Errno;

import com.oracle.truffle.r.runtime.ffi.*;

/**
 * A naive JNR-based factory that supports access to POSIX functions only. Naive because it uses
 * reflection to invoke the (generic) target method. Access to the base functions is as efficient as
 * it can be with JNR.
 */
public class JNR_RFFIFactory extends BaseRFFIFactory {
    protected static final POSIX posix = POSIXFactory.getPOSIX(new DefaultPOSIXHandler(), true);
    private static final Map<String, Method> posixFunctions = new HashMap<>();

    static {
        for (Method m : POSIX.class.getDeclaredMethods()) {
            posixFunctions.put(m.getName(), m);
        }
    }

    @Override
    protected RFFI createRFFI() {
        return this;
    }

    public Object invoke(Object handle, Object[] args) throws RFFIException {
        Method m = posixFunctions.get(handle);
        if (m == null) {
            throw new RFFIException("JNR function " + ((String) handle) + " not found", null);
        }
        try {
            return m.invoke(posix, args);
        } catch (InvocationTargetException | IllegalAccessException ex) {
            throw new RFFIException("JNR invoke exception", ex);
        }
    }

    public int getpid() {
        return posix.getpid();
    }

    public int setwd(String dir) {
        return posix.chdir(dir);
    }

    /**
     * Functions missing from JNR POSIX.
     */
    public interface LibCX {
        int getcwd(@Out byte[] path);

        long mkdtemp(@In @Out ByteBuffer template);

        int access(String path, int amode);
    }

    private static LibCX libcx;

    private static LibCX libcx() {
        if (libcx == null) {
            libcx = LibraryLoader.create(LibCX.class).load("c");
        }
        return libcx;
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

    public Object getHandle(String name) {
        return name;
    }

    public String readlink(String path) throws IOException {
        String s = posix.readlink(path);
        if (s == null) {
            int n = posix.errno();
            if (n == Errno.EINVAL.intValue()) {
                // not a link
            } else {
                // some other error
                throw new IOException();
            }
        }
        return s;
    }

    public boolean isWriteableDirectory(String path) {
        if (exists(path)) {
            FileStat fileStat = posix.stat(path);
            return fileStat.isDirectory() && fileStat.isWritable();
        } else {
            return false;
        }
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

    private static final int F_OK = 0;

    public boolean exists(String path) {
        int result = libcx().access(path, F_OK);
        return result == 0;
    }

}
