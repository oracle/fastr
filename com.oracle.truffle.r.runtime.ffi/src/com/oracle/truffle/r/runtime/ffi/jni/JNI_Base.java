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

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public class JNI_Base implements BaseRFFI {
    public static class JNI_GetpidNode extends GetpidNode {
        @TruffleBoundary
        @Override
        public int execute() {
            return native_getpid();
        }
    }

    public static class JNI_GetwdNode extends GetwdNode {
        @TruffleBoundary
        @Override
        public String execute() {
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
    }

    public static class JNI_SetwdNode extends SetwdNode {
        @TruffleBoundary
        @Override
        public int execute(String dir) {
            return native_setwd(dir);
        }
    }

    public static class JNI_ReadlinkNode extends ReadlinkNode {
        private static final int EINVAL = 22;

        @TruffleBoundary
        @Override
        public String execute(String path) throws IOException {
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
    }

    public static class JNI_MkdtempNode extends MkdtempNode {
        @TruffleBoundary
        @Override
        public String execute(String template) {
            /*
             * Not only must the (C) string end in XXXXXX it must also be null-terminated. Since it
             * is modified by mkdtemp we must make a copy.
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
    }

    public static class JNI_MkdirNode extends MkdirNode {
        @TruffleBoundary
        @Override
        public void execute(String dir, int mode) throws IOException {
            int rc = native_mkdir(dir, mode);
            if (rc != 0) {
                throw new IOException("mkdir " + dir + " failed");
            }
        }
    }

    public static class JNI_ChmodNode extends ChmodNode {
        @TruffleBoundary
        @Override
        public int execute(String path, int mode) {
            return native_chmod(path, mode);
        }
    }

    public static class JNI_StrolNode extends StrolNode {
        @TruffleBoundary
        @Override
        public long execute(String s, int base) throws IllegalArgumentException {
            int[] errno = new int[]{0};
            long result = native_strtol(s, base, errno);
            if (errno[0] != 0) {
                throw new IllegalArgumentException("strtol failure");
            } else {
                return result;
            }
        }
    }

    public static class JNI_UnameNode extends UnameNode {
        @TruffleBoundary
        @Override
        public UtsName execute() {
            return JNI_UtsName.get();
        }
    }

    public static class JNI_GlobNode extends GlobNode {
        @TruffleBoundary
        @Override
        public ArrayList<String> glob(String pattern) {
            return JNI_Glob.glob(pattern);
        }
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

    @Override
    public GetpidNode createGetpidNode() {
        return new JNI_GetpidNode();
    }

    @Override
    public GetwdNode createGetwdNode() {
        return new JNI_GetwdNode();
    }

    @Override
    public SetwdNode createSetwdNode() {
        return new JNI_SetwdNode();
    }

    @Override
    public MkdirNode createMkdirNode() {
        return new JNI_MkdirNode();
    }

    @Override
    public ReadlinkNode createReadlinkNode() {
        return new JNI_ReadlinkNode();
    }

    @Override
    public MkdtempNode createMkdtempNode() {
        return new JNI_MkdtempNode();
    }

    @Override
    public ChmodNode createChmodNode() {
        return new JNI_ChmodNode();
    }

    @Override
    public StrolNode createStrolNode() {
        return new JNI_StrolNode();
    }

    @Override
    public UnameNode createUnameNode() {
        return new JNI_UnameNode();
    }

    @Override
    public GlobNode createGlobNode() {
        return new JNI_GlobNode();
    }
}
