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

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public class JNI_Base implements BaseRFFI {
    @Override
    public int getpid() {
        return native_getpid();
    }

    @Override
    public int setwd(String dir) {
        return native_setwd(dir);
    }

    @Override
    public String getwd() {
        byte[] buf = new byte[4096];
        int rc = native_getwd(buf, buf.length);
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

    private static final int EINVAL = 22;

    @Override
    public String readlink(String path) throws IOException {
        int[] errno = new int[]{0};
        String s = native_readlink(path, errno);
        if (s == null) {
            if (errno[0] == EINVAL) {
                // not a link
            } else {
                // some other error
                throw new IOException("readlink failed: " + errno[0]);
            }
        }
        return s;
    }

    @Override
    public String mkdtemp(String template) {
        /*
         * Not only must the (C) string end in XXXXXX it must also be null-terminated. Since it is
         * modified by mkdtemp we must make a copy.
         */
        byte[] bytes = template.getBytes();
        byte[] ztbytes = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, ztbytes, 0, bytes.length);
        ztbytes[bytes.length] = 0;
        long result = native_mkdtemp(ztbytes);
        if (result == 0) {
            return null;
        } else {
            return new String(ztbytes, 0, bytes.length);
        }
    }

    @Override
    public void mkdir(String dir, int mode) throws IOException {
        int rc = native_mkdir(dir, mode);
        if (rc != 0) {
            throw new IOException("mkdir " + dir + " failed");
        }
    }

    @Override
    public int chmod(String path, int mode) {
        return native_chmod(path, mode);
    }

    @Override
    public long strtol(String s, int base) throws IllegalArgumentException {
        int[] errno = new int[]{0};
        long result = native_strtol(s, base, errno);
        if (errno[0] != 0) {
            throw new IllegalArgumentException("strtol failure");
        } else {
            return result;
        }
    }

    @Override
    public UtsName uname() {
        return JNI_UtsName.get();
    }

    @Override
    public ArrayList<String> glob(String pattern) {
        return JNI_Glob.glob(pattern);
    }

    // Checkstyle: stop method name

    private static native int native_getpid();

    private static native int native_getwd(byte[] buf, int buflength);

    private static native int native_setwd(String dir);

    private static native int native_mkdtemp(byte[] template);

    private static native int native_mkdir(String dir, int mode);

    private static native int native_chmod(String dir, int mode);

    private static native long native_strtol(String s, int base, int[] errno);

    private static native String native_readlink(String s, int[] errno);
}
