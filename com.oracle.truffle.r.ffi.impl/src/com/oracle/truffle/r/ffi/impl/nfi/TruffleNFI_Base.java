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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.base.GlobResult;
import com.oracle.truffle.r.ffi.impl.interop.base.ReadlinkResult;
import com.oracle.truffle.r.ffi.impl.interop.base.StrtolResult;
import com.oracle.truffle.r.ffi.impl.interop.base.UnameResult;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public class TruffleNFI_Base implements BaseRFFI {

    public static class TruffleNFI_GetpidNode extends GetpidNode {
        @Child private Node message = NFIFunction.getpid.createMessage();

        @Override
        public int execute() {
            try {
                int result = (int) ForeignAccess.sendExecute(message, NFIFunction.getpid.getFunction());
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_GetwdNode extends GetwdNode {
        @Child private Node message = NFIFunction.getcwd.createMessage();

        @TruffleBoundary
        @Override
        public String execute() {
            try {
                byte[] buf = new byte[4096];
                int result = (int) ForeignAccess.sendExecute(message, NFIFunction.getcwd.getFunction(), JavaInterop.asTruffleObject(buf), buf.length);
                if (result == 0) {
                    return null;
                } else {
                    int i = 0;
                    while (buf[i] != 0 && i < buf.length) {
                        i++;
                    }
                    return new String(buf, 0, i);
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_SetwdNode extends SetwdNode {
        @Child private Node message = NFIFunction.chdir.createMessage();

        @Override
        public int execute(String dir) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.chdir.getFunction(), dir);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_MkdirNode extends MkdirNode {
        @Child private Node message = NFIFunction.mkdir.createMessage();

        @Override
        public void execute(String dir, int mode) throws IOException {
            try {
                int result = (int) ForeignAccess.sendExecute(message, NFIFunction.mkdir.getFunction(), dir, mode);
                if (result != 0) {
                    throw new IOException("mkdir " + dir + " failed");
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_ReadlinkNode extends ReadlinkNode {
        private static final int EINVAL = 22;

        @Child private Node message = NFIFunction.readlink.createMessage();

        @Override
        public String execute(String path) throws IOException {
            try {
                ReadlinkResult data = new ReadlinkResult();
                ForeignAccess.sendExecute(message, NFIFunction.readlink.getFunction(), data, path);
                if (data.getLink() == null) {
                    if (data.getErrno() == EINVAL) {
                        return path;
                    } else {
                        // some other error
                        throw new IOException("readlink failed: " + data.getErrno());
                    }
                }
                return data.getLink();
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_MkdtempNode extends MkdtempNode {
        @Child private Node message = NFIFunction.mkdtemp.createMessage();

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
            try {
                int result = (int) ForeignAccess.sendExecute(message, NFIFunction.mkdtemp.getFunction(), JavaInterop.asTruffleObject(ztbytes));
                if (result == 0) {
                    return null;
                } else {
                    return new String(ztbytes, 0, bytes.length);
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_ChmodNode extends ChmodNode {
        @Child private Node message = NFIFunction.chmod.createMessage();

        @Override
        public int execute(String path, int mode) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.chmod.getFunction(), path, mode);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_StrolNode extends StrolNode {

        @Child private Node message = NFIFunction.strtol.createMessage();

        @Override
        public long execute(String s, int base) throws IllegalArgumentException {
            try {
                StrtolResult data = new StrtolResult();
                ForeignAccess.sendExecute(message, NFIFunction.strtol.getFunction(), data, s, base);
                if (data.getErrno() != 0) {
                    throw new IllegalArgumentException("strtol failure");
                } else {
                    return data.getResult();
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_UnameNode extends UnameNode {

        @Child private Node message = NFIFunction.uname.createMessage();

        @Override
        public UtsName execute() {
            UnameResult data = new UnameResult();
            try {
                ForeignAccess.sendExecute(message, NFIFunction.uname.getFunction(), data);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            return data;
        }
    }

    public static class TruffleNFI_GlobNode extends GlobNode {

        @Child private Node message = NFIFunction.glob.createMessage();

        @Override
        public ArrayList<String> glob(String pattern) {
            GlobResult data = new GlobResult();
            try {
                ForeignAccess.sendExecute(message, NFIFunction.glob.getFunction(), data, pattern);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
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
