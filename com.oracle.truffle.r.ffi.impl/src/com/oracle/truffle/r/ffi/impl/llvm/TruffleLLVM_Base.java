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
package com.oracle.truffle.r.ffi.impl.llvm;

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.r.ffi.impl.interop.NativeCharArray;
import com.oracle.truffle.r.ffi.impl.interop.base.GlobResult;
import com.oracle.truffle.r.ffi.impl.interop.base.ReadlinkResult;
import com.oracle.truffle.r.ffi.impl.interop.base.StrtolResult;
import com.oracle.truffle.r.ffi.impl.interop.base.UnameResult;
import com.oracle.truffle.r.ffi.impl.nfi.NativeFunction;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public class TruffleLLVM_Base implements BaseRFFI {
    private static final class TruffleLLVM_GetpidNode extends TruffleLLVM_DownCallNode implements GetpidNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.getpid;
        }

        @Override
        public int execute() {
            return (int) call();
        }
    }

    private static class TruffleLLVM_GetwdNode extends TruffleLLVM_DownCallNode implements GetwdNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.getcwd;
        }

        @Override
        public String execute() {
            byte[] buf = new byte[4096];
            NativeCharArray nativeBuf = new NativeCharArray(buf);
            int result = (int) call(nativeBuf, buf.length);
            if (result == 0) {
                return null;
            } else {
                byte[] mbuf = nativeBuf.getValue();
                int i = 0;
                while (mbuf[i] != 0 && i < mbuf.length) {
                    i++;
                }
                return new String(mbuf, 0, i);
            }
        }
    }

    private static class TruffleLLVM_SetwdNode extends TruffleLLVM_DownCallNode implements SetwdNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.chdir;
        }

        @Override
        public int execute(String dir) {
            NativeCharArray nativeBuf = new NativeCharArray(dir.getBytes());
            return (int) call(nativeBuf);
        }
    }

    private static class TruffleLLVM_MkdirNode extends TruffleLLVM_DownCallNode implements MkdirNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.mkdir;
        }

        @Override
        public void execute(String dir, int mode) throws IOException {
            NativeCharArray nativeBuf = new NativeCharArray(dir.getBytes());
            int result = (int) call(nativeBuf, mode);
            if (result != 0) {
                throw new IOException("mkdir " + dir + " failed");
            }
        }
    }

    private static class TruffleLLVM_ReadlinkNode extends TruffleLLVM_DownCallNode implements ReadlinkNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.readlink;
        }

        private static final int EINVAL = 22;

        @Override
        public String execute(String path) throws IOException {
            NativeCharArray nativePath = new NativeCharArray(path.getBytes());
            ReadlinkResult callback = new ReadlinkResult();
            call(callback, nativePath);
            String link = callback.getLink();
            if (link == null) {
                if (callback.getErrno() == EINVAL) {
                    return path;
                } else {
                    // some other error
                    throw new IOException("readlink failed: " + callback.getErrno());
                }
            }
            return link;
        }
    }

    private static class TruffleLLVM_MkdtempNode extends TruffleLLVM_DownCallNode implements MkdtempNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.mkdtemp;
        }

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
            NativeCharArray nativeZtbytes = new NativeCharArray(ztbytes);
            int result = (int) call(nativeZtbytes);
            if (result == 0) {
                return null;
            } else {
                byte[] mztBytes = nativeZtbytes.getValue();
                return new String(mztBytes, 0, bytes.length);
            }
        }
    }

    private static class TruffleLLVM_ChmodNode extends TruffleLLVM_DownCallNode implements ChmodNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.chmod;
        }

        @Override
        public int execute(String path, int mode) {
            NativeCharArray nativePath = new NativeCharArray(path.getBytes());
            return (int) call(nativePath, mode);
        }
    }

    private static class TruffleLLVM_StrolNode extends TruffleLLVM_DownCallNode implements StrolNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.strtol;
        }

        @Override
        public long execute(String s, int base) throws IllegalArgumentException {
            NativeCharArray nativeString = new NativeCharArray(s.getBytes());
            StrtolResult callback = new StrtolResult();
            call(callback, nativeString, base);
            if (callback.getErrno() != 0) {
                throw new IllegalArgumentException("strtol failure");
            } else {
                return callback.getResult();
            }
        }
    }

    private static class TruffleLLVM_UnameNode extends TruffleLLVM_DownCallNode implements UnameNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.uname;
        }

        @Override
        public UtsName execute() {
            UnameResult baseUnameResultCallback = new UnameResult();
            call(baseUnameResultCallback);
            return baseUnameResultCallback;
        }
    }

    private static class TruffleLLVM_GlobNode extends TruffleLLVM_DownCallNode implements GlobNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.glob;
        }

        @Override
        public ArrayList<String> glob(String pattern) {
            NativeCharArray nativePattern = new NativeCharArray(pattern.getBytes());
            GlobResult baseGlobResultCallback = new GlobResult();
            call(baseGlobResultCallback, nativePattern);
            return baseGlobResultCallback.getPaths();
        }
    }

    @Override
    public GetpidNode createGetpidNode() {
        return new TruffleLLVM_GetpidNode();
    }

    @Override
    public GetwdNode createGetwdNode() {
        return new TruffleLLVM_GetwdNode();
    }

    @Override
    public SetwdNode createSetwdNode() {
        return new TruffleLLVM_SetwdNode();
    }

    @Override
    public MkdirNode createMkdirNode() {
        return new TruffleLLVM_MkdirNode();
    }

    @Override
    public ReadlinkNode createReadlinkNode() {
        return new TruffleLLVM_ReadlinkNode();
    }

    @Override
    public MkdtempNode createMkdtempNode() {
        return new TruffleLLVM_MkdtempNode();
    }

    @Override
    public ChmodNode createChmodNode() {
        return new TruffleLLVM_ChmodNode();
    }

    @Override
    public StrolNode createStrolNode() {
        return new TruffleLLVM_StrolNode();
    }

    @Override
    public UnameNode createUnameNode() {
        return new TruffleLLVM_UnameNode();
    }

    @Override
    public GlobNode createGlobNode() {
        return new TruffleLLVM_GlobNode();
    }
}
