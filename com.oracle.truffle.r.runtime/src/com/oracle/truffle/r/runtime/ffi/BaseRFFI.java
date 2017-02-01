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
package com.oracle.truffle.r.runtime.ffi;

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * A statically typed interface to exactly those native functions required by the R {@code base}
 * package, because the functionality is not provided by the JDK. These methods do not necessarily
 * map 1-1 to a native function, they may involve the invocation of several native functions.
 */
public interface BaseRFFI {
    abstract class GetpidNode extends Node {
        public abstract int getpid();

        public static GetpidNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createGetpidNode();
        }
    }

    abstract class GetwdNode extends Node {
        /**
         * Returns the current working directory, in the face of calls to {@code setwd}.
         */
        public abstract String getwd();

        public static GetwdNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createGetwdNode();
        }
    }

    abstract class SetwdNode extends Node {
        /**
         * Sets the current working directory to {@code dir}. (cf. Unix {@code chdir}).
         *
         * @return 0 if successful.
         */
        public abstract int setwd(String dir);

        public static SetwdNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createSetwdNode();
        }
    }

    abstract class MkdirNode extends Node {
        /**
         * Create directory with given mode. Exception is thrown omn error.
         */
        public abstract void mkdir(String dir, int mode) throws IOException;

        public static MkdirNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createMkdirNode();
        }
    }

    abstract class ReadlinkNode extends Node {
        /**
         * Try to convert a symbolic link to it's target.
         *
         * @param path the link path
         * @return the target if {@code path} is a link else {@code null}
         * @throws IOException for any other error except "not a link"
         */
        public abstract String readlink(String path) throws IOException;

        public static ReadlinkNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createReadlinkNode();
        }
    }

    abstract class MkdtempNode extends Node {
        /**
         * Creates a temporary directory using {@code template} and return the resulting path or
         * {@code null} if error.
         */
        public abstract String mkdtemp(String template);

        public static MkdtempNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createMkdtempNode();
        }
    }

    abstract class ChmodNode extends Node {
        /**
         * Change the file mode of {@code path}.
         */
        public abstract int chmod(String path, int mode);

        public static ChmodNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createChmodNode();
        }
    }

    abstract class StrolNode extends Node {
        /**
         * Convert string to long.
         */
        public abstract long strtol(String s, int base) throws IllegalArgumentException;

        public static StrolNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createStrolNode();
        }
    }

    public interface UtsName {
        String sysname();

        String release();

        String version();

        String machine();

        String nodename();
    }

    abstract class UnameNode extends Node {
        /**
         * Return {@code utsname} info.
         */
        public abstract UtsName uname();

        public static UnameNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createUnameNode();
        }
    }

    abstract class GlobNode extends Node {
        /**
         * Returns an array of pathnames that match {@code pattern} using the OS glob function. This
         * is done in native code because it is very hard to write in Java in the face of
         * {@code setwd}.
         */
        public abstract ArrayList<String> glob(String pattern);

        public static GlobNode create() {
            return RFFIFactory.getRFFI().getBaseRFFI().createGlobNode();
        }
    }

    /*
     * The RFFI implementation influences exactly what subclass of the above nodes is created. Each
     * implementation must therefore, implement these methods that are called by the associated
     * "public static create()" methods above.
     */

    GetpidNode createGetpidNode();

    GetwdNode createGetwdNode();

    SetwdNode createSetwdNode();

    MkdirNode createMkdirNode();

    ReadlinkNode createReadlinkNode();

    MkdtempNode createMkdtempNode();

    ChmodNode createChmodNode();

    StrolNode createStrolNode();

    UnameNode createUnameNode();

    GlobNode createGlobNode();

    /*
     * Some functions are called from non-Truffle contexts, which requires a RootNode
     */

    abstract class BaseRFFIRootNode<T extends Node> extends RootNode {
        @Child T baseRFFINode;

        private BaseRFFIRootNode(T baseRFFINode) {
            super(RContext.getRRuntimeASTAccess().getTruffleRLanguage(), null, new FrameDescriptor());
            this.baseRFFINode = baseRFFINode;
            Truffle.getRuntime().createCallTarget(this);
        }
    }

    final class GetpidRootNode extends BaseRFFIRootNode<GetpidNode> {
        private static GetpidRootNode getpidRootNode;

        private GetpidRootNode() {
            super(RFFIFactory.getRFFI().getBaseRFFI().createGetpidNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return baseRFFINode.getpid();
        }

        public static GetpidRootNode create() {
            if (getpidRootNode == null) {
                getpidRootNode = new GetpidRootNode();
            }
            return getpidRootNode;
        }
    }

    final class GetwdRootNode extends BaseRFFIRootNode<GetwdNode> {
        private static GetwdRootNode getwdRootNode;

        private GetwdRootNode() {
            super(RFFIFactory.getRFFI().getBaseRFFI().createGetwdNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return baseRFFINode.getwd();
        }

        public static GetwdRootNode create() {
            if (getwdRootNode == null) {
                getwdRootNode = new GetwdRootNode();
            }
            return getwdRootNode;
        }
    }

    final class MkdtempRootNode extends BaseRFFIRootNode<MkdtempNode> {
        private static MkdtempRootNode mkdtempRootNode;

        private MkdtempRootNode() {
            super(RFFIFactory.getRFFI().getBaseRFFI().createMkdtempNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return baseRFFINode.mkdtemp((String) args[0]);
        }

        public static MkdtempRootNode create() {
            if (mkdtempRootNode == null) {
                mkdtempRootNode = new MkdtempRootNode();
            }
            return mkdtempRootNode;
        }
    }

    final class UnameRootNode extends BaseRFFIRootNode<UnameNode> {
        private static UnameRootNode unameRootNode;

        private UnameRootNode() {
            super(RFFIFactory.getRFFI().getBaseRFFI().createUnameNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return baseRFFINode.uname();
        }

        public static UnameRootNode create() {
            if (unameRootNode == null) {
                unameRootNode = new UnameRootNode();
            }
            return unameRootNode;
        }
    }
}
