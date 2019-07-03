/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ffi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.ffi.base.ESoftVersionResult;
import com.oracle.truffle.r.runtime.ffi.base.GlobResult;
import com.oracle.truffle.r.runtime.ffi.base.ReadlinkResult;
import com.oracle.truffle.r.runtime.ffi.base.StrtolResult;
import com.oracle.truffle.r.runtime.ffi.base.UnameResult;
import com.oracle.truffle.r.runtime.ffi.interop.NativeCharArray;

/**
 * A statically typed interface to exactly those native functions required by the R {@code base}
 * package, because the functionality is not provided by the JDK. These methods do not necessarily
 * map 1-1 to a native function, they may involve the invocation of several native functions.
 */
public final class BaseRFFI {

    private final DownCallNodeFactory downCallNodeFactory;
    private final DownCallNodeFactory eventLoopDownCallNodeFactory;

    public BaseRFFI(DownCallNodeFactory downCallNodeFactory, DownCallNodeFactory eventLoopDownCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
        this.eventLoopDownCallNodeFactory = eventLoopDownCallNodeFactory;
    }

    public static final class InitEventLoopNode extends NativeCallNode {

        private InitEventLoopNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public int execute(String fifoInPath, String fifoOutPath) {
            return (int) call(NativeFunction.initEventLoop, fifoInPath, fifoOutPath);
        }

        public static InitEventLoopNode create() {
            return RFFIFactory.getBaseRFFI().createInitEventLoopNode();
        }
    }

    public static final class DispatchHandlersNode extends NativeCallNode {

        private DispatchHandlersNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public int execute() {
            return (int) call(NativeFunction.dispatchHandlers);
        }

        public static DispatchHandlersNode create() {
            return RFFIFactory.getBaseRFFI().createDispatchHandlersNode();
        }
    }

    public static final class GetpidNode extends NativeCallNode {

        private GetpidNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public int execute() {
            return (int) call(NativeFunction.getpid);
        }

        public static GetpidNode create() {
            return RFFIFactory.getBaseRFFI().createGetpidNode();
        }
    }

    public static final class GetwdNode extends NativeCallNode {
        private static final int BUFFER_LEN = 4096;

        private GetwdNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        /**
         * Returns the current working directory, in the face of calls to {@code setwd}.
         */
        public String execute() {
            NativeCharArray nativeBuf = NativeCharArray.crateOutputBuffer(BUFFER_LEN);
            int result = (int) call(NativeFunction.getcwd, nativeBuf, BUFFER_LEN);
            if (result == 0) {
                return null;
            } else {
                return nativeBuf.getStringFromOutputBuffer();
            }
        }

        public static GetwdNode create() {
            return RFFIFactory.getBaseRFFI().createGetwdNode();
        }
    }

    public static final class SetwdNode extends NativeCallNode {

        private SetwdNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        /**
         * Sets the current working directory to {@code dir}. (cf. Unix {@code chdir}).
         *
         * @return 0 if successful.
         */
        public int execute(String dir) {
            return (int) call(NativeFunction.chdir, dir);
        }

        public static SetwdNode create() {
            return RFFIFactory.getBaseRFFI().createSetwdNode();
        }
    }

    public static final class ReadlinkNode extends NativeCallNode {
        private static final int EINVAL = 22;

        private ReadlinkNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        /**
         * Try to convert a symbolic link to it's target.
         *
         * @param path the link path
         * @return the target if {@code path} is a link else {@code null}
         * @throws IOException for any other error except "not a link"
         */
        public String execute(String path) throws IOException {
            ReadlinkResult data = new ReadlinkResult();
            call(NativeFunction.readlink, data, path);
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

        public static ReadlinkNode create() {
            return RFFIFactory.getBaseRFFI().createReadlinkNode();
        }
    }

    public static final class MkdtempNode extends NativeCallNode {

        private MkdtempNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        /**
         * Creates a temporary directory using {@code template} and return the resulting path or
         * {@code null} if error.
         */
        @TruffleBoundary
        public String execute(String template) {
            /*
             * Not only must the (C) string end in XXXXXX it must also be null-terminated. Since it
             * is modified by mkdtemp we must make a copy.
             */
            NativeCharArray nativeZtbytes = new NativeCharArray(template);
            int result = (int) call(NativeFunction.mkdtemp, nativeZtbytes);
            if (result == 0) {
                return null;
            } else {
                return nativeZtbytes.getString();
            }
        }

        public static MkdtempNode create() {
            return RFFIFactory.getBaseRFFI().createMkdtempNode();
        }
    }

    public static final class StrtolNode extends NativeCallNode {

        private StrtolNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        /**
         * Convert string to long.
         */
        public long execute(String s, int base) throws IllegalArgumentException {
            StrtolResult data = new StrtolResult();
            call(NativeFunction.strtol, data, s, base);
            if (data.getErrno() != 0) {
                throw new IllegalArgumentException("strtol failure");
            } else {
                return data.getResult();
            }
        }

        public static StrtolNode create() {
            return RFFIFactory.getBaseRFFI().createStrtolNode();
        }
    }

    public interface UtsName {
        String sysname();

        String release();

        String version();

        String machine();

        String nodename();
    }

    public static final class UnameNode extends NativeCallNode {

        private UnameNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        /**
         * Return {@code utsname} info.
         */
        public UtsName execute() {
            UnameResult data = new UnameResult();
            call(NativeFunction.uname, data);
            return data;
        }

        public static UnameNode create() {
            return RFFIFactory.getBaseRFFI().createUnameNode();
        }
    }

    public static final class GlobNode extends NativeCallNode {

        private GlobNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        /**
         * Returns an array of pathnames that match {@code pattern} using the OS glob function. This
         * is done in native code because it is very hard to write in Java in the face of
         * {@code setwd}.
         */
        public ArrayList<String> glob(String pattern) {
            GlobResult data = new GlobResult();
            call(NativeFunction.glob, data, pattern);
            return data.getPaths();
        }

        public static GlobNode create() {
            return RFFIFactory.getBaseRFFI().createGlobNode();
        }
    }

    public static final class ESoftVersionNode extends NativeCallNode {

        private ESoftVersionNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public Map<String, String> eSoftVersion() {
            ESoftVersionResult result = new ESoftVersionResult();
            call(NativeFunction.eSoftVersion, result);
            return result.getVersions();
        }

        public static ESoftVersionNode create() {
            return RFFIFactory.getBaseRFFI().createESoftVersionNode();
        }
    }

    public static final class UmaskNode extends NativeCallNode {

        private UmaskNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public int umask(int mode) {
            return (int) call(NativeFunction.umask, mode);
        }

        public static UmaskNode create() {
            return RFFIFactory.getBaseRFFI().createUmaskNode();
        }
    }

    public static final class CPolyrootNode extends NativeCallNode {

        private CPolyrootNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public int cpolyroot(double[] zr, double[] zi, int degree, double[] rr, double[] ri) {
            return (int) call(NativeFunction.cpolyroot, zr, zi, degree, rr, ri);
        }

        public static CPolyrootNode create() {
            return RFFIFactory.getBaseRFFI().createCPolyrootNode();
        }
    }

    public InitEventLoopNode createInitEventLoopNode() {
        return new InitEventLoopNode(eventLoopDownCallNodeFactory);
    }

    public DispatchHandlersNode createDispatchHandlersNode() {
        return new DispatchHandlersNode(eventLoopDownCallNodeFactory);
    }

    public GetpidNode createGetpidNode() {
        return new GetpidNode(downCallNodeFactory);
    }

    public GetwdNode createGetwdNode() {
        return new GetwdNode(downCallNodeFactory);
    }

    public SetwdNode createSetwdNode() {
        return new SetwdNode(downCallNodeFactory);
    }

    public ReadlinkNode createReadlinkNode() {
        return new ReadlinkNode(downCallNodeFactory);
    }

    public MkdtempNode createMkdtempNode() {
        return new MkdtempNode(downCallNodeFactory);
    }

    public StrtolNode createStrtolNode() {
        return new StrtolNode(downCallNodeFactory);
    }

    public UnameNode createUnameNode() {
        return new UnameNode(downCallNodeFactory);
    }

    public GlobNode createGlobNode() {
        return new GlobNode(downCallNodeFactory);
    }

    public ESoftVersionNode createESoftVersionNode() {
        return new ESoftVersionNode(downCallNodeFactory);
    }

    public UmaskNode createUmaskNode() {
        return new UmaskNode(downCallNodeFactory);
    }

    public CPolyrootNode createCPolyrootNode() {
        return new CPolyrootNode(downCallNodeFactory);
    }

    /*
     * Some functions are called from non-Truffle contexts, which requires a RootNode
     */
    public static final class GetpidRootNode extends RFFIRootNode<GetpidNode> {

        private GetpidRootNode() {
            super(RFFIFactory.getBaseRFFI().createGetpidNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return rffiNode.execute();
        }

        public static GetpidRootNode create() {
            return new GetpidRootNode();
        }
    }

    public static final class GetwdRootNode extends RFFIRootNode<GetwdNode> {

        private GetwdRootNode() {
            super(RFFIFactory.getBaseRFFI().createGetwdNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return rffiNode.execute();
        }

        public static GetwdRootNode create() {
            return new GetwdRootNode();
        }
    }

    public static final class MkdtempRootNode extends RFFIRootNode<MkdtempNode> {

        private MkdtempRootNode() {
            super(RFFIFactory.getBaseRFFI().createMkdtempNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((String) args[0]);
        }

        public static MkdtempRootNode create() {
            return new MkdtempRootNode();
        }
    }

    public static final class UnameRootNode extends RFFIRootNode<UnameNode> {

        private UnameRootNode() {
            super(RFFIFactory.getBaseRFFI().createUnameNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return rffiNode.execute();
        }

        public static UnameRootNode create() {
            return new UnameRootNode();
        }
    }

}
