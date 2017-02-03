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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;

public interface DLLRFFI {
    abstract class DLOpenNode extends Node {
        /**
         * Open a DLL.
         *
         * @return {@code null} on error, opaque handle for following calls otherwise.
         */
        public abstract Object execute(String path, boolean local, boolean now) throws UnsatisfiedLinkError;

        public static DLOpenNode create() {
            return RFFIFactory.getRFFI().getDLLRFFI().createDLOpenNode();
        }
    }

    abstract class DLSymNode extends Node {
        /**
         * Search for {@code symbol} in DLL specified by {@code handle}. To accommodate differing
         * implementations of this interface the result is {@link SymbolHandle}. For the standard OS
         * implementation this will encapsulate a {@link Long} or {@code null} if an error occurred.
         *
         */
        public abstract SymbolHandle execute(Object handle, String symbol) throws UnsatisfiedLinkError;

        public static DLSymNode create() {
            return RFFIFactory.getRFFI().getDLLRFFI().createDLSymNode();
        }
    }

    abstract class DLCloseNode extends Node {
        /**
         * Close DLL specified by {@code handle}.
         */
        public abstract int execute(Object handle);

        public static DLCloseNode create() {
            return RFFIFactory.getRFFI().getDLLRFFI().createDLCloseNode();
        }
    }

    DLOpenNode createDLOpenNode();

    DLSymNode createDLSymNode();

    DLCloseNode createDLCloseNode();

    // RootNodes

    final class DLOpenRootNode extends RFFIRootNode<DLOpenNode> {
        private static DLOpenRootNode dlOpenRootNode;

        private DLOpenRootNode() {
            super(RFFIFactory.getRFFI().getDLLRFFI().createDLOpenNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute((String) args[0], (boolean) args[1], (boolean) args[2]);
        }

        public static DLOpenRootNode create() {
            if (dlOpenRootNode == null) {
                dlOpenRootNode = new DLOpenRootNode();
            }
            return dlOpenRootNode;
        }
    }

    final class DLSymRootNode extends RFFIRootNode<DLSymNode> {
        private static DLSymRootNode dlSymRootNode;

        private DLSymRootNode() {
            super(RFFIFactory.getRFFI().getDLLRFFI().createDLSymNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute(args[0], (String) args[1]);
        }

        public static DLSymRootNode create() {
            if (dlSymRootNode == null) {
                dlSymRootNode = new DLSymRootNode();
            }
            return dlSymRootNode;
        }
    }

    final class DLCloseRootNode extends RFFIRootNode<DLCloseNode> {
        private static DLCloseRootNode dlCloseRootNode;

        private DLCloseRootNode() {
            super(RFFIFactory.getRFFI().getDLLRFFI().createDLCloseNode());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            return rffiNode.execute(args[0]);
        }

        public static DLCloseRootNode create() {
            if (dlCloseRootNode == null) {
                dlCloseRootNode = new DLCloseRootNode();
            }
            return dlCloseRootNode;
        }
    }

}
