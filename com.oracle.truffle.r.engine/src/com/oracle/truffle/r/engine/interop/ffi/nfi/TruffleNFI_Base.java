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
package com.oracle.truffle.r.engine.interop.ffi.nfi;

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;

public class TruffleNFI_Base implements BaseRFFI {

    private enum Function {
        getpid("(): sint32", true),
        getcwd("([uint8], sint32): sint32", true),
        chdir("(string): sint32", true),
        mkdir("(string, sint32): sint32", true),
        call_readlink("((string, sint32): void, string): void", false),
        mkdtemp("([uint8]): sint32", true),
        chmod("(string, sint32): sint32", true),
        call_strtol("((sint64, sint32): void, string, sint32): void", false),
        call_uname("((string, string, string, string, string): void): void", false),
        call_glob("(string, (string): void): void", false);

        private final int argCount;
        private final String signature;
        private final boolean useDefaultLibrary;
        private Node message;
        private TruffleObject function;

        Function() {
            this(null, true);
        }

        Function(String signature, boolean useDefaultLibrary) {
            this.argCount = TruffleNFI_Utils.getArgCount(signature);
            this.signature = signature;
            this.useDefaultLibrary = useDefaultLibrary;
        }

        private void initialize() {
            if (message == null) {
                message = Message.createExecute(argCount).createNode();
            }
            if (function == null) {
                function = TruffleNFI_Utils.lookupAndBind(name(), useDefaultLibrary, signature);
            }
        }
    }

    public static class TruffleNFI_GetpidNode extends GetpidNode {
        @Override
        public int execute() {
            Function.getpid.initialize();
            try {
                int result = (int) ForeignAccess.sendExecute(Function.getpid.message, Function.getpid.function);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }

        }
    }

    public static class TruffleNFI_GetwdNode extends GetwdNode {
        @Override
        public String execute() {
            byte[] buf = new byte[4096];
            Function.getcwd.initialize();
            try {
                int result = (int) ForeignAccess.sendExecute(Function.getcwd.message, Function.getcwd.function, JavaInterop.asTruffleObject(buf), buf.length);
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
        @Override
        public int execute(String dir) {
            Function.chdir.initialize();
            try {
                int result = (int) ForeignAccess.sendExecute(Function.chdir.message, Function.chdir.function, dir);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_MkdirNode extends MkdirNode {
        @Override
        public void execute(String dir, int mode) throws IOException {
            Function.mkdir.initialize();
            try {
                int result = (int) ForeignAccess.sendExecute(Function.mkdir.message, Function.mkdir.function, dir, mode);
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

        interface SetResult {
            void setResult(String link, int errno);
        }

        public static class SetResultImpl implements SetResult, RTruffleObject {
            private String link;
            private int errno;

            @Override
            public void setResult(String link, int errno) {
                this.link = link;
                this.errno = errno;
            }

        }

        @Override
        public String execute(String path) throws IOException {
            Function.call_readlink.initialize();
            try {
                SetResultImpl setResultImpl = new SetResultImpl();
                ForeignAccess.sendExecute(Function.call_readlink.message, Function.call_readlink.function, setResultImpl, path);
                if (setResultImpl.link == null) {
                    if (setResultImpl.errno == EINVAL) {
                        return path;
                    } else {
                        // some other error
                        throw new IOException("readlink failed: " + setResultImpl.errno);
                    }
                }
                return setResultImpl.link;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_MkdtempNode extends MkdtempNode {
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
            Function.mkdtemp.initialize();
            try {
                int result = (int) ForeignAccess.sendExecute(Function.mkdtemp.message, Function.mkdtemp.function, JavaInterop.asTruffleObject(ztbytes));
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
        @Override
        public int execute(String path, int mode) {
            Function.chmod.initialize();
            try {
                int result = (int) ForeignAccess.sendExecute(Function.chmod.message, Function.chmod.function, path, mode);
                return result;
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_StrolNode extends StrolNode {
        interface SetResult {
            void setResult(long result, int errno);
        }

        private static class SetResultImpl implements SetResult {
            private long result;
            private int errno;

            @Override
            public void setResult(long result, int errno) {
                this.result = result;
                this.errno = errno;
            }
        }

        @Override
        public long execute(String s, int base) throws IllegalArgumentException {
            Function.call_strtol.initialize();
            try {
                SetResultImpl setResultImpl = new SetResultImpl();
                ForeignAccess.sendExecute(Function.call_strtol.message, Function.call_strtol.function, JavaInterop.asTruffleFunction(SetResult.class, setResultImpl), s, base);
                if (setResultImpl.errno != 0) {
                    throw new IllegalArgumentException("strtol failure");
                } else {
                    return setResultImpl.result;
                }
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static class TruffleNFI_UnameNode extends UnameNode {
        private static UnameUpCallImpl unameUpCallImpl;

        private interface UnameUpCall {
            void unameUpCall(String sysname, String release, String version, String machine, String nodename);
        }

        public static class UnameUpCallImpl implements UnameUpCall, UtsName, RTruffleObject {
            private String sysname;
            private String release;
            private String version;
            private String machine;
            private String nodename;

            @Override
            public void unameUpCall(String sysnameA, String releaseA, String versionA, String machineA, String nodenameA) {
                sysname = sysnameA;
                release = releaseA;
                version = versionA;
                machine = machineA;
                nodename = nodenameA;
            }

            @Override
            public String sysname() {
                return sysname;
            }

            @Override
            public String release() {
                return release;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public String machine() {
                return machine;
            }

            @Override
            public String nodename() {
                return nodename;
            }

        }

        @Override
        public UtsName execute() {
            Function.call_uname.initialize();
            if (unameUpCallImpl == null) {
                unameUpCallImpl = new UnameUpCallImpl();
                try {
                    ForeignAccess.sendExecute(Function.call_uname.message, Function.call_uname.function, unameUpCallImpl);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }
            return unameUpCallImpl;
        }
    }

    public static class TruffleNFI_GlobNode extends GlobNode {
        private interface GlobUpCall {
            void addPath(String path);
        }

        public static class GlobUpCallImpl implements GlobUpCall, RTruffleObject {
            private ArrayList<String> paths = new ArrayList<>();

            @Override
            public void addPath(String path) {
                paths.add(path);
            }
        }

        @Override
        public ArrayList<String> glob(String pattern) {
            Function.call_glob.initialize();
            GlobUpCallImpl globUpCallImpl = new GlobUpCallImpl();
            try {
                ForeignAccess.sendExecute(Function.call_glob.message, Function.call_glob.function, pattern, globUpCallImpl);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
            return globUpCallImpl.paths;
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
