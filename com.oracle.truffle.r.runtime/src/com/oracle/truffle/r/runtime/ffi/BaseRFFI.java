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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInterface;

/**
 * A statically typed interface to exactly those native functions required by the R {@code base}
 * package, because the functionality is not provided by the JDK. These methods do not necessarily
 * map 1-1 to a native function, they may involve the invocation of several native functions.
 */
public interface BaseRFFI {

    interface GetpidNode extends NodeInterface {
        public abstract int execute();

        public static GetpidNode create() {
            return RFFIFactory.getBaseRFFI().createGetpidNode();
        }
    }

    interface GetwdNode extends NodeInterface {
        /**
         * Returns the current working directory, in the face of calls to {@code setwd}.
         */
        public abstract String execute();

        public static GetwdNode create() {
            return RFFIFactory.getBaseRFFI().createGetwdNode();
        }
    }

    interface SetwdNode extends NodeInterface {
        /**
         * Sets the current working directory to {@code dir}. (cf. Unix {@code chdir}).
         *
         * @return 0 if successful.
         */
        public abstract int execute(String dir);

        public static SetwdNode create() {
            return RFFIFactory.getBaseRFFI().createSetwdNode();
        }
    }

    interface MkdirNode extends NodeInterface {
        /**
         * Create directory with given mode. Exception is thrown on error.
         */
        public abstract void execute(String dir, int mode) throws IOException;

        public static MkdirNode create() {
            return RFFIFactory.getBaseRFFI().createMkdirNode();
        }
    }

    interface ReadlinkNode extends NodeInterface {
        /**
         * Try to convert a symbolic link to it's target.
         *
         * @param path the link path
         * @return the target if {@code path} is a link else {@code null}
         * @throws IOException for any other error except "not a link"
         */
        public abstract String execute(String path) throws IOException;

        public static ReadlinkNode create() {
            return RFFIFactory.getBaseRFFI().createReadlinkNode();
        }
    }

    interface MkdtempNode extends NodeInterface {
        /**
         * Creates a temporary directory using {@code template} and return the resulting path or
         * {@code null} if error.
         */
        public abstract String execute(String template);

        public static MkdtempNode create() {
            return RFFIFactory.getBaseRFFI().createMkdtempNode();
        }
    }

    interface ChmodNode extends NodeInterface {
        /**
         * Change the file mode of {@code path}.
         */
        public abstract int execute(String path, int mode);

        public static ChmodNode create() {
            return RFFIFactory.getBaseRFFI().createChmodNode();
        }
    }

    interface StrolNode extends NodeInterface {
        /**
         * Convert string to long.
         */
        public abstract long execute(String s, int base) throws IllegalArgumentException;

        public static StrolNode create() {
            return RFFIFactory.getBaseRFFI().createStrolNode();
        }
    }

    public interface UtsName {
        String sysname();

        String release();

        String version();

        String machine();

        String nodename();
    }

    interface UnameNode extends NodeInterface {
        /**
         * Return {@code utsname} info.
         */
        public abstract UtsName execute();

        public static UnameNode create() {
            return RFFIFactory.getBaseRFFI().createUnameNode();
        }
    }

    interface GlobNode extends NodeInterface {
        /**
         * Returns an array of pathnames that match {@code pattern} using the OS glob function. This
         * is done in native code because it is very hard to write in Java in the face of
         * {@code setwd}.
         */
        public abstract ArrayList<String> glob(String pattern);

        public static GlobNode create() {
            return RFFIFactory.getBaseRFFI().createGlobNode();
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

    final class GetpidRootNode extends RFFIRootNode<GetpidNode> {
        private static GetpidRootNode getpidRootNode;

        private GetpidRootNode() {
            super(RFFIFactory.getBaseRFFI().createGetpidNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return rffiNode.execute();
        }

        public static GetpidRootNode create() {
            if (getpidRootNode == null) {
                getpidRootNode = new GetpidRootNode();
            }
            return getpidRootNode;
        }
    }

    final class GetwdRootNode extends RFFIRootNode<GetwdNode> {
        private static GetwdRootNode getwdRootNode;

        private GetwdRootNode() {
            super(RFFIFactory.getBaseRFFI().createGetwdNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return rffiNode.execute();
        }

        public static GetwdRootNode create() {
            if (getwdRootNode == null) {
                getwdRootNode = new GetwdRootNode();
            }
            return getwdRootNode;
        }
    }

    final class MkdtempRootNode extends RFFIRootNode<MkdtempNode> {
        private static MkdtempRootNode mkdtempRootNode;

        private MkdtempRootNode() {
            super(RFFIFactory.getBaseRFFI().createMkdtempNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((String) args[0]);
        }

        public static MkdtempRootNode create() {
            if (mkdtempRootNode == null) {
                mkdtempRootNode = new MkdtempRootNode();
            }
            return mkdtempRootNode;
        }
    }

    final class UnameRootNode extends RFFIRootNode<UnameNode> {
        private static UnameRootNode unameRootNode;

        private UnameRootNode() {
            super(RFFIFactory.getBaseRFFI().createUnameNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return rffiNode.execute();
        }

        public static UnameRootNode create() {
            if (unameRootNode == null) {
                unameRootNode = new UnameRootNode();
            }
            return unameRootNode;
        }
    }
}
