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
package com.oracle.truffle.r.ffi.impl.nfi;

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.r.ffi.impl.interop.base.GlobResult;
import com.oracle.truffle.r.ffi.impl.interop.base.ReadlinkResult;
import com.oracle.truffle.r.ffi.impl.interop.base.StrtolResult;
import com.oracle.truffle.r.ffi.impl.interop.base.UnameResult;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public class TruffleNFI_Base implements BaseRFFI {

    private static class TruffleNFI_GetpidNode extends TruffleNFI_DownCallNode implements GetpidNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.getpid;
        }

        @Override
        public int execute() {
            return (int) call();
        }
    }

    private static final class TruffleNFI_GetwdNode extends TruffleNFI_DownCallNode implements GetwdNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.getcwd;
        }

        @TruffleBoundary
        @Override
        public String execute() {
            byte[] buf = new byte[4096];
            int result = (int) call(JavaInterop.asTruffleObject(buf), buf.length);
            if (result == 0) {
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

    private static class TruffleNFI_SetwdNode extends TruffleNFI_DownCallNode implements SetwdNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.chdir;
        }

        @Override
        public int execute(String dir) {
            return (int) call(dir);
        }
    }

    private static class TruffleNFI_MkdirNode extends TruffleNFI_DownCallNode implements MkdirNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.mkdir;
        }

        @Override
        public void execute(String dir, int mode) throws IOException {
            if ((int) call(dir, mode) != 0) {
                throw new IOException("mkdir " + dir + " failed");
            }
        }
    }

    private static class TruffleNFI_ReadlinkNode extends TruffleNFI_DownCallNode implements ReadlinkNode {
        private static final int EINVAL = 22;

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.readlink;
        }

        @Override
        public String execute(String path) throws IOException {
            ReadlinkResult data = new ReadlinkResult();
            call(data, path);
            if (data.getLink() == null) {
                if (data.getErrno() == EINVAL) {
                    return path;
                } else {
                    // some other error
                    throw new IOException("readlink failed: " + data.getErrno());
                }
            }
            return data.getLink();
        }
    }

    private static class TruffleNFI_MkdtempNode extends TruffleNFI_DownCallNode implements MkdtempNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.mkdtemp;
        }

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
            int result = (int) call(JavaInterop.asTruffleObject(ztbytes));
            if (result == 0) {
                return null;
            } else {
                return new String(ztbytes, 0, bytes.length);
            }
        }
    }

    private static class TruffleNFI_ChmodNode extends TruffleNFI_DownCallNode implements ChmodNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.chmod;
        }

        @Override
        public int execute(String path, int mode) {
            return (int) call(path, mode);
        }
    }

    private static class TruffleNFI_StrolNode extends TruffleNFI_DownCallNode implements StrolNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.strtol;
        }

        @Override
        public long execute(String s, int base) throws IllegalArgumentException {
            StrtolResult data = new StrtolResult();
            call(data, s, base);
            if (data.getErrno() != 0) {
                throw new IllegalArgumentException("strtol failure");
            } else {
                return data.getResult();
            }
        }
    }

    private static class TruffleNFI_UnameNode extends TruffleNFI_DownCallNode implements UnameNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.uname;
        }

        @Override
        public UtsName execute() {
            UnameResult data = new UnameResult();
            call(data);
            return data;
        }
    }

    private static class TruffleNFI_GlobNode extends TruffleNFI_DownCallNode implements GlobNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.glob;
        }

        @Override
        public ArrayList<String> glob(String pattern) {
            GlobResult data = new GlobResult();
            call(data, pattern);
            return data.getPaths();
        }
    }

    @Override
    public GetpidNode createGetpidNode() {
        return new TruffleNFI_GetpidNode();
    }

    @Override
    public GetwdNode createGetwdNode() {
        return new TruffleNFI_GetwdNode();
    }

    @Override
    public SetwdNode createSetwdNode() {
        return new TruffleNFI_SetwdNode();
    }

    @Override
    public MkdirNode createMkdirNode() {
        return new TruffleNFI_MkdirNode();
    }

    @Override
    public ReadlinkNode createReadlinkNode() {
        return new TruffleNFI_ReadlinkNode();
    }

    @Override
    public MkdtempNode createMkdtempNode() {
        return new TruffleNFI_MkdtempNode();
    }

    @Override
    public ChmodNode createChmodNode() {
        return new TruffleNFI_ChmodNode();
    }

    @Override
    public StrolNode createStrolNode() {
        return new TruffleNFI_StrolNode();
    }

    @Override
    public UnameNode createUnameNode() {
        return new TruffleNFI_UnameNode();
    }

    @Override
    public GlobNode createGlobNode() {
        return new TruffleNFI_GlobNode();
    }
}
